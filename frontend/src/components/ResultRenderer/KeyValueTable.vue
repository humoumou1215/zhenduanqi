<template>
  <el-descriptions :column="columns" border size="small">
    <el-descriptions-item
      v-for="(value, key) in data"
      :key="key"
      :label="labelMap[key] || key"
    >
      <template v-if="Array.isArray(value)">
        <el-tag v-for="(item, i) in value" :key="i" size="small" style="margin-right: 4px">
          {{ item }}
        </el-tag>
      </template>
      <code v-else-if="typeof value === 'string'">{{ value }}</code>
      <span v-else>{{ value }}</span>
    </el-descriptions-item>
  </el-descriptions>
</template>

<script setup>
const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
  labelMap: {
    type: Object,
    default: () => ({}),
  },
  columns: {
    type: Number,
    default: 2,
  },
});
</script>
