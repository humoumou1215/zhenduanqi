import { defineStore } from 'pinia';
import { getServers } from '../api';

const STORAGE_KEY = 'zhenduanqi_selected_server';

export const useServerStore = defineStore('servers', {
  state: () => ({
    list: [],
    loading: false,
    currentServerId: null,
  }),
  actions: {
    async fetchServers() {
      this.loading = true;
      try {
        const res = await getServers();
        this.list = res.data;
        this.restoreCurrentServer();
      } finally {
        this.loading = false;
      }
    },
    restoreCurrentServer() {
      const saved = localStorage.getItem(STORAGE_KEY);
      if (saved && this.list.some((s) => s.id === saved)) {
        this.currentServerId = saved;
      }
    },
    setCurrentServer(serverId) {
      this.currentServerId = serverId;
      if (serverId) {
        localStorage.setItem(STORAGE_KEY, serverId);
      }
    },
  },
});
