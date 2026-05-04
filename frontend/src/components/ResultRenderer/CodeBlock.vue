<template>
  <div class="code-block-container">
    <div class="code-header">
      <span class="language-tag">Java</span>
      <button class="copy-btn" @click="copyCode" :class="{ copied: isCopied }">
        <el-icon v-if="!isCopied"><DocumentCopy /></el-icon>
        <el-icon v-else><Check /></el-icon>
        {{ isCopied ? '已复制' : '复制' }}
      </button>
    </div>
    <div class="code-wrapper">
      <div class="line-numbers">
        <span v-for="n in lineCount" :key="n" class="line-number">{{ n }}</span>
      </div>
      <pre
        class="code-content"
      ><code ref="codeElement" :class="`language-${language}`">{{ code }}</code></pre>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue';
import { DocumentCopy, Check } from '@element-plus/icons-vue';
import Prism from 'prismjs';
import 'prismjs/components/prism-java';

const props = defineProps({
  code: {
    type: String,
    default: '',
  },
  language: {
    type: String,
    default: 'java',
  },
});

const codeElement = ref(null);
const isCopied = ref(false);

const lineCount = computed(() => {
  if (!props.code) return 0;
  return props.code.split('\n').length;
});

const highlightCode = async () => {
  await nextTick();
  if (codeElement.value) {
    Prism.highlightElement(codeElement.value);
  }
};

const copyCode = async () => {
  try {
    await navigator.clipboard.writeText(props.code);
    isCopied.value = true;
    setTimeout(() => {
      isCopied.value = false;
    }, 2000);
  } catch (err) {
    console.error('复制失败:', err);
  }
};

onMounted(() => {
  highlightCode();
});

watch(
  () => props.code,
  () => {
    highlightCode();
  }
);
</script>

<style scoped>
.code-block-container {
  background: #1e1e1e;
  border-radius: 4px;
  overflow: hidden;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
}

.code-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #2d2d2d;
  border-bottom: 1px solid #3e3e3e;
}

.language-tag {
  color: #858585;
  font-size: 12px;
}

.copy-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background: #3e3e3e;
  border: none;
  border-radius: 4px;
  color: #abb2bf;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.copy-btn:hover {
  background: #4e4e4e;
}

.copy-btn.copied {
  background: #2d5a2d;
  color: #7ecb7e;
}

.code-wrapper {
  display: flex;
  overflow-x: auto;
}

.line-numbers {
  display: flex;
  flex-direction: column;
  padding: 12px 0;
  background: #1e1e1e;
  border-right: 1px solid #3e3e3e;
  text-align: right;
  user-select: none;
  min-width: 50px;
}

.line-number {
  padding: 0 12px;
  color: #858585;
  font-size: 13px;
  line-height: 1.5;
}

.code-content {
  flex: 1;
  margin: 0;
  padding: 12px 16px;
  overflow-x: auto;
  background: #1e1e1e;
}

.code-content code {
  font-family: inherit;
  font-size: 13px;
  line-height: 1.5;
  color: #abb2bf;
  background: transparent;
}

:deep(.token.keyword) {
  color: #569cd6;
}

:deep(.token.class-name) {
  color: #4ec9b0;
}

:deep(.token.function) {
  color: #dcdcaa;
}

:deep(.token.string) {
  color: #ce9178;
}

:deep(.token.comment) {
  color: #6a9955;
}

:deep(.token.number) {
  color: #b5cea8;
}

:deep(.token.operator) {
  color: #d4d4d4;
}

:deep(.token.punctuation) {
  color: #d4d4d4;
}

:deep(.token.annotation) {
  color: #d7ba7d;
}

:deep(.token.modifier) {
  color: #569cd6;
}

:deep(.token.type) {
  color: #4ec9b0;
}

:deep(.token.variable) {
  color: #9cdcfe;
}

:deep(.token.import) {
  color: #569cd6;
}

:deep(.token.package) {
  color: #4ec9b0;
}

@media (max-width: 768px) {
  .code-content {
    padding: 8px 12px;
  }

  .code-content code {
    font-size: 12px;
  }

  .line-numbers {
    min-width: 40px;
  }

  .line-number {
    padding: 0 8px;
    font-size: 12px;
  }
}
</style>
