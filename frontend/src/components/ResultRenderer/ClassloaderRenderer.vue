<template>
  <div class="classloader-renderer">
    <!-- 默认模式：类型统计 -->
    <div v-if="mode === 'typeStats'" class="mode-section">
      <el-table :data="typeStatsData" stripe size="small">
        <el-table-column prop="name" label="类加载器名称" min-width="200" />
        <el-table-column prop="numberOfInstances" label="实例数量" width="120" align="right" />
        <el-table-column prop="loadedCountTotal" label="加载类总数" width="120" align="right" />
      </el-table>
    </div>

    <!-- -l 模式：实例统计 -->
    <div v-else-if="mode === 'instanceStats'" class="mode-section">
      <el-table :data="instanceStatsData" stripe size="small">
        <el-table-column prop="name" label="类加载器名称" min-width="200" />
        <el-table-column prop="loadedCount" label="加载类数" width="100" align="right" />
        <el-table-column prop="hash" label="Hash" width="120">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.hash }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="parent" label="父加载器" min-width="150" />
      </el-table>
    </div>

    <!-- -t 模式：继承树 -->
    <div v-else-if="mode === 'inheritanceTree'" class="mode-section">
      <div class="classloader-tree">
        <el-tree :data="treeData" :props="treeProps" default-expand-all size="small">
          <template #default="{ data }">
            <span class="tree-node">
              <el-icon><Connection /></el-icon>
              <span>{{ data.label }}</span>
              <el-tag v-if="data.hash" size="small" type="info" style="margin-left: 8px">
                {{ data.hash }}
              </el-tag>
            </span>
          </template>
        </el-tree>
      </div>
    </div>

    <!-- -c hash 模式：URL 列表 -->
    <div v-else-if="mode === 'urlList'" class="mode-section">
      <div class="url-list-header">
        <span class="loader-name">类加载器: {{ urlListData.loaderName }}</span>
        <el-tag v-if="urlListData.loaderHash" size="small" type="info">
          {{ urlListData.loaderHash }}
        </el-tag>
      </div>
      <el-table :data="urlListData.urls" stripe size="small">
        <el-table-column label="URL">
          <template #default="{ row }">
            <span class="url-cell">{{ row }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- --url-stat 模式：URL 使用统计 -->
    <div v-else-if="mode === 'urlStats'" class="mode-section">
      <div v-for="(loader, index) in urlStatsData" :key="index" class="url-stats-loader">
        <div class="loader-header">
          <span class="loader-name">{{ loader.name }}</span>
          <el-tag v-if="loader.hash" size="small" type="info">
            {{ loader.hash }}
          </el-tag>
        </div>
        <div v-if="loader.usedUrls && loader.usedUrls.length" class="url-section">
          <div class="section-title">
            <el-icon><SuccessFilled /></el-icon>
            <span>已使用 URLs ({{ loader.usedUrls.length }})</span>
          </div>
          <div class="url-grid">
            <span v-for="(url, i) in loader.usedUrls" :key="i" class="url-tag used">
              {{ url }}
            </span>
          </div>
        </div>
        <div v-if="loader.unusedUrls && loader.unusedUrls.length" class="url-section">
          <div class="section-title">
            <el-icon><InfoFilled /></el-icon>
            <span>未使用 URLs ({{ loader.unusedUrls.length }})</span>
          </div>
          <div class="url-grid">
            <span v-for="(url, i) in loader.unusedUrls" :key="i" class="url-tag unused">
              {{ url }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- --url-classes 模式：类与 jar 关系 -->
    <div v-else-if="mode === 'urlClasses'" class="mode-section">
      <el-table :data="urlClassesData" stripe size="small">
        <el-table-column prop="url" label="URL" min-width="250" />
        <el-table-column prop="loadedClassCount" label="加载类数" width="120" align="right" />
      </el-table>
    </div>

    <!-- 原始输出模式 -->
    <div v-else-if="mode === 'raw'" class="mode-section">
      <pre class="raw-output">{{ formattedRaw }}</pre>
    </div>

    <!-- 示例模式 -->
    <div v-else-if="isExample" class="placeholder">
      <el-icon><InfoFilled /></el-icon>
      <span>ClassLoader 信息将显示在这里</span>
    </div>

    <!-- 示例标记 -->
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
import { InfoFilled, SuccessFilled, Connection } from '@element-plus/icons-vue';

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
});

