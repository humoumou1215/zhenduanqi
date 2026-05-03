<template>
  <div>
    <div style="margin-bottom: 8px; font-weight: 500; font-size: 14px">
      监控统计: {{ currentData.class }}.{{ currentData.method }}
    </div>
    <el-table :data="monitorData" size="small" style="width: 100%">
      <el-table-column prop="timestamp" label="时间" width="180">
        <template #default="scope">
          {{ formatTimestamp(scope.row.timestamp) }}
        </template>
      </el-table-column>
      <el-table-column prop="total" label="总调用" width="80" />
      <el-table-column prop="success" label="成功" width="80" />
      <el-table-column prop="fail" label="失败" width="80" />
      <el-table-column prop="avgRt" label="平均耗时(ms)" width="120" />
      <el-table-column prop="successRate" label="成功率">
        <template #default="scope">
          <el-tag
            :type="
              scope.row.successRate >= 90
                ? 'success'
                : scope.row.successRate >= 70
                  ? 'warning'
                  : 'danger'
            "
            size="small"
          >
            {{ scope.row.successRate }}%
          </el-tag>
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

const defaultData = {
  timestamp: Date.now(),
  class: 'com.example.TestClass',
  method: 'testMethod',
  total: 10,
  success: 9,
  fail: 1,
  avgRt: 50,
};

const currentData = computed(() => {
  if (!props.data || Object.keys(props.data).length === 0) {
    return defaultData;
  }
  return props.data;
});

const isExample = computed(() => {
  return !props.data || Object.keys(props.data).length === 0;
});

const monitorData = computed(() => {
  const item = currentData.value;
  const successRate = item.total > 0 ? Math.round((item.success / item.total) * 100) : 0;
  return [
    {
      timestamp: item.timestamp,
      total: item.total,
      success: item.success,
      fail: item.fail,
      avgRt: item.avgRt,
      successRate,
    },
  ];
});

function formatTimestamp(timestamp) {
  if (!timestamp) return '-';
  const date = new Date(timestamp);
  return date.toLocaleString();
}
</script>
