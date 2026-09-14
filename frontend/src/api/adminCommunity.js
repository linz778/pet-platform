import request from './request'

export function pageCommunityPosts(params) {
  return request.get('/admin/community/posts/page', { params })
}

export function listCommunityComments(postId) {
  return request.get(`/admin/community/posts/${postId}/comments`)
}

export function setCommunityPostStatus(id, status) {
  return request.put(`/admin/community/posts/${id}/status`, null, { params: { status } })
}

export function setCommunityCommentStatus(id, status) {
  return request.put(`/admin/community/comments/${id}/status`, null, { params: { status } })
}
