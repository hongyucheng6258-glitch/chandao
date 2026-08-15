import request from '@/utils/request'

/** 通用 CRUD API 工厂 */
function crud(base) {
  return {
    page: (params) => request.get(`/${base}/page`, { params }),
    detail: (id) => request.get(`/${base}/${id}`),
    create: (data) => request.post(`/${base}`, data),
    update: (id, data) => request.put(`/${base}/${id}`, data),
    remove: (id) => request.delete(`/${base}/${id}`)
  }
}

export const userApi = {
  ...crud('users'),
  options: () => request.get('/users/options'),
  changeStatus: (id, status) => request.put(`/users/${id}/status`, { status })
}
export const roleApi = { ...crud('roles'), options: () => request.get('/roles/options') }
export const deptApi = {
  tree: () => request.get('/depts/tree'),
  create: (data) => request.post('/depts', data),
  update: (id, data) => request.put(`/depts/${id}`, data),
  remove: (id) => request.delete(`/depts/${id}`)
}
export const permApi = {
  tree: () => request.get('/perms/tree'),
  create: (data) => request.post('/perms', data),
  update: (id, data) => request.put(`/perms/${id}`, data),
  remove: (id) => request.delete(`/perms/${id}`)
}
export const logApi = {
  page: (params) => request.get('/logs/page', { params }),
  timeline: (params) => request.get('/logs/timeline', { params })
}
export const activityApi = {
  timeline: (objectType, objectId) => request.get('/logs/timeline', { params: { objectType: objectType, objectId } }),
  comment: (data) => request.post('/logs/comment', data),
  remove: (id) => request.delete(`/logs/${id}`)
}
export const attachmentApi = {
  list: (objectType, objectId) => request.get('/attachments', { params: { objectType, objectId } }),
  remove: (id) => request.delete(`/attachments/${id}`)
}

export const productApi = {
  ...crud('products'),
  options: () => request.get('/products/options'),
  plans: (id) => request.get(`/products/${id}/plans`),
  createPlan: (id, data) => request.post(`/products/${id}/plans`, data),
  removePlan: (planId) => request.delete(`/products/plans/${planId}`)
}
export const storyApi = {
  ...crud('stories'),
  options: (productId) => request.get('/stories/options', { params: { productId } }),
  flow: (id, action, closedReason) => request.put(`/stories/${id}/status`, { action, closedReason }),
  assign: (id, assignedTo) => request.put(`/stories/${id}/assign`, { assignedTo }),
  createTask: (id, data) => request.post(`/stories/${id}/tasks`, data),
  batchDelete: (ids) => request.post('/stories/batch-delete', { ids }),
  batchAssign: (ids, assignedTo) => request.post('/stories/batch-assign', { ids, assignedTo })
}
export const releaseApi = crud('releases')

export const projectApi = {
  ...crud('projects'),
  options: () => request.get('/projects/options'),
  flow: (id, action) => request.put(`/projects/${id}/status`, { action }),
  members: (id) => request.get(`/projects/${id}/members`),
  addMember: (id, data) => request.post(`/projects/${id}/members`, data),
  removeMember: (memberId) => request.delete(`/projects/members/${memberId}`)
}
export const sprintApi = {
  ...crud('sprints'),
  options: () => request.get('/sprints/options'),
  list: (projectId) => request.get('/sprints', { params: { projectId } }),
  detail: (id) => request.get(`/sprints/${id}`),
  create: (data) => request.post('/sprints', data),
  update: (id, data) => request.put(`/sprints/${id}`, data),
  remove: (id) => request.delete(`/sprints/${id}`),
  flow: (id, action) => request.put(`/sprints/${id}/status`, { action }),
  stories: (id) => request.get(`/sprints/${id}/stories`),
  linkStories: (id, storyIds) => request.post(`/sprints/${id}/stories`, { storyIds }),
  unlinkStory: (id, storyId) => request.delete(`/sprints/${id}/stories/${storyId}`)
}
export const taskApi = {
  ...crud('tasks'),
  board: (sprintId) => request.get('/tasks/board', { params: { sprintId } }),
  assign: (id, assignedTo) => request.put(`/tasks/${id}/assign`, { assignedTo }),
  flow: (id, action) => request.put(`/tasks/${id}/status`, { action }),
  logHours: (id, data) => request.put(`/tasks/${id}/hours`, data),
  move: (id, status) => request.put(`/tasks/${id}/move`, { status }),
  workhourSummary: (params) => request.get('/tasks/workhour-summary', { params })
}
export const bugApi = {
  ...crud('bugs'),
  assign: (id, assignedTo) => request.put(`/bugs/${id}/assign`, { assignedTo }),
  flow: (id, action, resolution) => request.put(`/bugs/${id}/status`, { action, resolution }),
  batchDelete: (ids) => request.post('/bugs/batch-delete', { ids }),
  batchAssign: (ids, assignedTo) => request.post('/bugs/batch-assign', { ids, assignedTo })
}
export const testcaseApi = crud('testcases')
export const testsuiteApi = {
  page: (params) => request.get('/test-suites/page', { params }),
  create: (data) => request.post('/test-suites', data),
  get: (id) => request.get(`/test-suites/${id}`),
  update: (id, data) => request.put(`/test-suites/${id}`, data),
  remove: (id) => request.delete(`/test-suites/${id}`),
  run: (id, data) => request.put(`/test-suites/${id}/run`, data)
}

export const dashboardApi = {
  summary: () => request.get('/dashboard/summary'),
  myTasks: () => request.get('/dashboard/my-tasks'),
  myBugs: () => request.get('/dashboard/my-bugs')
}
export const statsApi = {
  burndown: (sprintId) => request.get('/stats/burndown', { params: { sprintId } }),
  bugDistribution: (productId) => request.get('/stats/bug-distribution', { params: { productId } }),
  taskDistribution: (sprintId) => request.get('/stats/task-distribution', { params: { sprintId } })
}

export const searchApi = {
  search: (keyword, limit = 10) => request.get('/search', { params: { keyword, limit } })
}
