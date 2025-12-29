import ApiClient from './apiClient.js';
import MapManager from './mapManager.js';
import UIManager from './uiManager.js';

export default class App {
    constructor() {
        this.api = ApiClient;
        this.ui = new UIManager();
        this.map = new MapManager('map');

        this._wireEvents();
        this._init();
    }

    _wireEvents() {
        this.ui.bindSearchInput(async (q) => {
            console.log('Searching for:', q);
            const results = await this.api.search(q);
            this.ui.showResults(results);
        });

        this.ui.bindResultClicks((item) => this.selectSearchResult(item));

        this.ui.pSel.onchange = () => this.handleProvinceChange(this.ui.pSel.value);
        this.ui.dSel.onchange = () => this.handleDistrictChange(this.ui.dSel.value);
        this.ui.wSel.onchange = () => this.handleWardChange(this.ui.wSel.value);
    }

    async handleWardChange(wid) {
        if (!wid) {
            // if no ward selected, try fitting to current district or province
            if (this.ui.dSel.value) {
                // find district layer bounds
                // simply return for now — the map keeps district highlight
                return;
            }
            return;
        }
        // ensure the ward layer is present; highlight by id
        this.map.highlightWard(wid);
    }

    async _init() {
        console.log('App initialized');
        const provinces = await this.api.getProvinces();
        this.ui.populateProvinces(provinces);
    }

    async handleProvinceChange(pid) {
        if (!pid) { this.map.clearAll(true); return; }
        this.map.clearAll(false);
        const boundsData = await this.api.getProvinceBounds(pid);
        this.map.showProvinceGeojson(boundsData);

        const districts = await this.api.getDistricts(pid);
        this.ui.populateDistricts(districts);

        const geoData = await this.api.getDistrictsGeoJson(pid);
        this.map.showDistrictsGeojson(geoData);
    }

    async handleDistrictChange(did) {
        if (!did) {
            if (this.map.provinceLayer && this.map.provinceLayer.getLayers().length > 0)
                this.map.map.fitBounds(this.map.provinceLayer.getBounds());
            return;
        }

        // Highlight the selected district (style + fit bounds)
        this.map.highlightDistrict(did);

        this.map.setSelectedDistrictId(did);

        const wards = await this.api.getWards(did);
        this.ui.populateWards(wards);

        const geoData = await this.api.getWardsGeoJson(did);
        this.map.showWardsGeojson(geoData);
    }

    async selectSearchResult(item) {
        this.ui.hideResults();
        console.log('Selected search item:', item);
        this.ui.setInputValue(item.name.split(',')[0]);

        if (item.type === 'province') {
            this.ui.pSel.value = item.id;
            this.ui.pSel.dispatchEvent(new Event('change'));
        } else if (item.type === 'district') {
            this.ui.pSel.value = item.parent_id;
            // wait for province change to populate districts
            await this.handleProvinceChange(item.parent_id);
            this.ui.dSel.value = item.id;
            if (this.ui.dSel.value === item.id) {
                this.ui.dSel.dispatchEvent(new Event('change'));
            } else {
                console.warn('[REFR] Could not set district id immediately, retrying...');
            }
        }
    }
}
