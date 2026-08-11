<script setup lang="ts">
import { onLaunch, onShow } from '@dcloudio/uni-app'
import { fetchCurrentH5User, recoverH5Session } from './src/api/auth'
// #ifndef MP-WEIXIN
import { fetchAccessPrefix } from './src/api/upload'
// #endif

async function bootstrapSession() {
  await recoverH5Session(fetchCurrentH5User)
  // #ifndef MP-WEIXIN
  // H5 浏览器：预取 OSS 访问前缀，确保图片 URL 在页面渲染前能正确解析
  fetchAccessPrefix().catch(() => {})
  // #endif
}

onLaunch(() => {
  void bootstrapSession()
})

onShow(() => {
  void bootstrapSession()
})
</script>

<style>
@import './src/styles/figma-tokens.css';
@import './src/styles/h5-shared.css';

/* H5 网格员端（深色主题）：深色背景 + 浅色文字 */
/* #ifndef MP-WEIXIN */
page {
  background: linear-gradient(180deg, #030913 0%, #06121f 38%, #06101a 100%);
  color: #eaf5ff;
}
/* #endif */

/* 小程序居民端（浅色主题）：默认文字用深色，避免浅色文字看不清 */
/* #ifdef MP-WEIXIN */
page {
  background: #f5f7fa;
  color: #1f2937;
}
/* #endif */
</style>
