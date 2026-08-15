import { defineStore } from 'pinia'
import { login as loginApi, getInfo, logout as logoutApi } from '@/api/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    id: null,
    username: '',
    realName: '',
    avatar: '',
    roles: [],
    perms: [],
    menus: []
  }),
  getters: {
    isAdmin: (state) => state.perms.includes('*:*:*')
  },
  actions: {
    async login(username, password) {
      const res = await loginApi({ username, password })
      this.token = res.data.token
      localStorage.setItem('token', this.token)
    },
    async fetchInfo() {
      const res = await getInfo()
      this.id = res.data.id
      this.username = res.data.username
      this.realName = res.data.realName
      this.avatar = res.data.avatar
      this.roles = res.data.roles || []
      this.perms = res.data.perms || []
      this.menus = res.data.menus || []
    },
    async logout() {
      try {
        await logoutApi()
      } finally {
        this.reset()
      }
    },
    reset() {
      this.token = ''
      this.id = null
      this.username = ''
      this.realName = ''
      this.roles = []
      this.perms = []
      this.menus = []
      localStorage.removeItem('token')
    },
    hasPerm(key) {
      return this.perms.includes('*:*:*') || this.perms.includes(key)
    }
  }
})
