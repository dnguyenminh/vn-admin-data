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
import SearchBar from '~/components/SearchBar.vue';
import FilterPanel from '~/components/FilterPanel.vue';
import MapClient from '~/components/Map.client.vue';
</script>

<style>
html, body, #__nuxt, #__layout { height: 100%; }
</style>
