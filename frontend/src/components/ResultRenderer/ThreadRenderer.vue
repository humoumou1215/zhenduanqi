<template>
  <div class="thread-renderer">
    <div v-if="isDeadlockMode" class="deadlock-warning">
      <el-icon color="#f56c6c"><WarningFilled /></el-icon>
      <span>发现死锁线程</span>
    </div>

    <div v-if="isThreadListMode && threadOverview.total > 0" class="thread-overview">
      <div class="overview-header">
        <span class="overview-title">线程概览</span>
        <el-button
          v-if="showBaselineButton"
          size="small"
          link
          type="primary"
          @click="$emit('refresh-baseline')"
        >
          刷新基线
        </el-button>
      </div>
      <div class="overview-stats">
        <span class="stat-item">
          <span class="stat-label">总数:</span>
          <span class="stat-value">{{ threadOverview.total }}</span>
        </span>
        <span
          v-for="(count, state) in threadOverview.states"
          :key="state"
          class="stat-item"
          :class="'stat-' + state.toLowerCase()"
        >
          <span class="stat-label">{{ state }}:</span>
          <span class="stat-value">{{ count }}</span>
        </span>
      </div>
    </div>

    <div v-if="isDetailMode" class="thread-detail-header">
      <span class="detail-title">线程详情</span>
      <span class="detail-id">ID: {{ currentThread.threadId }}</span>
    </div>

    <div class="thread-list" v-if="tableData.length > 0">
      <div
        v-for="(thread, index) in tableData"
        :key="index"
        class="thread-item"
        :class="{ 'thread-blocked': thread.status === 'BLOCKED' || thread.status === 'BLOCKED (lock alert)'' }"
      >
        <div class="thread-main-row" @click="toggleExpand(thread)">
          <div class="thread-core">
            <span class="thread-id">{{ thread.threadId }}</span>
            <span class="thread-name">{{ thread.name }}</span>
            <el-tag size="small" :type="getStatusType(thread.status)" class="thread-status">
              {{ formatStatus(thread.status) }}
            </el-tag>
            <span class="thread-cpu">
              {{ thread.cpu != null ? thread.cpu.toFixed(1) + '%' : '-' }}
            </span>
            <span class="thread-delta">
              {{ thread.deltaTime != null ? thread.deltaTime : '-' }}
            </span>
            <span v-if="thread.threadId !== undefined" class="thread-id-col">
              {{ thread.threadId }}
            </span>
          </div>
          <el-icon class="expand-icon" :class="{ expanded: expandedThreads[index] }">
            <ArrowDown />
          </el-icon>
        </div>

        <div v-if="expandedThreads[index]" class="thread-details">
          <div class="detail-row">
            <span v-if="thread.threadGroup" class="detail-item">
              <span class="detail-label">线程组:</span>
              <span class="detail-value">{{ thread.threadGroup }}</span>
            </span>
            <span v-if="thread.priority" class="detail-item">
              <span class="detail-label">优先级:</span>
              <span class="detail-value">{{ thread.priority }}</span>
            </span>
            <span v-if="thread.daemon !== undefined" class="detail-item">
              <span class="detail-label">守护线程:</span>
              <span class="detail-value">{{ thread.daemon ? '是' : '否' }}</span>
            </span>
            <span v-if="thread.totalTime" class="detail-item">
              <span class="detail-label">总耗时:</span>
              <span class="detail-value">{{ formatTime(thread.totalTime) }}</span>
            </span>
            <span v-if="thread.interrupted" class="detail-item">
              <span class="detail-label">中断:</span>
              <span class="detail-value danger">是</span>
            </span>
          </div>

          <div v-if="thread.lockedMonitors > 0 || thread.blockedCount > 0" class="lock-warning">
            <el-icon color="#f56c6c"><Warning /></el-icon>
            <span v-if="thread.lockedMonitors > 0">阻塞 {{ thread.lockedMonitors }} 个监视器</span>
            <span v-if="thread.blockedCount > 0">阻塞了 {{ thread.blockedCount }} 个其他线程</span>
          </div>

          <div v-if="isDetailMode || hasStackTrace(thread)" class="stack-section">
            <div class="section-header" @click="toggleStack(thread)">
              <el-icon class="stack-icon" :class="{ expanded: expandedStacks[index] }">
                <ArrowDown />
              </el-icon>
              <span>
                堆栈信息 {{ thread.stackTrace?.length ? `(${thread.stackTrace.length} 层)` : '' }}
              </span>
            </div>
            <div v-if="expandedStacks[index] && thread.stackTrace" class="stack-content">
              <div v-for="(frame, fi) in thread.stackTrace" :key="fi" class="stack-frame">
                {{ frame }}
              </div>
            </div>
          </div>

          <div v-if="thread.lockedSynchronizers?.length > 0" class="lock-section">
            <div class="section-header" @click="toggleLock(thread)">
              <el-icon class="lock-icon" :class="{ expanded: expandedLocks[index] }">
                <ArrowDown />
              </el-icon>
              <span>锁住的监视器 ({{ thread.lockedSynchronizers.length }} 个)</span>
            </div>
            <div v-if="expandedLocks[index]" class="lock-content">
              <div v-for="(lock, li) in thread.lockedSynchronizers" :key="li" class="lock-item">
                {{ lock }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="isEmpty" class="empty-state">
      <el-empty description="暂无线程数据" :image-size="60" />
    </div>

    <div
      v-if="isExample"
      style="margin-top: 8px; font-size: 11px; color: #909399; font-style: italic"
    >
      (示例数据)
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { ArrowDown, Warning, WarningFilled } from '@element-plus/icons-vue';

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
});

