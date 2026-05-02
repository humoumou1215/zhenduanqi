<template>
  <div>
    <el-table :data="tableData" stripe size="small" max-height="400">
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
      <el-table-column prop="deltaTime" label="Delta(ms)" width="100" align="right">
        <template #default="{ row }">
          {{ row.deltaTime != null ? row.deltaTime : '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="lockedMonitors" label="阻塞锁" width="80" align="center">
        <template #default="{ row }">
          <span v-if="row.lockedMonitors > 0" style="color: #f56c6c">
            {{ row.lockedMonitors }}
          </span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="threadId" label="线程ID" width="80" align="center" />
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

const defaultThreadData = [
  { name: 'main', status: 'RUNNABLE', cpu: 0.5, deltaTime: 100, lockedMonitors: 0, threadId: 1 },
  { name: 'gc-thread-1', status: 'WAITING', cpu: 0.0, deltaTime: 50, lockedMonitors: 0, threadId: 2 },
  { name: 'http-nio-8080-exec-1', status: 'BLOCKED', cpu: 2.3, deltaTime: 200, lockedMonitors: 1, threadId: 15 },
  { name: 'pool-1-thread-1', status: 'TIMED_WAITING', cpu: 0.1, deltaTime: 80, lockedMonitors: 0, threadId: 8 }
];

const tableData = computed(() => {
  if (!props.data || Object.keys(props.data).length === 0) {
    return defaultThreadData;
  }
  if (Array.isArray(props.data)) {
    return props.data;
  }
  if (props.data?.threads) {
    return props.data.threads;
  }
  if (props.data?.name || props.data?.threadId) {
    return [props.data];
  }
  return defaultThreadData;
});

const isExample = computed(() => {
  return !props.data || Object.keys(props.data).length === 0;
});

function getStatusType(status) {
  if (status === 'RUNNABLE') return 'success';
  if (status === 'BLOCKED') return 'danger';
  if (status === 'WAITING' || status === 'TIMED_WAITING') return 'warning';
  return 'info';
}
</script>
