<template>
  <div class="search-container">
    <input v-model="q" @input="onInput" @keydown.escape="close" placeholder="Tìm tỉnh, huyện, xã..." />
    <div v-if="visible" class="results">
      <div v-for="item in results" :key="item.id" class="result" @mousedown.prevent="select(item)">
        <strong>{{ item.name }}</strong> <small>({{ item.type }})</small>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useGisApi } from '~/composables/useGisApi';
import { useGisStore } from '~/stores/gisStore';

const api = useGisApi();
const store = useGisStore();

const q = ref('');
const results = ref([] as any[]);
const visible = ref(false);
let timer: ReturnType<typeof setTimeout> | null = null;

function close() {
  visible.value = false;
}

async function onInput() {
  if (timer) clearTimeout(timer);
  if (q.value.length < 2) { visible.value = false; return; }
  timer = setTimeout(async () => {
    results.value = await api.search(q.value);
    visible.value = results.value.length > 0;
  }, 300);
}

async function select(item: any) {
  visible.value = false;
  // Follow the BRD: if selecting district/ward, wait for parent load
  if (item.type === 'province') {
    await store.setProvince(item.id);
  } else if (item.type === 'district') {
    await store.setProvince(item.province_id || item.parent_id);
    // wait for districts to be loaded by setProvince
    await store.setDistrict(item.id);
  } else if (item.type === 'ward') {
    await store.setProvince(item.province_id);
    await store.setDistrict(item.parent_id);
    store.setWard(item.id);
  }
}
</script>

<style scoped>
.search-container { position: relative; width: 360px; }
.results { position: absolute; top: 36px; left: 0; right: 0; background: white; border: 1px solid #ddd; z-index: 1000; }
.result { padding: 8px; border-bottom: 1px solid #eee; cursor: pointer; }
.result:hover { background: #f5faff; }
</style>