defineEmits(['refresh-baseline']);

const defaultThreadData = [
  {
    name: 'main',
    status: 'RUNNABLE',
    cpu: 0.5,
    deltaTime: '0.100',
    threadGroup: 'main',
    priority: 5,
    daemon: false,
    totalTime: '0:0.500',
    interrupted: false,
    lockedMonitors: 0,
    threadId: 1,
  },
  {
    name: 'C2 CompilerThread0',
    status: 'INTERNAL',
    cpu: 5.1,
    deltaTime: '0.01',
    threadGroup: 'system',
    priority: 5,
    daemon: true,
    totalTime: '0:0.973',
    interrupted: false,
    lockedMonitors: 0,
    threadId: -1,
  },
  {
    name: 'arthas-command-execute',
    status: 'RUNNABLE',
    cpu: 0.2,
    deltaTime: '0.00',
    threadGroup: 'system',
    priority: 5,
    daemon: true,
    totalTime: '0:0.226',
    interrupted: false,
    lockedMonitors: 0,
    threadId: 23,
  },
  {
    name: 'http-nio-8080-exec-1',
    status: 'BLOCKED',
    cpu: 0.5,
    deltaTime: '0.00',
    threadGroup: 'main',
    priority: 5,
    daemon: false,
    totalTime: '0:0.500',
    interrupted: false,
    lockedMonitors: 1,
    threadId: 15,
  },
  {
    name: 'pool-1-thread-1',
    status: 'TIMED_WAITING',
    cpu: 0.1,
    deltaTime: '0.080',
    threadGroup: 'main',
    priority: 5,
    daemon: false,
    totalTime: '0:1.200',
    interrupted: false,
    lockedMonitors: 0,
    threadId: 8,
  },
];

const expandedThreads = ref({});
const expandedStacks = ref({});
const expandedLocks = ref({});

watch(
  () => props.data,
  () => {
    expandedThreads.value = {};
    expandedStacks.value = {};
    expandedLocks.value = {};
  }
);

