import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/dashboard' },
  { path: '/dashboard', component: () => import('@/views/Dashboard.vue') },
  { path: '/vessel', component: () => import('@/views/VesselList.vue') },
  { path: '/voyage', component: () => import('@/views/VoyageList.vue') },
  { path: '/catch', component: () => import('@/views/CatchList.vue') },
  { path: '/alert', component: () => import('@/views/AlertList.vue') },
  { path: '/quota', component: () => import('@/views/QuotaPanel.vue') },
  { path: '/enforcement', component: () => import('@/views/EnforcementPanel.vue') },
  { path: '/purchase', component: () => import('@/views/PurchaseList.vue') }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
