import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 60000
})

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
