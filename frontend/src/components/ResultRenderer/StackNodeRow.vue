<template>
  <div
    class="stack-node-row"
    :style="{ paddingLeft: depth * 20 + 'px' }"
  >
    <span class="prefix">{{ prefix }}</span>
    <template v-if="isRoot">
      <span class="target-marker">@</span>
      <span class="method-name">{{ targetMethod }}</span>
    </template>
    <template v-else>
      <span class="caller-label">at</span>
      <span class="method-name">{{ callerMethod }}</span>
      <span
        v-if="node.line"
        class="line-info"
      >
        ({{ lineInfo }})
      </span>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  node: {
    type: Object,
    default: () => ({}),
  },
  depth: {
    type: Number,
    default: 0,
  },
  prefix: {
    type: String,
    default: '',
  },
  isRoot: {
    type: Boolean,
    default: false,
  },
});

const targetMethod = computed(() => {
  const method = props.node.method || '';
  return method.replace(/^@/, '');
});

const callerMethod = computed(() => {
  const method = props.node.method || '';
  const atIndex = method.indexOf('at ');
  if (atIndex >= 0) {
    return method.substring(atIndex + 3);
  }
  return method;
});

const lineInfo = computed(() => {
  const method = props.node.method || '';
  const match = method.match(/\(([^)]+)\)/);
  return match ? match[1] : '';
});
</script>

<style scoped>
.stack-node-row {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 3px 8px;
  border-radius: 2px;
  transition: background 0.2s;
}

.stack-node-row:hover {
  background: #f5f7fa;
}

.prefix {
  color: #909399;
  font-weight: bold;
}

.target-marker {
  color: #409eff;
  font-weight: bold;
}

.caller-label {
  color: #909399;
}

.method-name {
  color: #303133;
}

.line-info {
  color: #909399;
  font-size: 11px;
}
</style>
