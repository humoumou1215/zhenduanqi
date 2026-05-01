import { defineStore } from 'pinia';
import { ref } from 'vue';
import { loginApi, logoutApi, fetchMe } from '../api';

export const useUserStore = defineStore('user', () => {
  const username = ref('');
  const role = ref('');
  const realName = ref('');
  const isLoggedIn = ref(false);

  async function login(loginUsername, password) {
    const res = await loginApi(loginUsername, password);
    username.value = res.data.username;
    role.value = res.data.role;
    realName.value = res.data.realName || '';
    isLoggedIn.value = true;
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
