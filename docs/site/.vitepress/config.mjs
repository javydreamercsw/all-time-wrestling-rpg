import { defineConfig } from 'vitepress'
import sidebar from './sidebar.json'

export default defineConfig({
  title: "All Time Wrestling RPG",
  description: "The comprehensive game guide.",
  base: process.env.BASE_URL ? (process.env.BASE_URL.startsWith('/') ? process.env.BASE_URL : `/${process.env.BASE_URL}`) : "/",
  themeConfig: {
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