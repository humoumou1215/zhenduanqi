<template>
  <div class="diagnose-workbench">
    <div class="workbench-header">
      <div class="header-left">
        <h2>诊断工作台</h2>
        <p>选择服务器，查看实时监控，执行诊断场景</p>
      </div>
      <div class="header-right">
        <el-select
          v-model="selectedServerId"
          placeholder="选择目标服务器"
          style="width: 280px"
          @change="handleServerChange"
        >
          <el-option
            v-for="s in serverStore.list"
            :key="s.id"
            :label="s.name + ' (' + s.host + ':' + s.httpPort + ')'"
            :value="s.id"
          />
        </el-select>
      </div>
    </div>

    <div class="dashboard-section">
      <div class="section-header">
        <h3>实时监控</h3>
        <div class="dashboard-controls">
          <el-input-number
            v-model="refreshInterval"
            :min="3"
            :max="300"
            :step="1"
            size="small"
            style="width: 100px"
            :disabled="autoRefresh"
          />
          <span style="margin-left: 4px; color: #909399; font-size: 13px">秒</span>
          <el-button
            v-if="!autoRefresh"
            type="primary"
            size="small"
            :disabled="!selectedServerId"
            @click="startAutoRefresh"
          >
            <el-icon><VideoPlay /></el-icon>
            开始刷新
          </el-button>
          <el-button v-else type="danger" size="small" @click="stopAutoRefresh">
            <el-icon><VideoPause /></el-icon>
            暂停
          </el-button>
          <el-button
            size="small"
            :loading="manualRefreshing"
            :disabled="!selectedServerId"
            @click="refreshDashboard"
          >
            <el-icon><Refresh /></el-icon>
            立即刷新
          </el-button>
        </div>
      </div>
      <el-card v-loading="dashboardLoading" class="dashboard-card">
        <DashboardRenderer :data="dashboardData" :server-id="selectedServerId" />
        <div
          v-if="!dashboardData"
          style="margin-top: 8px; font-size: 11px; color: #909399; font-style: italic"
        >
          (请选择服务器并点击"开始刷新"查看真实数据)
        </div>
      </el-card>
    </div>

    <div class="scene-filter-section">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索场景名称或业务场景..."
        style="width: 300px"
        clearable
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <div class="category-tags">
        <el-tag
          v-for="cat in categoryDefinitions"
          :key="cat.code"
          :type="selectedCategory === cat.code ? '' : 'info'"
          :effect="selectedCategory === cat.code ? 'dark' : 'plain'"
          style="cursor: pointer"
          @click="toggleCategory(cat.code)"
        >
          {{ cat.name }}
        </el-tag>
        <el-tag
          v-if="selectedCategory"
          type="danger"
          effect="plain"
          style="cursor: pointer"
          @click="selectedCategory = ''"
        >
          清除筛选
        </el-tag>
      </div>
    </div>

    <div class="scene-list-section" v-loading="scenesLoading">
      <div v-if="filteredScenes.length === 0 && !scenesLoading" class="empty-scenes">
        <el-icon :size="48"><Folder /></el-icon>
        <p>{{ searchKeyword || selectedCategory ? '未找到匹配的场景' : '暂无场景数据' }}</p>
      </div>

      <el-collapse v-else v-model="activeSceneId" accordion class="scene-collapse">
        <el-collapse-item v-for="scene in filteredScenes" :key="scene.id" :name="scene.id">
          <template #title>
            <div class="scene-title-row">
              <div class="scene-title-info">
                <span class="scene-name">{{ scene.name }}</span>
                <el-tag size="small" :color="getCategoryColor(scene.category)" effect="dark">
                  {{ getCategoryName(scene.category) }}
                </el-tag>
              </div>
              <span class="scene-business">
                {{ scene.businessScenario || scene.description || '暂无描述' }}
              </span>
            </div>
          </template>

          <div class="scene-steps-container">
            <div v-if="!scene.steps || scene.steps.length === 0" class="empty-steps">
              <p>暂无诊断步骤</p>
            </div>
            <div v-else class="steps-list">
              <el-card
                v-for="(step, index) in scene.steps"
                :key="step.id"
                class="step-card"
                :class="{
                  'step-completed': getStepState(scene.id, index)?.state === 'completed',
                  'step-executing': getStepState(scene.id, index)?.state === 'executing',
                  'step-error': getStepState(scene.id, index)?.state === 'error',
                }"
              >
                <template #header>
                  <div class="step-header">
                    <div class="step-header-left">
                      <el-tag
                        :type="getStepTagType(getStepState(scene.id, index)?.state)"
                        size="small"
                      >
                        步骤 {{ index + 1 }}
                      </el-tag>
                      <span class="step-title">{{ step.title || '步骤 ' + (index + 1) }}</span>
                      <el-tag v-if="step.continuous" type="warning" size="small">连续输出</el-tag>
                    </div>
                    <div
                      v-if="getStepState(scene.id, index)?.state === 'completed'"
                      class="step-status completed"
                    >
                      <el-icon><CircleCheck /></el-icon>
                      已完成
                    </div>
                    <div
                      v-else-if="getStepState(scene.id, index)?.state === 'executing'"
                      class="step-status executing"
                    >
                      <el-icon class="is-loading"><Loading /></el-icon>
                      执行中...
                    </div>
                  </div>
                </template>

                <div class="step-content">
                  <p v-if="step.description" class="step-description">
                    <el-icon><InfoFilled /></el-icon>
                    {{ step.description }}
                  </p>

                  <div class="command-input-area">
                    <el-input
                      :model-value="getStepCommandValue(scene.id, index)"
                      @update:model-value="setStepCommandValue(scene.id, index, $event)"
                      :placeholder="step.command"
                      :disabled="getStepState(scene.id, index)?.state === 'executing'"
                      @keyup.enter="handleExecuteStep(scene, index)"
                    >
                      <template #append>
                        <el-button
                          v-if="
                            !step.continuous &&
                            (userStore.role === 'OPERATOR' || userStore.role === 'ADMIN')
                          "
                          :loading="getStepState(scene.id, index)?.state === 'executing'"
                          :disabled="
                            !selectedServerId ||
                            getStepState(scene.id, index)?.state === 'executing'
                          "
                          @click="handleExecuteStep(scene, index)"
                        >
                          <el-icon v-if="!getStepState(scene.id, index)?.state">
                            <VideoPlay />
                          </el-icon>
                          执行
                        </el-button>
                        <template
                          v-else-if="
                            step.continuous &&
                            (userStore.role === 'OPERATOR' || userStore.role === 'ADMIN')
                          "
                        >
                          <el-button
                            v-if="getStepState(scene.id, index)?.state !== 'executing'"
                            :disabled="!selectedServerId"
                            @click="handleExecuteStepAsync(scene, index)"
                          >
                            <el-icon><VideoPlay /></el-icon>
                            执行
                          </el-button>
                          <el-button
                            v-else
                            type="danger"
                            @click="handleStopStepAsync(scene, index)"
                          >
                            <el-icon><VideoPause /></el-icon>
                            停止
                          </el-button>
                        </template>
                        <el-tag v-else type="info" size="small">只读</el-tag>
                      </template>
                    </el-input>
                  </div>

                  <div v-if="getStepState(scene.id, index)?.result" class="step-result">
                    <div class="result-header">
                      <span>执行结果</span>
                      <el-tag
                        v-if="getStepState(scene.id, index)?.result.state === 'succeeded'"
                        type="success"
                        size="small"
                      >
                        成功
                      </el-tag>
                      <el-tag
                        v-else-if="getStepState(scene.id, index)?.result.state === 'failed'"
                        type="danger"
                        size="small"
                      >
                        失败
                      </el-tag>
                    </div>

                    <div v-if="getStepState(scene.id, index)?.result.error" class="result-error">
                      {{ getStepState(scene.id, index)?.result.error }}
                    </div>

                    <div
                      v-for="(r, i) in getStepState(scene.id, index)?.result.structuredResults"
                      :key="i"
                    >
                      <component :is="getRenderer(r.type)" :data="r.data" />
                    </div>

                    <div
                      v-if="
                        getStepState(scene.id, index)?.result.results &&
                        getStepState(scene.id, index)?.result.results.length > 0 &&
                        !getStepState(scene.id, index)?.result.structuredResults
                      "
                    >
                      <pre
                        v-for="(r, i) in getStepState(scene.id, index)?.result.results"
                        :key="'raw-' + i"
                        class="result-raw"
                        >{{ typeof r === 'string' ? r : JSON.stringify(r, null, 2) }}</pre
                      >
                    </div>
                  </div>

                  <div v-if="step.expectedHint" class="step-hint">
                    <el-icon><InfoFilled /></el-icon>
                    {{ step.expectedHint }}
                  </div>
                </div>
              </el-card>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { ElMessage } from 'element-plus';
