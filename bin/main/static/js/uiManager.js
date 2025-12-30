export default class UIManager {
    constructor() {
        this.searchInput = document.getElementById('searchInput');
        this.resultsContainer = document.getElementById('searchResults');
        this.pSel = document.getElementById('provinceSelect');
        this.dSel = document.getElementById('districtSelect');
        this.wSel = document.getElementById('wardSelect');
        this.cSel = document.getElementById('customerSelect');
        // Combobox input + results for customer (single control)
        this.cCombo = document.getElementById('customerCombo');
        this.cResults = document.getElementById('customerResults');
        this.aSel = document.getElementById('addressSelect');
        this.addressCombo = document.getElementById('addressCombo');
        this.addressResults = document.getElementById('addressResults');
        this.fcSel = document.getElementById('fcSelect');
        this.fcCombo = document.getElementById('fcCombo');
        this.fcResults = document.getElementById('fcResults');
        this.aSearch = document.getElementById('addressSearch');

        this.ignoreInput = false;
    }

    bindSearchInput(onInput) {
        let debounceTimer;
        this.searchInput.oninput = (e) => {
            if (this.ignoreInput) return;
            clearTimeout(debounceTimer);
            const q = e.target.value;
            if (q.length < 2) { this.hideResults(); return; }
            debounceTimer = setTimeout(() => onInput(q), 400);
        };
    }

    showResults(results) {
        this.resultsContainer.innerHTML = '';
        if (results.length === 0) { this.hideResults(); return; }
        results.forEach(item => {
            const div = document.createElement('div');
            div.className = 'search-item';
            div.setAttribute('data-json', JSON.stringify(item));
            div.innerHTML = `<strong>${item.name}</strong> <small>(${item.type})</small>`;
            this.resultsContainer.appendChild(div);
        });
        this.resultsContainer.style.display = 'block';
    }

    hideResults() { this.resultsContainer.style.display = 'none'; }

    bindResultClicks(onSelect) {
        this.resultsContainer.addEventListener('click', (e) => {
            const itemElement = e.target.closest('.search-item');
            if (!itemElement) return;
            e.stopPropagation();
            const itemData = JSON.parse(itemElement.getAttribute('data-json'));
            this.ignoreInput = true;
            onSelect(itemData);
            setTimeout(() => { this.ignoreInput = false; }, 0);
        });

        document.addEventListener('click', (e) => {
            if (!e.target.closest('.search-container')) this.hideResults();
        });
    }

    // Bind the combobox input for customers. onQuery(q, page) should return a Promise resolving to a paged envelope
    // onSelect(item) will be called when a specific customer is picked; onLoadMore() when the load-more item is clicked.
    bindCustomerCombo(onQuery, onSelect, onLoadMore) {
        if (!this.cCombo) return;
        let debounceTimer;
        let focusedIndex = -1;
        this.cCombo.oninput = (e) => {
            clearTimeout(debounceTimer);
            const q = e.target.value || '';
            debounceTimer = setTimeout(() => onQuery(q, 0), 300);
        };
        // Enter key selects the first visible result if any
        this.cCombo.onkeydown = (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                if (!this.cResults) return;
                const items = Array.from(this.cResults.querySelectorAll('.search-item:not([data-load-more])'));
                if (focusedIndex >= 0 && focusedIndex < items.length) {
                    items[focusedIndex].click();
                    return;
                }
                const first = items[0];
                if (first) { first.click(); return; }
                // if only a load-more sentinel exists, trigger load-more
                const lm = this.cResults.querySelector('.search-item[data-load-more]');
                if (lm && typeof onLoadMore === 'function') onLoadMore();
            } else if (e.key === 'ArrowDown') {
                e.preventDefault();
                if (!this.cResults || this.cResults.style.display === 'none') {
                    // populate results for current query
                    onQuery(this.cCombo.value || '', 0);
                    return;
                }
                const items = Array.from(this.cResults.querySelectorAll('.search-item:not([data-load-more])'));
                if (items.length === 0) return;
                focusedIndex = Math.min(focusedIndex + 1, items.length - 1);
                items.forEach((it, idx) => it.classList.toggle('focused', idx === focusedIndex));
                if (items[focusedIndex]) items[focusedIndex].scrollIntoView({ block: 'nearest' });
            } else if (e.key === 'ArrowUp') {
                e.preventDefault();
                const items = Array.from(this.cResults.querySelectorAll('.search-item:not([data-load-more])'));
                if (items.length === 0) return;
                focusedIndex = Math.max(focusedIndex - 1, 0);
                items.forEach((it, idx) => it.classList.toggle('focused', idx === focusedIndex));
                if (items[focusedIndex]) items[focusedIndex].scrollIntoView({ block: 'nearest' });
            }
        };
        // Clicking outside should hide results
        document.addEventListener('click', (ev) => {
            if (!ev.target.closest('.customer-combobox')) this.hideCustomerResults();
        });

        // Click handler for customer dropdown
        if (this.cResults) {
            this.cResults.addEventListener('click', (ev) => {
                const itemEl = ev.target.closest('.search-item');
                if (!itemEl) return;
                ev.stopPropagation();
                if (itemEl.dataset.loadMore === '1') {
                    if (typeof onLoadMore === 'function') onLoadMore();
                    return;
                }
                const id = itemEl.dataset.id;
                const name = itemEl.textContent.trim();
                this.setCustomerValue(name, id);
                this.hideCustomerResults();
                if (typeof onSelect === 'function') onSelect({ id, name });
            });
        }
    }

    bindAddressSearch(onQuery) {
        let debounceTimer;
        if (!this.aSearch) return;
        this.aSearch.oninput = (e) => {
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(() => onQuery(e.target.value), 300);
        };
    }

    setInputValue(val) {
        this.ignoreInput = true;
        this.searchInput.value = val;
        setTimeout(() => { this.ignoreInput = false; }, 0);
    }

    populateProvinces(list) {
        this.pSel.innerHTML = '<option value="">-- Chọn Tỉnh --</option>';
        list.forEach(p => this.pSel.add(new Option(p.name, p.id)));
    }

    populateDistricts(list) {
        this.dSel.innerHTML = '<option value="">-- Chọn Huyện --</option>';
        list.forEach(d => this.dSel.add(new Option(d.name, d.id)));
    }

    populateWards(list) {
        this.wSel.innerHTML = '<option value="">-- Chọn Xã --</option>';
        list.forEach(w => this.wSel.add(new Option(w.name, w.id)));
    }

    populateCustomers(list) {
        // Backwards-compatible: if previously called, populate combobox dropdown with passed data
        if (!this.cResults) return;
        this.showCustomerResults(list, false);
    }

    showCustomerResults(list, append = false) {
        if (!this.cResults) return;
        const items = list && list.items ? list.items : (list || []);
        if (!append) this.cResults.innerHTML = '';
        items.forEach(it => {
            const div = document.createElement('div');
            div.className = 'search-item';
            div.dataset.id = String(it.id);
            div.textContent = String(it.name);
            this.cResults.appendChild(div);
        });
        // Add load-more sentinel if paged envelope says more available
        if (list && list.total !== undefined && list.page !== undefined && list.size !== undefined) {
            const loaded = (list.page + 1) * list.size;
            if (loaded < (Number(list.total) || 0)) {
                const lm = document.createElement('div');
                lm.className = 'search-item';
                lm.dataset.loadMore = '1';
                lm.textContent = 'Load more...';
                this.cResults.appendChild(lm);
            }
        }
        this.cResults.style.display = (this.cResults.children.length > 0) ? 'block' : 'none';
    }

    hideCustomerResults() { if (this.cResults) this.cResults.style.display = 'none'; }

    setCustomerValue(name, id) { if (this.cCombo) { this.cCombo.value = name; this.cCombo.dataset.selectedId = String(id); } }

    getSelectedCustomerId() { return this.cCombo ? this.cCombo.dataset.selectedId : null; }

    // Address combobox support
    bindAddressCombo(onQuery, onSelect, onLoadMore) {
        if (!this.addressCombo) return;
        let debounceTimer;
        let focusedIndex = -1;
        this.addressCombo.oninput = (e) => {
            clearTimeout(debounceTimer);
            const q = e.target.value || '';
            debounceTimer = setTimeout(() => onQuery(q, 0), 300);
        };
        this.addressCombo.onkeydown = (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                const items = Array.from(this.addressResults.querySelectorAll('.search-item:not([data-load-more])'));
                if (focusedIndex >= 0 && focusedIndex < items.length) { items[focusedIndex].click(); return; }
                const first = items[0]; if (first) { first.click(); return; }
                const lm = this.addressResults.querySelector('.search-item[data-load-more]'); if (lm && typeof onLoadMore === 'function') onLoadMore();
            } else if (e.key === 'ArrowDown') {
                e.preventDefault();
                if (!this.addressResults || this.addressResults.style.display === 'none') { onQuery(this.addressCombo.value || '', 0); return; }
                const items = Array.from(this.addressResults.querySelectorAll('.search-item:not([data-load-more])'));
                if (items.length === 0) return; focusedIndex = Math.min(focusedIndex + 1, items.length - 1);
                items.forEach((it, idx) => it.classList.toggle('focused', idx === focusedIndex)); if (items[focusedIndex]) items[focusedIndex].scrollIntoView({ block: 'nearest' });
            } else if (e.key === 'ArrowUp') {
                e.preventDefault(); const items = Array.from(this.addressResults.querySelectorAll('.search-item:not([data-load-more])')); if (items.length === 0) return; focusedIndex = Math.max(focusedIndex - 1, 0); items.forEach((it, idx) => it.classList.toggle('focused', idx === focusedIndex)); if (items[focusedIndex]) items[focusedIndex].scrollIntoView({ block: 'nearest' });
            }
        };
        document.addEventListener('click', (ev) => { if (!ev.target.closest('.address-combobox')) this.hideAddressResults(); });
        if (this.addressResults) {
            this.addressResults.addEventListener('click', (ev) => {
                const itemEl = ev.target.closest('.search-item');
                if (!itemEl) return;
                ev.stopPropagation();
                if (itemEl.dataset.loadMore === '1') { if (typeof onLoadMore === 'function') onLoadMore(); return; }
                const id = itemEl.dataset.id; const name = itemEl.textContent.trim();
                this.setAddressValue(name, id); this.hideAddressResults(); if (typeof onSelect === 'function') onSelect({ id, name });
            });
        }
    }

    showAddressResults(list, append = false) {
        if (!this.addressResults) return;
        const items = list && list.items ? list.items : (list || []);
        if (!append) this.addressResults.innerHTML = '';
        items.forEach(it => { const div = document.createElement('div'); div.className = 'search-item'; div.dataset.id = String(it.id); div.textContent = String(it.name); this.addressResults.appendChild(div); });
        if (list && list.total !== undefined && list.page !== undefined && list.size !== undefined) { const loaded = (list.page + 1) * list.size; if (loaded < (Number(list.total) || 0)) { const lm = document.createElement('div'); lm.className = 'search-item'; lm.dataset.loadMore = '1'; lm.textContent = 'Load more...'; this.addressResults.appendChild(lm); } }
        this.addressResults.style.display = (this.addressResults.children.length > 0) ? 'block' : 'none';
    }

    hideAddressResults() { if (this.addressResults) this.addressResults.style.display = 'none'; }

    setAddressValue(name, id) { if (this.addressCombo) { this.addressCombo.value = name; this.addressCombo.dataset.selectedId = String(id); } }

    getSelectedAddressId() { return this.addressCombo ? this.addressCombo.dataset.selectedId : null; }

    // FC combobox support
    bindFcCombo(onQuery, onSelect, onLoadMore) {
        if (!this.fcCombo) return;
        let debounceTimer;
        let focusedIndex = -1;
        this.fcCombo.oninput = (e) => { clearTimeout(debounceTimer); const q = e.target.value || ''; debounceTimer = setTimeout(() => onQuery(q, 0), 300); };
        this.fcCombo.onkeydown = (e) => {
            if (e.key === 'Enter') {
                e.preventDefault(); const items = Array.from(this.fcResults.querySelectorAll('.search-item:not([data-load-more])'));
                if (focusedIndex >= 0 && focusedIndex < items.length) { items[focusedIndex].click(); return; }
                const first = items[0]; if (first) { first.click(); return; }
                const lm = this.fcResults.querySelector('.search-item[data-load-more]'); if (lm && typeof onLoadMore === 'function') onLoadMore();
            } else if (e.key === 'ArrowDown') {
                e.preventDefault(); if (!this.fcResults || this.fcResults.style.display === 'none') { onQuery(this.fcCombo.value || '', 0); return; } const items = Array.from(this.fcResults.querySelectorAll('.search-item:not([data-load-more])')); if (items.length === 0) return; focusedIndex = Math.min(focusedIndex + 1, items.length - 1); items.forEach((it, idx) => it.classList.toggle('focused', idx === focusedIndex)); if (items[focusedIndex]) items[focusedIndex].scrollIntoView({ block: 'nearest' });
            } else if (e.key === 'ArrowUp') { e.preventDefault(); const items = Array.from(this.fcResults.querySelectorAll('.search-item:not([data-load-more])')); if (items.length === 0) return; focusedIndex = Math.max(focusedIndex - 1, 0); items.forEach((it, idx) => it.classList.toggle('focused', idx === focusedIndex)); if (items[focusedIndex]) items[focusedIndex].scrollIntoView({ block: 'nearest' }); }
        };
        document.addEventListener('click', (ev) => { if (!ev.target.closest('.fc-combobox')) this.hideFcResults(); });
        if (this.fcResults) {
            this.fcResults.addEventListener('click', (ev) => {
                const itemEl = ev.target.closest('.search-item'); if (!itemEl) return; ev.stopPropagation(); if (itemEl.dataset.loadMore === '1') { if (typeof onLoadMore === 'function') onLoadMore(); return; } const id = itemEl.dataset.id; const name = itemEl.textContent.trim(); this.setFcValue(name, id); this.hideFcResults(); if (typeof onSelect === 'function') onSelect({ id, name });
            });
        }
    }

    showFcResults(list, append = false) {
        if (!this.fcResults) return;
        const items = list && list.items ? list.items : (list || []);
        if (!append) this.fcResults.innerHTML = '';
        items.forEach(it => { const div = document.createElement('div'); div.className = 'search-item'; div.dataset.id = String(it.id); div.textContent = String(it.name); this.fcResults.appendChild(div); });
        if (list && list.total !== undefined && list.page !== undefined && list.size !== undefined) { const loaded = (list.page + 1) * list.size; if (loaded < (Number(list.total) || 0)) { const lm = document.createElement('div'); lm.className = 'search-item'; lm.dataset.loadMore = '1'; lm.textContent = 'Load more...'; this.fcResults.appendChild(lm); } }
        this.fcResults.style.display = (this.fcResults.children.length > 0) ? 'block' : 'none';
    }

    hideFcResults() { if (this.fcResults) this.fcResults.style.display = 'none'; }

    setFcValue(name, id) { if (this.fcCombo) { this.fcCombo.value = name; this.fcCombo.dataset.selectedId = String(id); } }

    getSelectedFcId() { return this.fcCombo ? this.fcCombo.dataset.selectedId : null; }

    populateAddresses(list) {
        // Prefer combobox results if available
        if (this.addressResults) {
            this.showAddressResults(list, false);
            return;
        }
        // Fallback to legacy select if present
        if (this.aSel) {
            this.aSel.innerHTML = '<option value="">-- Chọn Địa chỉ --</option>';
            let items = list && list.items ? list.items : list || [];
            items.forEach(a => this.aSel.add(new Option(a.name, a.id)));
            if (list && list.total !== undefined && list.page !== undefined && list.size !== undefined) {
                const loaded = (list.page + 1) * list.size;
                if (loaded < (Number(list.total) || 0)) {
                    const opt = new Option('Load more...', '__load_more__');
                    this.aSel.add(opt);
                }
            }
        }
    }

    populateFcIds(list) {
        // Prefer combobox results if available
        if (this.fcResults) {
            this.showFcResults(list, false);
            return;
        }
        // Fallback to legacy select if present
        if (this.fcSel) {
            this.fcSel.innerHTML = '<option value="">-- Chọn Field Collector (fc_id) --</option>';
            (list || []).forEach(f => this.fcSel.add(new Option(f.name, f.id)));
        }
    }
}