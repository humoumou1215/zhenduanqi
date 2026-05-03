<template>
  <div class="dashboard-renderer">
    <div v-if="hasData" class="dashboard-content">
      <el-tabs type="border-card" size="small">
        <el-tab-pane label="线程">
          <el-table :data="threadData" stripe size="small" max-height="200">
            <el-table-column prop="name" label="线程名" min-width="150" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="getStatusType(row.status)">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="cpu" label="CPU%" width="70" align="right">
              <template #default="{ row }">
                {{ row.cpu != null ? row.cpu.toFixed(1) : '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="deltaTime" label="Delta(ms)" width="90" align="right">
              <template #default="{ row }">
                {{ row.deltaTime != null ? row.deltaTime : '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="threadId" label="线程ID" width="80" align="center" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="内存">
          <el-table :data="memoryData" stripe size="small">
            <el-table-column prop="name" label="区域" min-width="120" />
            <el-table-column prop="used" label="已用" width="100" align="right">
              <template #default="{ row }">
                {{ formatBytes(row.used) }}
              </template>
            </el-table-column>
            <el-table-column prop="total" label="总量" width="100" align="right">
              <template #default="{ row }">
                {{ formatBytes(row.total) }}
              </template>
            </el-table-column>
            <el-table-column label="使用率" width="150">
              <template #default="{ row }">
                <div style="display: flex; align-items: center; gap: 8px">
                  <el-progress
                    :percentage="getPercentage(row)"
                    :color="getProgressColor(row)"
                    :stroke-width="8"
                    style="flex: 1"
                  />
                  <span style="width: 35px; text-align: right">{{ getPercentage(row) }}%</span>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="GC">
          <el-table :data="gcData" stripe size="small">
            <el-table-column prop="name" label="GC收集器" min-width="150" />
            <el-table-column prop="count" label="次数" width="80" align="center" />
            <el-table-column prop="time" label="耗时(ms)" width="100" align="right">
              <template #default="{ row }">
                {{ row.time != null ? row.time : '-' }}
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>

    <div v-else class="dashboard-empty">
      <el-empty description="暂无 dashboard 数据" :image-size="60" />
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
import { computed } from 'vue';

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
});

const defaultData = {
  threads: [
    { name: 'main', status: 'RUNNABLE', cpu: 0.5, deltaTime: 100, threadId: 1 },
    { name: 'gc-thread-1', status: 'WAITING', cpu: 0.0, deltaTime: 50, threadId: 2 },
    { name: 'http-nio-8080-exec-1', status: 'BLOCKED', cpu: 2.3, deltaTime: 200, threadId: 15 },
  ],
  memory: [
    { name: 'Heap Memory', used: 268435456, total: 536870912 },
    { name: 'Eden Space', used: 134217728, total: 268435456 },
    { name: 'Survivor Space', used: 16777216, total: 33554432 },
    { name: 'Old Gen', used: 117440512, total: 268435456 },
  ],
  gc: [
    { name: 'Copy', count: 15, time: 120 },
    { name: 'MarkSweepCompact', count: 3, time: 80 },
  ],
};

const currentData = computed(() => {
  if (!props.data || Object.keys(props.data).length === 0) {
    return defaultData;
  }
  return props.data;
});

const hasData = computed(() => {
  return (
    currentData.value &&
    (currentData.value.threads?.length ||
      currentData.value.memory?.length ||
      currentData.value.gc?.length)
  );
});

const isExample = computed(() => {
  return !props.data || Object.keys(props.data).length === 0;
});

const threadData = computed(() => {
  if (currentData.value?.threads) {
    return currentData.value.threads.map(normalizeThread);
  }
  if (currentData.value?.threadList) {
    return currentData.value.threadList.map(normalizeThread);
  }
  return [];
});

const memoryData = computed(() => {
  if (currentData.value?.memory) {
    return normalizeMemoryData(currentData.value.memory);
  }
  return [];
});

const gcData = computed(() => {
  if (currentData.value?.gc) {
    return normalizeGcData(currentData.value.gc);
  }
  return [];
});

function normalizeThread(thread) {
  return {
    name: thread.name,
    status: thread.state || thread.status,
    cpu: thread.cpu,
    deltaTime: thread.deltaTime,
    threadId: thread.id || thread.threadId,
  };
}

function normalizeMemoryData(memory) {
  if (Array.isArray(memory)) {
    return memory;
  }
  return Object.entries(memory).map(([name, info]) => ({
    name,
    used: info.used,
    total: info.total,
  }));
}

function normalizeGcData(gc) {
  if (Array.isArray(gc)) {
    return gc;
  }
  return Object.entries(gc).map(([name, info]) => ({
    name,
    count: info.count,
    time: info.time,
  }));
}

function formatBytes(bytes) {
  if (!bytes) return '-';
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  if (bytes < 1024 * 1024 * 1024) return (bytes / 1024 / 1024).toFixed(1) + ' MB';
  return (bytes / 1024 / 1024 / 1024).toFixed(2) + ' GB';
}

function getPercentage(row) {
  if (!row.total || row.total === 0) return 0;
  return Math.round((row.used / row.total) * 100);
}

function getProgressColor(row) {
  const pct = getPercentage(row);
  if (pct >= 90) return '#f56c6c';
  if (pct >= 70) return '#e6a23c';
  return '#67c23a';
}

function getStatusType(status) {
  if (status === 'RUNNABLE') return 'success';
  if (status === 'BLOCKED') return 'danger';
  if (status === 'WAITING' || status === 'TIMED_WAITING') return 'warning';
  return 'info';
}
</script>

<style scoped>
.dashboard-renderer {
  width: 100%;
}

.dashboard-content {
  width: 100%;
}

.dashboard-empty {
  padding: 20px;
}
</style>
