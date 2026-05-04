<template>
  <div>
    <div v-if="sourceCode || classLoaders || location" class="jad-container">
      <div class="jad-header">
        <el-tag type="info" size="small">
          <el-icon><Document /></el-icon>
          {{ sourceCode?.className || '反编译源码' }}
        </el-tag>
        <el-tag v-if="sourceCode?.sourcePath" type="info" size="small">
          {{ sourceCode.sourcePath }}
        </el-tag>
        <span v-if="executionTime" class="execution-time">执行耗时: {{ executionTime }}ms</span>
      </div>

      <el-tabs v-if="showTabs" v-model="activeTab" class="jad-tabs">
        <el-tab-pane label="源代码" name="source">
          <CodeBlock v-if="sourceCode?.code" :code="sourceCode.code" />
          <div v-else class="no-source">无源代码</div>
        </el-tab-pane>
        <el-tab-pane label="ClassLoader" name="classloader" v-if="classLoaders">
          <ClassloaderTree :classLoaders="classLoaders" />
        </el-tab-pane>
        <el-tab-pane label="Location" name="location" v-if="location">
          <div class="location-info">
            <el-icon><Location /></el-icon>
            <span>{{ location }}</span>
          </div>
        </el-tab-pane>
      </el-tabs>

      <div v-else class="jad-source-only">
        <CodeBlock v-if="sourceCode?.code" :code="sourceCode.code" />
      </div>
    </div>

    <div v-else-if="isExample" class="placeholder">
      <el-icon><InfoFilled /></el-icon>
      <div class="placeholder-content">
        <p>反编译的 Java 源码将显示在这里，如：jad java.lang.String</p>
        <div class="placeholder-example">
          <p class="example-label">示例数据</p>
          <CodeBlock :code="defaultSourceCode.code" />
        </div>
      </div>
    </div>

    <div v-else-if="multipleClassLoaders" class="multiple-classloaders-warning">
      <div class="warning-header">
        <el-icon><WarningFilled /></el-icon>
        <span>找到多个匹配的 ClassLoader</span>
      </div>
      <el-table :data="multipleClassLoaders" size="small" class="classloader-table">
        <el-table-column prop="hashcode" label="Hashcode" width="120" />
        <el-table-column prop="classloader" label="ClassLoader" />
      </el-table>
      <p class="hint">请使用 `jad -c &lt;hashcode&gt;` 选择一个 ClassLoader</p>
    </div>

    <pre v-else class="raw-output">{{ formattedRaw }}</pre>
  </div>
</template>

<script setup>
import { ref, computed, defineAsyncComponent } from 'vue';
import { InfoFilled, Document, WarningFilled, Location } from '@element-plus/icons-vue';
import CodeBlock from './CodeBlock.vue';

const ClassloaderTree = defineAsyncComponent(() => import('./ClassloaderTree.vue'));

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
});

const activeTab = ref('source');

const defaultSourceCode = {
  className: 'java.lang.String',
  sourcePath: '/java/lang/String.java',
  code: `public final class String
    implements java.io.Serializable,
               Comparable<String>,
               CharSequence {
    private final char[] value;
    private int hash;

    public String() {
        this.value = new char[0];
    }

    public String(String original) {
        this.value = original.value;
        this.hash = original.hash;
    }

    public int length() {
        return value.length;
    }

    public boolean isEmpty() {
        return value.length == 0;
    }

    public char charAt(int index) {
        if (index < 0 || index >= value.length) {
            throw new StringIndexOutOfBoundsException(index);
        }
        return value[index];
    }

    public int hashCode() {
        int h = hash;
        if (h == 0 && value.length > 0) {
            char val[] = value;
            for (int i = 0; i < value.length; i++) {
                h = 31 * h + val[i];
            }
            hash = h;
        }
        return h;
    }
}`,
};

function parseJadOutput(input) {
  if (!input) return null;

  if (typeof input === 'string') {
    return parseStringJad(input);
  }

  if (input.className || input.code || input.source) {
    return normalizeJadResult(input);
  }

  return null;
}

function parseStringJad(str) {
  const lines = str.split('\n');
  const codeLines = [];
  let className = '';
  let sourcePath = '';
  const classLoaders = [];
  let location = '';
  let inClassLoaderSection = false;
  let currentClassLoader = null;

  for (const line of lines) {
    if (line.includes('ClassLoader:')) {
      inClassLoaderSection = true;
      continue;
    }

    if (inClassLoaderSection) {
      if (line.includes('Location:') || line.match(/^[A-Za-z]:/)) {
        inClassLoaderSection = false;
      } else if (line.match(/^[├└│]/)) {
        const classloaderMatch = line.replace(/[├└│─\s]/g, '').trim();
        if (classloaderMatch) {
          currentClassLoader = { name: classloaderMatch };
          classLoaders.push(currentClassLoader);
        }
        continue;
      } else if (line.match(/^\s*ClassLoader\s*[/\\]/)) {
        const loaderName = line.split('/').pop().trim();
        if (currentClassLoader) {
          currentClassLoader.name = loaderName;
        }
        continue;
      }
    }

    if (line.includes('Location:')) {
      location = line.replace('Location:', '').trim();
      continue;
    }

    const classMatch = line.match(/^public\s+(final\s+)?class\s+(\S+)/);
    if (classMatch) {
      className = classMatch[2];
    }

    const pathMatch = line.match(/Source:\s*(.+)/);
    if (pathMatch) {
      sourcePath = pathMatch[1].trim();
    }

    if (!inClassLoaderSection) {
      codeLines.push(line);
    }
  }

  const result = {
    className,
    sourcePath,
    code: codeLines.join('\n'),
  };

  if (classLoaders.length > 0) {
    result.classLoaders = classLoaders;
  }

  if (location) {
    result.location = location;
  }

  return result;
}

