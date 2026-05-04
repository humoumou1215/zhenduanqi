<template>
  <div class="memory-renderer">
    <div class="memory-header">
      <span>内存信息</span>
      <el-button size="small" @click="refreshBaseline" :loading="refreshing">刷新基线</el-button>
    </div>

    <el-table :data="tableData" stripe size="small">
      <el-table-column prop="name" label="区域" min-width="120" />
      <el-table-column prop="used" label="已用" width="100" align="right">
        <template #default="{ row }">
          {{ formatBytes(row.used) }}
        </template>
      </el-table-column>
      <el-table-column prop="delta" label="本次增量" width="100" align="right">
        <template #default="{ row }">
          <span :class="getDeltaClass(row.delta)">
            {{ formatDelta(row.delta) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="使用率" width="180">
        <template #default="{ row }">
          <div style="display: flex; align-items: center; gap: 8px">
            <el-progress
              :percentage="getPercentage(row)"
              :color="getProgressColor(row)"
              :stroke-width="10"
              style="flex: 1"
            />
            <span style="width: 40px; text-align: right">{{ getPercentage(row) }}%</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="trend" label="趋势" width="60" align="center">
        <template #default="{ row }">
          <span :class="getTrendClass(row.trend)">{{ row.trend }}</span>
        </template>
      </el-table-column>
    </el-table>

    <div
      v-if="isExample"
      style="margin-top: 8px; font-size: 11px; color: #909399; font-style: italic"
    >
      (示例数据)
    </div>

    <div class="trend-legend">
      <span class="legend-item">
        <span class="trend-arrow">▲</span>
        增长
      </span>
      <span class="legend-item">
        <span class="trend-arrow">▼</span>
        下降
      </span>
      <span class="legend-item">
        <span class="trend-dash">—</span>
        持平
      </span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue';
import { useServerStore } from '../../stores/servers';

const STORAGE_KEY = 'zhenduanqi_memory_baselines';

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
  serverId: {
    type: String,
    default: '',
  },
});

const serverStore = useServerStore();
const baselines = ref({});
const refreshing = ref(false);

const defaultMemoryData = [
  { name: 'Heap Memory', used: 268435456, total: 536870912, max: 4294967296 },
  { name: '├─ Eden Space', used: 134217728, total: 268435456, max: 268435456 },
  { name: '├─ Survivor Space', used: 16777216, total: 33554432, max: 33554432 },
  { name: '└─ Old Gen', used: 117440512, total: 268435456, max: 268435456 },
  { name: 'Non-Heap Memory', used: 67108864, total: 134217728, max: -1 },
  { name: '├─ Metaspace', used: 50331648, total: 100663296, max: -1 },
  { name: '└─ Code Cache', used: 16777216, total: 33554432, max: -1 },
];

const isExample = computed(() => {
  return !props.data || Object.keys(props.data).length === 0;
});

const currentBaseline = computed(() => {
  const effectiveServerId = props.serverId || serverStore.currentServerId || 'default';
  return baselines.value[effectiveServerId];
});

const tableData = computed(() => {
  let rawData;

  if (!props.data || Object.keys(props.data).length === 0) {
    rawData = defaultMemoryData;
  } else if (Array.isArray(props.data)) {
    rawData = props.data;
  } else if (props.data?.memory) {
    rawData = props.data.memory;
  } else if (props.data?.memoryInfo) {
    rawData = normalizeFromMemoryInfo(props.data.memoryInfo);
  } else if (props.data?.name) {
    rawData = [props.data];
  } else {
    rawData = defaultMemoryData;
  }

  return rawData.map((item) => {
    const normalized = normalizeMemoryItem(item);
    const delta = calculateDelta(normalized.name, normalized.used);
    const trend = calculateTrend(delta);

    return {
      ...normalized,
      delta,
      trend,
    };
  });
});

onMounted(() => {
  loadBaselines();
});

watch(
  () => props.data,
  () => {
    saveCurrentBaseline();
  },
  { deep: true }
);

function loadBaselines() {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
      const parsed = JSON.parse(stored);
      baselines.value = parsed.memory_baselines || {};
    }
  } catch (e) {
    console.warn('Failed to load memory baselines:', e);
    baselines.value = {};
  }
}

function saveCurrentBaseline() {
  if (isExample.value) return;

  const effectiveServerId = props.serverId || serverStore.currentServerId || 'default';
  const memoryData = {};

  if (Array.isArray(props.data)) {
    props.data.forEach((item) => {
      const normalized = normalizeMemoryItem(item);
      memoryData[normalized.key] = { used: normalized.used };
    });
  } else if (props.data?.memory) {
    props.data.memory.forEach((item) => {
      const normalized = normalizeMemoryItem(item);
      memoryData[normalized.key] = { used: normalized.used };
    });
  }

  baselines.value[effectiveServerId] = {
    timestamp: new Date().toISOString(),
    memory: memoryData,
  };

  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ memory_baselines: baselines.value }));
  } catch (e) {
    console.warn('Failed to save memory baselines:', e);
  }
}

