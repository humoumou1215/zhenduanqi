<template>
  <el-container v-if="userStore.isLoggedIn" style="min-height: 100vh">
    <el-header
      style="background: #409eff; color: white; display: flex; align-items: center; padding: 0 20px"
    >
      <h2 style="margin: 0; font-size: 18px">Arthas 远程诊断工具</h2>
      <div style="flex: 1" />
      <el-menu
        mode="horizontal"
        :ellipsis="false"
        :default-active="route.path"
        router
        style="background: transparent; border-bottom: none"
        active-text-color="#fff"
      >
        <el-menu-item index="/scenes" style="color: white">场景列表</el-menu-item>
        <el-menu-item index="/scenes/manage" v-if="userStore.role === 'ADMIN'" style="color: white">
          场景管理
        </el-menu-item>
        <el-menu-item index="/diagnose" style="color: white">执行诊断</el-menu-item>
        <el-menu-item index="/my-history" style="color: white">我的历史</el-menu-item>
        <el-menu-item index="/servers" v-if="userStore.role === 'ADMIN'" style="color: white">
          服务器管理
        </el-menu-item>
        <el-menu-item index="/users" v-if="userStore.role === 'ADMIN'" style="color: white">
          用户管理
        </el-menu-item>
        <el-menu-item index="/audit-logs" v-if="userStore.role === 'ADMIN'" style="color: white">
          审计日志
        </el-menu-item>
        <el-menu-item index="/command-guard" v-if="userStore.role === 'ADMIN'" style="color: white">
          命令守卫
        </el-menu-item>
        <el-menu-item index="/sessions" v-if="userStore.role === 'ADMIN'" style="color: white">
          会话管理
        </el-menu-item>
      </el-menu>
      <div style="margin-left: 16px; display: flex; align-items: center; gap: 8px">
        <el-tag size="small" :type="roleTagType" effect="dark">{{ userStore.role }}</el-tag>
        <span style="font-size: 14px">{{ userStore.realName || userStore.username }}</span>
        <el-button
          size="small"
          plain
          @click="handleLogout"
          style="color: white; border-color: rgba(255, 255, 255, 0.5)"
        >
          退出
        </el-button>
      </div>
    </el-header>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
  <router-view v-else />
</template>

<script setup>
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from './stores/user';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const roleTagType = computed(() => {
  if (userStore.role === 'ADMIN') return 'danger';
  if (userStore.role === 'OPERATOR') return 'warning';
  return 'info';
});

async function handleLogout() {
  await userStore.logout();
  router.push('/login');
}
</script>

<style>
body {
  margin: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}
</style>
