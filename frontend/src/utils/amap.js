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
export const AMAP_PLUGINS = [
  'AMap.ToolBar',
  'AMap.Scale',
  'AMap.Geolocation',
  'AMap.PlaceSearch',
  'AMap.DistrictSearch'
]

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

/**
 * 按关键词检索中心点附近的 POI。
 * @param {string} keyword 地点关键词，支持单个汉字
 * @param {[number, number]} center [经度, 纬度]
 * @param {number} radiusMeters 半径（米），高德允许 0-50000
 * @returns {Promise<Array<{id:string,name:string,address:string,district:string,distance:number,lng:number,lat:number}>>}
 */
export async function searchNearbyPois(keyword, center, radiusMeters = 1000) {
  const query = keyword?.trim()
  const lng = Number(center?.[0])
  const lat = Number(center?.[1])
  if (!query || !Number.isFinite(lng) || !Number.isFinite(lat)) return []

  const AMap = await loadAMap()
  const placeSearch = new AMap.PlaceSearch({
    pageSize: 10,
    pageIndex: 1,
    extensions: 'base'
  })

  return new Promise((resolve, reject) => {
    placeSearch.searchNearBy(query, [lng, lat], radiusMeters, (status, result) => {
      if (status === 'no_data') {
        resolve([])
        return
      }
      if (status !== 'complete' || !result?.poiList?.pois) {
        reject(new Error(typeof result === 'string' ? result : result?.info || '附近地址检索失败'))
        return
      }

      resolve(normalizePois(result.poiList.pois))
    })
  })
}

/**
 * 按关键词检索地点，用于当前位置未知时主动填写一个备用位置。
 * @param {string} keyword 地点名称或地址
 * @param {string} city 城市名称或行政区编码，缺省为全国
 * @returns {Promise<Array<{id:string,name:string,address:string,district:string,distance:number,lng:number,lat:number}>>}
 */
export async function searchPois(keyword, city = '全国') {
  const query = keyword?.trim()
  if (!query) return []

  const AMap = await loadAMap()
  const placeSearch = new AMap.PlaceSearch({
    city,
    citylimit: city !== '全国',
    pageSize: 10,
    pageIndex: 1,
    extensions: 'base'
  })

  return new Promise((resolve, reject) => {
    placeSearch.search(query, (status, result) => {
      if (status === 'no_data') {
        resolve([])
        return
      }
      if (status !== 'complete' || !result?.poiList?.pois) {
        reject(new Error(typeof result === 'string' ? result : result?.info || '地址检索失败'))
        return
      }
      resolve(normalizePois(result.poiList.pois))
    })
  })
}

/**
 * 加载行政区的直属下级，用于省 / 市 / 区三级选择。
 * @param {string} keyword 中国、行政区名称或 adcode
 * @param {'country'|'province'|'city'} level 当前行政区级别
 * @returns {Promise<Array<{name:string,adcode:string,citycode:string,level:string}>>}
 */
export async function searchAdministrativeChildren(keyword, level) {
  const AMap = await loadAMap()
  const districtSearch = new AMap.DistrictSearch({
    level,
    subdistrict: 1,
    extensions: 'base'
  })

  return new Promise((resolve, reject) => {
    districtSearch.search(keyword, (status, result) => {
      const children = result?.districtList?.[0]?.districtList
      if (status === 'no_data' || !children?.length) {
        resolve([])
        return
      }
      if (status !== 'complete') {
        reject(new Error(typeof result === 'string' ? result : result?.info || '行政区加载失败'))
        return
      }
      resolve(children.map((item) => ({
        name: String(item.name || ''),
        adcode: String(item.adcode || ''),
        citycode: Array.isArray(item.citycode) ? item.citycode.join(',') : String(item.citycode || ''),
        level: String(item.level || '')
      })))
    })
  })
}

function normalizePois(pois) {
  return pois
    .map((poi) => {
      const poiLng = Number(poi.location?.getLng?.() ?? poi.location?.lng)
      const poiLat = Number(poi.location?.getLat?.() ?? poi.location?.lat)
      if (!Number.isFinite(poiLng) || !Number.isFinite(poiLat)) return null
      return {
        id: poi.id,
        name: String(poi.name || ''),
        address: Array.isArray(poi.address) ? poi.address.join('') : String(poi.address || ''),
        district: String(poi.district || ''),
        distance: Number(poi.distance),
        lng: poiLng,
        lat: poiLat
      }
    })
    .filter(Boolean)
}
