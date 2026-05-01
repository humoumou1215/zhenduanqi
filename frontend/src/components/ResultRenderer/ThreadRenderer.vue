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
      <el-table-column prop="cpu" label="CPU%" width="70" align="right" />
      <el-table-column prop="deltaTime" label="Delta(ms)" width="100" align="right" />
      <el-table-column prop="lockedMonitors" label="阻塞锁" width="80" align="center">
        <template #default="{ row }">
          <span v-if="row.lockedMonitors > 0" style="color: #f56c6c">
            {{ row.lockedMonitors }}
          </span>
          <span v-else>-</span>
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
  if (props.data?.threads) {
    return props.data.threads;
  }
  return [props.data];
});

function getStatusType(status) {
  if (status === 'RUNNABLE') return 'success';
  if (status === 'BLOCKED') return 'danger';
  if (status === 'WAITING') return 'warning';
  return 'info';
}
</script>
