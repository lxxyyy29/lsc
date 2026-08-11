/**
 * H5 定位工具：优先使用浏览器原生定位（navigator.geolocation，仅 HTTPS 安全上下文可用），
 * 失败时回退 uni.getLocation，再失败回退高德 IP 定位（城市级大致位置）。
 */

export interface LocateResult {
  latitude: number
  longitude: number
  /** 是否为精确定位（false 表示 IP 大致定位） */
  precise: boolean
  /** 定位来源描述 */
  sourceText: string
}

const AMAP_KEY = '5e00e01d2d2b6ca9e1eed533a15572e4'
const AMAP_SECURITY_CODE = '0a57a5453a660300283bebf7323d8bce'

/** WGS84(浏览器原生) 转 GCJ02(高德/国内地图)，国内范围外原样返回 */
function wgs84ToGcj02(wgsLat: number, wgsLng: number): { latitude: number; longitude: number } {
  const a = 6378245.0
  const ee = 0.00669342162296594323
  const outOfChina = wgsLng < 72.004 || wgsLng > 137.8347 || wgsLat < 0.8293 || wgsLat > 55.8271
  if (outOfChina) return { latitude: wgsLat, longitude: wgsLng }
  let dLat = -100.0 + 2.0 * (wgsLng - 105.0) + 3.0 * (wgsLat - 35.0) + 0.2 * (wgsLat - 35.0) ** 2 +
    0.1 * (wgsLat - 35.0) * (wgsLng - 105.0) + 0.2 * Math.sqrt(Math.abs(wgsLng - 105.0))
  dLat += (20.0 * Math.sin(6.0 * (wgsLng - 105.0) * Math.PI) + 20.0 * Math.sin(2.0 * (wgsLat - 35.0) * Math.PI)) * 2.0 / 3.0
  dLat += (20.0 * Math.sin((wgsLat - 35.0) * Math.PI) + 40.0 * Math.sin((wgsLat - 35.0) / 3.0 * Math.PI)) * 2.0 / 3.0
  dLat += (160.0 * Math.sin((wgsLat - 35.0) / 12.0 * Math.PI) + 320 * Math.sin((wgsLat - 35.0) * Math.PI / 30.0)) * 2.0 / 3.0
  let dLng = 300.0 + (wgsLng - 105.0) + 2.0 * (wgsLat - 35.0) + 0.1 * (wgsLng - 105.0) ** 2 +
    0.1 * (wgsLng - 105.0) * (wgsLat - 35.0) + 0.1 * Math.sqrt(Math.abs(wgsLng - 105.0))
  dLng += (20.0 * Math.sin(6.0 * (wgsLng - 105.0) * Math.PI) + 20.0 * Math.sin(2.0 * (wgsLng - 105.0) * Math.PI)) * 2.0 / 3.0
  dLng += (20.0 * Math.sin((wgsLng - 105.0) * Math.PI) + 40.0 * Math.sin((wgsLng - 105.0) / 3.0 * Math.PI)) * 2.0 / 3.0
  dLng += (150.0 * Math.sin((wgsLng - 105.0) / 12.0 * Math.PI) + 300.0 * Math.sin((wgsLng - 105.0) / 30.0 * Math.PI)) * 2.0 / 3.0
  const radLat = wgsLat / 180.0 * Math.PI
  let magic = Math.sin(radLat)
  magic = 1 - ee * magic * magic
  const sqrtMagic = Math.sqrt(magic)
  dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * Math.PI)
  dLng = (dLng * 180.0) / (a / sqrtMagic * Math.cos(radLat) * Math.PI)
  return { latitude: wgsLat + dLat, longitude: wgsLng + dLng }
}

/** 浏览器原生定位（HTTPS 安全上下文），返回 GCJ02 坐标 */
function locateByBrowser(): Promise<LocateResult> {
  return new Promise((resolve, reject) => {
    if (typeof navigator === 'undefined' || !navigator.geolocation) {
      reject(new Error('浏览器不支持定位'))
      return
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const gcj = wgs84ToGcj02(pos.coords.latitude, pos.coords.longitude)
        resolve({
          latitude: gcj.latitude,
          longitude: gcj.longitude,
          precise: true,
          sourceText: `精确定位（精度约 ${Math.round(pos.coords.accuracy)} 米）`
        })
      },
      (err) => reject(err),
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 30000 }
    )
  })
}

function locateByUni(): Promise<LocateResult> {
  return new Promise((resolve, reject) => {
    // 小程序端：全局 uni 不存在，使用微信全局 wx
    // #ifdef MP-WEIXIN
    const uniRef = (globalThis as { wx?: { getLocation?: unknown } }).wx
    // #endif
    // #ifndef MP-WEIXIN
    const uniRef = (globalThis as { uni?: { getLocation?: unknown } }).uni
    // #endif
    if (!uniRef || typeof uniRef.getLocation !== 'function') {
      reject(new Error('uni 定位不可用'))
      return
    }
    uniRef.getLocation({
      type: 'gcj02',
      success: (res: any) => {
        resolve({
          latitude: res.latitude,
          longitude: res.longitude,
          precise: true,
          sourceText: '精确定位'
        })
      },
      fail: (err: any) => reject(err)
    })
  })
}

function loadAMap(): Promise<any> {
  const w = globalThis as any
  if (w.AMap) return Promise.resolve(w.AMap)
  w._AMapSecurityConfig = { securityJsCode: AMAP_SECURITY_CODE }
  return new Promise((resolve, reject) => {
    if (typeof document === 'undefined') {
      reject(new Error('非浏览器环境'))
      return
    }
    const script = document.createElement('script')
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${AMAP_KEY}&plugin=AMap.CitySearch`
    script.onload = () => (w.AMap ? resolve(w.AMap) : reject(new Error('AMap 加载失败')))
    script.onerror = () => reject(new Error('AMap 脚本加载失败'))
    document.head.appendChild(script)
  })
}

async function locateByAmapIp(): Promise<LocateResult> {
  const AMap = await loadAMap()
  return await new Promise((resolve, reject) => {
    try {
      const citySearch = new AMap.CitySearch()
      // AMap 2.0 的 CitySearch 实例方法是 getLocalCity（1.x 的 getLocalPosition 已不存在）
      citySearch.getLocalCity((status: string, result: any) => {
        if (status === 'complete' && result?.infocode === '10000') {
          // result.rectangle 格式："lng1,lat1;lng2,lat2"（左下;右上）
          if (result.rectangle) {
            const [sw, ne] = String(result.rectangle).split(';')
            const [lng1, lat1] = sw.split(',').map(Number)
            const [lng2, lat2] = ne.split(',').map(Number)
            resolve({
              latitude: (lat1 + lat2) / 2,
              longitude: (lng1 + lng2) / 2,
              precise: false,
              sourceText: `${result.city || '当前位置'}（大致定位）`
            })
            return
          }
        }
        reject(new Error('IP 定位失败'))
      })
    } catch (e) {
      reject(e)
    }
  })
}

/**
 * 获取当前位置：浏览器原生定位(HTTPS) → uni 定位 → 高德 IP 定位；全部失败时抛出异常
 * 小程序端：直接使用 uni.getLocation（navigator/document/AMap 均不可用）
 */
export async function locateWithFallback(): Promise<LocateResult> {
  // #ifdef MP-WEIXIN
  return await locateByUni()
  // #endif
  // #ifndef MP-WEIXIN
  try {
    return await locateByBrowser()
  } catch {
    // 继续回退
  }
  try {
    return await locateByUni()
  } catch {
    return await locateByAmapIp()
  }
  // #endif
}
