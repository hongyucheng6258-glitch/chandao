import request from '@/utils/request'

export function createCrudApi(resource) {
  return {
    page: (params) => request.get(`/${resource}/page`, { params }),
    detail: (id) => request.get(`/${resource}/${id}`),
    add: (data) => request.post(`/${resource}`, data),
    update: (id, data) => request.put(`/${resource}/${id}`, data),
    remove: (id) => request.delete(`/${resource}/${id}`)
  }
}
