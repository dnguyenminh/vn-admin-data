export default class MapManager {
    constructor(mapId) {
        this.map = L.map(mapId).setView([10.5, 105.1], 7);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(this.map);

        this.provinceLayer = L.geoJSON(null).addTo(this.map);
        this.districtLayer = L.geoJSON(null).addTo(this.map);
        this.wardLayer = L.geoJSON(null).addTo(this.map);
        this.labelGroup = L.layerGroup().addTo(this.map);
        this.addressLayer = L.geoJSON(null, { pointToLayer: (f, latlng) => L.circleMarker(latlng, { radius: 6, color: '#2c3e50', fillColor: '#34495e', fillOpacity: 0.6 }) }).addTo(this.map);
        this.checkinGroup = L.layerGroup().addTo(this.map);
        this.predictedLayer = L.layerGroup().addTo(this.map);
        this._predictedConnectorLayer = L.layerGroup().addTo(this.map);
        this._addressLatLngById = {}; // map address id -> L.LatLng
        this._allCheckinMarkers = []; // store markers for filtering
        this._fcColorMap = {}; // map fc_id -> color
        // Base styles for layers (used and adjusted based on zoom)
        this.styles = {
            province: { fillColor: '#ffcccc', fillOpacity: 0.12, color: '#e74c3c', weight: 3 },
            district: { fillColor: '#ffe6e6', fillOpacity: 0.06, color: '#e74c3c', weight: 1 },
            ward: { fillColor: '#e6f2ff', fillOpacity: 0.04, color: '#3498db', weight: 1 }
        };

        // Zoom threshold above which fills are minimized to let streets show
        this.zoomThreshold = 13;

        // Listen for zoom changes and adapt layer opacities
        if (this.map && this.map.on) {
            this.map.on('zoomend', this._onZoomChange.bind(this));
        }

        // Create a small legend explaining marker colors
        this._createLegend();
    }

    _createLegend() {
        const legend = L.control({ position: 'topright' });
        legend.onAdd = () => {
            const div = L.DomUtil.create('div', 'map-legend');
            div.innerHTML = `
                <div style="font-weight:600; margin-bottom:6px;">Map legend</div>
                <div class="legend-item"><span class="legend-swatch" style="background:#1abc9c;"></span><div>Exact address</div></div>
                <div class="legend-item"><span class="legend-swatch" style="background:#e74c3c;"></span><div>Non-exact / ambiguous address</div></div>
                <div class="legend-item"><span class="legend-swatch predicted-swatch">📍</span><div>Predicted location</div></div>
            `;
            // Prevent clicks on the legend from propagating to the map (avoid accidental pans)
            L.DomEvent.disableClickPropagation(div);
            return div;
        };
        legend.addTo(this.map);
        this.legend = legend;
    }

    clearAll(clearProvince = true) {
        if (clearProvince) this.provinceLayer.clearLayers();
        this.districtLayer.clearLayers();
        this.wardLayer.clearLayers();
        this.labelGroup.clearLayers();
    }

    showProvinceGeojson(geojson) {
        this.provinceLayer.clearLayers();
        this.provinceLayer.addData(geojson);
        // Use low fill opacity so basemap streets remain visible beneath the polygon
        this.provinceLayer.setStyle(this.styles.province);
        this.map.fitBounds(this.provinceLayer.getBounds());
        // Ensure polygon layer stays behind point markers so points remain clickable
        try { this.provinceLayer.bringToBack(); } catch (e) { /* ignore if not supported */ }
    }

    showDistrictsGeojson(geojson) {
        this.districtLayer.clearLayers();
        this.labelGroup.clearLayers();
        this.districtLayer.addData(geojson);
        // Slight translucent fill so streets remain readable; strokes keep boundaries clear
        this.districtLayer.setStyle(this.styles.district);
        this._attachDistrictInteractions();
        // Keep districts behind point markers
        try { this.districtLayer.bringToBack(); } catch (e) { /* ignore */ }
    }

    showAddressesGeojson(geojson) {
        this.addressLayer.clearLayers();
        // clear any predicted markers or connectors from previous render
        try { this.predictedLayer.clearLayers(); this._predictedConnectorLayer.clearLayers(); this._addressLatLngById = {}; } catch (e) { /* ignore */ }
        this.addressLayer.addData(geojson);
        // Bind popup for each address feature to show details and location
        this.addressLayer.eachLayer(l => {
            try {
                const p = l.feature && l.feature.properties ? l.feature.properties : {};
                // If the address is marked as non-exact (is_exact === false) draw it with a special color
                // so users can immediately see ambiguous/non-exact addresses on the map.
                if (p.hasOwnProperty('is_exact') && p.is_exact === false) {
                    l.setStyle && l.setStyle({ radius: 6, color: '#e74c3c', fillColor: '#e74c3c', fillOpacity: 0.8 });
                }
                const ll = l.getLatLng ? l.getLatLng() : null;
                const lat = ll ? ll.lat : null;
                const lng = ll ? ll.lng : null;
                const appl = p.appl_id || '';
                const addr = p.address || p.name || '';
                const atype = p.address_type || '';
                const loc = (lat !== null && lng !== null) ? `(${lat.toFixed(6)}, ${lng.toFixed(6)})` : '';
                const html = `<div><strong>appl_id:</strong> ${appl}<br/><strong>address:</strong> ${addr}<br/><strong>address_type:</strong> ${atype}<br/><strong>location(lat, long):</strong> ${loc}</div>`;
                l.bindPopup(html);
                // remember stored latlng by address id for potential connector drawing
                try {
                    const id = p.id || p.id === 0 ? p.id : null;
                    if (id && l.getLatLng) this._addressLatLngById[String(id)] = l.getLatLng();
                } catch (e) { /* ignore */ }
                // If the server embedded a predicted feature for this address, render it
                try {
                    if (p && p.predicted_feature && p.predicted_feature.geometry && p.predicted_feature.geometry.coordinates) {
                        const pc = p.predicted_feature.geometry.coordinates;
                        const platlng = [pc[1], pc[0]];
                        const predMarker = L.marker(platlng, { title: 'Predicted address', icon: L.divIcon({ className: 'predicted-marker', html: '📍', iconSize: [24,24] }) });
                        const conf = p.confidence || (p.predicted_feature.properties && p.predicted_feature.properties.confidence) || '';
                        const reason = p.resolvability_reason || (p.predicted_feature.properties && p.predicted_feature.properties.resolvability_reason) || '';
                        let predHtml = `<div><strong>Predicted</strong><br/>appl_id: ${appl}<br/>confidence: ${conf}<br/>reason: ${reason}</div>`;
                        predMarker.bindPopup(predHtml);
                        this.predictedLayer.addLayer(predMarker);
                        // draw connector line to stored point when available
                        try {
                            const aid = p.id ? String(p.id) : null;
                            const stored = aid ? this._addressLatLngById[aid] : null;
                            if (stored) {
                                    const predLL = L.latLng(platlng[0], platlng[1]);
                                    const line = L.polyline([stored, predLL], { color: '#f39c12', weight: 2, dashArray: '4 6', opacity: 0.95 });
                                    try { // compute distance in meters and attach permanent tooltip label
                                        const dist = Math.round(stored.distanceTo(predLL));
                                        // permanent tooltip shows numeric distance on the connector
                                        line.bindTooltip(`${dist} m`, { direction: 'center', permanent: true, className: 'connector-label' });
                                    } catch (e) { /* ignore distance errors */ }
                                    this._predictedConnectorLayer.addLayer(line);
                            }
                        } catch (e) { /* ignore connector errors */ }
                    }
                } catch (e) { /* ignore predicted render errors */ }
            } catch (e) {
                // ignore
            }
        });
        // center map to addresses if there is at least one feature
        if (this.addressLayer.getLayers().length > 0) {
            const bounds = this.addressLayer.getBounds();
            if (bounds.isValid()) this.map.fitBounds(bounds, { padding: [30, 30] });
        }
        // Ensure address markers render above polygon layers so they are clickable
        try { this.addressLayer.bringToFront(); } catch (e) { /* ignore if not supported */ }
        try { this.predictedLayer.bringToFront(); this._predictedConnectorLayer.bringToFront(); } catch (e) { /* ignore */ }
    }

    highlightAddress(addressId, options = { fit: true }) {
        let found;
        this.addressLayer.eachLayer(l => {
            try {
                const id = l.feature && l.feature.properties && l.feature.properties.id;
                if (String(id) === String(addressId)) {
                    l.setStyle && l.setStyle({ radius: 8, color: '#16a085', fillColor: '#1abc9c', fillOpacity: 0.9 });
                    found = l;
                    if (options.fit && l.getLatLng) {
                        this.map.setView(l.getLatLng(), Math.max(this.map.getZoom(), 15));
                    }
                } else {
                    // Reset style based on whether the address is exact or not. Non-exact addresses
                    // maintain the special color so they remain visually distinct.
                    const p = l.feature && l.feature.properties ? l.feature.properties : {};
                    if (p.hasOwnProperty('is_exact') && p.is_exact === false) {
                        l.setStyle && l.setStyle({ radius: 6, color: '#e74c3c', fillColor: '#e74c3c', fillOpacity: 0.8 });
                    } else {
                        l.setStyle && l.setStyle({ radius: 6, color: '#2c3e50', fillColor: '#34495e', fillOpacity: 0.6 });
                    }
                }
            } catch (e) {
                // ignore
            }
        });
        return !!found;
    }

    // Show a predicted address point (GeoJSON Feature). Replaces previous prediction marker.
    showPredictedAddress(feature) {
        try {
            this.predictedLayer.clearLayers();
            this._predictedConnectorLayer.clearLayers();
            if (!feature || !feature.geometry) return;
            const coords = feature.geometry.coordinates;
            if (!coords || coords.length < 2) return;
            const latlng = [coords[1], coords[0]];
            const marker = L.marker(latlng, { title: 'Predicted address', icon: L.divIcon({ className: 'predicted-marker', html: '📍', iconSize: [24, 24] }) });
            const props = feature.properties || {};
            let html = `<div><strong>Predicted</strong><br/>appl_id: ${props.appl_id || props.applId || ''}<br/>addressId: ${props.addressId || ''}<br/>adjusted: ${props.adjusted || false}<br/>areaLevel: ${props.areaLevel || ''}</div>`;
            marker.bindPopup(html);
            this.predictedLayer.addLayer(marker);
            // draw connector from stored address point to predicted point when possible
            try {
                const aid = props.addressId ? String(props.addressId) : null;
                if (aid && this._addressLatLngById && this._addressLatLngById[aid]) {
                    const stored = this._addressLatLngById[aid];
                    const predLL = L.latLng(latlng[0], latlng[1]);
                    const line = L.polyline([stored, predLL], { color: '#f39c12', weight: 2, dashArray: '4 6', opacity: 0.95 });
                    try { const dist = Math.round(stored.distanceTo(predLL)); line.bindTooltip(`${dist} m`, { direction: 'center', permanent: true, className: 'connector-label' }); } catch (e) { }
                    this._predictedConnectorLayer.addLayer(line);
                }
            } catch (e) { /* ignore connector errors */ }
            this.map.setView(latlng, Math.max(this.map.getZoom(), 15));
        } catch (e) { /* ignore */ }
    }

    // Clear any existing predicted marker
    clearPredicted() {
        try {
            this.predictedLayer.clearLayers();
        } catch (e) { /* ignore */ }
    }

    _attachDistrictInteractions() {
        this.districtLayer.eachLayer(l => {
            l.on('mouseover', (e) => {
                if (this._selectedDistrictId !== e.target.feature.properties.id)
                    e.target.setStyle({ fillColor: "#2ecc71", fillOpacity: 0.4 });
            });
            l.on('mouseout', (e) => {
                if (this._selectedDistrictId !== e.target.feature.properties.id)
                    this.districtLayer.resetStyle(e.target);
            });
            if (l.feature.properties.center) {
                var c = l.feature.properties.center.coordinates;
                L.marker([c[1], c[0]], {
                    icon: L.divIcon({ className: 'district-label', html: l.feature.properties.name, iconSize: [120, 20] })
                }).addTo(this.labelGroup);
            }
        });
    }

    // Highlight a district by id (set style and fit bounds)
    // options: { fit: true/false }
    highlightDistrict(districtId, options = { fit: true }) {
        if (!districtId) return;
        this._selectedDistrictId = districtId;
        this.districtLayer.eachLayer(l => {
            try {
                const id = l.feature && l.feature.properties && l.feature.properties.id;
                if (id === districtId || id === String(districtId) || id === Number(districtId)) {
                    l.setStyle && l.setStyle(this._getHighlightStyle('district'));
                    if (options.fit && l.getBounds) this.map.fitBounds(l.getBounds());
                } else {
                    this.districtLayer.resetStyle && this.districtLayer.resetStyle(l);
                }
            } catch (e) {
                // ignore
            }
        });
    }

    showWardsGeojson(geojson) {
        this.wardLayer.clearLayers();
        this.wardLayer.addData(geojson);
        // Very low fill for wards so detailed streets are still visible; keep strokes thin
        this.wardLayer.setStyle(this.styles.ward);
        this.wardLayer.eachLayer(l => {
            if (l.feature.properties.center) {
                var c = l.feature.properties.center.coordinates;
                L.marker([c[1], c[0]], {
                    icon: L.divIcon({ className: 'ward-label', html: l.feature.properties.name, iconSize: [100, 20] })
                }).addTo(this.labelGroup);
            }
        });
        // Wards should not obscure point markers
        try { this.wardLayer.bringToBack(); } catch (e) { /* ignore */ }
    }

    showCheckinsGeojson(geojson) {
        // geojson features expected to have properties: id, fc_id, customer_address_id, checkin_date
        this.clearCheckins();
        const onEach = (feature, layer) => {
            // Popup content created in pointToLayer where distance and precise coords are available
        };
        const pointToLayer = (feature, latlng) => {
            const fc = (feature.properties && feature.properties.fc_id) || '';
            const color = this._getColorForFc(fc);
            const marker = L.circleMarker(latlng, { radius: 6, color, fillColor: color, fillOpacity: 0.9 });
            marker.featureProps = feature.properties || {};
            // Build a richer popup including location and distance to the linked address if available
            try {
                const p = marker.featureProps || {};
                const lat = latlng.lat;
                const lng = latlng.lng;
                let html = `<div><strong>fc_id:</strong> ${p.fc_id || ''}<br/><strong>location(lat, long):</strong> (${lat.toFixed(6)}, ${lng.toFixed(6)})<br/>`;
                // attempt to compute distance to linked customer address if present and add to popup
                if (p.customer_address_id) {
                    let addrLatLng = null;
                    this.addressLayer.eachLayer(al => {
                        try {
                            const aid = al.feature && al.feature.properties && al.feature.properties.id;
                            if (String(aid) === String(p.customer_address_id)) {
                                if (al.getLatLng) addrLatLng = al.getLatLng();
                            }
                        } catch (e) { }
                    });
                    if (addrLatLng) {
                        const dist = Math.round(latlng.distanceTo(addrLatLng));
                        html += `<strong>distance (m):</strong> ${dist}<br/>`;
                        // Also draw a temporary connector line back to the address (non-permanent tooltip)
                        try {
                            const line = L.polyline([latlng, addrLatLng], { color: '#f39c12', weight: 2, dashArray: '4 6', opacity: 0.7 });
                            try { line.bindTooltip(`${dist} m`, { direction: 'center' }); } catch (e) { }
                            this._predictedConnectorLayer.addLayer(line);
                        } catch (e) { /* ignore connector errors */ }
                    }
                }
                html += `<strong>date:</strong> ${p.checkin_date || ''}</div>`;
                marker.bindPopup(html);

                // Perform a reverse lookup to show administrative info (province/district/ward) for this checkin location
                try {
                    const lon = lng;
                    const lat = lat;
                    fetch(`/api/map/reverse?lon=${encodeURIComponent(lon)}&lat=${encodeURIComponent(lat)}`)
                        .then(r => r.ok ? r.json() : {})
                        .then(obj => {
                            if (!obj) return;
                            const province = obj.provinceName || obj.province_name || '';
                            const district = obj.districtName || obj.district_name || '';
                            const ward = obj.wardName || obj.ward_name || '';
                            if (province || district || ward) {
                                const adminHtml = `<div style="margin-top:6px; font-size:12px;"><strong>Administrative:</strong><br/>${province ? 'Province: ' + province + '<br/>' : ''}${district ? 'District: ' + district + '<br/>' : ''}${ward ? 'Ward: ' + ward + '<br/>' : ''}</div>`;
                                // Append admin info to existing popup content
                                try {
                                    const existing = marker.getPopup() && marker.getPopup().getContent ? marker.getPopup().getContent() : html;
                                    marker.getPopup().setContent(existing + adminHtml);
                                } catch (e) {
                                    // Fallback: re-bind popup
                                    marker.bindPopup(html + adminHtml);
                                }
                            }
                        }).catch(e => {/* ignore reverse lookup errors */});
                } catch (e) { /* ignore reverse lookup setup errors */ }
            } catch (e) {
                // fallback: basic popup
                const p = marker.featureProps || {};
                marker.bindPopup(`<div><strong>fc_id:</strong> ${p.fc_id || ''}<br/><strong>addr_id:</strong> ${p.customer_address_id || ''}<br/><strong>date:</strong> ${p.checkin_date || ''}</div>`);
            }
            this._allCheckinMarkers.push(marker);
            return marker;
        };
        L.geoJSON(geojson, { pointToLayer, onEachFeature: onEach }).addTo(this.checkinGroup);
    }

    clearCheckins() {
        this._allCheckinMarkers.length = 0;
        this.checkinGroup.clearLayers();
    }

    // Filter checkins by customer_address_id (keep markers whose property matches)
    filterCheckinsByAddressId(addrId) {
        this.checkinGroup.clearLayers();
        const toAdd = this._allCheckinMarkers.filter(m => String(m.featureProps && m.featureProps.customer_address_id) === String(addrId));
        toAdd.forEach(m => this.checkinGroup.addLayer(m));
    }

    // Filter checkins by fc_id (when fcId is empty, show all)
    filterCheckinsByFcId(fcId) {
        this.checkinGroup.clearLayers();
        const toAdd = fcId ? this._allCheckinMarkers.filter(m => String(m.featureProps && m.featureProps.fc_id) === String(fcId)) : this._allCheckinMarkers.slice();
        toAdd.forEach(m => this.checkinGroup.addLayer(m));
    }

    _getColorForFc(fc) {
        if (!fc) return '#7f8c8d';
        if (this._fcColorMap[fc]) return this._fcColorMap[fc];
        // simple color generation using HSL from hash
        let hash = 0; for (let i = 0; i < fc.length; i++) { hash = fc.charCodeAt(i) + ((hash << 5) - hash); }
        const h = Math.abs(hash) % 360;
        const color = `hsl(${h} 70% 45%)`;
        this._fcColorMap[fc] = color;
        return color;
    }

    _onZoomChange() {
        const z = (this.map && this.map.getZoom) ? this.map.getZoom() : 0;
        if (z >= this.zoomThreshold) {
            // minimize fills to let basemap streets show
            this.provinceLayer.setStyle({ ...this.styles.province, fillOpacity: 0 });
            this.districtLayer.setStyle({ ...this.styles.district, fillOpacity: 0 });
            this.wardLayer.setStyle({ ...this.styles.ward, fillOpacity: 0 });
        } else {
            // restore base opacities
            this.provinceLayer.setStyle(this.styles.province);
            this.districtLayer.setStyle(this.styles.district);
            this.wardLayer.setStyle(this.styles.ward);
        }

        // Re-apply highlight styles so selected features stay visible
        if (this._selectedDistrictId) this.highlightDistrict(this._selectedDistrictId, { fit: false });
        if (this._selectedWardId) this.highlightWard(this._selectedWardId, { fit: false });
    }

    // Focus the map on a specific lat/lng (lng,lat or lat,lng depending on input).
    focusToLatLng(lat, lng, zoom = 15) {
        try {
            if (!this.map) return false;
            this.map.setView([lat, lng], Math.max(this.map.getZoom() || 0, zoom));
            return true;
        } catch (e) { return false; }
    }

    // Highlight a ward by id (set style and fit bounds)
    // options: { fit: true/false }
    highlightWard(wardId, options = { fit: true }) {
        this._selectedWardId = wardId;
        if (!wardId) return;
        this.wardLayer.eachLayer(l => {
            try {
                const id = l.feature && l.feature.properties && l.feature.properties.id;
                if (id === wardId || id === String(wardId) || id === Number(wardId)) {
                    l.setStyle && l.setStyle(this._getHighlightStyle('ward'));
                    if (options.fit && l.getBounds) this.map.fitBounds(l.getBounds());
                } else {
                    // reset style for non-selected wards
                    this.wardLayer.resetStyle && this.wardLayer.resetStyle(l);
                }
            } catch (e) {
                // ignore layers that don't conform to expected GeoJSON shape
            }
        });
    }

    setSelectedDistrictId(id) { this._selectedDistrictId = id; }

    // Return a style object for highlighted features that keeps fill low at
    // high zoom levels so the basemap street labels remain readable.
    _getHighlightStyle(layerType) {
        const zoom = (this.map && this.map.getZoom) ? this.map.getZoom() : 0;
        // Slightly different weights for district vs ward could be used; keep
        // both visually distinct while preserving labels.
        const weight = (layerType === 'district') ? 4 : 3;
        const fillOpacity = (zoom >= this.zoomThreshold) ? 0.12 : 0.35;
        return { fillColor: '#2ecc71', fillOpacity, color: '#27ae60', weight };
    }
}