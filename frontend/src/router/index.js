import { createRouter, createWebHashHistory } from 'vue-router';
import { ElMessage } from 'element-plus';

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/',
    redirect: '/workbench',
  },
  {
    path: '/scenes',
    name: 'SceneList',
    component: () => import('../views/SceneList.vue'),
    meta: { requiresAuth: true, title: '场景列表' },
  },
  {
    path: '/scenes/manage',
    name: 'SceneManage',
    component: () => import('../views/SceneManage.vue'),
    meta: { requiresAuth: true, requiresRole: 'ADMIN', title: '场景管理' },
  },
  {
    path: '/scenes/:id/diagnose',
    name: 'SceneDiagnose',
    component: () => import('../views/SceneDiagnose.vue'),
    meta: { requiresAuth: true, title: '场景诊断' },
  },
  {
    path: '/diagnose',
    name: 'Diagnose',
    component: () => import('../views/Diagnose.vue'),
    meta: { requiresAuth: true, title: '执行诊断' },
  },
  {
    path: '/workbench',
    name: 'DiagnoseWorkbench',
    component: () => import('../views/DiagnoseWorkbench.vue'),
    meta: { requiresAuth: true, title: '诊断工作台' },
  },
  {
    path: '/servers',
    name: 'ServerList',
    component: () => import('../views/ServerList.vue'),
    meta: { requiresAuth: true, requiresRole: 'ADMIN', title: '服务器管理' },
  },
  {
    path: '/users',
    name: 'UserManage',
    component: () => import('../views/UserManage.vue'),
    meta: { requiresAuth: true, requiresRole: 'ADMIN', title: '用户管理' },
  },
  {
    path: '/audit-logs',
    name: 'AuditLog',
    component: () => import('../views/AuditLog.vue'),
    meta: { requiresAuth: true, requiresRole: 'ADMIN', title: '审计日志' },
  },
  {
    path: '/command-guard',
    name: 'CommandGuard',
    component: () => import('../views/CommandGuard.vue'),
    meta: { requiresAuth: true, requiresRole: 'ADMIN', title: '命令守卫' },
  },
  {
    path: '/sessions',
    name: 'SessionManage',
    component: () => import('../views/SessionManage.vue'),
    meta: { requiresAuth: true, requiresRole: 'ADMIN', title: '会话管理' },
  },
  {
    path: '/my-history',
    name: 'MyHistory',
    component: () => import('../views/MyHistory.vue'),
    meta: { requiresAuth: true, title: '我的历史' },
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
      next('/workbench');
    } else {
      next();
    }
    return;
  }

  if (!userStore.isLoggedIn) {
    next('/login');
  } else if (to.meta.requiresRole && userStore.role !== to.meta.requiresRole) {
    ElMessage.error('权限不足，无权访问此页面');
    next('/workbench');
  } else {
    next();
  }
});

export default router;
