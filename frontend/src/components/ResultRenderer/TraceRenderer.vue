<template>
  <div>
    <div style="margin-bottom: 8px; font-weight: 500; font-size: 14px">
      方法调用追踪: {{ className }}.{{ methodName }}
    </div>
    <div v-if="timestamp" style="margin-bottom: 8px; font-size: 12px; color: #909399">
      时间: {{ formatTimestamp(timestamp) }} | 总耗时: {{ totalCost }}ms
    </div>
    <div
      style="
        background: #f5f7fa;
        padding: 12px;
        border-radius: 4px;
        font-size: 13px;
        line-height: 1.8;
        overflow-x: auto;
        white-space: pre;
        font-family: monospace;
      "
    >
      {{ formattedTrace }}
    </div>
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
  className: 'com.example.TestClass',
  methodName: 'testMethod',
  cost: 123.45,
  ts: Date.now(),
  trace: [
    {
      className: 'com.example.TestClass',
      methodName: 'testMethod',
      cost: 123.45,
      depth: 0,
    },
    {
      className: 'com.example.Helper',
      methodName: 'helperMethod',
      cost: 45.67,
      depth: 1,
    },
    {
      className: 'com.example.Helper',
      methodName: 'anotherMethod',
      cost: 23.45,
      depth: 1,
    },
  ],
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

const className = computed(() => {
  return currentData.value.className || currentData.value.class || '';
});

const methodName = computed(() => {
  return currentData.value.methodName || currentData.value.method || '';
});

const totalCost = computed(() => {
  const cost = currentData.value.cost || currentData.value.totalCost || 0;
  return typeof cost === 'number' ? cost.toFixed(2) : cost;
});

const timestamp = computed(() => {
  return currentData.value.ts || currentData.value.timestamp;
});

const formattedTrace = computed(() => {
  const trace = currentData.value.trace || currentData.value.children || [];
  if (trace.length === 0) {
    const rawValue = currentData.value.value || currentData.value.raw || '';
    if (rawValue) return rawValue;
    return formatSingleTrace(currentData.value);
  }
  return formatTraceTree(trace);
});

function formatSingleTrace(item) {
  const cost = item.cost || 0;
  const cls = item.className || item.class || '';
  const method = item.methodName || item.method || '';
  return `---[${cost.toFixed(2)}ms]${cls}:${method}()`;
}

function formatTraceTree(trace, depth = 0) {
  let result = '';
  for (const item of trace) {
    const indent = '  '.repeat(depth);
    const prefix = depth === 0 ? '`---' : '+---';
    const cost = item.cost || 0;
    const cls = item.className || item.class || '';
    const method = item.methodName || item.method || '';
    result += `${indent}${prefix}[${cost.toFixed(2)}ms]${cls}:${method}()\n`;
    if (item.children && item.children.length > 0) {
      result += formatTraceTree(item.children, depth + 1);
    }
  }
  return result.trim();
}

function formatTimestamp(ts) {
  if (!ts) return '-';
  const date = new Date(ts);
  return date.toLocaleString();
}
</script>
