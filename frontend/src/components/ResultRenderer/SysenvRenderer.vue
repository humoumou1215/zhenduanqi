<template>
  <div>
    <el-table :data="tableData" stripe size="small" max-height="400">
      <el-table-column prop="key" label="变量名" min-width="250">
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
      共 {{ tableData.length }} 个环境变量
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
  env: {
    JAVA_HOME: '/usr/lib/jvm/java-17',
    PATH: '/usr/local/bin:/usr/bin:/bin',
    USER: 'admin',
    LANG: 'en_US.UTF-8',
    JAVA_OPTS: '-Xms512m -Xmx2048m',
    CATALINA_HOME: '/opt/tomcat',
    SERVER_PORT: '8080',
    OS_NAME: 'Linux',
    OS_VERSION: '5.4.0-generic',
    OS_ARCH: 'amd64',
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
  if (currentData.value?.env) {
    return normalizeEnvData(currentData.value.env);
  }
  if (Array.isArray(currentData.value)) {
    return normalizeEnvData(currentData.value);
  }
  if (typeof currentData.value === 'object') {
    return normalizeEnvData(currentData.value);
  }
  return [];
});

function normalizeEnvData(env) {
  if (Array.isArray(env)) {
    return env.map((item) => {
      if (typeof item === 'string') {
        const parts = item.split('=');
        return {
          key: parts[0] || item,
          value: parts.slice(1).join('=') || '-',
        };
      }
      return {
        key: item.name || item.key || item.variable || '-',
        value: item.value || item.val || '-',
      };
    });
  }
  if (typeof env === 'object') {
    return Object.entries(env)
      .map(([key, value]) => ({
        key,
        value: value != null ? String(value) : '-',
      }))
      .sort((a, b) => a.key.localeCompare(b.key));
  }
  return [];
}
</script>
