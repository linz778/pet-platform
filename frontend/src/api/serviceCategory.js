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
 * @param {string} serviceStart 必须是 'yyyy-MM-dd HH:mm:ss'，后端 @DateTimeFormat 只认这个格式，
 *                              传 ISO 带 T 的串会直接 400
 */
export function previewPrice(categoryId, serviceStart) {
  return request.get('/service-category/price-preview', { params: { categoryId, serviceStart } })
}
