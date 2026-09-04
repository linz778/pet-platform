import request from './request'

/**
 * 上传图片，返回 FileVO { url, name, size }。
 * 走 axios 实例而非 el-upload 的 action：一是能自动带上 Authorization，
 * 二是本项目 HTTP status 恒为 200，el-upload 按状态码判成功会把业务失败当成上传成功。
 * @param {File|Blob} file 原始文件
 * @param {string} bizType 业务目录，可选 pet/evidence/cert/avatar/common
 */
export function uploadImage(file, bizType = 'common') {
  const form = new FormData()
  form.append('file', file)
  // 不手动设置 Content-Type，交给浏览器生成带 boundary 的 multipart 头
  return request.post('/file/upload', form, { params: { bizType } })
}
