<template>
  <div class="filter-panel">
    <select aria-label="province" v-model="store.selectedProvince" @change="onProvinceChange">
      <option value="">-- Chọn Tỉnh --</option>
      <option v-for="p in store.provinces" :key="p.id" :value="p.id">{{ p.name }}</option>
    </select>

    <select aria-label="district" v-model="store.selectedDistrict" @change="onDistrictChange">
      <option value="">-- Chọn Huyện --</option>
      <option v-for="d in store.districts" :key="d.id" :value="d.id">{{ d.name }}</option>
    </select>

    <select aria-label="ward" v-model="store.selectedWard" @change="onWardChange">
      <option value="">-- Chọn Xã --</option>
      <option v-for="w in store.wards" :key="w.id" :value="w.id">{{ w.name }}</option>
    </select>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import { useGisStore } from '~/stores/gisStore';

const store = useGisStore();

onMounted(() => store.loadProvinces());

function onProvinceChange(e: Event) {
  store.setProvince(store.selectedProvince);
}

function onDistrictChange(e: Event) {
  store.setDistrict(store.selectedDistrict);
}

function onWardChange(e: Event) {
  store.setWard(store.selectedWard);
}
</script>

<style scoped>
.filter-panel { display: flex; gap: 8px; align-items: center; }
select { padding: 8px; min-width: 160px; }
</style>
