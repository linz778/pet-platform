import request from './request'

export function pageSitterAudits(params) {
  return request.get('/admin/sitter-audit/page', { params })
}

export function decideSitterAudit(profileId, data) {
  return request.post(`/admin/sitter-audit/${profileId}/decision`, data)
}
