class MapManager {
    constructor(mapId) {
        // Remove any existing map on the container to prevent re-initialization error
        const container = L.DomUtil.get(mapId);
        if (container) {
            if (container._leaflet) {
                container._leaflet.remove();
            }
            // Clear any leftover Leaflet IDs to ensure clean initialization
            if (container._leaflet_id) {
                delete container._leaflet_id;
            }
        }
        this.map = L.map(mapId).setView([10.5, 105.1], 7);
        // Track tile loading so acceptance tests can reliably detect when the app's map is ready.
        const tileLayer = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(this.map);
        try {
            // When tiles finish loading at least once, set a global flag for tests to poll.
            tileLayer.on('load', () => { try { window.__app_map_ready = true; } catch (e) { /* ignore */ } });
        } catch (e) { /* ignore if event binding not available */ }
        // Also set a conservative immediate flag so tests don't hang if tile load events are missed.
        try { window.__app_map_ready = !!this.map; } catch (e) { /* ignore */ }

        this.labelGroup = L.layerGroup().addTo(this.map);
        // Single merged layer for all points (addresses, checkins, predicted)
        this.allLayer = L.layerGroup().addTo(this.map);
        // Instrument allLayer.addLayer to surface errors during add and to log additions
        try {
            const origAdd = this.allLayer.addLayer.bind(this.allLayer);
            this.allLayer.addLayer = (layer) => {
                try {
                    const res = origAdd(layer);
                    try { console.log('[MapManager] allLayer.addLayer ok, layer=', (layer && (layer._leaflet_id || (layer.options && layer.options.title) || (layer.options && layer.options.className)))); } catch(e){}
                    return res;
                } catch (e) {
                    console.error('[MapManager] allLayer.addLayer threw', e, layer);
                    throw e;
                }
            };
        } catch (e) { /* ignore if instrumentation fails */ }
        this.provinceLayer = L.geoJSON(null).addTo(this.map);
        this.districtLayer = L.geoJSON(null).addTo(this.map);
        this.wardLayer = L.geoJSON(null).addTo(this.map);
        this._addressLatLngById = {}; // map address id -> L.LatLng
        this._addressMarkersById = {}; // map address id -> marker (to re-order)
        this._predictedLatLngByAddressId = {}; // map address id -> L.LatLng for predicted point
        this._addressExactById = {}; // map address id -> boolean is_exact
        this._allCheckinMarkers = []; // array of { marker, featureProps }
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
        // Ensure layers are in place (address/checkin/predicted already added above)
        // Readiness flag for app/tests to know when layers finished updating
        this._layersReady = true;
        try { window.__map_layers_ready = true; } catch (e) { }
    }

