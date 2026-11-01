import { createRouter, createWebHistory } from 'vue-router'

import LoginView from '@/views/LoginView.vue'
import SettingsView from '@/views/SettingsView.vue'
import WorkbenchView from '@/views/v2/WorkbenchView.vue'
import { isLoggedIn } from '@/lib/auth.js'
import { isRemote } from '@/lib/v2/client.js'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/v2' },
    { path: '/login', name: 'login', component: LoginView },
    { path: '/v2', name: 'v2', component: WorkbenchView, meta: { requiresAuth: true } },
    { path: '/settings', name: 'settings', component: SettingsView, meta: { requiresAuth: true } },
    { path: '/:pathMatch(.*)*', redirect: '/v2' },
  ],
  scrollBehavior() {
    return { top: 0 }
  },
})

router.beforeEach((to) => {
  if (!isRemote) return true
  if (to.meta.requiresAuth && !isLoggedIn()) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && isLoggedIn()) {
    return { name: 'v2' }
  }
  return true
})

export default router