const tableData = computed(() => {
  if (!props.data || Object.keys(props.data).length === 0) {
    return defaultThreadData;
  }
  if (Array.isArray(props.data)) {
    return props.data.map(normalizeThread);
  }
  if (props.data?.busyThreads) {
    return props.data.busyThreads.map(normalizeThread);
  }
  if (props.data?.threads) {
    return props.data.threads.map(normalizeThread);
  }
  if (props.data?.deadlockInfos) {
    return normalizeDeadlockData(props.data.deadlockInfos);
  }
  if (props.data?.name || props.data?.threadId || props.data?.id) {
    return [normalizeThread(props.data)];
  }
  return defaultThreadData;
});

const isDetailMode = computed(() => {
  if (!props.data || Object.keys(props.data).length === 0) return false;
  return props.data?.name || props.data?.threadId || props.data?.id;
});

const isDeadlockMode = computed(() => {
  if (!props.data || Object.keys(props.data).length === 0) return false;
  return props.data?.deadlockInfos || props.data?.isDeadlock;
});

const isThreadListMode = computed(() => {
  if (!props.data || Object.keys(props.data).length === 0) return true;
  return !isDetailMode.value && !isDeadlockMode.value;
});

const currentThread = computed(() => {
  if (tableData.value.length === 1) {
    return tableData.value[0];
  }
  return {};
});

const showBaselineButton = computed(() => {
  return props.data?.supportBaselineRefresh === true;
});

const threadOverview = computed(() => {
  const threads = tableData.value;
  const overview = {
    total: threads.length,
    states: {},
  };
  const stateGroups = {
    RUNNABLE: 'RUNNABLE',
    BLOCKED: 'BLOCKED',
    WAITING: 'WAITING',
    TIMED_WAITING: 'TIMED_WAITING',
    INTERNAL: 'INTERNAL',
    NEW: 'NEW',
    TERMINATED: 'TERMINATED',
  };
  threads.forEach((t) => {
    const state = t.status || 'UNKNOWN';
    const group = stateGroups[state] || 'OTHER';
    overview.states[group] = (overview.states[group] || 0) + 1;
  });
  return overview;
});

const isEmpty = computed(() => {
  return tableData.value.length === 0;
});

const isExample = computed(() => {
  return !props.data || Object.keys(props.data).length === 0;
});

function normalizeThread(thread) {
  return {
    name: thread.name || thread.threadName,
    status: thread.state || thread.status || thread.threadState,
    cpu: thread.cpu,
    deltaTime: thread.deltaTime,
    threadId: thread.id || thread.threadId,
    threadGroup: thread.threadGroup || thread.groupName,
    priority: thread.priority,
    daemon: thread.daemon,
    totalTime: thread.totalTime,
    interrupted: thread.interrupted || thread.isInterrupted,
    lockedMonitors: thread.lockedMonitors?.length || thread.lockedMonitors || 0,
    blockedCount: thread.blockedCount || 0,
    stackTrace: thread.stackTrace || thread.stack || parseStackTrace(thread.stackTraceStr),
    lockedSynchronizers: thread.lockedSynchronizers || [],
  };
}

function normalizeDeadlockData(deadlockInfos) {
  if (!Array.isArray(deadlockInfos)) return [];
  return deadlockInfos.map((info) => ({
    ...normalizeThread(info),
    status: 'BLOCKED (lock alert)',
    blockedCount: info.blockedCount || 1,
  }));
}

function parseStackTrace(stackStr) {
  if (!stackStr || typeof stackStr !== 'string') return null;
  return stackStr.split('\n').filter((line) => line.trim());
}

function hasStackTrace(thread) {
  return thread.stackTrace?.length > 0 || thread.stackTraceStr;
}

function toggleExpand(thread) {
  const key = thread.threadId || thread.name;
  expandedThreads.value[key] = !expandedThreads.value[key];
}

function toggleStack(thread) {
  const key = thread.threadId || thread.name;
  expandedStacks.value[key] = !expandedStacks.value[key];
}

function toggleLock(thread) {
  const key = thread.threadId || thread.name;
  expandedLocks.value[key] = !expandedLocks.value[key];
}

