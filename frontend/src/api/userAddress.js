import request from './request'

/** 当前用户的服务地址簿，默认地址排在第一项。 */
export function listUserAddresses() {
  return request.get('/user/address')
}

export function createUserAddress(data) {
  return request.post('/user/address', data)
}

export function updateUserAddress(id, data) {
  return request.put(`/user/address/${id}`, data)
}

export function setDefaultUserAddress(id) {
  return request.post(`/user/address/${id}/default`)
}

export function deleteUserAddress(id) {
  return request.delete(`/user/address/${id}`)
}
