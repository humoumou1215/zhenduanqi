<template>
  <div>
    <el-table :data="tableData" stripe size="small">
      <el-table-column prop="name" label="区域" min-width="120" />
      <el-table-column prop="used" label="已用" width="120" align="right">
        <template #default="{ row }">
          {{ formatBytes(row.used) }}
        </template>
      </el-table-column>
      <el-table-column prop="total" label="总量" width="120" align="right">
        <template #default="{ row }">
          {{ formatBytes(row.total) }}
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
    </el-table>
    <div v-if="isExample" style="margin-top: 8px; font-size: 11px; color: #909399; font-style: italic">
      (示例数据)
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  data: {
    type: Object,
    default: () => ({})
  }
});

const defaultMemoryData = [
  { name: 'Heap Memory', used: 268435456, total: 536870912 },
  { name: 'Non-Heap Memory', used: 67108864, total: 134217728 },
  { name: 'Eden Space', used: 134217728, total: 268435456 },
  { name: 'Survivor Space', used: 16777216, total: 33554432 },
  { name: 'Old Gen', used: 117440512, total: 268435456 },
  { name: 'Metaspace', used: 50331648, total: 100663296 }
];

const tableData = computed(() => {
  if (!props.data || Object.keys(props.data).length === 0) {
    return defaultMemoryData;
  }
  if (Array.isArray(props.data)) {
    return props.data;
  }
  if (props.data?.memory) {
    return props.data.memory;
  }
  if (props.data?.name) {
    return [props.data];
  }
  return defaultMemoryData;
});

const isExample = computed(() => {
  return !props.data || Object.keys(props.data).length === 0;
});

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
</script>
