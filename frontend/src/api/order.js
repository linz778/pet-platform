import request from './request'

/** 下单，返回订单详情 */
export function createOrder(data) {
  return request.post('/order', data)
}

/**
 * 我的订单分页。
 * @param {{page?:number,size?:number,status?:number}} params status 为空表示全部
 */
export function pageMyOrders(params) {
  return request.get('/order/my/page', { params })
}

/** 订单详情。commission / sitterIncome 只对管理员与该单接单员返回，用户侧这两个键压根不存在 */
export function getOrder(id) {
  return request.get(`/order/${id}`)
}

/** 支付（模拟）：余额转入平台担保冻结 */
export function payOrder(id) {
  return request.post(`/order/${id}/pay`)
}

/** 取消订单。仅待支付 / 待接单可取消，已支付的全额退回余额 */
export function cancelOrder(id, reason) {
  return request.post(`/order/${id}/cancel`, reason ? { reason } : {})
}
