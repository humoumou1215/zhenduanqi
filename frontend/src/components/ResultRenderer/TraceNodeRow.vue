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
  padding: 4px 8px;
  border-radius: 4px;
  transition: background 0.2s;
}

.trace-node-row:hover {
  background: #f5f7fa;
}

.prefix {
  color: #909399;
  font-weight: bold;
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
}

.method-name {
  color: #303133;
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 13px;
}

.line-number {
  color: #909399;
  font-size: 11px;
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
}

.cost-bar {
  display: inline-flex;
  align-items: center;
  margin-left: 8px;
  flex: 1;
  min-width: 150px;
  max-width: 400px;
  height: 20px;
  background: linear-gradient(90deg, #f5f7fa 0%, #ebeef5 100%);
  border-radius: 4px;
  overflow: hidden;
  position: relative;
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.05);
}

.cost-bar-fill {
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  border-radius: 4px;
  transition: width 0.5s ease-out;
}

.cost-bar-fill.cost-high {
  background: linear-gradient(90deg, #f56c6c 0%, #f78989 100%);
  box-shadow: 0 0 8px rgba(245, 108, 108, 0.3);
}

.cost-bar-fill.cost-medium {
  background: linear-gradient(90deg, #e6a23c 0%, #ebb563 100%);
  box-shadow: 0 0 8px rgba(230, 162, 60, 0.3);
}

.cost-bar-fill.cost-low {
  background: linear-gradient(90deg, #67c23a 0%, #85ce61 100%);
  box-shadow: 0 0 8px rgba(103, 194, 58, 0.3);
}

.cost-value {
  position: relative;
  z-index: 1;
  padding-left: 8px;
  font-size: 12px;
  font-weight: 600;
  color: #303133;
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
  text-shadow: 0 1px 2px rgba(255, 255, 255, 0.8);
}

.throws-indicator {
  color: #f56c6c;
  font-size: 11px;
  margin-left: 8px;
  font-weight: 600;
}

.aggregated-info {
  color: #909399;
  font-size: 10px;
  margin-left: 8px;
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
}
</style>