import {
  Search,
  Folder,
  VideoPlay,
  VideoPause,
  Refresh,
  CircleCheck,
  Loading,
  InfoFilled,
  Clock,
  Odometer,
  DataAnalysis,
  Connection,
} from '@element-plus/icons-vue';
import {
  getScenes,
  getSceneSteps,
  executeCommand,
  createSession,
  pullSessionResults,
  interruptSessionJob,
  closeSession,
} from '../api';
import { useServerStore } from '../stores/servers';
import { useUserStore } from '../stores/user';
import { getRenderer } from '../components/ResultRenderer';
import DashboardRenderer from '../components/ResultRenderer/DashboardRenderer.vue';

const serverStore = useServerStore();
const userStore = useUserStore();

const STORAGE_KEY = 'selectedServerId';

const selectedServerId = ref('');
const searchKeyword = ref('');
const selectedCategory = ref('');
const scenes = ref([]);
const scenesLoading = ref(false);
const activeSceneId = ref('');

const dashboardData = ref(null);
const dashboardLoading = ref(false);
const autoRefresh = ref(false);
const refreshInterval = ref(10);
const manualRefreshing = ref(false);
let refreshTimer = null;

const sceneStepCommands = ref({});
const sceneStepStates = ref({});
const sceneAsyncSessions = ref({});
const pollIntervals = ref({});

