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
}