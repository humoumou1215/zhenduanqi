import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/servers'
  },
  {
    path: '/servers',
    name: 'ServerList',
    component: () => import('../views/ServerList.vue')
  },
  {
    path: '/diagnose',
    name: 'Diagnose',
    component: () => import('../views/Diagnose.vue')
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
