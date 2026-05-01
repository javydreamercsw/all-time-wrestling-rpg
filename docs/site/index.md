---
layout: home

hero:
  name: "All Time Wrestling RPG"
  text: "Game Guide"
  tagline: "The comprehensive guide to ATW RPG features and mechanics."
  actions:
    - theme: brand
      text: Get Started
      link: /guide/campaign
    - theme: alt
      text: View on GitHub
      link: https://github.com/javydreamercsw/all-time-wrestling-rpg

features:
  - title: Campaign Mode
    details: Lead your wrestler to glory in an immersive single-player campaign.
  - title: Booking Mode
    details: Take control of the promotion and book the ultimate card.
  - title: Multiplayer
    details: Challenge other players in online leagues and tournaments.

---

## Download & Play

Non-technical users can download the game for their platform below. No complex setup required!

::: tip Recommended
For the best experience, use the **Native Installer** for your operating system.
:::

<div style="display: flex; gap: 10px; flex-wrap: wrap; margin-top: 20px;">
  <a :href="`${__GITHUB_DOWNLOAD_BASE__}/All.Time.Wrestling-${__RELEASE_VERSION__}.msi`" class="vplug-button brand">Download for Windows (.msi)</a>
  <a :href="`${__GITHUB_DOWNLOAD_BASE__}/All.Time.Wrestling-${__RELEASE_VERSION__}.dmg`" class="vplug-button brand">Download for macOS (.dmg)</a>
  <a :href="`${__GITHUB_DOWNLOAD_BASE__}/all-time-wrestling-rpg-${__RELEASE_VERSION__}.deb`" class="vplug-button brand">Download for Linux (.deb)</a>
</div>

### Other Formats

*   **Portable ZIP**: Download, unzip, and run without installation. <a :href="`${__GITHUB_DOWNLOAD_BASE__}/all-time-wrestling-rpg-${__RELEASE_VERSION__}.zip`">Get Portable Version</a>
*   **Docker**: For advanced users. `docker pull javydreamercsw/all-time-wrestling-rpg`