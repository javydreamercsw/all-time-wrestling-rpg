import { defineConfig } from 'vitepress'

export default defineConfig({
  title: "All Time Wrestling RPG",
  description: "The comprehensive game guide.",
  base: "/atw-rpg/docs/", 
  themeConfig: {
    nav: [
      { text: 'Home', link: '/' },
      { text: 'Guide', link: '/guide/campaign' }
    ],

    sidebar: [
      {
        text: 'Game Guide',
        items: [
          { text: 'Campaign', link: '/guide/campaign' }
        ]
      }
    ],

    socialLinks: [
      { icon: 'github', link: 'https://github.com/javydreamercsw/all-time-wrestling-rpg' }
    ]
  }
})
