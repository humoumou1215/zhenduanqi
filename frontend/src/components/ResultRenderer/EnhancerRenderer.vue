<template>
  <div>
    <el-tag :type="tagType" size="small">
      {{ tagText }}
    </el-tag>
    <div v-if="props.data?.effect" style="margin-top: 8px; color: #606266; font-size: 13px">
      <span>增强耗时: {{ props.data.effect.cost }}ms</span>
      <span style="margin-left: 16px">类数: {{ props.data.effect.classCount }}</span>
      <span style="margin-left: 16px">方法数: {{ props.data.effect.methodCount }}</span>
    </div>
    <div v-if="props.data?.message" style="margin-top: 8px; color: #909399; font-size: 13px">
      {{ props.data.message }}
    </div>
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

const tagType = computed(() => {
  return props.data?.success === true ? 'success' : 'danger';
});

const tagText = computed(() => {
  if (props.data?.success === true) {
    return '增强成功';
  }
  return '增强失败';
});
</script>
