import { defineConfig } from 'vitepress'

export default defineConfig({
  title: "All Time Wrestling RPG",
  description: "The comprehensive game guide.",
  base: process.env.BASE_URL || "/docs/", 
  themeConfig: {
    nav: [
      { text: 'Home', link: '/' },
      { text: 'Guide', link: '/guide/campaign' }
    ],

    sidebar: [
      {
        text: 'Game Guide',
        items: [
          { text: 'Admin Tools', link: '/guide/admin' },
          { text: 'Booker Guide', link: '/guide/booker' },
          { text: 'Campaign Mode', link: '/guide/campaign' },
          { text: 'Wrestler Profile', link: '/guide/wrestler' }
        ]
      }
    ],

    socialLinks: [
      { icon: 'github', link: 'https://github.com/javydreamercsw/all-time-wrestling-rpg' }
    ]
  }
})