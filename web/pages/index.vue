<template>
  <div>
    <div style="position: absolute; z-index: 1000; left: 30px; top: 20px;">
      <SearchBar />
      <FilterPanel />
    </div>
    <MapClient />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import { useRoute } from 'vue-router';
import SearchBar from '~/components/SearchBar.vue';
import FilterPanel from '~/components/FilterPanel.vue';
import MapClient from '~/components/Map.client.vue';
import { useGisStore } from '~/stores/gisStore';

const route = useRoute();
const store = useGisStore();

onMounted(async () => {
  // Ensure provinces loaded, then if there's an `id` query param, use it as initial province selection
  await store.loadProvinces();
  const id = route.query.id as string | undefined;
  if (id) {
    // Defensive: only set if not empty
    store.setProvince(id);
  }
});
</script>

<style>
html, body, #__nuxt, #__layout { height: 100%; }
</style>
