<template>
  <div>
    <pre
      style="
        background: #f5f7fa;
        padding: 12px;
        border-radius: 4px;
        font-size: 13px;
        line-height: 1.6;
        overflow-x: auto;
        white-space: pre-wrap;
        word-break: break-all;
      "
      >{{ formattedData }}</pre
    >
    <div
      v-if="isExample"
      style="margin-top: 8px; font-size: 11px; color: #909399; font-style: italic"
    >
      (示例数据)
    </div>
    <div v-else style="margin-top: 8px; font-size: 12px; color: #909399; font-style: italic">
      后续版本将支持结构化展示
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
  type: 'unknown',
  message: '这是一个示例响应数据',
  timestamp: Date.now(),
  value: {
    field1: '示例值1',
    field2: '示例值2',
    nested: {
      key: 'value',
    },
  },
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

const formattedData = computed(() => {
  if (typeof currentData.value === 'string') {
    return currentData.value;
  }
  return JSON.stringify(currentData.value, null, 2);
});
</script>
