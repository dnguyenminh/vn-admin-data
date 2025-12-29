export default class UIManager {
    constructor() {
        this.searchInput = document.getElementById('searchInput');
        this.resultsContainer = document.getElementById('searchResults');
        this.pSel = document.getElementById('provinceSelect');
        this.dSel = document.getElementById('districtSelect');
        this.wSel = document.getElementById('wardSelect');
        this.cSel = document.getElementById('customerSelect');
        this.aSel = document.getElementById('addressSelect');
        this.fcSel = document.getElementById('fcSelect');

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
        this.cSel.innerHTML = '<option value="">-- Chọn Khách hàng (appl_id) --</option>';
        list.forEach(c => this.cSel.add(new Option(c.name, c.id)));
    }

    populateAddresses(list) {
        this.aSel.innerHTML = '<option value="">-- Chọn Địa chỉ --</option>';
        list.forEach(a => this.aSel.add(new Option(a.name, a.id)));
    }

    populateFcIds(list) {
        this.fcSel.innerHTML = '<option value="">-- Chọn Field Collector (fc_id) --</option>';
        list.forEach(f => this.fcSel.add(new Option(f.name, f.id)));
    }
}