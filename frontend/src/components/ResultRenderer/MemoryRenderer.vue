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
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  data: {
    type: Object,
    required: true
  }
});

const tableData = computed(() => {
  if (Array.isArray(props.data)) {
    return props.data;
  }
  if (props.data?.memory) {
    return props.data.memory;
  }
  return [props.data];
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
