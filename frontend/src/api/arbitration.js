import request from './request'

/** 用户对待验收订单提交服务申诉，evidenceUrls 为 1-5 张已上传图片地址。 */
export function submitArbitration(orderId, data) {
  return request.post(`/arbitration/order/${orderId}`, data)
}

/** 当事人双方查看某个订单的申诉；未申诉时返回 null。 */
export function getOrderArbitration(orderId) {
  return request.get(`/arbitration/order/${orderId}`)
}

/** 管理端申诉分页。 */
export function pageArbitrations(params) {
  return request.get('/arbitration/admin/page', { params })
}

/** 管理员审核；approved=true 全额退款，false 驳回并恢复待验收。 */
export function decideArbitration(id, data) {
  return request.post(`/arbitration/${id}/decision`, data)
}
