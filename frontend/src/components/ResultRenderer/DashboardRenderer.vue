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
          <div class="gc-header">
            <el-button v-if="hasBaseline" type="warning" size="small" @click="refreshBaseline">
              刷新基线
            </el-button>
            <span v-if="baselineTimestamp" class="baseline-info">
              基线时间: {{ formatBaselineTime(baselineTimestamp) }}
            </span>
          </div>
          <el-table :data="gcDataWithDelta" stripe size="small">
            <el-table-column prop="name" label="GC收集器" min-width="150" />
            <el-table-column label="累积次数" width="100" align="center">
              <template #header>
                <el-tooltip content="从 JVM 启动到当前的总 GC 次数" placement="top">
                  <span>累积次数</span>
                </el-tooltip>
              </template>
              <template #default="{ row }">
                {{ row.count }}
              </template>
            </el-table-column>
            <el-table-column label="本次增量" width="100" align="center">
              <template #header>
                <el-tooltip content='与上次采集的差值（首次采集显示"首次"）' placement="top">
                  <span>本次增量</span>
                </el-tooltip>
              </template>
              <template #default="{ row }">
                <span v-if="row.countDelta === null" class="first-tag">首次</span>
                <span v-else-if="row.countDelta === 0" class="delta-zero">+0</span>
                <span v-else class="delta-positive">+{{ row.countDelta }}</span>
              </template>
            </el-table-column>
            <el-table-column label="累积耗时(ms)" width="120" align="right">
              <template #header>
                <el-tooltip content="从 JVM 启动到当前的总 GC 耗时" placement="top">
                  <span>累积耗时(ms)</span>
                </el-tooltip>
              </template>
              <template #default="{ row }">
                {{ row.time != null ? row.time : '-' }}
              </template>
            </el-table-column>
            <el-table-column label="本次增量" width="100" align="center">
              <template #header>
                <el-tooltip content='与上次采集的差值（首次采集显示"首次"）' placement="top">
                  <span>本次增量</span>
                </el-tooltip>
              </template>
              <template #default="{ row }">
                <span v-if="row.timeDelta === null" class="first-tag">首次</span>
                <span v-else-if="row.timeDelta === 0" class="delta-zero">+0</span>
                <span v-else class="delta-positive">+{{ row.timeDelta }}</span>
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
import { useDashboardBaselineStore } from '@/stores/dashboardBaseline';

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
  serverId: {
    type: String,
    default: null,
  },
});

const baselineStore = useDashboardBaselineStore();

const defaultData = {
  threads: [
    { name: 'main', state: 'RUNNABLE', cpu: 0.5, deltaTime: 100, id: 1 },
    { name: 'gc-thread-1', state: 'WAITING', cpu: 0.0, deltaTime: 50, id: 2 },
    { name: 'http-nio-8080-exec-1', state: 'BLOCKED', cpu: 2.3, deltaTime: 200, id: 15 },
  ],
  memoryInfo: {
    heap: [
      { name: 'heap', used: 268435456, total: 536870912, max: 838860800 },
      { name: 'ps_eden_space', used: 134217728, total: 268435456, max: 268435456 },
      { name: 'ps_survivor_space', used: 16777216, total: 33554432, max: 33554432 },
      { name: 'ps_old_gen', used: 117440512, total: 268435456, max: 536870912 },
    ],
    nonheap: [
      { name: 'nonheap', used: 57752248, total: 60882944, max: -1 },
      { name: 'metaspace', used: 37178712, total: 38928384, max: -1 },
    ],
  },
  gcInfos: [
    { name: 'ps_scavenge', collectionCount: 15, collectionTime: 120 },
    { name: 'ps_marksweep', collectionCount: 3, collectionTime: 80 },
  ],
};

const defaultGcData = [
  { name: 'PS Scavenge', count: 15, time: 120 },
  { name: 'PS MarkSweep', count: 3, time: 80 },
];

const currentData = computed(() => {
  if (!props.data || Object.keys(props.data).length === 0) {
    return defaultData;
  }
  return props.data;
});

const hasData = computed(() => {
  const data = currentData.value;
  return (
    data &&
    (data.threads?.length ||
      data.memoryInfo?.heap?.length ||
      data.memoryInfo?.nonheap?.length ||
      data.gcInfos?.length)
  );
});

const isExample = computed(() => {
  return !props.data || Object.keys(props.data).length === 0;
});

const hasBaseline = computed(() => {
  return baselineStore.getBaseline(props.serverId) !== null;
});

const baselineTimestamp = computed(() => {
  const baseline = baselineStore.getBaseline(props.serverId);
  return baseline?.timestamp || null;
});

const currentGcData = computed(() => {
  const data = currentData.value;
  if (data?.gcInfos) {
    return normalizeGcInfos(data.gcInfos);
  }
  if (data?.gc) {
    return normalizeGcData(data.gc);
  }
  return [];
});

