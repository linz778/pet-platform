import request from './request'

export const pageUsers = (params) => request.get('/admin/accounts/users/page', { params })

export const setUserStatus = (id, status) => request.put(`/admin/accounts/users/${id}/status`, null, { params: { status } })
