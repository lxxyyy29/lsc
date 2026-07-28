import { createRouter, createWebHashHistory } from 'vue-router'
import { getSession } from '../api'

const routes = [
  { path: '/login', component: () => import('../views/LoginView.vue') },
  { path: '/register', component: () => import('../views/RegisterView.vue') },
  { path: '/report', component: () => import('../views/ReportView.vue') },
  { path: '/history', component: () => import('../views/HistoryView.vue') },
  { path: '/mine', component: () => import('../views/MineView.vue') },
  { path: '/', redirect: '/report' }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to) => {
  if (to.path !== '/login' && to.path !== '/register' && !getSession()?.token) {
    return '/login'
  }
})

export default router
