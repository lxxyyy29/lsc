/**
 * map-navigation.ts
 * Opens a native map navigation app to the given coordinates.
 *
 * On WeChat Mini Program: uses uni.openLocation (built-in).
 * On H5 (browser): opens AMap web navigation URL.
 *
 * Falls back silently if coordinates are invalid.
 */

const DEFAULT_LNG = 113.866
const DEFAULT_LAT = 22.982

export function openNavigation(lng: number, lat: number, name = '目的地'): void {
  if (!Number.isFinite(lng) || !Number.isFinite(lat)) {
    uni.showToast({ title: '坐标无效，无法导航', icon: 'none' })
    return
  }

  // #ifdef MP-WEIXIN
  uni.openLocation({
    longitude: lng,
    latitude: lat,
    name,
    fail() {
      uni.showToast({ title: '打开地图失败', icon: 'none' })
    }
  })
  // #endif

  // #ifndef MP-WEIXIN
  const url = `https://uri.amap.com/navigation?to=${lng},${lat},${encodeURIComponent(name)}&mode=car&callnative=1`
  // @ts-ignore - window is available in H5 context
  window.open(url, '_blank')
  // #endif
}

export { DEFAULT_LNG, DEFAULT_LAT }
