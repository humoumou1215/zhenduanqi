<template>
  <div class="trace-node-row" :style="{ paddingLeft: depth * 20 + 'px' }">
    <span class="prefix">{{ prefix }}</span>
    <span class="method-name">{{ node.method }}</span>
    <span v-if="node.line" class="line-number">#{{ node.line }}</span>
    <span v-if="node.cost != null" class="cost-bar" :style="costBarStyle">
      <span class="cost-bar-fill" :class="costBarClass"></span>
      <span class="cost-value">{{ formatCost(node.cost) }}</span>
    </span>
    <span v-if="node.throws" class="throws-indicator">[throws {{ node.throws }}]</span>
    <span v-if="node.aggregated" class="aggregated-info">
      [min={{ formatCost(node.aggregated.min) }},max={{ formatCost(node.aggregated.max) }},total={{
        formatCost(node.aggregated.total)
      }},count={{ node.aggregated.count }}]
    </span>
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
  maxCost: {
    type: Number,
    default: 0,
  },
  prefix: {
    type: String,
    default: '',
  },
});

const costBarStyle = computed(() => {
  if (props.maxCost <= 0) return {};
  const percentage = Math.min((props.node.cost / props.maxCost) * 100, 100);
  return {
    '--bar-width': `${percentage}%`,
  };
});

const costBarClass = computed(() => {
  const cost = props.node.cost || 0;
  if (cost > 100) return 'cost-high';
  if (cost > 10) return 'cost-medium';
  return 'cost-low';
});

function formatCost(cost) {
  if (cost == null) return '-';
  if (cost < 1) return `${(cost * 1000).toFixed(0)}μs`;
  return `${cost.toFixed(2)}ms`;
}
</script>

<style scoped>
.trace-node-row {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 3px 8px;
  border-radius: 2px;
  transition: background 0.2s;
}

.trace-node-row:hover {
  background: #f5f7fa;
}

.prefix {
  color: #909399;
  font-weight: bold;
}

.method-name {
  color: #303133;
}

.line-number {
  color: #909399;
  font-size: 11px;
}

.cost-bar {
  display: inline-flex;
  align-items: center;
  margin-left: 8px;
  min-width: 120px;
  height: 16px;
  background: #ebeef5;
  border-radius: 2px;
  overflow: hidden;
  position: relative;
}

.cost-bar-fill {
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  border-radius: 2px;
  transition: width 0.3s;
}

.cost-bar-fill.cost-high {
  background: #f56c6c;
}

.cost-bar-fill.cost-medium {
  background: #e6a23c;
}

.cost-bar-fill.cost-low {
  background: #67c23a;
}

.cost-value {
  position: relative;
  z-index: 1;
  padding-left: 6px;
  font-size: 11px;
  color: #606266;
}

.throws-indicator {
  color: #f56c6c;
  font-size: 11px;
  margin-left: 8px;
}

.aggregated-info {
  color: #909399;
  font-size: 10px;
  margin-left: 8px;
}
</style>
