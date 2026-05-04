<template>
  <div class="watch-renderer">
    <div class="watch-header">
      <span class="watch-title">观察: {{ className }}.{{ methodName }}</span>
    </div>
    <div class="watch-info">
      <span class="watch-point-badge" :class="watchPointClass">{{ watchPointLabel }}</span>
      <span class="watch-count">已观察: {{ watchList.length }} 次</span>
    </div>

    <div class="watch-overview" v-if="overviewData">
      <div class="overview-item">
        <span class="overview-label">已观察:</span>
        <span class="overview-value">{{ overviewData.total }}</span>
      </div>
      <div class="overview-item" v-if="watchPoint !== '-b'">
        <span class="overview-label">平均耗时:</span>
        <span class="overview-value">{{ overviewData.avgCost || '-' }}</span>
      </div>
      <div class="overview-item" v-if="watchPoint === '-f'">
        <span class="overview-label">成功:</span>
        <span class="overview-value success">{{ overviewData.success }}</span>
      </div>
      <div class="overview-item" v-if="watchPoint === '-f'">
        <span class="overview-label">异常:</span>
        <span class="overview-value danger">{{ overviewData.fail }}</span>
      </div>
      <div class="overview-item" v-if="watchPoint === '-e'">
        <span class="overview-label">全部异常</span>
      </div>
    </div>

    <el-table
      :data="displayData"
      size="small"
      style="width: 100%"
      :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
      max-height="400"
    >
      <el-table-column prop="index" label="#" width="60" fixed />
      <el-table-column :label="timestampLabel" width="160">
        <template #default="scope">
          {{ formatTime(scope.row.ts) }}
        </template>
      </el-table-column>
      <el-table-column v-if="watchPoint === '-f'" prop="cost" label="耗时" width="100">
        <template #default="scope">
          {{ scope.row.cost != null ? scope.row.cost + 'ms' : '-' }}
        </template>
      </el-table-column>
      <el-table-column
        v-if="watchPoint === '-b' || watchPoint === '-f'"
        label="入参"
        min-width="200"
      >
        <template #default="scope">
          <div class="cell-content" @click="copyContent(formatParams(scope.row.params))">
            {{ truncateContent(formatParams(scope.row.params)) }}
          </div>
        </template>
      </el-table-column>
      <el-table-column
        v-if="watchPoint === '-s' || watchPoint === '-f'"
        label="返回值 / 异常"
        min-width="250"
      >
        <template #default="scope">
          <div
            class="cell-content"
            :class="{ 'has-error': scope.row.throwExp, 'has-success': scope.row.returnObj }"
            @click="copyContent(getReturnOrErrorContent(scope.row))"
          >
            <span v-if="scope.row.returnObj" class="success-prefix">↩ 返回: </span>
            <span v-if="scope.row.throwExp" class="error-prefix">⚠️ 异常: </span>
            {{ truncateContent(formatReturnValue(scope.row)) }}
          </div>
        </template>
      </el-table-column>
      <el-table-column v-if="watchPoint === '-e'" label="异常信息" min-width="250">
        <template #default="scope">
          <div class="cell-content has-error" @click="copyContent(formatValue(scope.row.throwExp))">
            ⚠️ {{ truncateContent(formatValue(scope.row.throwExp)) }}
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="isExample" class="example-hint">(示例数据)</div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { ElMessage } from 'element-plus';

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
});

const defaultData = {
  className: 'demo.MathGame',
  methodName: 'primeFactors',
  location: 'AtExit',
  params: [{ type: 'Integer', value: 100 }],
  returnObj: { type: 'List', value: [2, 5, 13, 17, 23] },
  throwExp: null,
  cost: 0.38,
  ts: Date.now(),
};

const exampleDataList = [
  { ...defaultData, index: 3, ts: Date.now() - 1000 },
  { ...defaultData, index: 2, ts: Date.now() - 2000, params: [{ type: 'Integer', value: 50 }], returnObj: { type: 'List', value: [2, 5, 13, 17] } },
  { ...defaultData, index: 1, ts: Date.now() - 3000, params: [{ type: 'Integer', value: 2 }], returnObj: { type: 'List', value: [2] } },
];

