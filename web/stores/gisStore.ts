import { defineStore } from 'pinia';
import { ref } from 'vue';
import { useGisApi } from '~/composables/useGisApi';

export const useGisStore = defineStore('gis', () => {
  const provinces = ref([]);
  const districts = ref([]);
  const wards = ref([]);

  const selectedProvince = ref('');
  const selectedDistrict = ref('');
  const selectedWard = ref('');

  const loadingDistricts = ref(false);
  const loadingWards = ref(false);

  const api = useGisApi();

  async function loadProvinces() {
    provinces.value = await api.getProvinces();
  }

  async function setProvince(id) {
    if (!id) {
      selectedProvince.value = '';
      districts.value = [];
      selectedDistrict.value = '';
      wards.value = [];
      selectedWard.value = '';
      return;
    }
    selectedProvince.value = id;
    // reset downstream
    selectedDistrict.value = '';
    wards.value = [];
    selectedWard.value = '';

    loadingDistricts.value = true;
    districts.value = await api.getDistricts(id);
    loadingDistricts.value = false;
  }

  async function setDistrict(id) {
    if (!id) {
      selectedDistrict.value = '';
      wards.value = [];
      selectedWard.value = '';
      return;
    }
    selectedDistrict.value = id;
    selectedWard.value = '';

    loadingWards.value = true;
    wards.value = await api.getWards(id);
    loadingWards.value = false;
  }

  function setWard(id) {
    selectedWard.value = id || '';
  }

  return {
    // state
    provinces,
    districts,
    wards,
    selectedProvince,
    selectedDistrict,
    selectedWard,
    loadingDistricts,
    loadingWards,
    // actions
    loadProvinces,
    setProvince,
    setDistrict,
    setWard
  };
});
