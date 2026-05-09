<template>
  <div>
    <el-table :data="tableData" stripe size="small" max-height="400">
      <el-table-column prop="key" label="属性名" min-width="250">
        <template #default="{ row }">
          <span style="font-weight: 500; color: #409eff">{{ row.key }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="value" label="值" min-width="300">
        <template #default="{ row }">
          <span style="word-break: break-all">{{ row.value }}</span>
        </template>
      </el-table-column>
    </el-table>
    <div
      v-if="isExample"
      style="margin-top: 8px; font-size: 11px; color: #909399; font-style: italic"
    >
      (示例数据)
    </div>
    <div v-if="tableData.length > 0" style="margin-top: 8px; font-size: 12px; color: #909399">
      共 {{ tableData.length }} 个系统属性
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
  properties: {
    java.version: '17.0.8',
    java.home: '/usr/lib/jvm/java-17',
    java.vendor: 'Oracle Corporation',
    os.name: 'Linux',
    os.version: '5.4.0-generic',
    os.arch: 'amd64',
    user.name: 'admin',
    user.home: '/home/admin',
    user.dir: '/opt/app',
    java.class.path: '/opt/app/lib/*',
    java.library.path: '/usr/java/packages/lib',
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

const tableData = computed(() => {
  if (currentData.value?.properties) {
    return normalizePropertiesData(currentData.value.properties);
  }
  if (Array.isArray(currentData.value)) {
    return normalizePropertiesData(currentData.value);
  }
  if (typeof currentData.value === 'object') {
    return normalizePropertiesData(currentData.value);
  }
  return [];
});

function normalizePropertiesData(properties) {
  if (Array.isArray(properties)) {
    return properties.map((item) => {
      if (typeof item === 'string') {
        const parts = item.split('=');
        return {
          key: parts[0] || item,
          value: parts.slice(1).join('=') || '-',
        };
      }
      return {
        key: item.name || item.key || item.property || '-',
        value: item.value || item.val || '-',
      };
    });
  }
  if (typeof properties === 'object') {
    return Object.entries(properties)
      .map(([key, value]) => ({
        key,
        value: value != null ? String(value) : '-',
      }))
      .sort((a, b) => a.key.localeCompare(b.key));
  }
  return [];
}
</script>
