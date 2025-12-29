export default class MapManager {
    constructor(mapId) {
        this.map = L.map(mapId).setView([10.5, 105.1], 7);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(this.map);

        this.provinceLayer = L.geoJSON(null).addTo(this.map);
        this.districtLayer = L.geoJSON(null).addTo(this.map);
        this.wardLayer = L.geoJSON(null).addTo(this.map);
        this.labelGroup = L.layerGroup().addTo(this.map);
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
                    l.setStyle && l.setStyle({ fillColor: '#2ecc71', fillOpacity: 0.7, color: '#27ae60', weight: 4 });
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
                    l.setStyle && l.setStyle({ fillColor: '#2ecc71', fillOpacity: 0.7, color: '#27ae60', weight: 4 });
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
}