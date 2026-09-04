import request from './request'

export function listMyPets() {
  return request.get('/pet/my')
}

export function getPet(id) {
  return request.get(`/pet/${id}`)
}

export function createPet(data) {
  return request.post('/pet', data)
}

export function updatePet(id, data) {
  return request.put(`/pet/${id}`, data)
}

export function deletePet(id) {
  return request.delete(`/pet/${id}`)
}
