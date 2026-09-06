import request from './request'

/** 提交当前登录用户对订单另一方的评价；被评价人由后端按订单关系确定 */
export function submitOrderReview(data) {
  return request.post('/review', data)
}

/** 查询某笔订单的双方评价 */
export function listOrderReviews(orderId) {
  return request.get(`/review/order/${orderId}`)
}

/** 查询当前登录用户收到的评价 */
export function pageReceivedReviews(params) {
  return request.get('/review/received/page', { params })
}