function getStatusType(status) {
  if (status === 'RUNNABLE') return 'success';
  if (status === 'BLOCKED' || status === 'BLOCKED (lock alert)') return 'danger';
  if (status === 'WAITING') return 'warning';
  if (status === 'TIMED_WAITING') return 'warning';
  if (status === 'INTERNAL') return 'info';
  return 'info';
}

function formatStatus(status) {
  if (status === 'BLOCKED (lock alert)') return 'BLOCKED';
  return status;
}

function formatTime(timeStr) {
  return timeStr || '-';
}
</script>

<style scoped>
.thread-renderer {
  width: 100%;
}

.deadlock-warning {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: #fef0f0;
  border: 1px solid #fde2e2;
  border-radius: 4px;
  margin-bottom: 12px;
  color: #f56c6c;
  font-weight: 500;
}

.thread-overview {
  background: #f5f7fa;
  border-radius: 4px;
  padding: 12px;
  margin-bottom: 12px;
}

.overview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.overview-title {
  font-weight: 500;
  color: #303133;
}

.overview-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.stat-item {
  display: flex;
  gap: 4px;
  align-items: center;
}

.stat-label {
  color: #606266;
}

.stat-value {
  font-weight: 500;
  color: #303133;
}

.stat-runnable .stat-value {
  color: #67c23a;
}

.stat-blocked .stat-value {
  color: #f56c6c;
}

.stat-waiting .stat-value {
  color: #e6a23c;
}

.thread-detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}

.detail-title {
  font-weight: 500;
  color: #303133;
}

.detail-id {
  color: #909399;
  font-size: 13px;
}

.thread-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.thread-item {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
}

.thread-item.thread-blocked {
  border-color: #fde2e2;
  background: #fef0f0;
}

.thread-main-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  cursor: pointer;
  transition: background 0.2s;
}

.thread-main-row:hover {
  background: #f5f7fa;
}

.thread-core {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.thread-id {
  min-width: 40px;
  text-align: center;
  color: #909399;
  font-size: 12px;
}

.thread-name {
  min-width: 150px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.thread-status {
  min-width: 100px;
}

.thread-cpu {
  min-width: 50px;
  text-align: right;
  color: #606266;
}

.thread-delta {
  min-width: 70px;
  text-align: right;
  color: #606266;
}

.thread-id-col {
  min-width: 50px;
  text-align: center;
  color: #c0c4cc;
  font-size: 12px;
}

.expand-icon {
  transition: transform 0.3s;
  color: #c0c4cc;
}

.expand-icon.expanded {
  transform: rotate(180deg);
}

.thread-details {
  padding: 8px 12px 12px;
  background: #fafafa;
  border-top: 1px solid #ebeef5;
}

.detail-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 8px;
}

.detail-item {
  display: flex;
  gap: 4px;
}

.detail-label {
  color: #909399;
}

.detail-value {
  color: #606266;
}

.detail-value.danger {
  color: #f56c6c;
}

.lock-warning {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  background: #fef0f0;
  border-radius: 4px;
  margin-bottom: 8px;
  color: #f56c6c;
  font-size: 13px;
}

.stack-section,
.lock-section {
  margin-top: 8px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  padding: 6px 0;
  color: #409eff;
  font-size: 13px;
  transition: color 0.2s;
}

.section-header:hover {
  color: #66b1ff;
}

.stack-icon,
.lock-icon {
  transition: transform 0.3s;
}

.stack-icon.expanded,
.lock-icon.expanded {
  transform: rotate(180deg);
}

.stack-content,
.lock-content {
  padding: 8px 0 0 20px;
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 12px;
  line-height: 1.6;
  color: #606266;
}

.stack-frame {
  padding: 2px 0;
  white-space: pre-wrap;
  word-break: break-all;
}

.lock-item {
  padding: 2px 0;
}

.empty-state {
  padding: 20px;
}
</style>
