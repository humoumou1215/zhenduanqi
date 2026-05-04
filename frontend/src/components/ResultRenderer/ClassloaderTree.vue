<template>
  <div class="classloader-tree">
    <div v-if="flatClassLoaders.length > 0" class="tree-list">
      <div
        v-for="(loader, index) in flatClassLoaders"
        :key="index"
        class="tree-item"
        :class="{ 'has-parent': loader.parent !== undefined }"
      >
        <span class="tree-connector" :style="{ paddingLeft: loader.level * 16 + 'px' }">
          <span v-if="loader.level > 0" class="connector-line">├─</span>
        </span>
        <span class="loader-name">{{ loader.name }}</span>
      </div>
    </div>
    <div v-else class="no-classloader">无 ClassLoader 信息</div>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  classLoaders: {
    type: [Array, Object],
    default: () => [],
  },
});

const flatClassLoaders = computed(() => {
  if (!props.classLoaders) return [];

  const flatList = [];

  function flatten(loaders, level = 0) {
    if (Array.isArray(loaders)) {
      loaders.forEach((loader, index) => {
        const isLast = index === loaders.length - 1;
        flatList.push({
          name: typeof loader === 'string' ? loader : (loader.name || loader.className || JSON.stringify(loader)),
          level,
          isLast,
          parent: level > 0 ? level - 1 : undefined,
        });
      });
    } else if (typeof loaders === 'object') {
      const name = loaders.name || loaders.className || JSON.stringify(loaders);
      flatList.push({ name, level: 0 });
    }
  }

  flatten(props.classLoaders);
  return flatList;
});
</script>

<style scoped>
.classloader-tree {
  background: #1e1e1e;
  padding: 12px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
}

.tree-list {
  display: flex;
  flex-direction: column;
}

.tree-item {
  display: flex;
  align-items: center;
  color: #abb2bf;
}

.tree-connector {
  display: inline-block;
  color: #5c6370;
  margin-right: 4px;
  white-space: pre;
}

.connector-line {
  color: #5c6370;
}

.loader-name {
  color: #e06c75;
}

.no-classloader {
  color: #858585;
  text-align: center;
  padding: 12px;
}

@media (max-width: 768px) {
  .classloader-tree {
    padding: 8px;
    font-size: 12px;
  }
}
</style>
