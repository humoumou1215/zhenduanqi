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
      style="
        margin-top: 8px;
        font-size: 12px;
        color: #909399;
        font-style: italic;
      "
    >
      后续版本将支持结构化展示
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

const formattedData = computed(() => {
  if (typeof props.data === 'string') {
    return props.data;
  }
  return JSON.stringify(props.data, null, 2);
});
</script>