const watchList = ref([]);
const watchPoint = ref('-f');

const currentData = computed(() => {
  if (!props.data || Object.keys(props.data).length === 0) {
    return null;
  }
  return props.data;
});

const isExample = computed(() => {
  return !currentData.value;
});

const className = computed(() => {
  return currentData.value?.className || currentData.value?.class || 'demo.MathGame';
});

const methodName = computed(() => {
  return currentData.value?.methodName || currentData.value?.method || 'primeFactors';
});

const location = computed(() => {
  return currentData.value?.location || 'AtExit';
});

const watchPointLabel = computed(() => {
  const pointMap = {
    '-b': '-b (调用前)',
    '-s': '-s (返回后)',
    '-e': '-e (异常后)',
    '-f': '-f (结束后)',
  };
  return pointMap[watchPoint.value] || '-f (结束后)';
});

const watchPointClass = computed(() => {
  return `watch-point-${watchPoint.value.replace('-', '')}`;
});

const timestampLabel = computed(() => {
  const labelMap = {
    '-b': '调用时间',
    '-s': '返回时间',
    '-e': '异常时间',
    '-f': '结束时间',
  };
  return labelMap[watchPoint.value] || '时间';
});

const displayData = computed(() => {
  if (isExample.value) {
    return generateExampleData();
  }
  return watchList.value;
});

function generateExampleData() {
  const examples = [];
  if (watchPoint.value === '-f') {
    examples.push({
      index: 3,
      ts: Date.now() - 1000,
      cost: 0.58,
      params: [{ type: 'Integer', value: 100 }],
      returnObj: { type: 'List', value: [2, 5, 13, 17, 23] },
      throwExp: null,
    });
    examples.push({
      index: 2,
      ts: Date.now() - 2000,
      cost: 0.35,
      params: [{ type: 'Integer', value: 50 }],
      returnObj: { type: 'List', value: [2, 5, 13, 17] },
      throwExp: null,
    });
    examples.push({
      index: 1,
      ts: Date.now() - 3000,
      cost: 0.22,
      params: [{ type: 'Integer', value: 2 }],
      returnObj: null,
      throwExp: { type: 'IllegalArgumentException', value: 'number is: -50000' },
    });
  } else if (watchPoint.value === '-b') {
    examples.push({ index: 3, ts: Date.now() - 1000, params: [{ type: 'Integer', value: 100 }] });
    examples.push({ index: 2, ts: Date.now() - 2000, params: [{ type: 'Integer', value: 50 }] });
    examples.push({ index: 1, ts: Date.now() - 3000, params: [{ type: 'Integer', value: 2 }] });
  } else if (watchPoint.value === '-s') {
    examples.push({ index: 3, ts: Date.now() - 1000, returnObj: { type: 'List', value: [2, 5, 13, 17, 23] } });
    examples.push({ index: 2, ts: Date.now() - 2000, returnObj: { type: 'List', value: [2, 5, 13, 17] } });
    examples.push({ index: 1, ts: Date.now() - 3000, returnObj: { type: 'List', value: [2] } });
  } else if (watchPoint.value === '-e') {
    examples.push({
      index: 2,
      ts: Date.now() - 1000,
      throwExp: { type: 'IllegalArgumentException', value: 'number is: -179173...' },
    });
    examples.push({
      index: 1,
      ts: Date.now() - 2000,
      throwExp: { type: 'IllegalArgumentException', value: 'number is: -50000' },
    });
  }
  return examples;
}