const treeProps = {
  children: 'children',
  label: 'label',
};

// 示例数据
const exampleTypeStats = [
  { name: 'BootstrapClassLoader', numberOfInstances: 1, loadedCountTotal: 200 },
  { name: 'sun.misc.Launcher$ExtClassLoader', numberOfInstances: 1, loadedCountTotal: 50 },
  { name: 'sun.misc.Launcher$AppClassLoader', numberOfInstances: 1, loadedCountTotal: 150 },
];

const exampleInstanceStats = [
  { name: 'BootstrapClassLoader', loadedCount: 200, hash: '@N/A', parent: 'null' },
  { name: 'sun.misc.Launcher$ExtClassLoader', loadedCount: 50, hash: '@12345678', parent: 'BootstrapClassLoader' },
  { name: 'sun.misc.Launcher$AppClassLoader', loadedCount: 150, hash: '@abcdef12', parent: 'sun.misc.Launcher$ExtClassLoader@12345678' },
];

const exampleTreeData = [
  {
    label: 'BootstrapClassLoader',
    isClassLoader: true,
    children: [
      {
        label: 'sun.misc.Launcher$ExtClassLoader@12345678',
        hash: '@12345678',
        isClassLoader: true,
        children: [
          {
            label: 'sun.misc.Launcher$AppClassLoader@abcdef12',
            hash: '@abcdef12',
            isClassLoader: true,
            children: [],
          },
        ],
      },
    ],
  },
];

const exampleUrlList = {
  loaderName: 'sun.misc.Launcher$AppClassLoader',
  loaderHash: '@abcdef12',
  urls: [
    'file:/path/to/classes/',
    'file:/path/to/lib/commons-lang3-3.12.0.jar',
    'file:/path/to/lib/spring-core-5.3.0.jar',
  ],
};

const exampleUrlStats = [
  {
    name: 'sun.misc.Launcher$AppClassLoader',
    hash: '@abcdef12',
    usedUrls: [
      'file:/path/to/classes/',
      'file:/path/to/lib/spring-core-5.3.0.jar',
    ],
    unusedUrls: [
      'file:/path/to/lib/commons-lang3-3.12.0.jar',
    ],
  },
];

const exampleUrlClasses = [
  { url: 'file:/path/to/classes/', loadedClassCount: 50 },
  { url: 'file:/path/to/lib/spring-core-5.3.0.jar', loadedClassCount: 100 },
];

// 判断数据模式
const mode = computed(() => {
  const d = props.data;
  if (!d || Object.keys(d).length === 0) return 'example';

  // 类型统计模式
  if (d.typeStats || (Array.isArray(d) && d[0]?.name && d[0]?.numberOfInstances !== undefined)) {
    return 'typeStats';
  }

  // 实例统计模式
  if (d.instanceStats || (Array.isArray(d) && d[0]?.name && d[0]?.loadedCount !== undefined)) {
    return 'instanceStats';
  }

  // 继承树模式
  if (d.tree || d.loaders || (Array.isArray(d) && d[0]?.children) || isTreeLike(d)) {
    return 'inheritanceTree';
  }

  // URL 列表模式
  if (d.urls && d.loaderName) {
    return 'urlList';
  }

  // URL 统计模式
  if (d.urlStats || (Array.isArray(d) && d[0]?.usedUrls && d[0]?.unusedUrls)) {
    return 'urlStats';
  }

  // URL 类关系模式
  if (d.urlClasses || (Array.isArray(d) && d[0]?.url && d[0]?.loadedClassCount !== undefined)) {
    return 'urlClasses';
  }

  return 'raw';
});

const typeStatsData = computed(() => {
  const d = props.data;
  if (!d) return exampleTypeStats;
  if (d.typeStats) return d.typeStats;
  if (Array.isArray(d) && d[0]?.name && d[0]?.numberOfInstances !== undefined) return d;
  return exampleTypeStats;
});

const instanceStatsData = computed(() => {
  const d = props.data;
  if (!d) return exampleInstanceStats;
  if (d.instanceStats) return d.instanceStats;
  if (Array.isArray(d) && d[0]?.name && d[0]?.loadedCount !== undefined) return d;
  return exampleInstanceStats;
});

