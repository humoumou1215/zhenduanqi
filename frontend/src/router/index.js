import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/',
    redirect: '/diagnose'
  },
  {
    path: '/servers',
    name: 'ServerList',
    component: () => import('../views/ServerList.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/diagnose',
    name: 'Diagnose',
    component: () => import('../views/Diagnose.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/users',
    name: 'UserManage',
    component: () => import('../views/UserManage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/audit-logs',
    name: 'AuditLog',
    component: () => import('../views/AuditLog.vue'),
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  if (to.meta.requiresAuth === false) {
    next()
    return
  }

  const { useUserStore } = await import('../stores/user')
  const userStore = useUserStore()

  if (!userStore.isLoggedIn) {
    await userStore.restoreSession()
  }

  if (to.path === '/login') {
    if (userStore.isLoggedIn) {
      next('/diagnose')
    } else {
      next()
    }
    return
  }

  if (!userStore.isLoggedIn) {
    next('/login')
  } else {
    next()
  }
})

export default router
