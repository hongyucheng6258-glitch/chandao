import request from '@/utils/request'

// 获取全部配置详情(管理页面用)
export function getConfigList() {
  return request({ url: '/configs', method: 'get' })
}

// 获取全部配置键值对(运行时读取)
export function getConfigValues() {
  return request({ url: '/configs/values', method: 'get' })
}

// 批量更新配置
export function updateConfig(data) {
  return request({ url: '/configs', method: 'put', data })
}

// 刷新缓存
export function refreshConfig() {
  return request({ url: '/configs/refresh', method: 'post' })
}