const categoryDefinitions = [
  { code: 'SLOW_RESPONSE', name: '接口响应慢', color: '#E6A23C' },
  { code: 'CPU_HIGH', name: 'CPU 高', color: '#F56C6C' },
  { code: 'MEMORY_HIGH', name: '内存高', color: '#9B59B6' },
  { code: 'GC_FREQUENT', name: 'GC 频繁', color: '#67C23A' },
  { code: 'THREAD_POOL_HIGH', name: '线程池高', color: '#409EFF' },
  { code: 'CLASS_LOAD_ERROR', name: '类加载异常', color: '#909399' },
];

const filteredScenes = computed(() => {
  let result = scenes.value;

  if (selectedCategory.value) {
    result = result.filter((s) => s.category === selectedCategory.value);
  }

  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase();
    result = result.filter(
      (s) =>
        s.name.toLowerCase().includes(keyword) ||
        (s.businessScenario && s.businessScenario.toLowerCase().includes(keyword)) ||
        (s.description && s.description.toLowerCase().includes(keyword))
    );
  }

  return result.sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0));
});

function getCategoryName(code) {
  const cat = categoryDefinitions.find((c) => c.code === code);
  return cat ? cat.name : code;
}

function getCategoryColor(code) {
  const cat = categoryDefinitions.find((c) => c.code === code);
  return cat ? cat.color : '#909399';
}

function toggleCategory(code) {
  selectedCategory.value = selectedCategory.value === code ? '' : code;
}

function handleServerChange(serverId) {
  localStorage.setItem(STORAGE_KEY, serverId);
  dashboardData.value = null;
  stopAutoRefresh();
}

function restoreSelectedServer() {
  const saved = localStorage.getItem(STORAGE_KEY);
  if (saved && serverStore.list.some((s) => s.id === saved)) {
    selectedServerId.value = saved;
  }
}

async function fetchScenes() {
  scenesLoading.value = true;
  try {
    const res = await getScenes();
    const scenesWithSteps = await Promise.all(
      res.data.map(async (scene) => {
        try {
          const stepsRes = await getSceneSteps(scene.id);
          return { ...scene, steps: stepsRes.data };
        } catch {
          return { ...scene, steps: [] };
        }
      })
    );
    scenes.value = scenesWithSteps;
  } catch (e) {
    ElMessage.error('加载场景列表失败');
  } finally {
    scenesLoading.value = false;
  }
}

async function refreshDashboard() {
  if (!selectedServerId.value) return;

  dashboardLoading.value = true;
  manualRefreshing.value = true;
  try {
    const res = await executeCommand(selectedServerId.value, 'dashboard');
    const result = res.data;
    if (result.state === 'succeeded' && result.structuredResults?.length > 0) {
      const dashboardResult = result.structuredResults.find((r) => r.type === 'dashboard');
      if (dashboardResult) {
        dashboardData.value = dashboardResult.data;
      }
    }
  } catch (e) {
    console.error('Dashboard refresh error:', e);
  } finally {
    dashboardLoading.value = false;
    manualRefreshing.value = false;
  }
}

