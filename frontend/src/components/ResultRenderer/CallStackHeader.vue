<template>
  <div class="callstack-header">
    <div class="header-main">
      <span class="command-label">{{ titleLabel }}:</span>
      <span class="command-value">{{ command }}</span>
      <el-button v-if="isRunning" type="danger" size="small" @click="$emit('stop')">停止</el-button>
    </div>
    <div class="header-stats">
      <span class="stat-item">{{ statLabel }}: {{ sampleCount }} 次</span>
      <span v-if="mode === 'trace'" class="stat-item">耗时阈值: {{ costThreshold || '-' }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  mode: {
    type: String,
    default: 'trace',
    validator: (v) => ['trace', 'stack'].includes(v),
  },
  command: {
    type: String,
    default: '',
  },
  sampleCount: {
    type: Number,
    default: 0,
  },
  isRunning: {
    type: Boolean,
    default: false,
  },
  costThreshold: {
    type: String,
    default: '',
  },
});

defineEmits(['stop']);

const titleLabel = computed(() => {
  return props.mode === 'trace' ? '追踪' : '堆栈';
});

const statLabel = computed(() => {
  return props.mode === 'trace' ? '已追踪' : '已捕获';
});
</script>

<style scoped>
.callstack-header {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px 4px 0 0;
  margin-bottom: 8px;
}

.header-main {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.command-label {
  font-weight: 500;
  font-size: 14px;
  color: #303133;
}

.command-value {
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 13px;
  color: #409eff;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.header-stats {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #909399;
}

.stat-item {
  display: flex;
  align-items: center;
}
</style>
