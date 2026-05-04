<template>
  <CallStackRenderer
    mode="stack"
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
  value:
    'at com.example.TestClass.testMethod(TestClass.java:10)\n' +
    'at com.example.Caller.call(Caller.java:20)\n' +
    'at java.lang.Thread.run(Thread.java:748)',
  ts: Date.now(),
  threadName: 'main',
  priority: 5,
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
  const stackValue = data.value || data.stackTrace || data.stack || '';

  if (stackValue) {
    const lines = stackValue.split('\n').filter((line) => line.trim());
    const nodes = lines.map((line) => ({
      method: line,
      depth: 0,
      children: [],
    }));

    samples.push({
      id: 1,
      timestamp: data.ts || data.timestamp || Date.now(),
      threadName: data.threadName || data.thread || '',
      priority: data.priority || 5,
      nodes: nodes,
    });
  }

  if (data.stack && Array.isArray(data.stack)) {
    data.stack.forEach((s, idx) => {
      samples.push({
        id: s.id || idx + 1,
        timestamp: s.ts || s.timestamp || Date.now(),
        threadName: s.threadName || s.thread || '',
        priority: s.priority || 5,
        nodes: normalizeStackNodes(s),
      });
    });
  }

  return samples;
});

function normalizeStackNodes(stackItem) {
  if (typeof stackItem === 'string') {
    return stackItem
      .split('\n')
      .filter((line) => line.trim())
      .map((line) => ({
        method: line,
        depth: 0,
        children: [],
      }));
  }

  if (Array.isArray(stackItem)) {
    return stackItem.map((line) => ({
      method: typeof line === 'string' ? line : line.method || line.classMethod || '',
      depth: 0,
      children: [],
    }));
  }

  const nodes = [];
  const addLine = (line) => {
    if (line) {
      nodes.push({
        method: line.method || line.classMethod || line,
        depth: 0,
        children: [],
      });
    }
  };

  if (stackItem.stackTrace) {
    const lines = Array.isArray(stackItem.stackTrace)
      ? stackItem.stackTrace
      : stackItem.stackTrace.split('\n');
    lines.forEach(addLine);
  }

  if (stackItem.stack) {
    const lines = Array.isArray(stackItem.stack) ? stackItem.stack : stackItem.stack.split('\n');
    lines.forEach(addLine);
  }

  return nodes;
}

function handleFocus(index) {}

watch(
  () => props.data,
  (newData) => {
    isRunning.value = newData?.isRunning || false;
  },
  { immediate: true }
);
</script>
