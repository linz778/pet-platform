import request from './request'

/** 管理端真实经营看板，days 支持 7 / 14 / 30。 */
export function getAdminDashboard(days = 7) {
  return request.get('/admin/dashboard', { params: { days } })
}
