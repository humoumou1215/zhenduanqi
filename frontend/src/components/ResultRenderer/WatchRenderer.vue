<template>
  <div>
    <div style="margin-bottom: 8px; font-weight: 500; font-size: 14px">
      方法观察: {{ className }}.{{ methodName }}
    </div>
    <div v-if="timestamp" style="margin-bottom: 8px; font-size: 12px; color: #909399">
      时间: {{ formatTimestamp(timestamp) }} | 耗时: {{ cost }}ms | 位置: {{ location }}
    </div>
    <div v-if="params" style="margin-bottom: 12px">
      <div style="font-weight: 500; margin-bottom: 4px; font-size: 13px">入参:</div>
      <pre class="watch-code-block">{{ formatValue(params) }}</pre>
    </div>
    <div v-if="returnObj !== undefined" style="margin-bottom: 12px">
      <div style="font-weight: 500; margin-bottom: 4px; font-size: 13px">返回值:</div>
      <pre class="watch-code-block">{{ formatValue(returnObj) }}</pre>
    </div>
    <div v-if="throwExp" style="margin-bottom: 12px">
      <div style="font-weight: 500; margin-bottom: 4px; font-size: 13px; color: #f56c6c">异常:</div>
      <pre class="watch-code-block" style="color: #f56c6c">{{ formatValue(throwExp) }}</pre>
    </div>
    <div v-if="object" style="margin-bottom: 12px">
      <div style="font-weight: 500; margin-bottom: 4px; font-size: 13px">对象:</div>
      <pre class="watch-code-block">{{ formatValue(object) }}</pre>
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
  cost: 12.34,
  ts: Date.now(),
  location: 'AtExit',
  params: [
    { type: 'String', value: 'test-param' },
    { type: 'Integer', value: 123 },
  ],
  returnObj: { type: 'String', value: 'result-value' },
  throwExp: null,
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

const cost = computed(() => {
  const c = currentData.value.cost || 0;
  return typeof c === 'number' ? c.toFixed(2) : c;
});

const timestamp = computed(() => {
  return currentData.value.ts || currentData.value.timestamp;
});

const location = computed(() => {
  return currentData.value.location || 'AtExit';
});

const params = computed(() => {
  return currentData.value.params || currentData.value.parameters;
});

const returnObj = computed(() => {
  return currentData.value.returnObj || currentData.value.returnValue;
});

const throwExp = computed(() => {
  return currentData.value.throwExp || currentData.value.throwException;
});

const object = computed(() => {
  return currentData.value.object || currentData.value.target;
});

function formatValue(value) {
  if (value === null) return 'null';
  if (value === undefined) return 'undefined';
  if (typeof value === 'string') return value;
  if (typeof value === 'object') {
    try {
      return JSON.stringify(value, null, 2);
    } catch {
      return String(value);
    }
  }
  return String(value);
}

function formatTimestamp(ts) {
  if (!ts) return '-';
  const date = new Date(ts);
  return date.toLocaleString();
}
</script>

<style scoped>
.watch-code-block {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.6;
  overflow-x: auto;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
