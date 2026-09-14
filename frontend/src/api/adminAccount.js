import request from './request'

export const pageUsers = (params) => request.get('/admin/accounts/users/page', { params })

export const setUserStatus = (id, status) => request.put(`/admin/accounts/users/${id}/status`, null, { params: { status } })

export const pageSitters = (params) => request.get('/admin/accounts/sitters/page', { params })

export const setSitterStatus = (id, status) => request.put(`/admin/accounts/sitters/${id}/status`, null, { params: { status } })

export const setSitterAvailable = (id, status) => request.put(`/admin/accounts/sitters/${id}/available`, null, { params: { status } })