    _escapeHtml(str) {
        if (!str && str !== 0) return '';
        return String(str)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    _createLegend() {
        const legend = L.control({ position: 'topright' });
        legend.onAdd = () => {
            const div = L.DomUtil.create('div', 'map-legend');
            div.innerHTML = `
                <div style="font-weight:600; margin-bottom:6px;">Map legend</div>
                <div class="legend-item"><span class="legend-swatch" style="background:#1abc9c;"></span><div>Exact address</div></div>
                <div class="legend-item"><span class="legend-swatch" style="background:#e74c3c;"></span><div>Non-exact / ambiguous address</div></div>
                <div class="legend-item"><span class="legend-swatch predicted-swatch" title="Last checking of the field Collector">📍</span><div>Last checking of the field Collector</div></div>
                <div class="legend-item"><span class="legend-swatch predicted-swatch" title="Predicted address">🔮</span><div>Predicted address</div></div>
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
        if (this.allLayer && this.allLayer.clearLayers) this.allLayer.clearLayers();
        // reset runtime caches
        this._addressMarkersById = {};
        this._allCheckinMarkers.length = 0;
        this._addressLatLngById = {};
        this._predictedLatLngByAddressId = {};
        this._addressExactById = {};
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
        console.log('[MapManager] showAddressesGeojson called, features=', (geojson && geojson.features) ? geojson.features.length : 0);
        // Mark layers not-ready while updating
        this._layersReady = false; try { window.__map_layers_ready = false; } catch (e) { }
        // Remove existing address markers from allLayer
        try {
            for (const id in this._addressMarkersById) {
                try { this.allLayer.removeLayer(this._addressMarkersById[id]); } catch (e) { }
            }
        } catch (e) { /* ignore */ }
        this._addressMarkersById = {};
        this._addressLatLngById = {};
        this._predictedLatLngByAddressId = {};
        this._addressExactById = {};

        geojson.features.forEach(feature => {
            console.log('[MapManager] adding address feature id=', feature && feature.properties && feature.properties.id);
            const coords = feature.geometry.coordinates;
            const latlng = [coords[1], coords[0]];
            const p = feature.properties || {};
            const isExact = (p.is_exact === true) || (String(p.is_exact) === 'true');
            const marker = L.circleMarker(latlng, {
                radius: 6,
                color: isExact ? '#2c3e50' : '#e74c3c',
                fillColor: isExact ? '#34495e' : '#e74c3c',
                fillOpacity: isExact ? 0.6 : 0.8
            });
            const html = `<div><strong>appl_id:</strong> ${this._escapeHtml(p.appl_id)}<br/><strong>address:</strong> ${this._escapeHtml(p.address || p.name)}<br/><strong>address_type:</strong> ${this._escapeHtml(p.address_type)}<br/><strong>location(lat, long):</strong> (${latlng[0].toFixed(6)}, ${latlng[1].toFixed(6)})</div>`;
            marker.bindPopup(html);
            try { marker.featureProps = p; marker.feature = feature; } catch (e) { }
            try { marker.addTo(this.map); } catch (e) { try { this.allLayer.addLayer(marker); } catch (e2) { console.error('[MapManager] add address marker failed', e, e2); } }
            if (p.id) {
                this._addressMarkersById[String(p.id)] = marker;
                this._addressLatLngById[String(p.id)] = L.latLng(latlng[0], latlng[1]);
                this._addressExactById[String(p.id)] = isExact;
            }
            // Add predicted if present
            if (p.predicted_feature) {
                const pc = p.predicted_feature.geometry.coordinates;
                const platlng = [pc[1], pc[0]];
                const predMarker = L.marker(platlng, { icon: L.divIcon({ className: 'predicted-marker', html: '🔮', iconSize: [24,24] }) });
                predMarker.bindPopup(`<div><strong>Predicted address</strong><br/>appl_id: ${this._escapeHtml(p.appl_id)}</div>`);
                try { predMarker.addTo(this.map); } catch (e) { try { this.allLayer.addLayer(predMarker); } catch (e2) { console.error('[MapManager] add predicted marker failed', e, e2); } }
                const line = L.polyline([latlng, platlng], { color: '#f39c12', weight: 2, dashArray: '4 6' });
                line.bindTooltip(`${Math.round(L.latLng(latlng[0], latlng[1]).distanceTo(L.latLng(platlng[0], platlng[1])))} m`, { permanent: true, className: 'connector-label' });
                try { line.addTo(this.map); } catch (e) { try { this.allLayer.addLayer(line); } catch (e2) { console.error('[MapManager] add predicted line failed', e, e2); } }
                if (p.id) this._predictedLatLngByAddressId[String(p.id)] = L.latLng(platlng[0], platlng[1]);
            }
        });

        // Fit bounds if any
        if (geojson.features.length > 0) {
            const bounds = L.latLngBounds(geojson.features.map(f => [f.geometry.coordinates[1], f.geometry.coordinates[0]]));
            try { this.map.fitBounds(bounds, { padding: [30, 30] }); } catch (e) { }
        }

        // Bring address markers to front so they remain visible above other markers
        try {
            Object.values(this._addressMarkersById).forEach(m => { try { if (m && m.bringToFront) m.bringToFront(); } catch (e) {} });
        } catch (e) { /* ignore */ }
        // mark ready
        this._layersReady = true;
        try { window.__map_layers_ready = true; } catch (e) { }
    }

    highlightAddress(addressId, options = { fit: true }) {
        let found;
        // Iterate known address markers and style accordingly
        try {
            Object.keys(this._addressMarkersById || {}).forEach(key => {
                const l = this._addressMarkersById[key];
                try {
                    const p = l.featureProps || (l.feature && l.feature.properties);
                    const id = p && p.id;
                    if (String(id) === String(addressId)) {
                        let isExactSel = false;
                        try {
                            if (p) {
                                isExactSel = (p.is_exact === true) || (String(p.is_exact) === 'true');
                                if (!isExactSel && this._addressExactById && p.id) {
                                    isExactSel = !!this._addressExactById[String(p.id)];
                                }
                            }
                        } catch (e) { isExactSel = false; }
                        if (l.setStyle) {
                            if (isExactSel) {
                                l.setStyle({ radius: 8, color: '#16a085', fillColor: '#1abc9c', fillOpacity: 0.9 });
                            } else {
                                l.setStyle({ radius: 8, color: '#c0392b', fillColor: '#e74c3c', fillOpacity: 0.95 });
                            }
                        }
                        found = l;
                        if (options.fit && l.getLatLng) {
                            this.map.setView(l.getLatLng(), Math.max(this.map.getZoom(), 15));
                        }
                    } else {
                        let isExact = false;
                        try {
                            if (p) {
                                isExact = (p.is_exact === true) || (String(p.is_exact) === 'true');
                                if (!isExact && this._addressExactById && p.id) {
                                    isExact = !!this._addressExactById[String(p.id)];
                                }
                            }
                        } catch (e) { isExact = false; }
                        if (l.setStyle) l.setStyle({ radius: 6, color: isExact ? '#2c3e50' : '#e74c3c', fillColor: isExact ? '#34495e' : '#e74c3c', fillOpacity: isExact ? 0.6 : 0.8 });
                    }
                } catch (e) { /* ignore per-marker errors */ }
            });
        } catch (e) { /* ignore */ }
        // Bring address markers to front so they remain visible above checkins/predictions
        try { Object.values(this._addressMarkersById || {}).forEach(m => { try { if (m && m.bringToFront) m.bringToFront(); } catch (e) {} }); } catch (e) { }
        return !!found;
    }

    // Returns a Promise that resolves when the last show* update has finished.
    // Useful for tests and for app flow to wait until markers and connectors are rendered.
    waitForMapLayersReady(timeoutMs = 3000) {
        return new Promise((resolve, reject) => {
            const start = Date.now();
            const check = () => {
                try { if (this._layersReady || (typeof window !== 'undefined' && window.__map_layers_ready)) return resolve(true); } catch (e) { }
                if (Date.now() - start > timeoutMs) return reject(new Error('map layers ready timeout'));
                setTimeout(check, 50);
            };
            check();
        });
    }

    // Show a predicted address point (GeoJSON Feature). Replaces previous prediction marker.
    showPredictedAddress(feature) {
        try {
            // Clear previous predicted visuals
            this.clearPredicted();
            if (!feature || !feature.geometry) return;
            const coords = feature.geometry.coordinates;
            if (!coords || coords.length < 2) return;
            const latlng = [coords[1], coords[0]];
            const props = feature.properties || {};
            const isFcPrediction = props && (props.fc_id || props.areaLevel === 'fc_prediction');
            const iconHtml = '🔮';
            const titleText = 'Predicted address';
            const marker = L.marker(latlng, { title: titleText, icon: L.divIcon({ className: 'predicted-marker', html: iconHtml, iconSize: [24, 24] }) });
            let html = `<div><strong>${this._escapeHtml(titleText)}</strong><br/>appl_id: ${this._escapeHtml(props.appl_id || props.applId)}<br/>addressId: ${this._escapeHtml(props.addressId)}<br/>adjusted: ${this._escapeHtml(props.adjusted)}<br/>areaLevel: ${this._escapeHtml(props.areaLevel)}</div>`;
            marker.bindPopup(html);
            try { this.allLayer.addLayer(marker); } catch (e) { this.allLayer.addLayer(marker); }
            // draw connector if possible
            try {
                const aid = props.addressId ? String(props.addressId) : null;
                if (aid && this._addressLatLngById && this._addressLatLngById[aid]) {
                    const stored = this._addressLatLngById[aid];
                    const predLL = L.latLng(latlng[0], latlng[1]);
                    const line = L.polyline([stored, predLL], { color: '#f39c12', weight: 2, dashArray: '4 6' });
                    line.bindTooltip(`${Math.round(stored.distanceTo(predLL))} m`, { permanent: true, className: 'connector-label' });
                    try { this.allLayer.addLayer(line); } catch (e) { this.allLayer.addLayer(line); }
                }
            } catch (e) { /* ignore connector errors */ }
            this.map.setView(latlng, Math.max(this.map.getZoom(), 15));
        } catch (e) { /* ignore */ }
    }

    // Clear any existing predicted marker (including markers rendered on the topPointsLayer)
    clearPredicted() {
        try {
            // Remove predicted markers from the allLayer (markers using 'predicted-marker' class)
            if (this.allLayer && this.allLayer.eachLayer) {
                this.allLayer.eachLayer(l => {
                    try {
                        if (l && l.options && l.options.icon && l.options.icon.options && l.options.icon.options.className === 'predicted-marker') {
                            this.allLayer.removeLayer(l);
                        }
                    } catch (e) { /* ignore per-layer errors */ }
                });
            }
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
                    icon: L.divIcon({ className: 'district-label', html: this._escapeHtml(l.feature.properties.name), iconSize: [120, 20] })
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
                    icon: L.divIcon({ className: 'ward-label', html: this._escapeHtml(l.feature.properties.name), iconSize: [100, 20] })
                }).addTo(this.labelGroup);
            }
        });
        // Wards should not obscure point markers
        try { this.wardLayer.bringToBack(); } catch (e) { /* ignore */ }
    }

    showCheckinsGeojson(geojson) {
        // Mark layers not-ready while updating
        this._layersReady = false; try { window.__map_layers_ready = false; } catch (e) { }
        // geojson features expected to have properties: id, fc_id, customer_address_id, checkin_date
        this.clearCheckins();
        // Determine last checkin per FC (by checkin_date if present, else by id)
        const lastByFc = {};
        try {
            (geojson.features || []).forEach(f => {
                try {
                    const fc = (f.properties && f.properties.fc_id) || '';
                    const cur = lastByFc[fc];
                    if (!cur) { lastByFc[fc] = f; return; }
                    const da = (f.properties && f.properties.checkin_date) || '';
                    const db = (cur.properties && cur.properties.checkin_date) || '';
                    if (da && db) {
                        if (da > db) lastByFc[fc] = f;
                    } else if ((f.properties && f.properties.id) && (cur.properties && cur.properties.id)) {
                        if (Number(f.properties.id) > Number(cur.properties.id)) lastByFc[fc] = f;
                    }
                } catch (e) { /* ignore */ }
            });
        } catch (e) { /* ignore */ }

        geojson.features.forEach(feature => {
            const latlng = [feature.geometry.coordinates[1], feature.geometry.coordinates[0]];
            const fc = (feature.properties && feature.properties.fc_id) || '';
            const isLastForFc = lastByFc[fc] && feature.properties && String(feature.properties.id) === String(lastByFc[fc].properties && lastByFc[fc].properties.id);
            const color = this._getColorForFc(fc);
            let marker;
            if (isLastForFc) {
                // Render last checkin with pin icon to match legend and focus button
                marker = L.marker(latlng, { icon: L.divIcon({ className: 'last-checkin', html: '📍', iconSize: [18, 18] }), zIndexOffset: 1000 });
            } else {
                marker = L.circleMarker(latlng, { radius: 6, color, fillColor: color, fillOpacity: 0.9 });
            }
            marker.featureProps = feature.properties || {};
            // Ensure last-checkin markers are visually above other markers
            try {
                if (isLastForFc && marker && typeof marker.setZIndexOffset === 'function') {
                    try { marker.setZIndexOffset(1000); } catch (e) { /* ignore */ }
                    try { if (marker.bringToFront) marker.bringToFront(); } catch (e) { /* ignore */ }
                }
            } catch (e) { /* ignore */ }
            // Build popup
            const p = marker.featureProps;
            let html = `<div><strong>fc_id:</strong> ${this._escapeHtml(p.fc_id)}<br/><strong>appl_id:</strong> ${this._escapeHtml(p.appl_id)}<br/><strong>customer_address_id:</strong> ${this._escapeHtml(p.customer_address_id)}<br/><strong>location(lat, long):</strong> (${latlng[0].toFixed(6)}, ${latlng[1].toFixed(6)})<br/>`;
            // Include the stored checking address text when available
            if (p.checking_address) {
                html += `<strong>checking_address:</strong> ${this._escapeHtml(p.checking_address)}<br/>`;
            }
            if (p.customer_address_id) {
                // compute distance to the stored customer address when available
                let addrLatLng = null;
                if (this._addressLatLngById && this._addressLatLngById[String(p.customer_address_id)]) {
                    addrLatLng = this._addressLatLngById[String(p.customer_address_id)];
                } else {
                    try {
                        const markers = this._addressMarkersById || {};
                        Object.values(markers).forEach(l => {
                            try {
                                if (l.featureProps && String(l.featureProps.id) === String(p.customer_address_id)) {
                                    addrLatLng = l.getLatLng();
                                }
                            } catch (e) { /* ignore */ }
                        });
                    } catch (e) { /* ignore */ }
                }
                if (addrLatLng) {
                    const dist = Math.round(L.latLng(latlng[0], latlng[1]).distanceTo(addrLatLng));
                    html += `<strong>distance (m):</strong> ${dist}<br/>`;
                    const line = L.polyline([latlng, addrLatLng], { color: '#f39c12', weight: 2, dashArray: '4 6' });
                    line.bindTooltip(`${dist} m`, { permanent: false, className: 'connector-label' });
                    try { line.addTo(this.map); } catch (e) { try { this.allLayer.addLayer(line); } catch (e2) { /* ignore */ } }
                } else {
                    // If we don't yet have the customer address coords locally, asynchronously fetch addresses
                    // for this application and update the marker popup & connector when found.
                    try {
                        // Use the global ApiClient if available to fetch address page data (contains address_lat/address_long)
                        const appl = (feature.properties && feature.properties.appl_id) || (feature.properties && feature.properties.applId) || null;
                        if (typeof ApiClient !== 'undefined' && appl) {
                            ApiClient.getAddressesPage(appl, '', 0, 1000).then(resp => {
                                try {
                                    const items = (resp && resp.items) ? resp.items : (resp || []);
                                    const item = items.find(i => String(i.id) === String(p.customer_address_id));
                                    if (item && (item.address_lat || item.address_long)) {
                                        const foundLatLng = L.latLng(Number(item.address_lat), Number(item.address_long));
                                        const dist2 = Math.round(L.latLng(latlng[0], latlng[1]).distanceTo(foundLatLng));
                                        // Append distance line to popup content
                                        try {
                                            const pop = marker.getPopup && marker.getPopup();
                                            if (pop && typeof pop.setContent === 'function') {
                                                const old = pop.getContent();
                                                pop.setContent((old || '') + `<strong>distance (m):</strong> ${dist2}<br/>`);
                                            }
                                        } catch (e) { /* ignore popup update errors */ }
                                        const line2 = L.polyline([latlng, foundLatLng], { color: '#f39c12', weight: 2, dashArray: '4 6' });
                                        line2.bindTooltip(`${dist2} m`, { permanent: false, className: 'connector-label' });
                                        try { line2.addTo(this.map); } catch (e) { try { this.allLayer.addLayer(line2); } catch (e2) { /* ignore */ } }
                                    }
                                } catch (e) { /* ignore per-checkin async errors */ }
                            }).catch(e => { /* ignore fetch errors */ });
                        }
                    } catch (e) { /* ignore */ }
                }

                // compute distance to predicted address when available
                try {
                    const predLL = this._predictedLatLngByAddressId && this._predictedLatLngByAddressId[String(p.customer_address_id)];
                    if (predLL) {
                        const distPred = Math.round(L.latLng(latlng[0], latlng[1]).distanceTo(predLL));
                        html += `<strong>distance to predicted (m):</strong> ${distPred}<br/>`;
                        const line2 = L.polyline([latlng, predLL], { color: '#9b59b6', weight: 2, dashArray: '2 6' });
                        line2.bindTooltip(`${distPred} m`, { permanent: false, className: 'connector-label' });
                        try { line2.addTo(this.map); } catch (e) { try { this.allLayer.addLayer(line2); } catch (e2) { /* ignore */ } }
                    }
                } catch (e) { /* ignore predicted distance errors */ }
            }
            html += `<strong>date:</strong> ${this._escapeHtml(p.checkin_date)}</div>`;
            marker.bindPopup(html);
                this._allCheckinMarkers.push({ marker, featureProps: marker.featureProps });
            try { 
                // Emit an application-level event when a checkin marker is clicked so the app can update UI state
                marker.on && marker.on('click', (ev) => {
                    try {
                        const aid = marker.featureProps && marker.featureProps.customer_address_id;
                        window.dispatchEvent(new CustomEvent('app:checkinClicked', { detail: { addressId: aid } }));
                    } catch (e) { /* ignore */ }
                });
                marker.addTo(this.map);
            } catch (e) { try { this.allLayer.addLayer(marker); } catch (e2) { console.error('[MapManager] add checkin marker failed', e, e2); } }
        });
        // Bring address markers to front so they remain visible above checkins/predictions
        try { Object.values(this._addressMarkersById || {}).forEach(m => { try { if (m && m.bringToFront) m.bringToFront(); } catch (e) {} }); } catch (e) { }
        // mark ready
        this._layersReady = true;
        try { window.__map_layers_ready = true; } catch (e) { }
    }

    clearCheckins() {
        // remove checkin markers from allLayer
        this._allCheckinMarkers.forEach(obj => {
            try { if (this.map && this.map.removeLayer) this.map.removeLayer(obj.marker); else this.allLayer.removeLayer(obj.marker); } catch (e) { try { this.allLayer.removeLayer(obj.marker); } catch (e2) { /* ignore */ } }
        });
        this._allCheckinMarkers.length = 0;
    }

    // Filter checkins by customer_address_id (keep markers whose property matches)
    filterCheckinsByAddressId(addrId) {
        // Remove all checkin markers, then add back the filtered ones
        this._allCheckinMarkers.forEach(obj => {
            try { if (this.map && this.map.removeLayer) this.map.removeLayer(obj.marker); else this.allLayer.removeLayer(obj.marker); } catch (e) { try { this.allLayer.removeLayer(obj.marker); } catch (e2) { /* ignore */ } }
        });
        const toAdd = this._allCheckinMarkers.filter(obj => String(obj.featureProps && obj.featureProps.customer_address_id) === String(addrId));
        toAdd.forEach(obj => {
            try { obj.marker.addTo(this.map); } catch (e) { try { this.allLayer.addLayer(obj.marker); } catch (e2) { /* ignore */ } }
        });
        // Ensure address markers are above checkins after filtering
        try { Object.values(this._addressMarkersById || {}).forEach(m => { try { if (m && m.bringToFront) m.bringToFront(); } catch (e) {} }); } catch (e) { }
    }

    // Filter checkins by fc_id (when fcId is empty, show all)
    filterCheckinsByFcId(fcId) {
        // Remove all checkin markers, then add back the filtered ones
        this._allCheckinMarkers.forEach(obj => {
            try { if (this.map && this.map.removeLayer) this.map.removeLayer(obj.marker); else this.allLayer.removeLayer(obj.marker); } catch (e) { try { this.allLayer.removeLayer(obj.marker); } catch (e2) { /* ignore */ } }
        });
        const toAdd = fcId ? this._allCheckinMarkers.filter(obj => String(obj.featureProps && obj.featureProps.fc_id) === String(fcId)) : this._allCheckinMarkers.slice();
        toAdd.forEach(obj => {
            try { obj.marker.addTo(this.map); } catch (e) { try { this.allLayer.addLayer(obj.marker); } catch (e2) { /* ignore */ } }
        });
        // Ensure address markers are above checkins after filtering
        try { Object.values(this._addressMarkersById || {}).forEach(m => { try { if (m && m.bringToFront) m.bringToFront(); } catch (e) {} }); } catch (e) { }
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
