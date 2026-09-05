import request from './request'

export function listCategories() {
  return request.get('/service-category/list')
}

export function getCategory(id) {
  return request.get(`/service-category/${id}`)
}

/**
 * 计价预览，返回 { categoryId, categoryName, serviceStart, holiday, amount, commission, sitterIncome }。
 * @param {number} categoryId
 * @param {string} serviceStart 传 'yyyy-MM-dd HH:mm:ss'，即 el-date-picker 默认吐出的格式。
 *                              后端标了 @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")，
 *                              实测空格分隔与 ISO 带 T 两种写法都能解析；摘掉该注解后只有 ISO 能过。
 */
export function previewPrice(categoryId, serviceStart) {
  return request.get('/service-category/price-preview', { params: { categoryId, serviceStart } })
}
