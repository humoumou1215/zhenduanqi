<template>
  <div class="stack-node">
    <div v-for="(node, index) in nodes" :key="index" class="stack-node-item">
      <StackNodeRow
        :node="node"
        :depth="node.depth || 0"
        :prefix="getPrefix(index, nodes.length)"
        :is-root="index === 0"
      />
      <StackNode v-if="node.children && node.children.length > 0" :nodes="node.children" />
    </div>
  </div>
</template>

<script setup>
import StackNodeRow from './StackNodeRow.vue';

const props = defineProps({
  nodes: {
    type: Array,
    default: () => [],
  },
});

function getPrefix(index, total) {
  if (index === total - 1) {
    return '└─';
  }
  return '├─';
}
</script>

<style scoped>
.stack-node {
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 12px;
  line-height: 1.8;
}

.stack-node-item {
  position: relative;
}
</style>
