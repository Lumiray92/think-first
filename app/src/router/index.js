import { createRouter, createWebHashHistory } from 'vue-router'

import HomeView from '@/views/HomeView.vue'
import GuideView from '@/views/GuideView.vue'
import ChatView from '@/views/ChatView.vue'
import DoneView from '@/views/DoneView.vue'

const router = createRouter({
  // GitHub Pages 没有 SPA 回退，用 hash 路由保证刷新与深链可用
  history: createWebHashHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/guide', name: 'guide', component: GuideView },
    { path: '/chat', name: 'chat', component: ChatView },
    { path: '/done', name: 'done', component: DoneView },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
  scrollBehavior() {
    return { top: 0 }
  },
})

export default router
