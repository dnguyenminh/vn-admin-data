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
        // Comboboxes (customer/address/fc) are handled by their own bind methods in _init();
        // keep province/district/ward onchange handlers above.
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
        // setup customers combobox (single control)
        this.customersPage = 0;
        this.customersQ = '';
        this.customersSize = 50;
        // Do not preload customers until the controls sidebar is opened (lazy-load)
        this.customersLoaded = false;
        this.ui.bindSidebarToggle(async () => {
            if (this.customersLoaded) return;
            try {
                const first = await this.api.getCustomersAfter('', '', this.customersSize);
                this.customersAfter = first.after || null;
                this.ui.showCustomerResults(first, false);
                this.customersLoaded = true;
            } catch (e) {
                console.warn('Failed to preload customers on sidebar open', e);
            }
        }, (expanded) => {
            // When the sidebar opens or closes, ensure the map invalidates its size after layout transition
            setTimeout(() => { try { this.map.map.invalidateSize(); } catch (e) { /* ignore */ } }, 220);
        });

        this.ui.bindCustomerCombo(
            // onQuery
            async (q, page) => {
                const p = (page === undefined || page === null) ? 0 : page;
                // For fresh queries request first page via keyset (no COUNT/OFFSET)
                const resp = await this.api.getCustomersAfter('', q, this.customersSize);
                this.customersQ = q;
                this.customersPage = resp.page || p;
                this.customersAfter = resp.after || null;
                this.ui.showCustomerResults(resp, p > 0);
            },
            // onSelect
            (item) => {
                this.selectedCustomerId = item.id;
                this.handleCustomerChange(item.id);
            },
            // onLoadMore
            async () => {
                // If we have a keyset cursor, use it; otherwise fall back to page
                if (this.customersAfter) {
                    const resp = await this.api.getCustomersAfter(this.customersAfter, this.customersQ || '', this.customersSize);
                    this.customersAfter = resp.after || null;
                    this.ui.showCustomerResults(resp, true);
                } else {
                    const next = (this.customersPage || 0) + 1;
                    const resp = await this.api.getCustomersPage(this.customersQ || '', next, this.customersSize);
                    this.customersPage = resp.page || next;
                    this.customersAfter = resp.after || null;
                    this.ui.showCustomerResults(resp, true);
                }
            }
        );
        // bind address combobox
        this.addressesPage = 0;
        this.addressesQ = '';
        this.addressesSize = 50;
        this.ui.bindAddressCombo(
            async (q, page) => {
                const applId = this.selectedCustomerId || this.ui.getSelectedCustomerId();
                if (!applId) return;
                const p = (page === undefined || page === null) ? 0 : page;
                const resp = await this.api.getAddressesPage(applId, q, p, this.addressesSize);
                this.addressesQ = q;
                this.addressesPage = resp.page || p;
                this.ui.showAddressResults(resp, p > 0);
            },
            // onSelect
            (item) => { this.selectedAddressId = item.id; this.handleAddressChange(item.id); },
            // onLoadMore
            async () => {
                const applId = this.selectedCustomerId || this.ui.getSelectedCustomerId();
                if (!applId) return;
                const next = (this.addressesPage || 0) + 1;
                const resp = await this.api.getAddressesPage(applId, this.addressesQ || '', next, this.addressesSize);
                this.addressesPage = resp.page || next;
                this.ui.showAddressResults(resp, true);
            }
        );

        // bind fc combobox (populated when customer selected)
        this.fcPage = 0;
        this.fcQ = '';
        this.fcSize = 50;
        this.ui.bindFcCombo(
            async (q, page) => {
                const applId = this.selectedCustomerId || this.ui.getSelectedCustomerId();
                if (!applId) return;
                const p = (page === undefined || page === null) ? 0 : page;
                const resp = await this.api.getCheckinFcIdsPage(applId, q, p, this.fcSize);
                this.fcQ = q;
                this.fcPage = resp.page || p;
                this.ui.showFcResults(resp, p > 0);
            },
            (item) => { this.selectedFcId = item.id; this.handleFcChange(item.id); },
            async () => {
                const applId = this.selectedCustomerId || this.ui.getSelectedCustomerId();
                if (!applId) return;
                const next = (this.fcPage || 0) + 1;
                const resp = await this.api.getCheckinFcIdsPage(applId, this.fcQ || '', next, this.fcSize);
                this.fcPage = resp.page || next;
                this.ui.showFcResults(resp, true);
            }
        );
    }

    async loadCustomersPage(q, page) {
        // Use keyset 'after' cursor if present to avoid OFFSET costs
        if (this.customersAfter) {
            const resp = await this.api.getCustomersAfter(this.customersAfter, q, this.customersSize);
            this.customersAfter = resp.after || null;
            this.ui.populateCustomers(resp);
            return resp;
        }
        const resp = await this.api.getCustomersPage(q, page, this.customersSize);
        this.customersPage = resp.page || page;
        this.customersAfter = resp.after || null;
        this.customersTotal = resp.total || 0;
        this.ui.populateCustomers(resp);
        return resp;
    }

    async loadAddressesPage(applId, q, page) {
        const resp = await this.api.getAddresses(applId, q, page, this.addressesSize || 50);
        this.addressesPage = resp.page || page;
        this.addressesTotal = resp.total || 0;
        this.ui.populateAddresses(resp);
    }

    async handleCustomerChange(applId) {
        if (!applId) {
            // clear related UI and layers
            this.ui.populateAddresses([]);
            this.ui.populateFcIds([]);
            this.map.addressLayer.clearLayers();
            this.map.clearCheckins();
            return;
        }
        // load first page of addresses and checkins for this customer
        this.addressesPage = 0;
        this.addressesQ = '';
        const addressesResp = await this.api.getAddresses(applId, this.addressesQ, 0, this.addressesSize || 50);
        this.ui.populateAddresses(addressesResp);

        const addrGeo = await this.api.getAddressesGeoJson(applId, 0, this.addressesSize || 50);
        this.map.showAddressesGeojson(addrGeo);

        const checkinsGeo = await this.api.getCheckinsGeoJson(applId, '', 0, 1000); // page checkins with reasonable default
        this.map.showCheckinsGeojson(checkinsGeo);

        const fcids = await this.api.getCheckinFcIds(applId);
        this.ui.populateFcIds(fcids);
    }

    async handleAddressChange(addrId) {
        if (!addrId) {
            // show all checkins
            const fcId = this.selectedFcId || this.ui.getSelectedFcId();
            this.map.filterCheckinsByFcId(fcId || '');
            return;
        }
        // Highlight and center address
        this.map.highlightAddress(addrId, { fit: true });
        // Filter checkins to this address
        this.map.filterCheckinsByAddressId(addrId);

        // Reverse-geocode the selected address and populate administrative selects
        try {
            const rev = await this.api.getReverseForAddress(addrId);
            if (rev && (rev.provinceId || rev.districtId || rev.wardId)) {
                // If province determined, set province and load its districts
                if (rev.provinceId) {
                    // ensure provinces are populated (they are at init)
                    this.ui.pSel.value = rev.provinceId;
                    await this.handleProvinceChange(rev.provinceId);
                }
                // If district determined, populate and set
                if (rev.districtId) {
                    // populate districts for province (handleProvinceChange should have already done this)
                    this.ui.dSel.value = rev.districtId;
                    await this.handleDistrictChange(rev.districtId);
                }
                // If ward determined, populate and set
                if (rev.wardId) {
                    // ensure wards for district are loaded
                    try {
                        const wards = await this.api.getWards(rev.districtId);
                        this.ui.populateWards(wards);
                    } catch (e) { /* ignore */ }
                    this.ui.wSel.value = rev.wardId;
                    // highlight ward on map if possible
                    try { this.map.highlightWard(rev.wardId); } catch (e) { /* ignore */ }
                }
            }
        } catch (e) {
            console.warn('Reverse lookup failed for address', addrId, e);
        }
    }

    async handleFcChange(fcId) {
        // filter checkins by fc
        this.map.filterCheckinsByFcId(fcId);
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
