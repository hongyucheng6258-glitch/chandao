import { createCrudApi } from './crud'
import request from '@/utils/request'

const baseApi = createCrudApi('users')

// 上传头像
export const uploadAvatar = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/users/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 获取当前用户信息
export const getCurrentUser = () => request.get('/users/me')

export default {
  ...baseApi,
  uploadAvatar,
  getCurrentUser
}
