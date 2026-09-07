import request from './request'

export function pageCommunityPosts(params) {
  return request.get('/community/posts', { params })
}

export function getCommunityPost(id) {
  return request.get(`/community/posts/${id}`)
}

export function createCommunityPost(data) {
  return request.post('/community/posts', data)
}

export function listCommunityComments(id) {
  return request.get(`/community/posts/${id}/comments`)
}

export function createCommunityComment(id, content) {
  return request.post(`/community/posts/${id}/comments`, { content })
}

export function toggleCommunityLike(id) {
  return request.post(`/community/posts/${id}/like`)
}
