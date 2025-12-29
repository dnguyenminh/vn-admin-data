export default class ApiClient {
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
        const res = await fetch('/api/map/customers');
        return res.json();
    }

    static async getAddresses(applId) {
        const res = await fetch(`/api/map/addresses?applId=${encodeURIComponent(applId)}`);
        return res.json();
    }

    static async getAddressesGeoJson(applId) {
        const res = await fetch(`/api/map/addresses/geojson?applId=${encodeURIComponent(applId)}`);
        return res.json();
    }

    static async getCheckinsGeoJson(applId, fcId = '') {
        const fcParam = fcId ? `&fcId=${encodeURIComponent(fcId)}` : '';
        const res = await fetch(`/api/map/checkins/geojson?applId=${encodeURIComponent(applId)}${fcParam}`);
        return res.json();
    }

    static async getCheckinFcIds(applId) {
        const res = await fetch(`/api/map/checkins/fcids?applId=${encodeURIComponent(applId)}`);
        return res.json();
    }
}