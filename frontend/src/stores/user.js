import { defineStore } from 'pinia';
import { ref } from 'vue';
import { loginApi, logoutApi, fetchMe } from '../api';

export const useUserStore = defineStore('user', () => {
  const username = ref('');
  const role = ref('');
  const realName = ref('');
  const isLoggedIn = ref(false);

  // 最后一次登录失败的时间（用于记录，实际生产中可移除）
  let lastLoginFailTime = null;

  async function login(loginUsername, password) {
    try {
      const res = await loginApi(loginUsername, password);
      username.value = res.data.username;
      role.value = res.data.role;
      realName.value = res.data.realName || '';
      isLoggedIn.value = true;
      // 重置失败时间
      lastLoginFailTime = null;
    } catch (error) {
      // 记录失败时间
      lastLoginFailTime = new Date();
      console.log('登录失败:', error);
      // 重新抛出错误让调用方处理
      throw error;
    }
  }

  async function logout() {
    try {
      await logoutApi();
    } finally {
      username.value = '';
      role.value = '';
      realName.value = '';
      isLoggedIn.value = false;
    }
  }

  async function restoreSession() {
    try {
      const res = await fetchMe();
      username.value = res.data.username;
      role.value = res.data.role;
      realName.value = res.data.realName || '';
      isLoggedIn.value = true;
    } catch {
      isLoggedIn.value = false;
    }
  }

  return { username, role, realName, isLoggedIn, login, logout, restoreSession };
});
