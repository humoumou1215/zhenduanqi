<template>
  <div class="sample-card" :class="{ 'is-focused': isFocused }" @click="$emit('click')">
    <div class="sample-header" @click.stop="toggleExpand">
      <el-icon class="expand-icon" :class="{ expanded: isExpanded }">
        <ArrowDown />
      </el-icon>
      <span class="sample-index">#{{ index + 1 }}</span>
      <span class="sample-time">{{ formatTime(sample.timestamp) }}</span>
      <template v-if="mode === 'trace'">
        <span class="sample-cost">总耗时: {{ formatCost(sample.cost || sample.totalCost) }}</span>
      </template>
      <template v-else>
        <span v-if="sample.threadName" class="sample-thread">线程: {{ sample.threadName }}</span>
        <span v-if="sample.priority" class="sample-priority">优先级: {{ sample.priority }}</span>
      </template>
      <el-button v-if="isFocused" size="small" link type="primary" class="focus-indicator">
        聚焦
      </el-button>
    </div>

    <div v-if="isExpanded" class="sample-content">
      <TraceNode v-if="mode === 'trace'" :nodes="parsedNodes" :max-cost="maxCost" />
      <StackNode v-else :nodes="parsedNodes" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { ArrowDown } from '@element-plus/icons-vue';
import TraceNode from './TraceNode.vue';
import StackNode from './StackNode.vue';

const props = defineProps({
  sample: {
    type: Object,
    default: () => ({}),
  },
  index: {
    type: Number,
    default: 0,
  },
  mode: {
    type: String,
    default: 'trace',
    validator: (v) => ['trace', 'stack'].includes(v),
  },
  maxCost: {
    type: Number,
    default: 0,
  },
  isFocused: {
    type: Boolean,
    default: false,
  },
});

defineEmits(['click']);

const isExpanded = ref(true);

const parsedNodes = computed(() => {
  if (props.mode === 'trace') {
    return parseTraceNodes(props.sample);
  }
  return parseStackNodes(props.sample);
});

function parseTraceNodes(sample) {
  if (sample.nodes) return sample.nodes;
  if (sample.trace) return normalizeTraceNodes(sample.trace);
  if (Array.isArray(sample)) return normalizeTraceNodes(sample);
  return [];
}

function normalizeTraceNodes(trace) {
  if (!Array.isArray(trace)) return [];
  return trace.map((item) => ({
    method: item.method || `${item.class || ''}:${item.methodName || item.method || ''}()`,
    cost: item.cost || item.time || 0,
    line: item.line,
    throws: item.throws,
    depth: item.depth || 0,
    children: item.children ? normalizeTraceNodes(item.children) : [],
    aggregated: item.aggregated,
  }));
}

function parseStackNodes(sample) {
  if (sample.nodes) return sample.nodes;
  if (sample.stack) return normalizeStackNodes(sample.stack);
  if (sample.stackTrace) return normalizeStackNodes(sample.stackTrace);
  return [];
}

function normalizeStackNodes(stack) {
  if (typeof stack === 'string') {
    return stack
      .split('\n')
      .filter((line) => line.trim())
      .map((line) => ({
        method: line,
        depth: 0,
        children: [],
      }));
  }
  if (!Array.isArray(stack)) return [];
  return stack.map((item) => ({
    method: item.method || item.classMethod || item,
    line: item.line,
    depth: item.depth || 0,
    children: item.children ? normalizeStackNodes(item.children) : [],
  }));
}

function toggleExpand() {
  isExpanded.value = !isExpanded.value;
}

function formatTime(ts) {
  if (!ts) return '-';
  const date = new Date(ts);
  return date.toLocaleTimeString();
}

function formatCost(cost) {
  if (cost == null) return '-';
  return `${cost.toFixed(2)}ms`;
}
</script>

<style scoped>
.sample-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.sample-card:hover {
  border-color: #c0c4cc;
}

.sample-card.is-focused {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.sample-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  cursor: pointer;
}

.expand-icon {
  transition: transform 0.3s;
  color: #c0c4cc;
}

.expand-icon.expanded {
  transform: rotate(180deg);
}

.sample-index {
  font-weight: 500;
  color: #409eff;
  min-width: 30px;
}

.sample-time {
  color: #909399;
  font-size: 12px;
  min-width: 80px;
}

.sample-cost,
.sample-thread {
  color: #606266;
  font-size: 13px;
}

.sample-priority {
  color: #909399;
  font-size: 12px;
}

.focus-indicator {
  margin-left: auto;
}

.sample-content {
  padding: 8px 12px 12px;
  background: #fafafa;
  border-top: 1px solid #ebeef5;
}
</style>
