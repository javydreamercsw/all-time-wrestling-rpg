import DefaultTheme from 'vitepress/theme'
import DownloadLinks from './DownloadLinks.vue'

export default {
  extends: DefaultTheme,
  enhanceApp({ app }) {
    app.component('DownloadLinks', DownloadLinks)
  }
}