function normalizeJadResult(data) {
  return {
    className: data.className || data.classname || '',
    sourcePath: data.sourcePath || data.source || '',
    code: data.code || data.source || '',
    classLoaders: data.classLoaders || data.classloader || null,
    location: data.location || data.file || '',
    executionTime: data.executionTime || data.cost || null,
  };
}

function parseMultipleClassLoaders(data) {
  if (data.classLoaders && Array.isArray(data.classLoaders)) {
    return data.classLoaders.map((cl) => ({
      hashcode: cl.hashcode || cl.hash || '',
      classloader: cl.name || cl.className || JSON.stringify(cl),
    }));
  }
  return null;
}

const parsedData = computed(() => {
  if (!props.data || Object.keys(props.data).length === 0) {
    return null;
  }
  return parseJadOutput(props.data);
});

const sourceCode = computed(() => {
  if (!parsedData.value) return null;
  return {
    className: parsedData.value.className,
    sourcePath: parsedData.value.sourcePath,
    code: parsedData.value.code,
  };
});

const classLoaders = computed(() => {
  if (!parsedData.value) return null;
  return parsedData.value.classLoaders;
});

const location = computed(() => {
  if (!parsedData.value) return '';
  return parsedData.value.location || '';
});

const executionTime = computed(() => {
  if (!parsedData.value) return null;
  return parsedData.value.executionTime;
});

const showTabs = computed(() => {
  return classLoaders.value || location.value;
});

const multipleClassLoaders = computed(() => {
  if (!props.data || !props.data.classLoaders) return null;
  return parseMultipleClassLoaders(props.data);
});

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
.jad-container {
  background: #1e1e1e;
  border-radius: 4px;
  overflow: hidden;
}

.jad-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #21252b;
  border-bottom: 1px solid #3e4451;
  flex-wrap: wrap;
}

.execution-time {
  margin-left: auto;
  color: #858585;
  font-size: 12px;
}

.jad-tabs {
  background: #1e1e1e;
}

.jad-tabs :deep(.el-tabs__header) {
  margin: 0;
  background: #2d2d2d;
  padding: 0 12px;
}

.jad-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.jad-tabs :deep(.el-tabs__item) {
  color: #858585;
  font-size: 13px;
}

.jad-tabs :deep(.el-tabs__item.is-active) {
  color: #61afef;
}

.jad-tabs :deep(.el-tabs__active-bar) {
  background-color: #61afef;
}

.jad-tabs :deep(.el-tabs__content) {
  padding: 0;
}

.jad-source-only {
  background: #1e1e1e;
}

.no-source {
  color: #858585;
  padding: 12px;
  text-align: center;
}

.location-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  color: #abb2bf;
  font-size: 13px;
  background: #1e1e1e;
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
  color: #909399;
  font-size: 13px;
  padding: 12px;
  background: #fdf6ec;
  border-radius: 4px;
}

.placeholder-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.placeholder-example {
  margin-top: 8px;
}

.example-label {
  color: #e6a23c;
  font-size: 12px;
  margin-bottom: 8px;
}

.multiple-classloaders-warning {
  background: #2d2d30;
  border: 1px solid #3e3e42;
  border-radius: 4px;
  padding: 16px;
}

.warning-header {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #e06c75;
  font-size: 14px;
  margin-bottom: 12px;
}

.classloader-table {
  background: #1e1e1e;
}

.classloader-table :deep(.el-table__header th) {
  background: #2d2d2d;
  color: #abb2bf;
}

.classloader-table :deep(.el-table__body tr) {
  background: #1e1e1e;
}

.classloader-table :deep(.el-table__body td) {
  color: #abb2bf;
  border-color: #3e3e3e;
}

.hint {
  color: #858585;
  font-size: 12px;
  margin-top: 12px;
}

@media (max-width: 768px) {
  .jad-header {
    padding: 8px;
  }

  .execution-time {
    width: 100%;
    margin-left: 0;
    margin-top: 4px;
  }

  .jad-tabs :deep(.el-tabs__item) {
    font-size: 12px;
    padding: 0 12px;
  }
}
</style>
