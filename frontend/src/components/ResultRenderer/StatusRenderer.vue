<template>
  <div>
    <el-tag :type="statusType" size="small">
      {{ statusText }}
    </el-tag>
    <span v-if="message" style="margin-left: 8px; color: #606266; font-size: 13px">
      {{ message }}
    </span>
    <div v-if="isExample" style="margin-top: 4px; font-size: 11px; color: #909399; font-style: italic">
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

const defaultData = {
  statusCode: 0,
  message: '命令执行成功'
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

const statusType = computed(() => {
  return currentData.value?.statusCode === 0 ? 'success' : 'danger';
});

const statusText = computed(() => {
  return currentData.value?.statusCode === 0 ? '成功' : '失败';
});

const message = computed(() => {
  return currentData.value?.message || '';
});
</script>
