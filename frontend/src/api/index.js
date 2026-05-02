import axios from 'axios';
import { ElMessage } from 'element-plus';

const api = axios.create({
  baseURL: '/api',
  timeout: 60000,
  withCredentials: true,
  headers: {
    'Accept': 'application/json',
  },
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      import('../stores/user').then(({ useUserStore }) => {
        const store = useUserStore();
        store.isLoggedIn = false;
      });
      window.location.hash = '#/login';
    }
    return Promise.reject(error);
  }
);

export function loginApi(username, password) {
  return api.post('/auth/login', { username, password });
}

export function logoutApi() {
  return api.post('/auth/logout');
}

export function fetchMe() {
  return api.get('/auth/me');
}

export function getServers() {
  return api.get('/servers');
}

export function getServerById(id) {
  return api.get(`/servers/${id}`);
}

export function createServer(data) {
  return api.post('/servers', data);
}

export function updateServer(id, data) {
  return api.put(`/servers/${id}`, data);
}

export function deleteServer(id) {
  return api.delete(`/servers/${id}`);
}

export function getServerStatus(id) {
  return api.get(`/servers/${id}/status`);
}

export function executeCommand(serverId, command) {
  return api.post('/execute', { serverId, command });
}

export function getUsers() {
  return api.get('/users');
}

export function createUser(data) {
  return api.post('/users', data);
}

export function updateUser(id, data) {
  return api.put(`/users/${id}`, data);
}

export function resetPassword(id, newPassword) {
  return api.put(`/users/${id}/reset-password`, { newPassword });
}

export function getRoles() {
  return api.get('/roles');
}

export function getAuditLogs(params) {
  return api.get('/audit-logs', { params });
}

export function getGuardRules() {
  return api.get('/command-guard/rules');
}

export function createGuardRule(data) {
  return api.post('/command-guard/rules', data);
}

export function updateGuardRule(id, data) {
  return api.put(`/command-guard/rules/${id}`, data);
}

export function deleteGuardRule(id) {
  return api.delete(`/command-guard/rules/${id}`);
}

export function getScenes(params) {
  return api.get('/scenes', { params });
}

export function getSceneById(id) {
  return api.get(`/scenes/${id}`);
}

export function getSceneSteps(sceneId) {
  return api.get(`/scenes/${sceneId}/steps`);
}

export function createScene(data) {
  return api.post('/scenes', data);
}

export function updateScene(id, data) {
  return api.put(`/scenes/${id}`, data);
}

export function deleteScene(id) {
  return api.delete(`/scenes/${id}`);
}

export function addSceneStep(sceneId, data) {
  return api.post(`/scenes/${sceneId}/steps`, data);
}

export function updateSceneStep(stepId, data) {
  return api.put(`/scenes/steps/${stepId}`, data);
}

export function deleteSceneStep(stepId) {
  return api.delete(`/scenes/steps/${stepId}`);
}

export function reorderSceneSteps(sceneId, stepIds) {
  return api.put(`/scenes/${sceneId}/steps/reorder`, { stepIds });
}

export function createSession(data) {
  return api.post('/arthas-sessions', data);
}

export function getActiveSessions(params) {
  return api.get('/arthas-sessions', { params });
}

export function pullSessionResults(id) {
  return api.get(`/arthas-sessions/${id}/results`);
}

export function interruptSessionJob(id) {
  return api.post(`/arthas-sessions/${id}/interrupt`);
}

export function closeSession(id) {
  return api.post(`/arthas-sessions/${id}/close`);
}
