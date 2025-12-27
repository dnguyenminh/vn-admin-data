<template>
  <ClientOnly>
    <div id="map" style="height: 100vh; width: 100%;"></div>
  </ClientOnly>
</template>

<script setup lang="ts">
import { onMounted, watch } from 'vue';
import { useGisStore } from '~/stores/gisStore';
import { useGisApi } from '~/composables/useGisApi';

const store = useGisStore();
const api = useGisApi();

let map: any;
let provinceLayer: any;
let districtLayer: any;
let wardLayer: any;
let L: any;

onMounted(async () => {
  // Import Leaflet only on the client to avoid SSR "window is not defined" errors
  L = (await import('leaflet')).default;
  // load default tile layer and create layers
  map = L.map('map').setView([10.5, 105.1], 7);
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);

  provinceLayer = L.geoJSON().addTo(map);
  districtLayer = L.geoJSON().addTo(map);
  wardLayer = L.geoJSON().addTo(map);
});

watch(() => store.selectedProvince, async (pid) => {
  if (!pid) { provinceLayer.clearLayers(); map.setView([10.5, 105.1], 7); return; }
  const boundsGeo = await api.getProvinceBounds(pid);
  try {
    provinceLayer.clearLayers();
    provinceLayer.addData(boundsGeo);
    map.fitBounds(provinceLayer.getBounds());
  } catch (e) {
    console.warn('Invalid bounds geojson', e);
  }
  // also load district geojson for rendering
  const dgeo = await api.getDistrictsGeoJson(pid);
  districtLayer.clearLayers(); districtLayer.addData(dgeo);
});

watch(() => store.selectedDistrict, async (did) => {
  if (!did) { wardLayer.clearLayers(); return; }
  const wgeo = await api.getWardsGeoJson(did);
  wardLayer.clearLayers(); wardLayer.addData(wgeo);
});

</script>

<style scoped>
#map { height: 100vh; }
</style>
