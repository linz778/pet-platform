import request from './request'

export const getInbox = (limit = 20) => request.get('/notifications', { params: { limit }, silent: true })
export const markNotificationRead = (id) => request.put(`/notifications/${id}/read`)
export const markAllNotificationsRead = () => request.put('/notifications/read-all')
