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

/**
 * 到达定位打卡：已接单 → 服务中。
 * 坐标距服务地址超出后端 pet-platform.geo.check-in-radius（默认 200 米）返回 2004，
 * 提示语里带着实际距离，直接弹给用户看即可。
 * @param {number} orderId
 * @param {{lat:number, lng:number}} data
 */
export function checkInOrder(orderId, data) {
  return request.post(`/sitter/order/${orderId}/checkin`, data)
}

/**
 * 上传作业清单单项存证。checkItem 必须来自该服务类别的清单，否则 400；
 * 订单不在「服务中」返回 2003；同一项重复提交是重拍覆盖，不会多出一条。
 * @param {number} orderId
 * @param {{checkItem:string, imageUrl:string, lat?:number, lng?:number, remark?:string}} data
 */
export function saveOrderEvidence(orderId, data) {
  return request.post(`/sitter/order/${orderId}/evidence`, data)
}

/**
 * 上传散步轨迹。points 为 [{lat,lng,time}]，time 缺省由服务端补当前时间；
 * 空数组 400，同一单可多次上传（分段遛）。
 * @param {number} orderId
 * @param {{points:Array<{lat:number,lng:number,time?:string}>, remark?:string}} data
 */
export function saveOrderTrack(orderId, data) {
  return request.post(`/sitter/order/${orderId}/track`, data)
}

/**
 * 标记服务完成：服务中 → 待验收。
 * 清单还有没拍照的项时返回 2008，提示语会列出缺哪几项。
 * @param {number} orderId
 */
export function finishOrder(orderId) {
  return request.post(`/sitter/order/${orderId}/finish`)
}
