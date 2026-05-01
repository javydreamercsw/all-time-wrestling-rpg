import { defineConfig } from 'vitepress'

const releaseVersion = process.env.RELEASE_VERSION || '1.8.0-SNAPSHOT'
const releaseTag = `v${releaseVersion}`
const githubBase = 'https://github.com/javydreamercsw/all-time-wrestling-rpg/releases/download'

export default defineConfig({
  title: "All Time Wrestling RPG",
  description: "The comprehensive game guide.",
  base: process.env.BASE_URL ? (process.env.BASE_URL.startsWith('/') ? process.env.BASE_URL : `/${process.env.BASE_URL}`) : "/",
  define: {
    __RELEASE_VERSION__: JSON.stringify(releaseVersion),
    __RELEASE_TAG__: JSON.stringify(releaseTag),
    __GITHUB_DOWNLOAD_BASE__: JSON.stringify(`${githubBase}/${releaseTag}`),
  },
  themeConfig: {
    nav: [
      { text: 'Home', link: '/' },
      { text: 'Guide', link: '/guide/campaign' }
    ],

    sidebar: [
      {
        text: 'Overview',
        items: [
          { text: 'Game Mechanics', link: '/guide/game-mechanics' },
          { text: 'AI Features', link: '/guide/ai-features' }
        ]
      },
      {
        text: 'Game Modes',
        items: [
          { text: 'Campaign Mode', link: '/guide/campaign' },
          { text: 'Booker Guide', link: '/guide/booker' },
          { text: 'GM Mode', link: '/guide/general-manager' },
          { text: 'Leagues', link: '/guide/leagues' }
        ]
      },
      {
        text: 'Dashboards & Features',
        items: [
          { text: 'Dashboards', link: '/guide/dashboards' },
          { text: 'Player Dashboard', link: '/guide/player-dashboard' },
          { text: 'News', link: '/guide/news' },
          { text: 'Wrestler Profile', link: '/guide/wrestler' },
          { text: 'NPCs', link: '/guide/npc' }
        ]
      },
      {
        text: 'Management',
        items: [
          { text: 'Entities', link: '/guide/entities' },
          { text: 'Admin Tools', link: '/guide/admin' },
          { text: 'User Settings', link: '/guide/user-settings' }
        ]
      }
    ],

    socialLinks: [
      { icon: 'github', link: 'https://github.com/javydreamercsw/all-time-wrestling-rpg' }
    ]
  }
})