function startAutoRefresh() {
  if (!selectedServerId.value) return;
  autoRefresh.value = true;
  refreshDashboard();
  refreshTimer = setInterval(refreshDashboard, refreshInterval.value * 1000);
}

function stopAutoRefresh() {
  autoRefresh.value = false;
  if (refreshTimer) {
    clearInterval(refreshTimer);
    refreshTimer = null;
  }
}

function getStepState(sceneId, stepIndex) {
  const key = `${sceneId}-${stepIndex}`;
  return sceneStepStates.value[key] || {};
}

function getStepCommandValue(sceneId, stepIndex) {
  const key = `${sceneId}-${stepIndex}`;
  if (!sceneStepCommands.value[key]) {
    const scene = scenes.value.find((s) => s.id === sceneId);
    if (scene && scene.steps && scene.steps[stepIndex]) {
      sceneStepCommands.value[key] = scene.steps[stepIndex].command || '';
    }
  }
  return sceneStepCommands.value[key] || '';
}

function setStepCommandValue(sceneId, stepIndex, value) {
  const key = `${sceneId}-${stepIndex}`;
  sceneStepCommands.value[key] = value;
}

function getStepTagType(state) {
  if (state === 'completed') return 'success';
  if (state === 'executing') return 'warning';
  if (state === 'error') return 'danger';
  return 'info';
}

async function handleExecuteStep(scene, stepIndex) {
  const step = scene.steps[stepIndex];
  const key = `${scene.id}-${stepIndex}`;
  const command = sceneStepCommands.value[key] || step.command;

  if (!command || !command.trim()) {
    ElMessage.warning('请输入命令');
    return;
  }

  if (!selectedServerId.value) {
    ElMessage.warning('请先选择服务器');
    return;
  }

  sceneStepStates.value[key] = { state: 'executing', result: null };

  try {
    const res = await executeCommand(selectedServerId.value, command);
    const result = res.data;
    sceneStepStates.value[key] = { state: 'completed', result };
  } catch (e) {
    sceneStepStates.value[key] = {
      state: 'error',
      result: {
        state: 'failed',
        error: e.response?.data?.error || e.message || '执行失败',
        results: [],
      },
    };
    ElMessage.error(e.response?.data?.error || '命令执行失败');
  }
}

async function handleExecuteStepAsync(scene, stepIndex) {
  const step = scene.steps[stepIndex];
  const key = `${scene.id}-${stepIndex}`;
  const command = sceneStepCommands.value[key] || step.command;

  if (!command || !command.trim()) {
    ElMessage.warning('请输入命令');
    return;
  }

  if (!selectedServerId.value) {
    ElMessage.warning('请先选择服务器');
    return;
  }

  sceneStepStates.value[key] = {
    state: 'executing',
    result: {
      state: 'scheduled',
      structuredResults: [],
      results: [],
    },
  };

  try {
    const createRes = await createSession({
      serverId: selectedServerId.value,
      sceneId: scene.id,
      stepId: step.id,
      command: command,
      maxExecTime: step.max_exec_time,
    });
    const session = createRes.data;
    sceneAsyncSessions.value[key] = session;

    pollIntervals.value[key] = setInterval(async () => {
      try {
        const res = await pullSessionResults(session.id);
        const results = res.data;

        if (results && results.length > 0) {
          const statusResult = results.find((r) => r.type === 'status');
          const isTaskCompleted = statusResult?.data?.status === 'terminated';

          if (!sceneStepStates.value[key].result) {
            sceneStepStates.value[key].result = { structuredResults: [], results: [] };
          }
          sceneStepStates.value[key].result.structuredResults = [
            ...(sceneStepStates.value[key].result.structuredResults || []),
            ...results,
          ];
          sceneStepStates.value[key].result.results = [
            ...(sceneStepStates.value[key].result.results || []),
            ...results.map((r) => JSON.stringify(r)),
          ];

          if (isTaskCompleted) {
            clearInterval(pollIntervals.value[key]);
            delete pollIntervals.value[key];

            try {
              await closeSession(session.id);
            } catch (e) {
              console.error('Close session error:', e);
            }

            delete sceneAsyncSessions.value[key];
            sceneStepStates.value[key].state = 'completed';
          }
        }
      } catch (e) {
        console.error('Poll results error:', e);
      }
    }, 2000);
  } catch (e) {
    sceneStepStates.value[key] = {
      state: 'error',
      result: {
        state: 'failed',
        error: e.response?.data?.error || e.message || '执行失败',
        results: [],
      },
    };
    ElMessage.error(e.response?.data?.error || '命令执行失败');
  }
}

