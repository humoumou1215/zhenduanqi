import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: '/api',
  timeout: 60000,
  withCredentials: true
})

api.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      const userStore = null
      import('../stores/user').then(({ useUserStore }) => {
        const store = useUserStore()
        store.isLoggedIn = false
      })
      window.location.hash = '#/login'
    }
    return Promise.reject(error)
  }
)

export function loginApi(username, password) {
  return api.post('/auth/login', { username, password })
}

export function logoutApi() {
  return api.post('/auth/logout')
}

export function fetchMe() {
  return api.get('/auth/me')
}

export function getServers() {
  return api.get('/servers')
}

export function getServerById(id) {
  return api.get(`/servers/${id}`)
}

export function createServer(data) {
  return api.post('/servers', data)
}

export function updateServer(id, data) {
  return api.put(`/servers/${id}`, data)
}

export function deleteServer(id) {
  return api.delete(`/servers/${id}`)
}

export function getServerStatus(id) {
  return api.get(`/servers/${id}/status`)
}

export function executeCommand(serverId, command) {
  return api.post('/execute', { serverId, command })
}

export function getUsers() {
  return api.get('/users')
}

export function createUser(data) {
  return api.post('/users', data)
}

export function updateUser(id, data) {
  return api.put(`/users/${id}`, data)
}

export function resetPassword(id, newPassword) {
  return api.put(`/users/${id}/reset-password`, { newPassword })
}

export function getRoles() {
  return api.get('/roles')
}
