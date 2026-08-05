/**
 * H5 定位工具：优先使用浏览器精确定位（uni.getLocation），
 * 在非 HTTPS 环境被浏览器拒绝时，回退到高德 IP 定位（城市级大致位置）。
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

function locateByUni(): Promise<LocateResult> {
  return new Promise((resolve, reject) => {
    uni.getLocation({
      type: 'gcj02',
      success: (res) => {
        resolve({
          latitude: res.latitude,
          longitude: res.longitude,
          precise: true,
          sourceText: '精确定位'
        })
      },
      fail: (err) => reject(err)
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
      citySearch.getLocalPosition((status: string, result: any) => {
        if (status === 'complete' && result?.bounds) {
          const bounds = result.bounds
          const latitude = (bounds.getSouthWest().getLat() + bounds.getNorthEast().getLat()) / 2
          const longitude = (bounds.getSouthWest().getLng() + bounds.getNorthEast().getLng()) / 2
          resolve({
            latitude,
            longitude,
            precise: false,
            sourceText: `${result.city || '当前位置'}（大致定位）`
          })
        } else {
          reject(new Error('IP 定位失败'))
        }
      })
    } catch (e) {
      reject(e)
    }
  })
}

/**
 * 获取当前位置：先尝试精确定位，失败则回退 IP 定位；两者都失败时抛出异常
 */
export async function locateWithFallback(): Promise<LocateResult> {
  try {
    return await locateByUni()
  } catch {
    return await locateByAmapIp()
  }
}
