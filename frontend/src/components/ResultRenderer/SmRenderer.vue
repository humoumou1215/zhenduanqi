<template>
  <div>
    <div v-if="isSimpleMode" class="sm-container">
      <el-table :data="simpleList" size="small">
        <el-table-column prop="className" label="类名" width="250">
          <template #default="{ row }">
            <code>{{ row.className || row.classname }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="methodName" label="方法名">
          <template #default="{ row }">
            <code>{{ row.methodName || row.methodname }}</code>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div v-else-if="isDetailMode" class="sm-container">
      <KeyValueTable :data="methodDetail" :label-map="labelMap" />
    </div>

    <div v-else-if="isMultiDetailMode" class="sm-container">
      <el-collapse accordion>
        <el-collapse-item
          v-for="(method, index) in methodList"
          :key="index"
          :name="index"
        >
          <template #title>
            <div class="collapse-title">
              <el-tag size="small" type="info">{{ method.modifier || '-' }}</el-tag>
              <code>{{ method.declaringClass || method.declaringclass }}.{{ method.methodName || method.methodname }}</code>
            </div>
          </template>
          <KeyValueTable :data="method" :label-map="labelMap" />
        </el-collapse-item>
      </el-collapse>
    </div>

    <div v-else-if="isExample" class="placeholder">
      <el-icon><InfoFilled /></el-icon>
      <span>请输入类名后执行命令，如：sm java.lang.String</span>
    </div>

    <pre v-else class="raw-output">{{ formattedRaw }}</pre>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { InfoFilled } from '@element-plus/icons-vue';
import KeyValueTable from './KeyValueTable.vue';

const props = defineProps({
  data: {
    type: [Object, Array],
    default: () => ({}),
  },
});

const labelMap = {
  'declaring-class': '声明类',
  'declaringClass': '声明类',
  'declaringclass': '声明类',
  'method-name': '方法名',
  'methodName': '方法名',
  'methodname': '方法名',
  'modifier': '修饰符',
  'annotation': '注解',
  'annotations': '注解',
  'parameters': '参数',
  'parameter-types': '参数类型',
  'parameterTypes': '参数类型',
  'parametertypes': '参数类型',
  'return': '返回类型',
  'return-type': '返回类型',
  'returnType': '返回类型',
  'returntype': '返回类型',
  'exceptions': '异常',
  'exception-types': '异常类型',
  'exceptionTypes': '异常类型',
  'exceptiontypes': '异常类型',
};

const defaultSimpleList = [
  { className: 'java.lang.String', methodName: 'length' },
  { className: 'java.lang.String', methodName: 'charAt' },
  { className: 'java.lang.String', methodName: 'substring' },
];

const defaultMethodDetail = {
  'declaring-class': 'java.lang.String',
  'method-name': 'length',
  'modifier': 'public',
  'annotation': [],
  'parameters': [],
  'return': 'int',
  'exceptions': [],
};

const defaultMethodList = [
  {
    'declaring-class': 'java.lang.String',
    'method-name': 'length',
    'modifier': 'public',
    'return': 'int',
  },
  {
    'declaring-class': 'java.lang.String',
    'method-name': 'charAt',
    'modifier': 'public',
    'parameters': ['int'],
    'return': 'char',
  },
];

const isExample = computed(() => {
  return !props.data || (Array.isArray(props.data) && props.data.length === 0) || (typeof props.data === 'object' && Object.keys(props.data).length === 0);
});

const isSimpleMode = computed(() => {
  if (!props.data) return false;
  if (Array.isArray(props.data)) {
    return props.data.length > 0 && (props.data[0].methodName || props.data[0].methodname) && !props.data[0]['declaring-class'] && !props.data[0].declaringClass;
  }
  return false;
});

const isDetailMode = computed(() => {
  if (!props.data) return false;
  if (typeof props.data === 'object' && !Array.isArray(props.data)) {
    return (props.data['declaring-class'] || props.data.declaringClass || props.data.declaringclass) && (props.data['method-name'] || props.data.methodName || props.data.methodname);
  }
  return false;
});

const isMultiDetailMode = computed(() => {
  if (!props.data) return false;
  if (Array.isArray(props.data)) {
    return props.data.length > 0 && (props.data[0]['declaring-class'] || props.data[0].declaringClass || props.data[0].declaringclass);
  }
  return false;
});

const simpleList = computed(() => {
  if (isSimpleMode.value) {
    return props.data;
  }
  return defaultSimpleList;
});

const methodDetail = computed(() => {
  if (isDetailMode.value) {
    return props.data;
  }
  return defaultMethodDetail;
});

const methodList = computed(() => {
  if (isMultiDetailMode.value) {
    return props.data;
  }
  return defaultMethodList;
});

const formattedRaw = computed(() => {
  if (typeof props.data === 'string') {
    return props.data;
  }
  return JSON.stringify(props.data, null, 2);
});
</script>

<style scoped>
.sm-container {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
}

.collapse-title {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.collapse-title code {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.raw-output {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.6;
  overflow-x: auto;
  white-space: pre-wrap;
  margin: 0;
}

.placeholder {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #909399;
  font-size: 13px;
  padding: 12px;
  background: #fdf6ec;
  border-radius: 4px;
}
</style>
