import request from './request'

export function pageDispatchOrders(params) {
  return request.get('/admin/dispatch/page', { params })
}

export function listAssignableSitters(orderId) {
  return request.get(`/admin/dispatch/${orderId}/sitters`)
}

export function assignOrder(orderId, sitterId) {
  return request.post(`/admin/dispatch/${orderId}/assign/${sitterId}`)
}
