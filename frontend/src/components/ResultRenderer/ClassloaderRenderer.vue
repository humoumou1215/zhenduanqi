<template>
  <div>
    <div v-if="treeData && treeData.length" class="classloader-tree">
      <el-tree :data="treeData" :props="treeProps" default-expand-all size="small">
        <template #default="{ data }">
          <span class="tree-node">
            <el-icon v-if="data.isClassLoader"><Connection /></el-icon>
            <el-icon v-else><Document /></el-icon>
            <span>{{ data.label }}</span>
            <el-tag v-if="data.hash" size="small" type="info" style="margin-left: 8px">
              {{ data.hash }}
            </el-tag>
          </span>
        </template>
      </el-tree>
    </div>
    <div v-else-if="isExample" class="placeholder">
      <el-icon><InfoFilled /></el-icon>
      <span>ClassLoader 继承树将显示在这里</span>
    </div>
    <pre v-else class="raw-output">{{ formattedRaw }}</pre>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { InfoFilled, Connection, Document } from '@element-plus/icons-vue';

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

const defaultTreeData = [
  {
    label: 'sun.misc.Launcher$ExtClassLoader@12345678',
    hash: '@12345678',
    isClassLoader: true,
    children: [
      {
        label: 'sun.misc.Launcher$AppClassLoader@abcdef12',
        hash: '@abcdef12',
        isClassLoader: true,
        children: [
          {
            label: 'com.example.MyClassLoader@1a2b3c4d',
            hash: '@1a2b3c4d',
            isClassLoader: true,
            children: [],
          },
        ],
      },
    ],
  },
];

function parseClassloaderTree(input) {
  if (!input) return [];

  if (Array.isArray(input)) {
    return input.map((item) => parseNode(item));
  }

  if (typeof input === 'string') {
    const lines = input.split('\n').filter((l) => l.trim());
    return lines.map((line) => ({
      label: line.trim(),
      hash: extractHash(line),
      isClassLoader: true,
      children: [],
    }));
  }

  if (input.loaders) {
    return parseNode(input);
  }

  return [];
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

function extractHash(str) {
  if (!str) return '';
  const match = str.match(/@[a-f0-9]+/i);
  return match ? match[0] : '';
}

const treeData = computed(() => {
  if (!props.data || Object.keys(props.data).length === 0) {
    return null;
  }
  const parsed = parseClassloaderTree(props.data);
  return parsed.length > 0 ? parsed : null;
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
