import { createRouter, createWebHashHistory } from 'vue-router';
import { ElMessage } from 'element-plus';

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
  },
  {
    path: '/',
    redirect: '/scenes',
  },
  {
    path: '/scenes',
    name: 'DiagnoseWorkbench',
    component: () => import('../views/DiagnoseWorkbench.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/scenes/manage',
    name: 'SceneManage',
    component: () => import('../views/SceneManage.vue'),
    meta: { requiresAuth: true, requiresRole: 'ADMIN' },
  },
  {
    path: '/diagnose',
    name: 'Diagnose',
    component: () => import('../views/Diagnose.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/servers',
    name: 'ServerList',
    component: () => import('../views/ServerList.vue'),
    meta: { requiresAuth: true, requiresRole: 'ADMIN' },
  },
  {
    path: '/users',
    name: 'UserManage',
    component: () => import('../views/UserManage.vue'),
    meta: { requiresAuth: true, requiresRole: 'ADMIN' },
  },
  {
    path: '/audit-logs',
    name: 'AuditLog',
    component: () => import('../views/AuditLog.vue'),
    meta: { requiresAuth: true, requiresRole: 'ADMIN' },
  },
  {
    path: '/command-guard',
    name: 'CommandGuard',
    component: () => import('../views/CommandGuard.vue'),
    meta: { requiresAuth: true, requiresRole: 'ADMIN' },
  },
  {
    path: '/sessions',
    name: 'SessionManage',
    component: () => import('../views/SessionManage.vue'),
    meta: { requiresAuth: true, requiresRole: 'ADMIN' },
  },
  {
    path: '/my-history',
    name: 'MyHistory',
    component: () => import('../views/MyHistory.vue'),
    meta: { requiresAuth: true },
  },
];

const router = createRouter({
  history: createWebHashHistory(),
  routes,
});

router.beforeEach(async (to, from, next) => {
  if (to.meta.requiresAuth === false) {
    next();
    return;
  }

  const { useUserStore } = await import('../stores/user');
  const userStore = useUserStore();

  if (!userStore.isLoggedIn) {
    await userStore.restoreSession();
  }

  if (to.path === '/login') {
    if (userStore.isLoggedIn) {
      next('/scenes');
    } else {
      next();
    }
    return;
  }

  if (!userStore.isLoggedIn) {
    next('/login');
  } else if (to.meta.requiresRole && userStore.role !== to.meta.requiresRole) {
    ElMessage.error('权限不足，无权访问此页面');
    next('/scenes');
  } else {
    next();
  }
});

export default router;
