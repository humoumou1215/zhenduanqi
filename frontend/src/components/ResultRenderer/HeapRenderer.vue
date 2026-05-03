<template>
  <div>
    <el-table :data="tableData" stripe size="small" max-height="400">
      <el-table-column prop="className" label="类名" min-width="200" />
      <el-table-column prop="count" label="实例数" width="100" align="right" />
      <el-table-column prop="shallowSize" label="浅堆大小" width="120" align="right">
        <template #default="{ row }">
          {{ formatBytes(row.shallowSize) }}
        </template>
      </el-table-column>
      <el-table-column prop="retainedSize" label="深堆大小" width="120" align="right">
        <template #default="{ row }">
          {{ formatBytes(row.retainedSize) }}
        </template>
      </el-table-column>
    </el-table>
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

const defaultHeapData = [
  {
    className: 'java.lang.String',
    count: 1500,
    shallowSize: 123456,
    retainedSize: 456789,
  },
  {
    className: 'java.util.HashMap',
    count: 200,
    shallowSize: 23456,
    retainedSize: 123456,
  },
];

const tableData = computed(() => {
  if (!props.data || Object.keys(props.data).length === 0) {
    return defaultHeapData;
  }
  if (Array.isArray(props.data)) {
    return props.data;
  }
  if (props.data?.histogram) {
    return props.data.histogram;
  }
  if (props.data?.className) {
    return [props.data];
  }
  return defaultHeapData;
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
</script>
