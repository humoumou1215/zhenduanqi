<template>
  <div>
    <el-tag :type="tagType" size="small">
      {{ tagText }}
    </el-tag>
    <div v-if="currentData?.effect" style="margin-top: 8px; color: #606266; font-size: 13px">
      <span>增强耗时: {{ currentData.effect.cost }}ms</span>
      <span style="margin-left: 16px">类数: {{ currentData.effect.classCount }}</span>
      <span style="margin-left: 16px">方法数: {{ currentData.effect.methodCount }}</span>
    </div>
    <div v-if="currentData?.message" style="margin-top: 8px; color: #909399; font-size: 13px">
      {{ currentData.message }}
    </div>
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
  success: true,
  effect: {
    cost: 24,
    classCount: 1,
    methodCount: 3
  }
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

const tagType = computed(() => {
  return currentData.value?.success === true ? 'success' : 'danger';
});

const tagText = computed(() => {
  if (currentData.value?.success === true) {
    return '增强成功';
  }
  return '增强失败';
});
</script>
