export function useGisApi() {
  const base = '/api/map';

  async function fetchJson(path) {
    const res = await fetch(path);
    if (!res.ok) throw new Error(`HTTP ${res.status} ${res.statusText}`);
    return res.json();
  }

  return {
    getProvinces: async () => fetchJson(`${base}/provinces`),
    getDistricts: async (provinceId) => fetchJson(`${base}/districts?provinceId=${encodeURIComponent(provinceId)}`),
    getWards: async (districtId) => fetchJson(`${base}/wards?districtId=${encodeURIComponent(districtId)}`),
    getDistrictsGeoJson: async (provinceId) => fetchJson(`${base}/districts/geojson?provinceId=${encodeURIComponent(provinceId)}`),
    getWardsGeoJson: async (districtId) => fetchJson(`${base}/wards/geojson?districtId=${encodeURIComponent(districtId)}`),
    getProvinceBounds: async (provinceId) => fetchJson(`${base}/province/bounds?provinceId=${encodeURIComponent(provinceId)}`),
    search: async (q) => fetchJson(`${base}/search?q=${encodeURIComponent(q)}`)
  };
}