function refreshBaseline() {
  refreshing.value = true;
  const effectiveServerId = props.serverId || serverStore.currentServerId || 'default';

  baselines.value[effectiveServerId] = {
    timestamp: new Date().toISOString(),
    memory: {},
  };

  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ memory_baselines: baselines.value }));
  } catch (e) {
    console.warn('Failed to refresh baselines:', e);
  }

  setTimeout(() => {
    refreshing.value = false;
  }, 300);
}

function calculateDelta(name, currentUsed) {
  if (!currentBaseline.value?.memory) return null;

  const key = getMemoryKey(name);
  const baseline = currentBaseline.value.memory[key];

  if (!baseline || baseline.used === undefined) return null;

  return currentUsed - baseline.used;
}

function calculateTrend(delta) {
  if (delta === null || delta === undefined) return '—';
  if (delta > 1024) return '▲';
  if (delta < -1024) return '▼';
  return '—';
}

function normalizeMemoryItem(item) {
  const name = formatMemoryName(item.name || item);
  const key = getMemoryKey(name);
  const used = item.used ?? item.memUsed ?? 0;
  const total = item.total ?? item.memTotal ?? item.max ?? -1;
  const max = item.max ?? -1;

  return { name, key, used, total, max };
}

function normalizeFromMemoryInfo(memoryInfo) {
  const result = [];

  if (memoryInfo.heap && Array.isArray(memoryInfo.heap)) {
    memoryInfo.heap.forEach((item) => {
      result.push({
        name: item.name,
        used: item.used,
        total: item.total,
        max: item.max,
      });
    });
  }

  if (memoryInfo.nonheap && Array.isArray(memoryInfo.nonheap)) {
    memoryInfo.nonheap.forEach((item) => {
      result.push({
        name: item.name,
        used: item.used,
        total: item.total,
        max: item.max,
      });
    });
  }

  return result;
}

function getMemoryKey(name) {
  const keyMap = {
    'Heap Memory': 'heap',
    '├─ Eden Space': 'eden',
    '├─ Survivor Space': 'survivor',
    '└─ Old Gen': 'old',
    'Non-Heap Memory': 'nonheap',
    '├─ Metaspace': 'metaspace',
    '└─ Code Cache': 'code_cache',
  };
  return keyMap[name] || name.toLowerCase().replace(/\s+/g, '_');
}

function formatMemoryName(name) {
  const nameMap = {
    heap: 'Heap Memory',
    nonheap: 'Non-Heap Memory',
    ps_eden_space: '├─ Eden Space',
    'eden space': '├─ Eden Space',
    eden: '├─ Eden Space',
    ps_survivor_space: '├─ Survivor Space',
    'survivor space': '├─ Survivor Space',
    survivor: '├─ Survivor Space',
    ps_old_gen: '└─ Old Gen',
    'old space': '└─ Old Gen',
    'old gen': '└─ Old Gen',
    old: '└─ Old Gen',
    metaspace: '├─ Metaspace',
    code_cache: '└─ Code Cache',
    'code cache': '└─ Code Cache',
  };
  return nameMap[name?.toLowerCase()] || nameMap[name] || name;
}

function formatBytes(bytes) {
  if (bytes === null || bytes === undefined || bytes === -1) return '-';
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  if (bytes < 1024 * 1024 * 1024) return (bytes / 1024 / 1024).toFixed(1) + ' MB';
  return (bytes / 1024 / 1024 / 1024).toFixed(2) + ' GB';
}

function formatDelta(delta) {
  if (delta === null || delta === undefined) return '首次';
  if (delta === 0) return '—';
  const sign = delta > 0 ? '+' : '';
  return sign + formatBytes(Math.abs(delta));
}

function getDeltaClass(delta) {
  if (delta === null || delta === undefined) return 'delta-initial';
  if (delta > 0) return 'delta-positive';
  if (delta < 0) return 'delta-negative';
  return 'delta-neutral';
}

function getTrendClass(trend) {
  if (trend === '▲') return 'trend-up';
  if (trend === '▼') return 'trend-down';
  return 'trend-neutral';
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
</script>

<style scoped>
.memory-renderer {
  width: 100%;
}

.memory-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-weight: 500;
  color: #303133;
}

.delta-positive {
  color: #f56c6c;
}

.delta-negative {
  color: #67c23a;
}

.delta-neutral {
  color: #909399;
}

.delta-initial {
  color: #409eff;
  font-style: italic;
}

.trend-up {
  color: #f56c6c;
  font-size: 14px;
}

.trend-down {
  color: #67c23a;
  font-size: 14px;
}

.trend-neutral {
  color: #909399;
}

.trend-legend {
  margin-top: 8px;
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #606266;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.trend-arrow {
  font-size: 12px;
}

.trend-arrow:has(+ .trend-up) {
  color: #f56c6c;
}

.trend-dash {
  color: #909399;
}
</style>
