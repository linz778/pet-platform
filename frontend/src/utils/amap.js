import AMapLoader from '@amap/amap-jsapi-loader'

// 高德 JS API 2.0 安全密钥需在加载前挂到 window._AMapSecurityConfig
const SECURITY_CODE = import.meta.env.VITE_AMAP_SECURITY_CODE
if (SECURITY_CODE) {
  window._AMapSecurityConfig = { securityJsCode: SECURITY_CODE }
}

let amapPromise = null

/**
 * 加载高德地图 JS API 2.0（单例，避免重复注入脚本）。
 * @param {string[]} plugins 需要额外加载的插件，如 ['AMap.Geolocation','AMap.Marker']
 * @returns {Promise<typeof AMap>}
 */
export function loadAMap(plugins = []) {
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
