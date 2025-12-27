import { defineNuxtConfig } from 'nuxt/config'

export default defineNuxtConfig({
  ssr: true,
  modules: ['@pinia/nuxt'],
  css: ['~/assets/main.css']
})
