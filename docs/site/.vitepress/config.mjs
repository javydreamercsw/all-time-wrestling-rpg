import { defineConfig } from 'vitepress'
import sidebar from './sidebar.json'

const releaseVersion = process.env.RELEASE_VERSION || '1.8.0-SNAPSHOT'
const releaseTag = `v${releaseVersion}`
const githubBase = 'https://github.com/javydreamercsw/all-time-wrestling-rpg/releases/download'

export default defineConfig({
  title: "All Time Wrestling RPG",
  description: "The comprehensive game guide.",
  base: process.env.BASE_URL ? (process.env.BASE_URL.startsWith('/') ? process.env.BASE_URL : `/${process.env.BASE_URL}`) : "/",
  themeConfig: {
    releaseVersion,
    downloadBase: `${githubBase}/${releaseTag}`,
    nav: [
      { text: 'Home', link: '/' },
      { text: 'Guide', link: sidebar.length > 0 ? sidebar[0].items[0].link : '/guide/campaign' }
    ],

    sidebar: sidebar,

    socialLinks: [
      { icon: 'github', link: 'https://github.com/javydreamercsw/all-time-wrestling-rpg' }
    ]
  }
})