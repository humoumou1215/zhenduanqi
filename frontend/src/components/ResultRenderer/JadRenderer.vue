<template>
  <div>
    <div v-if="sourceCode" class="jad-container">
      <div class="jad-header">
        <el-tag type="info" size="small">
          <el-icon><Document /></el-icon>
          {{ sourceCode.className || '反编译源码' }}
        </el-tag>
        <el-tag v-if="sourceCode.sourcePath" type="info" size="small">
          {{ sourceCode.sourcePath }}
        </el-tag>
      </div>
      <pre class="code-block"><code>{{ sourceCode.code }}</code></pre>
    </div>
    <div v-else-if="isExample" class="placeholder">
      <el-icon><InfoFilled /></el-icon>
      <span>反编译的 Java 源码将显示在这里，如：jad java.lang.String</span>
    </div>
    <pre v-else class="raw-output">{{ formattedRaw }}</pre>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { InfoFilled, Document } from '@element-plus/icons-vue';

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
});

const defaultSourceCode = {
  className: 'java.lang.String',
  sourcePath: '/java/lang/String.java',
  code: `/*
 * Decompiled with CFR 0.152.
 */
package java.lang;

import java.io.ObjectStreamField;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Formatter;
import java.util.Locale;
import java.util.Spliterator;
import java.util.StringJoiner;
import java.util.regex.Pattern;

public final class String
    implements java.io.Serializable,
               Comparable<String>,
               CharSequence {
    private final char[] value;
    private int hash;
    // ... more code
}`,
};

function parseJadOutput(input) {
  if (!input) return null;

  if (typeof input === 'string') {
    return parseStringJad(input);
  }

  if (input.className || input.code || input.source) {
    return normalizeJadResult(input);
  }

  return null;
}

function parseStringJad(str) {
  const lines = str.split('\n');
  const codeLines = [];
  let className = '';
  let sourcePath = '';

  for (const line of lines) {
    if (line.includes('ClassLoader:')) {
      continue;
    }

    const classMatch = line.match(/^public\s+(final\s+)?class\s+(\S+)/);
    if (classMatch) {
      className = classMatch[2];
    }

    const pathMatch = line.match(/Source:\s*(.+)/);
    if (pathMatch) {
      sourcePath = pathMatch[1].trim();
    }

    codeLines.push(line);
  }

  if (codeLines.length > 0) {
    return {
      className,
      sourcePath,
      code: codeLines.join('\n'),
    };
  }

  return null;
}

function normalizeJadResult(data) {
  return {
    className: data.className || data.classname || '',
    sourcePath: data.sourcePath || data.source || '',
    code: data.code || data.source || JSON.stringify(data, null, 2),
  };
}

const sourceCode = computed(() => {
  if (!props.data || Object.keys(props.data).length === 0) {
    return null;
  }
  const parsed = parseJadOutput(props.data);
  return parsed;
});

const isExample = computed(() => {
  return !props.data || Object.keys(props.data).length === 0;
});

const formattedRaw = computed(() => {
  if (typeof props.data === 'string') {
    return props.data;
  }
  return JSON.stringify(props.data, null, 2);
});
</script>

<style scoped>
.jad-container {
  background: #282c34;
  border-radius: 4px;
  overflow: hidden;
}

.jad-header {
  display: flex;
  gap: 8px;
  padding: 8px 12px;
  background: #21252b;
  border-bottom: 1px solid #3e4451;
}

.code-block {
  margin: 0;
  padding: 12px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.5;
  color: #abb2bf;
  overflow-x: auto;
  white-space: pre;
}

.raw-output {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.6;
  overflow-x: auto;
  white-space: pre-wrap;
  margin: 0;
}

.placeholder {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #909399;
  font-size: 13px;
  padding: 12px;
  background: #fdf6ec;
  border-radius: 4px;
}
</style>