watch(
  () => props.data,
  (newData) => {
    if (newData && Object.keys(newData).length > 0) {
      if (newData.watchPoint) {
        watchPoint.value = newData.watchPoint;
      }
      if (newData.list) {
        watchList.value = newData.list;
      } else {
        const item = {
          index: watchList.value.length + 1,
          ts: newData.ts || Date.now(),
          cost: newData.cost,
          params: newData.params || newData.parameters,
          returnObj: newData.returnObj || newData.returnValue,
          throwExp: newData.throwExp || newData.throwException,
        };
        watchList.value.unshift(item);
      }
    }
  },
  { immediate: true, deep: true }
);

const overviewData = computed(() => {
  const data = isExample.value ? displayData.value : watchList.value;
  if (!data || data.length === 0) return null;

  const total = data.length;
  const costs = data.filter((d) => d.cost != null).map((d) => d.cost);
  const avgCost = costs.length > 0 ? (costs.reduce((a, b) => a + b, 0) / costs.length).toFixed(2) + 'ms' : null;
  const success = data.filter((d) => d.returnObj && !d.throwExp).length;
  const fail = data.filter((d) => d.throwExp).length;

  return { total, avgCost, success, fail };
});

function formatTime(ts) {
  if (!ts) return '-';
  const date = new Date(ts);
  return date.toLocaleTimeString();
}

function formatParams(params) {
  if (!params) return '';
  if (Array.isArray(params)) {
    return params.map((p) => `${p.type}=${p.value}`).join(', ');
  }
  return formatValue(params);
}

function formatReturnValue(row) {
  if (row.returnObj) {
    return formatValue(row.returnObj);
  }
  if (row.throwExp) {
    return formatValue(row.throwExp);
  }
  return '';
}

function getReturnOrErrorContent(row) {
  if (row.returnObj) {
    return formatValue(row.returnObj);
  }
  if (row.throwExp) {
    return formatValue(row.throwExp);
  }
  return '';
}

function formatValue(value) {
  if (value === null) return 'null';
  if (value === undefined) return 'undefined';
  if (typeof value === 'string') return value;
  if (typeof value === 'object') {
    try {
      return JSON.stringify(value, null, 2);
    } catch {
      return String(value);
    }
  }
  return String(value);
}

function truncateContent(content, maxLength = 50) {
  if (!content) return '';
  if (content.length <= maxLength) return content;
  return content.substring(0, maxLength) + '...';
}

async function copyContent(content) {
  if (!content) return;
  try {
    await navigator.clipboard.writeText(content);
    ElMessage.success('已复制到剪贴板');
  } catch {
    ElMessage.error('复制失败');
  }
}
</script>

<style scoped>
.watch-renderer {
  font-size: 13px;
}

.watch-header {
  margin-bottom: 8px;
  font-weight: 500;
  font-size: 14px;
}

.watch-title {
  color: #303133;
}

.watch-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  font-size: 12px;
  color: #909399;
}

.watch-point-badge {
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
}

.watch-point-b {
  background: #ecf5ff;
  color: #409eff;
}

.watch-point-s {
  background: #f0f9eb;
  color: #67c23a;
}

.watch-point-e {
  background: #fef0f0;
  color: #f56c6c;
}

.watch-point-f {
  background: #f4f4f5;
  color: #606266;
}

.watch-count {
  color: #606266;
}

.watch-overview {
  display: flex;
  gap: 16px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
  margin-bottom: 12px;
  font-size: 12px;
}

.overview-item {
  display: flex;
  gap: 4px;
}

.overview-label {
  color: #909399;
}

.overview-value {
  color: #303133;
  font-weight: 500;
}

.overview-value.success {
  color: #67c23a;
}

.overview-value.danger {
  color: #f56c6c;
}

.cell-content {
  cursor: pointer;
  padding: 4px 0;
  transition: background 0.2s;
  word-break: break-all;
}

.cell-content:hover {
  background: #f5f7fa;
  border-radius: 4px;
}

.has-success .success-prefix {
  color: #67c23a;
}

.has-error {
  color: #f56c6c;
}

.error-prefix {
  color: #f56c6c;
}

.example-hint {
  margin-top: 8px;
  font-size: 11px;
  color: #909399;
  font-style: italic;
}
</style>