const treeData = computed(() => {
  const d = props.data;
  if (!d || Object.keys(d).length === 0) return exampleTreeData;
  if (d.tree) return parseClassloaderTree(d.tree);
  if (d.loaders) return parseClassloaderTree(d.loaders);
  return parseClassloaderTree(d);
});

const urlListData = computed(() => {
  const d = props.data;
  if (!d) return exampleUrlList;
  if (d.urls && d.loaderName) return d;
  return exampleUrlList;
});

const urlStatsData = computed(() => {
  const d = props.data;
  if (!d) return exampleUrlStats;
  if (d.urlStats) return d.urlStats;
  if (Array.isArray(d) && d[0]?.usedUrls && d[0]?.unusedUrls) return d;
  return exampleUrlStats;
});

const urlClassesData = computed(() => {
  const d = props.data;
  if (!d) return exampleUrlClasses;
  if (d.urlClasses) return d.urlClasses;
  if (Array.isArray(d) && d[0]?.url && d[0]?.loadedClassCount !== undefined) return d;
  return exampleUrlClasses;
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

function isTreeLike(data) {
  if (Array.isArray(data)) {
    return data.some((item) => item.children || item.loaders);
  }
  return !!data.children || !!data.loaders;
}

function parseClassloaderTree(input) {
  if (!input) return [];

  if (Array.isArray(input)) {
    return input.map((item) => parseNode(item));
  }

  if (typeof input === 'string') {
    const lines = input.split('\n').filter((l) => l.trim());
    return parseTreeFromLines(lines);
  }

  return [parseNode(input)];
}

function parseNode(node) {
  if (typeof node === 'string') {
    return {
      label: node,
      hash: extractHash(node),
      isClassLoader: true,
      children: [],
    };
  }

  const result = {
    label: node.name || node.label || node.classLoader || JSON.stringify(node),
    hash: node.hash || node.hashCode || node.classLoaderHash || extractHash(node.name),
    isClassLoader: true,
    children: [],
  };

  if (node.children && Array.isArray(node.children)) {
    result.children = node.children.map(parseNode);
  } else if (node.loaders && Array.isArray(node.loaders)) {
    result.children = node.loaders.map(parseNode);
  }

  return result;
}

function parseTreeFromLines(lines) {
  const root = [];
  const stack = [{ level: -1, children: root }];

  for (const line of lines) {
    const level = countLeadingSymbols(line);
    const label = line.replace(/^[+\-|\s]+/, '').trim();

    while (stack.length > 1 && stack[stack.length - 1].level >= level) {
      stack.pop();
    }

    const node = {
      label,
      hash: extractHash(label),
      isClassLoader: true,
      children: [],
    };

    stack[stack.length - 1].children.push(node);
    stack.push({ level, children: node.children });
  }

  return root;
}

function countLeadingSymbols(str) {
  let count = 0;
  for (const ch of str) {
    if (ch === ' ' || ch === '|' || ch === '+' || ch === '-' || ch === '├' || ch === '└') {
      count++;
    } else {
      break;
    }
  }
  return Math.floor(count / 2);
}

function extractHash(str) {
  if (!str) return '';
  const match = str.match(/@[a-f0-9]+/i);
  return match ? match[0] : '';
}
</script>

<style scoped>
.classloader-renderer {
  width: 100%;
}

.mode-section {
  width: 100%;
}

.classloader-tree {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 4px;
}

.url-list-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  margin-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}

.loader-name {
  font-weight: 500;
  color: #303133;
}

.url-cell {
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 12px;
  word-break: break-all;
}

.url-stats-loader {
  margin-bottom: 16px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
}

.loader-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-weight: 500;
}

.url-section {
  margin-bottom: 12px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
}

.url-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.url-tag {
  display: inline-block;
  padding: 4px 8px;
  font-size: 12px;
  border-radius: 4px;
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
  word-break: break-all;
  max-width: 100%;
}

.url-tag.used {
  background: #f0f9eb;
  color: #67c23a;
}

.url-tag.unused {
  background: #f4f4f5;
  color: #909399;
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
  background: #fef0f0;
  border-radius: 4px;
}
</style>
