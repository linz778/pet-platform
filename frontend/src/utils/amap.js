import AMapLoader from '@amap/amap-jsapi-loader'

// 高德 JS API 2.0 安全密钥需在加载前挂到 window._AMapSecurityConfig
const SECURITY_CODE = import.meta.env.VITE_AMAP_SECURITY_CODE
if (SECURITY_CODE) {
  window._AMapSecurityConfig = { securityJsCode: SECURITY_CODE }
}

let amapPromise = null

/**
 * 全应用唯一的高德插件清单，AmapView 默认使用它。
 *
 * loadAMap 用 amapPromise 做了单例缓存，插件只在全应用【首次】调用时生效，
 * 之后任何页面传入的 plugins 都会被静默忽略。所以插件必须集中声明在这里，
 * 页面不要各自传一份看似生效、实则无效的列表。
 *
 * Marker / InfoWindow / Circle / Polyline 在 JS API 2.0 属于核心包，无需在此声明。
 */
export const AMAP_PLUGINS = ['AMap.ToolBar', 'AMap.Scale', 'AMap.Geolocation']

/**
 * 加载高德地图 JS API 2.0（单例，避免重复注入脚本）。
 * @param {string[]} plugins 需要额外加载的插件，缺省用 AMAP_PLUGINS
 * @returns {Promise<typeof AMap>}
 */
export function loadAMap(plugins = AMAP_PLUGINS) {
  if (!import.meta.env.VITE_AMAP_KEY) {
    return Promise.reject(new Error('未配置高德地图 key，请在 .env 中设置 VITE_AMAP_KEY'))
  }
  if (!amapPromise) {
    amapPromise = AMapLoader.load({
      key: import.meta.env.VITE_AMAP_KEY,
      version: '2.0',
      plugins
    })
  }
  return amapPromise
}

/**
 * 获取浏览器当前定位（经纬度）。用于接单员上传当前位置、LBS 检索周边订单。
 * @returns {Promise<{lng:number,lat:number}>}
 */
export function getCurrentPosition() {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error('当前浏览器不支持定位'))
      return
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => resolve({ lng: pos.coords.longitude, lat: pos.coords.latitude }),
      (err) => reject(err),
      { enableHighAccuracy: true, timeout: 10000 }
    )
  })
}
