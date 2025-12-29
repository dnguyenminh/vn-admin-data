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
    }

    showDistrictsGeojson(geojson) {
        this.districtLayer.clearLayers();
        this.labelGroup.clearLayers();
        this.districtLayer.addData(geojson);
        // Slight translucent fill so streets remain readable; strokes keep boundaries clear
        this.districtLayer.setStyle(this.styles.district);
        this._attachDistrictInteractions();
    }

    showAddressesGeojson(geojson) {
        this.addressLayer.clearLayers();
        this.addressLayer.addData(geojson);
        // center map to addresses if there is at least one feature
        if (this.addressLayer.getLayers().length > 0) {
            const bounds = this.addressLayer.getBounds();
            if (bounds.isValid()) this.map.fitBounds(bounds, { padding: [30, 30] });
        }
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
                    l.setStyle && l.setStyle({ radius: 6, color: '#2c3e50', fillColor: '#34495e', fillOpacity: 0.6 });
                }
            } catch (e) {
                // ignore
            }
        });
        return !!found;
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
    }

    showCheckinsGeojson(geojson) {
        // geojson features expected to have properties: id, fc_id, customer_address_id, checkin_date
        this.clearCheckins();
        const onEach = (feature, layer) => {
            const p = feature.properties || {};
            layer.bindPopup(`<div><strong>fc_id:</strong> ${p.fc_id || ''}<br/><strong>addr_id:</strong> ${p.customer_address_id || ''}<br/><strong>date:</strong> ${p.checkin_date || ''}</div>`);
        };
        const pointToLayer = (feature, latlng) => {
            const fc = (feature.properties && feature.properties.fc_id) || '';
            const color = this._getColorForFc(fc);
            const marker = L.circleMarker(latlng, { radius: 6, color, fillColor: color, fillOpacity: 0.9 });
            marker.featureProps = feature.properties || {};
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