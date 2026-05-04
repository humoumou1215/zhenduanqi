<template>
  <div>
    <div style="margin-bottom: 8px; font-weight: 500; font-size: 14px">方法调用栈</div>
    <div v-if="timestamp" style="margin-bottom: 8px; font-size: 12px; color: #909399">
      时间: {{ formatTimestamp(timestamp) }}
    </div>
    <div
      v-if="stackValue"
      style="
        background: #f5f7fa;
        padding: 12px;
        border-radius: 4px;
        font-size: 13px;
        line-height: 1.8;
        overflow-x: auto;
        white-space: pre-wrap;
        word-break: break-all;
      "
    >
      {{ stackValue }}
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
  value:
    'at com.example.TestClass.testMethod(TestClass.java:10)\n' +
    'at com.example.Caller.call(Caller.java:20)\n' +
    'at java.lang.Thread.run(Thread.java:748)',
  ts: Date.now(),
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

const stackValue = computed(() => {
  return currentData.value.value || currentData.value.stackTrace || '';
});

const timestamp = computed(() => {
  return currentData.value.ts || currentData.value.timestamp;
});

function formatTimestamp(ts) {
  if (!ts) return '-';
  const date = new Date(ts);
  return date.toLocaleString();
}
</script>
