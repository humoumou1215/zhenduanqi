<template>
  <div>
    <div v-if="classInfo" class="class-info-container">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="类名">
          <code>{{ classInfo.className || classInfo.classname }}</code>
        </el-descriptions-item>
        <el-descriptions-item label="类加载器 Hash">
          <el-tag v-if="classInfo.classLoaderHash" size="small" type="info">
            {{ classInfo.classLoaderHash }}
          </el-tag>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="修饰符" :span="2">
          <el-tag size="small">{{ classInfo.modifier || '-' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="接口" :span="2">
          <div v-if="classInfo.interfaces && classInfo.interfaces.length">
            <el-tag
              v-for="iface in classInfo.interfaces"
              :key="iface"
              size="small"
              style="margin-right: 4px"
            >
              {{ iface }}
            </el-tag>
          </div>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="父类">
          <code>{{ classInfo.superClass || classInfo.superclass || '-' }}</code>
        </el-descriptions-item>
        <el-descriptions-item label="ClassLoader 类名">
          <code>{{ classInfo.classLoader || '-' }}</code>
        </el-descriptions-item>
        <el-descriptions-item label="codeSource" :span="2">
          <code class="code-source">{{ classInfo.codeSource || '-' }}</code>
        </el-descriptions-item>
      </el-descriptions>

      <div v-if="classInfo.methods && classInfo.methods.length" class="methods-section">
        <h4 style="margin: 12px 0 8px 0; font-size: 14px">方法列表</h4>
        <el-table :data="classInfo.methods" size="small" max-height="200">
          <el-table-column prop="name" label="方法名" width="150" />
          <el-table-column prop="modifier" label="修饰符" width="100" />
          <el-table-column prop="descriptor" label="签名" min-width="200" />
        </el-table>
      </div>
    </div>
    <div v-else-if="isExample" class="placeholder">
      <el-icon><InfoFilled /></el-icon>
      <span>请输入类名后执行命令，如：sc -d java.lang.String</span>
    </div>
    <pre v-else class="raw-output">{{ formattedRaw }}</pre>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { InfoFilled } from '@element-plus/icons-vue';

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
});

const defaultClassInfo = {
  className: 'java.lang.String',
  classLoaderHash: '@12345678',
  modifier: 'public final',
  interfaces: ['java.io.Serializable', 'java.lang.Comparable', 'java.lang.CharSequence'],
  superClass: 'java.lang.Object',
  classLoader: 'sun.misc.Launcher$AppClassLoader@12345678',
  codeSource: 'jar:file:/usr/local/app/lib/java-1.0.jar!/java/lang/String.class',
  methods: [
    { name: 'length', modifier: 'public', descriptor: '()I' },
    { name: 'charAt', modifier: 'public', descriptor: '(I)C' },
    { name: 'substring', modifier: 'public', descriptor: '(I)Ljava/lang/String;' },
  ],
};

const classInfo = computed(() => {
  if (!props.data || Object.keys(props.data).length === 0) {
    return null;
  }
  if (props.data.className || props.data.classname || props.data.classLoaderHash) {
    return normalizeClassInfo(props.data);
  }
  return null;
});

function normalizeClassInfo(data) {
  const result = {};
  if (data.className) result.className = data.className;
  else if (data.classname) result.className = data.classname;
  if (data.classLoaderHash) result.classLoaderHash = data.classLoaderHash;
  if (data.modifier) result.modifier = data.modifier;
  if (data.interfaces) result.interfaces = data.interfaces;
  else if (data.interface)
    result.interfaces = Array.isArray(data.interface) ? data.interface : [data.interface];
  if (data.superClass) result.superClass = data.superClass;
  else if (data.superclass) result.superClass = data.superclass;
  if (data.classLoader) result.classLoader = data.classLoader;
  if (data.codeSource) result.codeSource = data.codeSource;
  if (data.methods) result.methods = data.methods;
  return result;
}

const isExample = computed(() => {
  return !props.data || Object.keys(props.data).length === 0;
});

const formattedRaw = computed(() => {
  if (typeof props.data === 'string') {
    return props.data;
  }
  return JSON.stringify(props.data, null, 2);
});
</script>

<style scoped>
.class-info-container {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
}

.code-source {
  font-size: 12px;
  word-break: break-all;
}

.methods-section {
  margin-top: 12px;
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
