<template>
  <div>
    <el-tag :type="tagType" size="small">
      {{ tagText }}
    </el-tag>
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
    const classes = props.data.enhancedClasses || 0;
    const methods = props.data.enhancedMethods || 0;
    return `增强成功: ${classes} 类 / ${methods} 方法`;
  }
  return '增强失败';
});
</script>