const gcDataWithDelta = computed(() => {
  const currentGc = currentGcData.value;
  if (!currentGc.length) return [];

  const baseline = baselineStore.getBaseline(props.serverId);
  if (!baseline || !baseline.gc) {
    baselineStore.setBaseline(props.serverId, currentGc);
    return currentGc.map((item) => ({
      ...item,
      countDelta: null,
      timeDelta: null,
    }));
  }

  const deltaData = baselineStore.calculateDelta(currentGc, baseline.gc);
  if (!deltaData) {
    return currentGc.map((item) => ({
      ...item,
      countDelta: null,
      timeDelta: null,
    }));
  }

  return deltaData;
});

const threadData = computed(() => {
  if (currentData.value?.threads) {
    return currentData.value.threads.map(normalizeThread);
  }
  return [];
});

const memoryData = computed(() => {
  const data = currentData.value;
  if (data?.memoryInfo) {
    return normalizeMemoryInfo(data.memoryInfo);
  }
  if (data?.memory) {
    return normalizeMemoryData(data.memory);
  }
  return [];
});

function refreshBaseline() {
  const currentGc = currentGcData.value;
  if (currentGc.length) {
    baselineStore.setBaseline(props.serverId, currentGc);
  }
}

function normalizeThread(thread) {
  return {
    name: thread.name,
    status: thread.state || thread.status,
    cpu: thread.cpu,
    deltaTime: thread.deltaTime,
    threadId: thread.id ?? thread.threadId,
  };
}

function normalizeMemoryInfo(memoryInfo) {
  const result = [];
  if (memoryInfo.heap && Array.isArray(memoryInfo.heap)) {
    memoryInfo.heap.forEach((item) => {
      result.push({
        name: formatMemoryName(item.name),
        used: item.used,
        total: item.total,
        max: item.max,
      });
    });
  }
  return result;
}

function normalizeMemoryData(memory) {
  if (Array.isArray(memory)) {
    return memory.map((item) => ({
      name: formatMemoryName(item.name),
      used: item.used,
      total: item.total,
      max: item.max,
    }));
  }
  return Object.entries(memory).map(([name, info]) => ({
    name: formatMemoryName(name),
    used: info.used,
    total: info.total,
    max: info.max,
  }));
}

function normalizeGcInfos(gcInfos) {
  if (!Array.isArray(gcInfos)) return [];
  return gcInfos.map((gc) => ({
    name: formatGcName(gc.name),
    count: gc.collectionCount,
    time: gc.collectionTime,
  }));
}

function normalizeGcData(gc) {
  if (Array.isArray(gc)) {
    return gc.map((item) => ({
      name: formatGcName(item.name),
      count: item.count ?? item.collectionCount,
      time: item.time ?? item.collectionTime,
    }));
  }
  return Object.entries(gc).map(([name, info]) => ({
    name: formatGcName(name),
    count: info.count ?? info.collectionCount,
    time: info.time ?? info.collectionTime,
  }));
}

function formatMemoryName(name) {
  const nameMap = {
    heap: 'Heap Memory',
    nonheap: 'Non-Heap Memory',
    ps_eden_space: 'Eden Space',
    ps_survivor_space: 'Survivor Space',
    ps_old_gen: 'Old Gen',
    metaspace: 'Metaspace',
    code_cache: 'Code Cache',
    compressed_class_space: 'Compressed Class Space',
  };
  return nameMap[name] || name;
}

function formatGcName(name) {
  const nameMap = {
    ps_scavenge: 'PS Scavenge',
    ps_marksweep: 'PS MarkSweep',
    copy: 'Copy',
    marksweepcompact: 'MarkSweepCompact',
    g1_young_generation: 'G1 Young',
    g1_old_generation: 'G1 Old',
  };
  return nameMap[name?.toLowerCase()] || name;
}

function formatBytes(bytes) {
  if (!bytes) return '-';
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  if (bytes < 1024 * 1024 * 1024) return (bytes / 1024 / 1024).toFixed(1) + ' MB';
  return (bytes / 1024 / 1024 / 1024).toFixed(2) + ' GB';
}

function getPercentage(row) {
  const base = row.max != null && row.max > 0 ? row.max : row.total;
  if (!base || base <= 0) return 0;
  return Math.round((row.used / base) * 100);
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

function formatBaselineTime(timestamp) {
  if (!timestamp) return '';
  const date = new Date(timestamp);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
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

.gc-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.baseline-info {
  font-size: 12px;
  color: #909399;
}

.first-tag {
  color: #909399;
  font-style: italic;
}

.delta-zero {
  color: #67c23a;
}

.delta-positive {
  color: #e6a23c;
}
</style>
