<template>
  <div class="trace-node">
    <div v-for="(node, index) in nodes" :key="index" class="trace-node-item">
      <TraceNodeRow
        :node="node"
        :depth="node.depth || 0"
        :max-cost="maxCost"
        :prefix="getPrefix(index, nodes.length)"
      />
      <TraceNode
        v-if="node.children && node.children.length > 0"
        :nodes="node.children"
        :max-cost="maxCost"
      />
    </div>
  </div>
</template>

<script setup>
import TraceNodeRow from './TraceNodeRow.vue';

const props = defineProps({
  nodes: {
    type: Array,
    default: () => [],
  },
  maxCost: {
    type: Number,
    default: 0,
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
.trace-node {
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 12px;
  line-height: 1.6;
}

.trace-node-item {
  position: relative;
}
</style>
