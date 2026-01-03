class ApiClient {
    static async search(q) {
        const res = await fetch(`/api/map/search?q=${encodeURIComponent(q)}`);
        return res.json();
    }

    static async getProvinces() {
        const res = await fetch('/api/map/provinces');
        return res.json();
    }

    static async getProvinceBounds(pid) {
        const res = await fetch(`/api/map/province/bounds?provinceId=${pid}`);
        return res.json();
    }

    static async getDistricts(pid) {
        const res = await fetch(`/api/map/districts?provinceId=${pid}`);
        return res.json();
    }

    static async getDistrictsGeoJson(pid) {
        const res = await fetch(`/api/map/districts/geojson?provinceId=${pid}`);
        return res.json();
    }

    static async getWards(did) {
        const res = await fetch(`/api/map/wards?districtId=${did}`);
        return res.json();
    }

    static async getWardsGeoJson(did) {
        const res = await fetch(`/api/map/wards/geojson?districtId=${did}`);
        return res.json();
    }

    static async getCustomers() {
        return this.getCustomersPage('', 0, 50);
    }

    static async getCustomersPage(q = '', page = 0, size = 50) {
        const qParam = q ? `&q=${encodeURIComponent(q)}` : '';
        const res = await fetch(`/api/map/customers?page=${page}&size=${size}${qParam}`);
        return res.json();
    }

    static async getCustomersAfter(after = '', q = '', size = 50) {
        const qParam = q ? `&q=${encodeURIComponent(q)}` : '';
        const afterParam = after ? `&after=${encodeURIComponent(after)}` : '';
        const res = await fetch(`/api/map/customers?size=${size}${afterParam}${qParam}`);
        return res.json();
    }

    static async getAddresses(applId, q = '', page = 0, size = 50) {
        const qParam = q ? `&q=${encodeURIComponent(q)}` : '';
        const res = await fetch(`/api/map/addresses?applId=${encodeURIComponent(applId)}&page=${page}&size=${size}${qParam}`);
        return res.json();
    }

    static async getAddressesPage(applId, q = '', page = 0, size = 50) {
        return this.getAddresses(applId, q, page, size);
    }

    static async getAddressesGeoJson(applId, page = null, size = null) {
        const pageParam = (page !== null && size !== null) ? `&page=${page}&size=${size}` : '';
        const res = await fetch(`/api/map/addresses/geojson?applId=${encodeURIComponent(applId)}${pageParam}`);
        return res.json();
    }

    static async getReverseForAddress(addressId) {
        const res = await fetch(`/api/map/reverse/address?addressId=${encodeURIComponent(addressId)}`);
        return res.json();
    }

    static async getCheckinsGeoJson(applId, fcId = '', page = null, size = null) {
        const fcParam = fcId ? `&fcId=${encodeURIComponent(fcId)}` : '';
        const pageParam = (page !== null && size !== null) ? `&page=${page}&size=${size}` : '';
        const res = await fetch(`/api/map/checkins/geojson?applId=${encodeURIComponent(applId)}${fcParam}${pageParam}`);
        return res.json();
    }

    static async getCheckinFcIds(applId) {
        return this.getCheckinFcIdsPage(applId, '', 0, 50);
    }

    static async getCheckinFcIdsPage(applId, q = '', page = 0, size = 50) {
        const qParam = q ? `&q=${encodeURIComponent(q)}` : '';
        const res = await fetch(`/api/map/checkins/fcids?applId=${encodeURIComponent(applId)}&page=${page}&size=${size}${qParam}`);
        return res.json();
    }

    static async getPredictedAddress(applId, addressId) {
        const res = await fetch(`/api/map/addresses/predict?applId=${encodeURIComponent(applId)}&addressId=${encodeURIComponent(addressId)}`);
        return res.json();
    }
}