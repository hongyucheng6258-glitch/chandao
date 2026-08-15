import request from '@/utils/request'

export function getBurndown(sprintId) {
  return request.get('/stats/burndown', { params: { sprintId } })
}

export function getBugDistribution() {
  return request.get('/stats/bug-distribution')
}
