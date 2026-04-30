import { defineStore } from 'pinia'
import { getServers } from '../api'

export const useServerStore = defineStore('servers', {
  state: () => ({
    list: [],
    loading: false
  }),
  actions: {
    async fetchServers() {
      this.loading = true
      try {
        const res = await getServers()
        this.list = res.data
      } finally {
        this.loading = false
      }
    }
  }
})
