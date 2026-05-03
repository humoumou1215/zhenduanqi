<template>
  <div>
    <el-table :data="tableData" stripe size="small" max-height="400">
      <el-table-column prop="name" label="参数名" min-width="200">
        <template #default="{ row }">
          <span style="font-weight: 500; color: #409eff">{{ row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="value" label="当前值" min-width="150">
        <template #default="{ row }">
          <span style="color: #67c23a">{{ row.value }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="origin" label="默认值" min-width="150">
        <template #default="{ row }">
          <span v-if="row.origin && row.origin !== row.value" style="color: #909399">
            {{ row.origin }}
          </span>
          <span v-else style="color: #c0c4cc">-</span>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="80" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.origin && row.origin !== row.value" size="small" type="warning">
            修改
          </el-tag>
          <el-tag v-else size="small" type="info"> 默认 </el-tag>
        </template>
      </el-table-column>
    </el-table>
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
  options: [
    { name: '-XX:+UseG1GC', value: 'true', origin: 'true' },
    { name: '-XX:MaxGCPauseMillis', value: '200', origin: '200' },
    { name: '-XX:+UseStringDeduplication', value: 'true', origin: 'false' },
    { name: '-Xms512m', value: '512m', origin: '512m' },
    { name: '-Xmx2048m', value: '2048m', origin: '1024m' },
    { name: '-XX:NewRatio', value: '2', origin: '2' },
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

const tableData = computed(() => {
  if (currentData.value?.options) {
    return normalizeOptionsData(currentData.value.options);
  }
  if (Array.isArray(currentData.value)) {
    return normalizeOptionsData(currentData.value);
  }
  return [];
});

function normalizeOptionsData(options) {
  if (Array.isArray(options)) {
    return options.map((opt) => {
      if (typeof opt === 'string') {
        const parts = opt.split('=');
        return {
          name: parts[0] || opt,
          value: parts[1] || '-',
          origin: parts[1] || '-',
        };
      }
      return {
        name: opt.name || opt.key || opt.option || opt.parameter || '-',
        value: opt.value || opt.current || opt.currentValue || '-',
        origin: opt.origin || opt.default || opt.originValue || '-',
      };
    });
  }
  return [];
}
</script>
