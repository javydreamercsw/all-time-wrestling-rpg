<template>
  <div>
    <!-- Loading state -->
    <div v-if="loading" style="margin-top: 20px; color: var(--vp-c-text-2);">
      Loading download links…
    </div>

    <!-- Error state -->
    <div v-else-if="error" style="margin-top: 20px;">
      <p style="color: var(--vp-c-danger-1);">Could not load release information.</p>
      <a href="https://github.com/javydreamercsw/all-time-wrestling-rpg/releases/latest" target="_blank" rel="noopener">
        View all releases on GitHub →
      </a>
    </div>

    <!-- Loaded state -->
    <template v-else>
      <!-- Stable release -->
      <template v-if="stableRelease">
        <div style="display: flex; gap: 10px; flex-wrap: wrap; margin-top: 20px;">
          <a :href="assetUrl(stableRelease, 'msi')" class="vplug-button brand">Download for Windows (.msi)</a>
          <a :href="assetUrl(stableRelease, 'dmg')" class="vplug-button brand">Download for macOS (.dmg)</a>
          <a :href="assetUrl(stableRelease, 'deb')" class="vplug-button brand">Download for Linux (.deb)</a>
        </div>
        <p style="margin-top: 12px;">
          <strong>Portable ZIP:</strong> Download, unzip, and run without installation.
          <a :href="assetUrl(stableRelease, 'zip')">Get Portable Version</a>
        </p>
        <p style="margin-top: 6px; font-size: 0.875rem; color: var(--vp-c-text-2);">
          Latest stable release: <strong>{{ stableRelease.tag_name }}</strong>
          &nbsp;·&nbsp;
          <a :href="stableRelease.html_url" target="_blank" rel="noopener">Release notes</a>
        </p>
      </template>
      <p v-else style="margin-top: 20px; color: var(--vp-c-text-2);">
        No stable release available yet.
        <a href="https://github.com/javydreamercsw/all-time-wrestling-rpg/releases" target="_blank" rel="noopener">
          View all releases on GitHub →
        </a>
      </p>

      <!-- Release Candidates -->
      <template v-if="rcReleases.length > 0">
        <h3 style="margin-top: 40px; border-top: 1px solid var(--vp-c-divider); padding-top: 24px;">
          Release Candidates
        </h3>
        <p style="color: var(--vp-c-text-2); font-size: 0.9rem; margin-top: 4px;">
          Preview upcoming features. Release candidates may contain bugs — recommended for testing only.
        </p>
        <div
          v-for="rc in rcReleases"
          :key="rc.tag_name"
          style="margin-top: 20px; padding: 16px; border: 1px solid var(--vp-c-border); border-radius: 8px;"
        >
          <p style="margin: 0 0 12px; font-weight: 600;">
            {{ rc.tag_name }}
            &nbsp;·&nbsp;
            <a :href="rc.html_url" target="_blank" rel="noopener" style="font-weight: normal;">Release notes</a>
          </p>
          <div style="display: flex; gap: 8px; flex-wrap: wrap; margin-top: 8px;">
            <a :href="assetUrl(rc, 'msi')" class="vplug-button alt">Windows (.msi)</a>
            <a :href="assetUrl(rc, 'dmg')" class="vplug-button alt">macOS (.dmg)</a>
            <a :href="assetUrl(rc, 'deb')" class="vplug-button alt">Linux (.deb)</a>
          </div>
          <p style="margin-top: 10px; font-size: 0.875rem;">
            <a :href="assetUrl(rc, 'zip')">Portable ZIP</a>
          </p>
        </div>
      </template>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const loading = ref(true)
const error = ref(false)
const stableRelease = ref(null)
const rcReleases = ref([])

const RELEASES_API = 'https://api.github.com/repos/javydreamercsw/all-time-wrestling-rpg/releases?per_page=20'

function assetUrl(release, ext) {
  if (!release) return '#'
  const v = release.tag_name.replace(/^v/, '')
  const base = `https://github.com/javydreamercsw/all-time-wrestling-rpg/releases/download/${release.tag_name}`
  switch (ext) {
    case 'msi': return `${base}/All.Time.Wrestling-${v}.msi`
    case 'dmg': return `${base}/All.Time.Wrestling-${v}.dmg`
    case 'deb': return `${base}/all-time-wrestling-rpg-${v}.deb`
    case 'zip': return `${base}/all-time-wrestling-rpg-${v}.zip`
    default:    return release.html_url
  }
}

onMounted(async () => {
  try {
    const res = await fetch(RELEASES_API)
    if (!res.ok) throw new Error(`GitHub API error: ${res.status}`)
    const releases = await res.json()

    // Latest stable = first non-prerelease, non-draft entry
    stableRelease.value = releases.find(r => !r.prerelease && !r.draft) ?? null

    // RCs = prerelease, non-draft, version tag contains -rc (case-insensitive)
    rcReleases.value = releases.filter(
      r => r.prerelease && !r.draft && /-rc\d*/i.test(r.tag_name)
    )
  } catch (e) {
    error.value = true
  } finally {
    loading.value = false
  }
})
</script>
