import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    component: () => import('@/views/login/index.vue')
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('@/views/dashboard/index.vue'), meta: { title: '工作台' } },
      { path: 'product/list', component: () => import('@/views/product/list.vue'), meta: { title: '产品列表' } },
      { path: 'product/story', component: () => import('@/views/product/story.vue'), meta: { title: '需求管理' } },
      { path: 'product/release', component: () => import('@/views/product/release.vue'), meta: { title: '发布管理' } },
      { path: 'project/list', component: () => import('@/views/project/list.vue'), meta: { title: '项目列表' } },
      { path: 'project/detail/:id', component: () => import('@/views/project/detail.vue'), meta: { title: '项目详情' } },
      { path: 'project/board', component: () => import('@/views/project/board.vue'), meta: { title: '迭代看板' } },
      { path: 'qa/bug', component: () => import('@/views/qa/bug.vue'), meta: { title: 'Bug管理' } },
      { path: 'qa/case', component: () => import('@/views/qa/case.vue'), meta: { title: '测试用例' } },
      { path: 'qa/suite', component: () => import('@/views/qa/suite.vue'), meta: { title: '测试单' } },
      { path: 'stats', component: () => import('@/views/stats/index.vue'), meta: { title: '统计报表' } },
      { path: 'search', component: () => import('@/views/search/index.vue'), meta: { title: '全局搜索' } },
      { path: 'system/user', component: () => import('@/views/system/user.vue'), meta: { title: '用户管理' } },
      { path: 'system/role', component: () => import('@/views/system/role.vue'), meta: { title: '角色管理' } },
      { path: 'system/perm', component: () => import('@/views/system/perm.vue'), meta: { title: '权限管理' } },
      { path: 'system/dept', component: () => import('@/views/system/dept.vue'), meta: { title: '部门管理' } },
      { path: 'system/log', component: () => import('@/views/system/log.vue'), meta: { title: '操作日志' } },
      { path: 'system/config', component: () => import('@/views/system/Config.vue'), meta: { title: '系统配置' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (!token && to.path !== '/login') {
    return '/login'
  }
  if (token && to.path === '/login') {
    return '/dashboard'
  }
})

export default router
