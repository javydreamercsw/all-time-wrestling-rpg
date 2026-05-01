<template>
  <div style="display: flex; gap: 10px; flex-wrap: wrap; margin-top: 20px;">
    <a :href="`${downloadBase}/All.Time.Wrestling-${releaseVersion}.msi`" class="vplug-button brand">Download for Windows (.msi)</a>
    <a :href="`${downloadBase}/All.Time.Wrestling-${releaseVersion}.dmg`" class="vplug-button brand">Download for macOS (.dmg)</a>
    <a :href="`${downloadBase}/all-time-wrestling-rpg-${releaseVersion}.deb`" class="vplug-button brand">Download for Linux (.deb)</a>
  </div>
  <p style="margin-top: 12px;">
    <strong>Portable ZIP:</strong> Download, unzip, and run without installation.
    <a :href="`${downloadBase}/all-time-wrestling-rpg-${releaseVersion}.zip`">Get Portable Version</a>
  </p>
</template>

<script setup>
import { useData } from 'vitepress'
const { theme } = useData()
const releaseVersion = theme.value.releaseVersion
const downloadBase = theme.value.downloadBase
</script>
