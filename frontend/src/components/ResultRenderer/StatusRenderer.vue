<template>
  <div>
    <el-tag :type="statusType" size="small">
      {{ statusText }}
    </el-tag>
    <span v-if="message" style="margin-left: 8px; color: #606266; font-size: 13px">
      {{ message }}
    </span>
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

const statusType = computed(() => {
  return props.data?.statusCode === 0 ? 'success' : 'danger';
});

const statusText = computed(() => {
  return props.data?.statusCode === 0 ? '成功' : '失败';
});

const message = computed(() => {
  return props.data?.message || '';
});
</script>
