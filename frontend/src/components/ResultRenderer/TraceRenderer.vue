<template>
  <CallStackRenderer
    mode="trace"
    :command="commandText"
    :samples="parsedSamples"
    :is-running="isRunning"
    @stop="$emit('stop')"
    @focus="handleFocus"
  />
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import CallStackRenderer from './CallStackRenderer.vue';

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
});

defineEmits(['stop']);

const isRunning = ref(false);

const defaultData = {
  className: 'com.example.TestClass',
  methodName: 'testMethod',
  cost: 123.45,
  ts: Date.now(),
  trace: [
    {
      className: 'com.example.TestClass',
      methodName: 'testMethod',
      cost: 123.45,
      depth: 0,
      line: 24,
    },
    {
      className: 'com.example.Helper',
      methodName: 'helperMethod',
      cost: 45.67,
      depth: 1,
      line: 36,
    },
    {
      className: 'com.example.Helper',
      methodName: 'anotherMethod',
      cost: 23.45,
      depth: 1,
      line: 52,
      children: [
        {
          className: 'java.util.Random',
          methodName: 'nextInt',
          cost: 1.23,
          depth: 2,
          line: 45,
        },
      ],
    },
  ],
};

const currentData = computed(() => {
  if (!props.data || Object.keys(props.data).length === 0) {
    return defaultData;
  }
  return props.data;
});

const isExample = computed(() => {
  return !props.data || Object.keys(props.data).length === 0;
});

const commandText = computed(() => {
  const cls = currentData.value.className || currentData.value.class || '';
  const method = currentData.value.methodName || currentData.value.method || '';
  return `${cls}.${method}()`;
});

const parsedSamples = computed(() => {
  const data = currentData.value;
  const samples = [];

  if (data.trace) {
    const sample = {
      id: 1,
      timestamp: data.ts || data.timestamp || Date.now(),
      cost: data.cost || data.totalCost || calculateTotalCost(data.trace),
      totalCost: data.cost || data.totalCost || calculateTotalCost(data.trace),
      nodes: normalizeTraceNodes(data.trace),
    };
    samples.push(sample);
  }

  if (data.samples) {
    data.samples.forEach((s, idx) => {
      samples.push({
        id: s.id || idx + 1,
        timestamp: s.ts || s.timestamp || Date.now(),
        cost: s.cost || s.totalCost || 0,
        totalCost: s.cost || s.totalCost || 0,
        nodes: s.nodes || normalizeTraceNodes(s.trace || []),
      });
    });
  }

  if (data.children && data.children.length > 0) {
    const sample = {
      id: 1,
      timestamp: data.ts || Date.now(),
      cost: data.cost || 0,
      totalCost: data.cost || 0,
      nodes: normalizeTraceNodes(data.children),
    };
    samples.push(sample);
  }

  return samples;
});

function normalizeTraceNodes(trace) {
  if (!Array.isArray(trace)) return [];
  return trace.map((item) => ({
    method: formatMethodName(item),
    cost: item.cost || item.time || 0,
    line: item.line,
    throws: item.throws,
    depth: item.depth || 0,
    children: item.children ? normalizeTraceNodes(item.children) : [],
    aggregated: item.aggregated,
  }));
}

function formatMethodName(item) {
  const cls = item.className || item.class || '';
  const method = item.methodName || item.method || '';
  return `${cls}:${method}()`;
}

function calculateTotalCost(nodes) {
  if (!Array.isArray(nodes) || nodes.length === 0) return 0;
  return nodes.reduce((sum, node) => {
    const cost = node.cost || 0;
    const childCost = node.children ? calculateTotalCost(node.children) : 0;
    return sum + Math.max(cost, childCost);
  }, 0);
}

function handleFocus(index) {
}

watch(
  () => props.data,
  (newData) => {
    isRunning.value = newData?.isRunning || false;
  },
  { immediate: true }
);
</script>
