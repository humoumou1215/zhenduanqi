<template>
  <el-tag :type="statusType" size="small">
    {{ statusText }}
  </el-tag>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  data: {
    type: Object,
    required: true
  }
});

const statusType = computed(() => {
  return props.data?.statusCode === 0 ? 'success' : 'danger';
});

const statusText = computed(() => {
  return props.data?.statusCode === 0 ? '成功' : '失败';
});
</script>
