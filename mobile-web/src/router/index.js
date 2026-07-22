import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/home' },
  { path: '/home', component: () => import('@/views/Home.vue') },
  { path: '/declare', component: () => import('@/views/Declare.vue') },
  { path: '/catch', component: () => import('@/views/Catch.vue') },
  { path: '/profile', component: () => import('@/views/Profile.vue') },
  { path: '/history', component: () => import('@/views/History.vue') }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