async function handleStopStepAsync(scene, stepIndex) {
  const key = `${scene.id}-${stepIndex}`;
  const session = sceneAsyncSessions.value[key];
  if (!session) return;

  try {
    await interruptSessionJob(session.id);
    ElMessage.success('命令已中断');
  } catch (e) {
    console.error('Interrupt error:', e);
  }

  if (pollIntervals.value[key]) {
    clearInterval(pollIntervals.value[key]);
    delete pollIntervals.value[key];
  }

  try {
    await closeSession(session.id);
  } catch (e) {
    console.error('Close session error:', e);
  }

  delete sceneAsyncSessions.value[key];
  sceneStepStates.value[key].state = 'completed';
}

watch(activeSceneId, (newId) => {
  if (newId) {
    const scene = scenes.value.find((s) => s.id === newId);
    if (scene && scene.steps) {
      scene.steps.forEach((step, index) => {
        const key = `${scene.id}-${index}`;
        if (!sceneStepCommands.value[key]) {
          sceneStepCommands.value[key] = step.command || '';
        }
      });
    }
  }
});

onMounted(async () => {
  await serverStore.fetchServers();
  restoreSelectedServer();
  await fetchScenes();
});

onUnmounted(() => {
  stopAutoRefresh();
  for (const key in pollIntervals.value) {
    if (pollIntervals.value[key]) {
      clearInterval(pollIntervals.value[key]);
    }
  }
});
</script>

<style scoped>
.diagnose-workbench {
  padding: 0;
}

.workbench-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.header-left h2 {
  margin: 0 0 4px 0;
  font-size: 20px;
  color: #303133;
}

.header-left p {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

.dashboard-section {
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.section-header h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
}

.dashboard-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dashboard-card {
  min-height: 200px;
}

.scene-filter-section {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.category-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.scene-list-section {
  min-height: 300px;
}

.empty-scenes {
  text-align: center;
  padding: 60px 0;
  color: #909399;
}

.empty-scenes p {
  margin-top: 16px;
}

.scene-collapse {
  border: none;
}

.scene-collapse :deep(.el-collapse-item__header) {
  height: auto;
  padding: 16px 20px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 8px;
  transition: all 0.2s;
}

.scene-collapse :deep(.el-collapse-item__header:hover) {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
}

.scene-collapse :deep(.el-collapse-item__wrap) {
  border: none;
  background: transparent;
}

.scene-collapse :deep(.el-collapse-item__content) {
  padding: 0;
  margin-bottom: 8px;
}

.scene-title-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.scene-title-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.scene-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.scene-business {
  font-size: 13px;
  color: #909399;
}

.scene-steps-container {
  padding: 16px 20px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}

.empty-steps {
  text-align: center;
  padding: 40px 0;
  color: #909399;
}

.steps-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.step-card {
  border-left: 4px solid #dcdfe6;
  transition: border-color 0.3s;
}

.step-card.step-completed {
  border-left-color: #67c23a;
}

.step-card.step-executing {
  border-left-color: #409eff;
}

.step-card.step-error {
  border-left-color: #f56c6c;
}

.step-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.step-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.step-title {
  font-weight: 600;
  color: #303133;
}

.step-status {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
}

.step-status.completed {
  color: #67c23a;
}

.step-status.executing {
  color: #409eff;
}

.step-content {
  padding: 4px 0;
}

.step-description {
  color: #606266;
  font-size: 14px;
  margin: 0 0 12px 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

.command-input-area {
  margin-bottom: 8px;
}

.step-result {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  margin-top: 16px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-weight: 500;
}

.result-error {
  color: #f56c6c;
  margin-bottom: 8px;
}

.result-raw {
  background: #fff;
  padding: 12px;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.6;
  overflow-x: auto;
  margin: 8px 0 0 0;
}

.step-hint {
  background: #fdf6ec;
  color: #e6a23c;
  font-size: 13px;
  padding: 8px 12px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
}
</style>
