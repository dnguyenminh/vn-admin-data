class App {
    constructor() {
        this.api = ApiClient;
        this.ui = new UIManager();
        this.map = new MapManager('map');

        this._wireEvents();
        this._init();
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

    logToPage(msg) {
        const div = document.getElementById('logConsole');
        if (div) {
            div.innerHTML += new Date().toLocaleTimeString() + ': ' + this._escapeHtml(msg) + '<br>';
            div.scrollTop = div.scrollHeight;
        }
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
        if (this.initialized) return; // Prevent double initialization
        this.initialized = true;
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
        // bind show-all checkins toggle
        this.ui.bindShowAllCheckins(async (checked) => {
            const applId = this.selectedCustomerId || this.ui.getSelectedCustomerId();
            if (!applId) return;
            if (checked) {
                const checkins = await this.api.getCheckinsGeoJson(applId, '', 0, 1000);
                this.map.showCheckinsGeojson(checkins);
                try { await this.map.waitForMapLayersReady(2000); } catch (e) { }
                try { if (this.map && this.map.map && typeof this.map.map.invalidateSize === 'function') this.map.map.invalidateSize(); } catch (e) { }
            } else {
                if (this.selectedFcId) {
                    const checkins = await this.api.getCheckinsGeoJson(applId, this.selectedFcId, 0, 1000);
                    this.map.showCheckinsGeojson(checkins);
                    try { await this.map.waitForMapLayersReady(2000); } catch (e) { }
                    try { if (this.map && this.map.map && typeof this.map.map.invalidateSize === 'function') this.map.map.invalidateSize(); } catch (e) { }
                } else {
                    this.map.clearCheckins();
                }
            }
        });

        // bind focus buttons
        this.ui.bindFocusAddress(async () => {
            const applId = this.selectedCustomerId || this.ui.getSelectedCustomerId();
            const addrId = this.selectedAddressId || this.ui.getSelectedAddressId();
            if (!applId || !addrId) return;
            // ensure address markers are present, then highlight
            const addrGeo = await this.api.getAddressesGeoJson(applId, 0, this.addressesSize || 50);
            this.map.showAddressesGeojson(addrGeo);
            try { await this.map.waitForMapLayersReady(2000); } catch (e) { }
            const ok = this.map.highlightAddress(addrId, { fit: true });
            if (!ok) {
                // fallback: fetch addresses list and center on lat/lng if available
                const resp = await this.api.getAddressesPage(applId, '', 0, 1000);
                const item = (resp && resp.items) ? resp.items.find(i => String(i.id) === String(addrId)) : null;
                if (item && item.address_lat && item.address_long) {
                    this.map.focusToLatLng(Number(item.address_lat), Number(item.address_long), 16);
                }
            }
            // show prediction for this address when focusing it
            try {
                // Prefer embedded prediction on the currently-loaded address layer to avoid an extra request
                let embeddedPred = null;
                try {
                    const markers = this.map._addressMarkersById || {};
                    Object.values(markers).forEach(al => {
                        try {
                            const id = al.feature && al.feature.properties && al.feature.properties.id || (al.featureProps && al.featureProps.id);
                            const props = al.feature && al.feature.properties ? al.feature.properties : (al.featureProps || {});
                            if (String(id) === String(addrId) && props && props.predicted_feature) {
                                embeddedPred = props.predicted_feature;
                            }
                        } catch (e) { /* ignore */ }
                    });
                } catch (e) { /* ignore */ }
                if (embeddedPred) {
                    this.map.showPredictedAddress(embeddedPred);
                } else {
                    const pred = await this.api.getPredictedAddress(applId, addrId);
                    if (pred && pred.geometry) this.map.showPredictedAddress(pred);
                }
            } catch (e) { /* ignore prediction errors */ }

            // Additionally: if there are checkins associated with this address, select the
            // FC that last checked in here and show its checkins so subsequent FC-focused
            // actions (e.g., "show all checkins" toggle) behave as users expect.
            try {
                const allCheckins = await this.api.getCheckinsGeoJson(applId, '', 0, 1000);
                const features = (allCheckins && allCheckins.features) ? allCheckins.features : [];
                const addrCheckins = features.filter(f => String((f.properties && f.properties.customer_address_id) || '') === String(addrId));
                if (addrCheckins.length > 0) {
                    // Sort by checkin_date (if present) and pick the latest
                    addrCheckins.sort((a, b) => { const da = (a.properties && a.properties.checkin_date) || ''; const db = (b.properties && b.properties.checkin_date) || ''; return da < db ? -1 : (da > db ? 1 : 0); });
                    const last = addrCheckins[addrCheckins.length - 1];
                    const lastFc = last && last.properties ? last.properties.fc_id : null;
                    if (lastFc) {
                        this.selectedFcId = String(lastFc);
                        try { this.ui.setFcValue(String(lastFc), String(lastFc)); } catch (e) { /* ignore */ }
                        // Show checkins for this FC and focus to the last checkin location
                        const fcCheckins = await this.api.getCheckinsGeoJson(applId, this.selectedFcId, 0, 1000);
                        this.map.showCheckinsGeojson(fcCheckins);
                        try { await this.map.waitForMapLayersReady(2000); } catch (e) { }
                        try { if (this.map && this.map.map && typeof this.map.map.invalidateSize === 'function') this.map.map.invalidateSize(); } catch (e) { }
                        try { const c = last.geometry && last.geometry.coordinates; if (c && c.length >= 2) this.map.focusToLatLng(c[1], c[0], 15); } catch (e) { /* ignore */ }
                    }
                }
            } catch (e) { /* ignore checkin inference errors */ }
        });

        this.ui.bindFocusFc(async () => {
            const applId = this.selectedCustomerId || this.ui.getSelectedCustomerId();
            const fcId = this.selectedFcId || this.ui.getSelectedFcId();
            this.logToPage('focusFc clicked, applId: ' + applId + ', fcId: ' + fcId);
            console.log('focusFc clicked, applId:', applId, 'fcId:', fcId);
            if (!applId || !fcId) {
                this.logToPage('missing applId or fcId');
                console.log('missing applId or fcId');
                return;
            }
            // Load and show the selected FC's checkins, then focus on the first one
            try {
                const checkins = await this.api.getCheckinsGeoJson(applId, fcId, 0, 1000);
                this.logToPage('checkins loaded: ' + JSON.stringify(checkins).substring(0, 100) + '...');
                console.log('checkins loaded:', checkins);
                this.map.showCheckinsGeojson(checkins);
                try { await this.map.waitForMapLayersReady(2000); } catch (e) { }
                try { if (this.map && this.map.map && typeof this.map.map.invalidateSize === 'function') this.map.map.invalidateSize(); } catch (e) { }
                const features = (checkins && checkins.features) ? checkins.features : [];
                this.logToPage('features length: ' + features.length);
                console.log('features length:', features.length);
                if (features.length > 0) {
                    // Sort by checkin_date descending to find the latest
                    features.sort((a, b) => {
                        const da = (a.properties && a.properties.checkin_date) || '';
                        const db = (b.properties && b.properties.checkin_date) || '';
                        return (da < db) ? 1 : ((da > db) ? -1 : 0);
                    });

                    // Focus on the first checkin (now the latest)
                    const first = features[0];
                    const lat = first.geometry.coordinates[1];
                    const lng = first.geometry.coordinates[0];
                    this.logToPage('focusing on: ' + lat + ', ' + lng);
                    console.log('focusing on:', lat, lng);
                    try { if (this.map && this.map.map) this.map.map.setView([lat, lng], Math.max(this.map.map.getZoom() || 0, 15)); } catch (e) { this.logToPage('setView error: ' + e); console.log('setView error:', e); }
                } else {
                    this.logToPage('no checkins to focus on');
                    console.log('no checkins to focus on');
                }
            } catch (e) {
                this.logToPage('error loading checkins: ' + e);
                console.log('error loading checkins:', e);
            }
        });

        // bind show-predicted-for-fc button
        this.ui.bindShowFcPrediction(async () => {
            const applId = this.selectedCustomerId || this.ui.getSelectedCustomerId();
            const fcId = this.selectedFcId || this.ui.getSelectedFcId();
            if (!applId || !fcId) return;
            const checkins = await this.api.getCheckinsGeoJson(applId, fcId, 0, 1000);
            try {
                const features = (checkins && checkins.features) ? checkins.features : [];
                if (features.length === 0) return;
                let sumLat = 0, sumLng = 0, count = 0;
                features.forEach(f => {
                    try {
                        const c = f.geometry && f.geometry.coordinates;
                        if (c && c.length >= 2) { sumLng += c[0]; sumLat += c[1]; count++; }
                    } catch (e) { /* ignore */ }
                });
                if (count === 0) return;
                const avgLng = sumLng / count;
                const avgLat = sumLat / count;
                const fakeFeature = { geometry: { coordinates: [avgLng, avgLat] }, properties: { appl_id: applId, fc_id: fcId, adjusted: true, areaLevel: 'fc_prediction' } };
                this.map.showPredictedAddress(fakeFeature);
            } catch (e) { /* ignore */ }
        });
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
        // Expose a simple readiness flag for acceptance tests to poll. This becomes true
        // once the app has completed initial UI and map wiring so tests can avoid race conditions.
        try { window.__app_ready = true; window.__app = this; } catch (e) { /* ignore */ }

        // Listen for checkin marker clicks to update selected address and Show Predicted button state
        try {
            window.addEventListener('app:checkinClicked', async (ev) => {
                try {
                    const addrId = ev && ev.detail && ev.detail.addressId;
                    if (!addrId) return;
                    this.selectedAddressId = String(addrId);

                    // Attempt to fetch/refresh addresses so the map marker appears and we can determine is_exact.
                    const appl = this.selectedCustomerId || this.ui.getSelectedCustomerId() || (ev && ev.detail && ev.detail.applId);
                    let isExact = false;
                    if (appl) {
                        try {
                            const addrGeo = await this.api.getAddressesGeoJson(appl, 0, 1000).catch(() => null);
                            if (addrGeo && Array.isArray(addrGeo.features)) {
                                try { this.map.showAddressesGeojson(addrGeo); } catch (e) { /* ignore */ }
                                const feat = addrGeo.features.find(f => String((f.properties && f.properties.id) || '') === String(addrId));
                                if (feat && feat.properties) {
                                    isExact = (feat.properties.is_exact === true) || (String(feat.properties.is_exact) === 'true');
                                    if (this.map && this.map._addressExactById) this.map._addressExactById[String(addrId)] = isExact;
                                }
                            }
                            // Fallback to page API if not found in geojson
                            if (!isExact) {
                                try {
                                    const resp = await this.api.getAddressesPage(appl, '', 0, 1000).catch(() => null);
                                    const items = (resp && resp.items) ? resp.items : (resp || []);
                                    const item = items.find(i => String(i.id) === String(addrId));
                                    if (item) {
                                        isExact = !!(item.is_exact === true || String(item.is_exact) === 'true');
                                        if (this.map && this.map._addressExactById) this.map._addressExactById[String(addrId)] = isExact;
                                    }
                                } catch (e) { /* ignore */ }
                            }
                        } catch (e) { /* ignore */ }
                    }
                    // Ensure we also re-highlight address marker now that we refreshed addresses
                    try { if (this.selectedAddressId) this.map.highlightAddress(this.selectedAddressId, { fit: false }); } catch (e) { /* ignore */ }
                    // Finally update the Show Predicted button according to the discovered exactness
                    try { this.ui.setShowFcPredEnabled(!isExact); } catch (e) { /* ignore */ }
                } catch (e) { /* ignore */ }
            });
        } catch (e) { /* ignore */ }
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
            // clear address markers
            this.map.showAddressesGeojson({ type: 'FeatureCollection', features: [] });
            this.map.clearCheckins();
            this.map.clearPredicted();
            return;
        }
        // load first page of addresses and checkins for this customer
        this.addressesPage = 0;
        this.addressesQ = '';
        const addressesResp = await this.api.getAddresses(applId, this.addressesQ, 0, this.addressesSize || 50);
        this.ui.populateAddresses(addressesResp);

        const addrGeo = await this.api.getAddressesGeoJson(applId, 0, this.addressesSize || 50);
        this.map.showAddressesGeojson(addrGeo);
        try { await this.map.waitForMapLayersReady(2000); } catch (e) { }

        // Update 'Show predicted' button enabled state based on the currently-selected address
        try {
            const selAddr = this.selectedAddressId || this.ui.getSelectedAddressId();
            if (selAddr) {
                const isExact = !!(this.map._addressExactById && this.map._addressExactById[String(selAddr)]);
                this.ui.setShowFcPredEnabled(!isExact);
            } else {
                // If only one address exists for this customer and it's exact, disable the button
                try {
                    const items = (addressesResp && addressesResp.items) ? addressesResp.items : (addressesResp || []);
                    if (items && items.length === 1) {
                        const only = items[0];
                        const exactOnly = !!(only && (only.is_exact === true || String(only.is_exact) === 'true'));
                        this.ui.setShowFcPredEnabled(!exactOnly);
                    }
                } catch (e) { /* ignore */ }
            }
        } catch (e) { /* ignore */ }

        const checkinsGeo = await this.api.getCheckinsGeoJson(applId, '', 0, 1000); // page checkins with reasonable default
        this.map.showCheckinsGeojson(checkinsGeo);
        try { await this.map.waitForMapLayersReady(2000); } catch (e) { }

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
        let ok = this.map.highlightAddress(addrId, { fit: true });
        
        // If address marker not found (e.g. not in the initial page of 50), load more addresses
        if (!ok) {
            try {
                const applId = this.selectedCustomerId || this.ui.getSelectedCustomerId();
                if (applId) {
                    // Fetch a larger batch (e.g. 1000) to likely include the selected address
                    const addrGeo = await this.api.getAddressesGeoJson(applId, 0, 1000);
                    this.map.showAddressesGeojson(addrGeo);
                    // Try highlighting again
                    ok = this.map.highlightAddress(addrId, { fit: true });
                }
            } catch (e) { console.warn('Fallback load for address failed', e); }
        }

        // Update Show Predicted button: disable when selected address is exact
        try {
            const isExact = !!(this.map._addressExactById && this.map._addressExactById[String(addrId)]);
            this.ui.setShowFcPredEnabled(!isExact);
        } catch (e) { /* ignore */ }
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

        // Fetch and show predicted marker for this address so user sees the predicted location
        try {
            // prefer embedded prediction if present on the currently-loaded address layer
            let embeddedPred = null;
            try {
                const markers = this.map._addressMarkersById || {};
                Object.values(markers).forEach(al => {
                    try {
                        const id = al.feature && al.feature.properties && al.feature.properties.id || (al.featureProps && al.featureProps.id);
                        const props = al.feature && al.feature.properties ? al.feature.properties : (al.featureProps || {});
                        if (String(id) === String(addrId) && props && props.predicted_feature) {
                            embeddedPred = props.predicted_feature;
                        }
                    } catch (e) { /* ignore */ }
                });
            } catch (e) { /* ignore if address markers missing */ }
            if (embeddedPred) {
                this.map.showPredictedAddress(embeddedPred);
            } else {
                const pred = await this.api.getPredictedAddress(this.selectedCustomerId || this.ui.getSelectedCustomerId(), addrId);
                if (pred && pred.geometry) this.map.showPredictedAddress(pred);
            }
        } catch (e) { /* ignore */ }

        // Enable/disable 'Show predicted' button: disable when the selected address is exact because
        // an exact address has no prediction per app logic.
        try {
            const isExact = !!(this.map._addressExactById && this.map._addressExactById[String(addrId)]);
            this.ui.setShowFcPredEnabled(!isExact);
        } catch (e) { /* ignore UI failures */ }
    }

    async handleFcChange(fcId) {
        // filter checkins by fc
        this.map.filterCheckinsByFcId(fcId);
        // Disable/enable the Show Predicted button when there's insufficient variation in checkins
        try {
            const applId = this.selectedCustomerId || this.ui.getSelectedCustomerId();
            if (!applId || !fcId) return;
            const checkins = await this.api.getCheckinsGeoJson(applId, fcId, 0, 1000);
            const cnt = (checkins && checkins.features) ? checkins.features.length : 0;
            // If only one checking location, predicted address will equal the customer address -> disable
            let disable = cnt <= 1;
            // Also force disable if the currently selected address is Exact (Verified)
            if (!disable && this.selectedAddressId) {
                try {
                    const isExact = !!(this.map._addressExactById && this.map._addressExactById[String(this.selectedAddressId)]);
                    if (isExact) disable = true;
                } catch (e) { /* ignore */ }
            }
            this.ui.setShowFcPredEnabled(!disable);
        } catch (e) { /* ignore */ }
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
