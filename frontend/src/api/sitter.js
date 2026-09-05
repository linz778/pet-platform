import request from './request'

/**
 * 我的资质档案。
 * 身份证号只会拿到脱敏值（idCardMasked），未填写时该键压根不存在（Jackson non_null）。
 */
export function getMySitterProfile() {
  return request.get('/sitter/profile')
}

/** 提交 / 重新提交资质。提交后一律回到待审状态；已通过的档案后端会拒绝（1007） */
export function submitSitterProfile(data) {
  return request.post('/sitter/profile', data)
}

/**
 * 接单大厅：以当前坐标为圆心按距离升序检索附近的待接单订单。
 * 列表里没有下单用户的身份与备注，抢到单之后从订单详情里取。
 * @param {{lng:number, lat:number, radiusKm:number, page?:number, size?:number}} params
 */
export function pageHallOrders(params) {
  return request.get('/sitter/hall/page', { params })
}

/** 抢单。被抢先返回 2002，资质未过审返回 1005，订单已取消返回 2003 */
export function grabOrder(orderId) {
  return request.post(`/sitter/hall/${orderId}/grab`)
}

/**
 * 我抢到的订单分页，带下单用户昵称。
 * @param {{page?:number, size?:number, status?:number}} params status 为空表示全部
 */
export function pageMyTakenOrders(params) {
  return request.get('/sitter/order/page', { params })
}
