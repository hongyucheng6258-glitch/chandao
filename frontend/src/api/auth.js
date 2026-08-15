import request from '@/utils/request'

export const login = (data) => request.post('/auth/login', data)
export const getInfo = () => request.get('/auth/info')
export const logout = () => request.post('/auth/logout')
