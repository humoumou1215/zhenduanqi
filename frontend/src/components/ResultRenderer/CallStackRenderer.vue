<template>
  <div class="callstack-renderer">
    <CallStackHeader
      :mode="mode"
      :command="command"
      :sample-count="samples.length"
      :is-running="isRunning"
      @stop="$emit('stop')"
    />

    <div class="sample-list" ref="sampleListRef">
      <CallStackSampleCard
        v-for="(sample, index) in samples"
        :key="sample.id || index"
        :sample="sample"
        :index="index"
        :mode="mode"
        :max-cost="maxCost"
        :is-focused="focusedIndex === index"
        @click="$emit('focus', index)"
      />
    </div>

    <CallStackFocusButton
      v-if="samples.length > 1"
      :visible="showFocusButton"
      @click="scrollToLatest"
    />

    <div
      v-if="samples.length === 0"
      class="empty-state"
    >
      <el-empty :description="emptyDescription" :image-size="60" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue';
import CallStackHeader from './CallStackHeader.vue';
import CallStackSampleCard from './CallStackSampleCard.vue';
import CallStackFocusButton from './CallStackFocusButton.vue';

const props = defineProps({
  mode: {
    type: String,
    default: 'trace',
    validator: (v) => ['trace', 'stack'].includes(v),
  },
  command: {
    type: String,
    default: '',
  },
  samples: {
    type: Array,
    default: () => [],
  },
  isRunning: {
    type: Boolean,
    default: false,
  },
});

defineEmits(['stop', 'focus']);

const sampleListRef = ref(null);
const focusedIndex = ref(0);
const showFocusButton = ref(false);

const maxCost = computed(() => {
  if (props.mode !== 'trace') return 0;
  let max = 0;
  for (const sample of props.samples) {
    const cost = sample.cost || sample.totalCost || 0;
    if (cost > max) max = cost;
  }
  return max;
});

const emptyDescription = computed(() => {
  return props.mode === 'trace' ? '暂无追踪数据' : '暂无堆栈数据';
});

watch(
  () => props.samples.length,
  (newLen) => {
    if (newLen > 0) {
      focusedIndex.value = newLen - 1;
      nextTick(() => {
        if (showFocusButton.value) {
          scrollToLatest();
        }
      });
    }
  }
);

function scrollToLatest() {
  if (!sampleListRef.value) return;
  const cards = sampleListRef.value.querySelectorAll('.sample-card');
  if (cards.length > 0) {
    cards[cards.length - 1].scrollIntoView({ behavior: 'smooth', block: 'start' });
    focusedIndex.value = cards.length - 1;
    showFocusButton.value = false;
  }
}

function handleScroll() {
  if (!sampleListRef.value) return;
  const { scrollTop, clientHeight, scrollHeight } = sampleListRef.value;
  showFocusButton.value = scrollTop + clientHeight < scrollHeight - 100;
}

watch(
  () => props.samples.length,
  () => {
    nextTick(() => {
      if (sampleListRef.value) {
        sampleListRef.value.addEventListener('scroll', handleScroll);
      }
    });
  },
  { immediate: true }
);
</script>

<style scoped>
.callstack-renderer {
  width: 100%;
  position: relative;
}

.sample-list {
  max-height: 400px;
  overflow-y: auto;
  padding: 8px 0;
}

.empty-state {
  padding: 20px;
}
</style>
