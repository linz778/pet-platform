import request from './request'

export function listServiceRules() {
  return request.get('/admin/service-rules')
}

export function updateServiceRule(categoryId, data) {
  return request.put(`/admin/service-rules/${categoryId}`, data)
}
