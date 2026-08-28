import { computed } from 'vue'

/**
 * 自定义导航栏（navigationStyle: 'custom'）页面的状态栏占位高度。
 * - 小程序端：返回真实状态栏高度（px），避免内容顶到状态栏
 * - H5 端：返回 0，不影响浏览器布局
 *
 * 用法：根元素绑定 :style="{ paddingTop: statusBarPadding }"
 */
export function useStatusBar() {
  const statusBarHeight = (() => {
    // #ifdef MP-WEIXIN
    try {
      return uni.getSystemInfoSync().statusBarHeight || 0
    } catch {
      return 0
    }
    // #endif
    // #ifndef MP-WEIXIN
    return 0
    // #endif
  })()

  const statusBarPadding = computed(() =>
    statusBarHeight > 0 ? `${statusBarHeight}px` : '0'
  )

  return { statusBarHeight, statusBarPadding }
}
