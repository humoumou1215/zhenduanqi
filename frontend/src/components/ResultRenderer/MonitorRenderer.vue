<template>
  <div class="monitor-renderer">
    <div class="monitor-header">
      <div class="monitor-title">
        <span class="title-icon">📊</span>
        <span class="title-text">监控: {{ className }}.{{ methodName }}</span>
      </div>
      <div class="monitor-controls">
        <el-tag v-if="cycleSeconds" type="info" size="small">统计周期: {{ cycleSeconds }}s</el-tag>
        <el-tag v-if="elapsedTime" type="info" size="small">已运行: {{ elapsedTime }}</el-tag>
        <el-tag v-if="isRunning" type="success" size="small">
          <el-icon class="is-loading"><Loading /></el-icon>
          运行中
        </el-tag>
        <el-tag v-else type="info" size="small">已停止</el-tag>
        <el-button v-if="isRunning" type="danger" size="small" @click="handleStop">
          <el-icon><VideoPause /></el-icon>
          停止
        </el-button>
      </div>
    </div>

    <div v-if="hasData" class="monitor-content">
      <div class="stats-overview">
        <div class="stats-card">
          <div class="stats-label">总调用</div>
          <div class="stats-value">{{ totalCalls }}</div>
        </div>
        <div class="stats-card success">
          <div class="stats-label">成功</div>
          <div class="stats-value">{{ totalSuccess }}</div>
        </div>
        <div class="stats-card" :class="{ danger: totalFail > 0 }">
          <div class="stats-label">失败</div>
          <div class="stats-value">{{ totalFail }}</div>
        </div>
        <div class="stats-card" :class="getRtClass(totalAvgRt)">
          <div class="stats-label">平均RT</div>
          <div class="stats-value">{{ totalAvgRt.toFixed(1) }}ms</div>
        </div>
        <div class="stats-card" :class="{ danger: totalFailRate > 0 }">
          <div class="stats-label">失败率</div>
          <div class="stats-value">{{ totalFailRate.toFixed(1) }}%</div>
        </div>
      </div>

      <div class="history-section">
        <div class="history-header">
          <span>历史记录 ({{ historyData.length }} 个周期)</span>
        </div>
        <el-table :data="historyData" stripe size="small" style="width: 100%" max-height="300">
          <el-table-column label="周期起止" min-width="180">
            <template #default="{ row }">{{ row.periodStart }} - {{ row.periodEnd }}</template>
          </el-table-column>
          <el-table-column prop="total" label="总" width="60" align="center" />
          <el-table-column prop="success" label="成功" width="60" align="center" />
          <el-table-column prop="fail" label="失败" width="60" align="center">
            <template #default="{ row }">
              <span :class="{ 'cell-danger': row.fail > 0 }">{{ row.fail }}</span>
            </template>
          </el-table-column>
          <el-table-column label="平均RT" width="90" align="right">
            <template #default="{ row }">
              <span :class="getRtClass(row.avgRt)">{{ row.avgRt.toFixed(1) }}ms</span>
            </template>
          </el-table-column>
          <el-table-column label="失败率" width="80" align="right">
            <template #default="{ row }">
              <span :class="{ 'cell-danger': row.failRate > 0 }">
                {{ row.failRate.toFixed(1) }}%
              </span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <div v-else class="monitor-empty">
      <div class="placeholder-card">
        <div class="placeholder-header">
          <span class="title-icon">📊</span>
          <span class="title-text">监控: demo.MathGame.primeFactors</span>
        </div>
        <div class="stats-overview">
          <div class="stats-card">
            <div class="stats-label">总调用</div>
            <div class="stats-value">28</div>
          </div>
          <div class="stats-card success">
            <div class="stats-label">成功</div>
            <div class="stats-value">22</div>
          </div>
          <div class="stats-card danger">
            <div class="stats-label">失败</div>
            <div class="stats-value">6</div>
          </div>
          <div class="stats-card danger">
            <div class="stats-label">平均RT</div>
            <div class="stats-value">15.3ms</div>
          </div>
          <div class="stats-card danger">
            <div class="stats-label">失败率</div>
            <div class="stats-value">21.0%</div>
          </div>
        </div>
        <div class="history-section">
          <div class="history-header">
            <span>历史记录 (4 个周期)</span>
          </div>
          <el-table :data="placeholderHistory" stripe size="small" style="width: 100%">
            <el-table-column label="周期起止" min-width="180">
              <template #default="{ row }">{{ row.periodStart }} - {{ row.periodEnd }}</template>
            </el-table-column>
            <el-table-column prop="total" label="总" width="60" align="center" />
            <el-table-column prop="success" label="成功" width="60" align="center" />
            <el-table-column prop="fail" label="失败" width="60" align="center">
              <template #default="{ row }">
                <span :class="{ 'cell-danger': row.fail > 0 }">{{ row.fail }}</span>
              </template>
            </el-table-column>
            <el-table-column label="平均RT" width="90" align="right">
              <template #default="{ row }">
                <span :class="getRtClass(row.avgRt)">{{ row.avgRt }}ms</span>
              </template>
            </el-table-column>
            <el-table-column label="失败率" width="80" align="right">
              <template #default="{ row }">
                <span :class="{ 'cell-danger': row.failRate > 0 }">{{ row.failRate }}%</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>

    <div
      v-if="isExample"
      style="margin-top: 8px; font-size: 11px; color: #909399; font-style: italic"
    >
      (示例数据)
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { VideoPause, Loading } from '@element-plus/icons-vue';

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
  isRunning: {
    type: Boolean,
    default: false,
  },
  startTime: {
    type: Number,
    default: null,
  },
  cycleSeconds: {
    type: Number,
    default: null,
  },
  onStop: {
    type: Function,
    default: null,
  },
});

const emit = defineEmits(['stop']);

const defaultData = {
  timestamp: Date.now(),
  class: 'com.example.TestClass',
  method: 'testMethod',
  total: 10,
  success: 9,
  fail: 1,
  avgRt: 50,
};

const placeholderHistory = [
  {
    periodStart: '19:09:00',
    periodEnd: '19:09:05',
    total: 5,
    success: 4,
    fail: 1,
    avgRt: 12.3,
    failRate: 20,
  },
  {
    periodStart: '19:08:55',
    periodEnd: '19:09:00',
    total: 5,
    success: 5,
    fail: 0,
    avgRt: 8.2,
    failRate: 0,
  },
  {
    periodStart: '19:08:50',
    periodEnd: '19:08:55',
    total: 6,
    success: 4,
    fail: 2,
    avgRt: 25.1,
    failRate: 33,
  },
  {
    periodStart: '19:08:45',
    periodEnd: '19:08:50',
    total: 5,
    success: 3,
    fail: 2,
    avgRt: 18.5,
    failRate: 40,
  },
];

const currentData = computed(() => {
  if (!props.data || Object.keys(props.data).length === 0) {
    return defaultData;
  }
  return props.data;
});

const isExample = computed(() => {
  return !props.data || Object.keys(props.data).length === 0;
});

const className = computed(() => {
  return currentData.value.class || currentData.value.className || 'Unknown';
});

const methodName = computed(() => {
  return currentData.value.method || 'Unknown';
});

const elapsedTime = computed(() => {
  if (!props.startTime) return null;
  const elapsed = Math.floor((Date.now() - props.startTime) / 1000);
  if (elapsed < 60) return `${elapsed}秒`;
  const minutes = Math.floor(elapsed / 60);
  const seconds = elapsed % 60;
  if (minutes < 60) return `${minutes}分${seconds}秒`;
  const hours = Math.floor(minutes / 60);
  const mins = minutes % 60;
  return `${hours}小时${mins}分`;
});

const historyData = computed(() => {
  const data = currentData.value;
  if (!data.history || !Array.isArray(data.history) || data.history.length === 0) {
    const successRate = data.total > 0 ? Math.round((data.success / data.total) * 100) : 0;
    const failRate = data.total > 0 ? Math.round((data.fail / data.total) * 100) : 0;
    const cycleSec = props.cycleSeconds || 60;
    const now = new Date();
    const endStr = formatTime(now);
    const start = new Date(now.getTime() - cycleSec * 1000);
    const startStr = formatTime(start);
    return [
      {
        periodStart: startStr,
        periodEnd: endStr,
        total: data.total,
        success: data.success,
        fail: data.fail,
        avgRt: data.avgRt || 0,
        failRate,
      },
    ];
  }
  return data.history.map((item) => {
    const total = item.total || 0;
    const success = item.success || 0;
    const fail = item.fail || 0;
    const failRate = total > 0 ? (fail / total) * 100 : 0;
    return {
      periodStart: item.periodStart || item.startTime || '-',
      periodEnd: item.periodEnd || item.endTime || '-',
      total,
      success,
      fail,
      avgRt: item.avgRt || 0,
      failRate,
    };
  });
});

const hasData = computed(() => {
  const data = currentData.value;
  return data && (data.total > 0 || (data.history && data.history.length > 0));
});

const totalCalls = computed(() => {
  const history = historyData.value;
  return history.reduce((sum, item) => sum + item.total, 0);
});

const totalSuccess = computed(() => {
  const history = historyData.value;
  return history.reduce((sum, item) => sum + item.success, 0);
});

const totalFail = computed(() => {
  const history = historyData.value;
  return history.reduce((sum, item) => sum + item.fail, 0);
});

const totalAvgRt = computed(() => {
  const history = historyData.value;
  if (history.length === 0) return 0;
  const totalRtSum = history.reduce((sum, item) => sum + item.avgRt * item.total, 0);
  const totalCalls = history.reduce((sum, item) => sum + item.total, 0);
  return totalCalls > 0 ? totalRtSum / totalCalls : 0;
});

const totalFailRate = computed(() => {
  const total = totalCalls.value;
  return total > 0 ? (totalFail.value / total) * 100 : 0;
});

function getRtClass(rt) {
  if (rt > 1000) return 'danger';
  if (rt > 100) return 'warning';
  return '';
}

function formatTime(date) {
  const h = String(date.getHours()).padStart(2, '0');
  const m = String(date.getMinutes()).padStart(2, '0');
  const s = String(date.getSeconds()).padStart(2, '0');
  return `${h}:${m}:${s}`;
}

function handleStop() {
  if (props.onStop) {
    props.onStop();
  }
  emit('stop');
}
</script>

<style scoped>
.monitor-renderer {
  width: 100%;
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
}

.monitor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 6px;
  color: #fff;
}

.monitor-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-icon {
  font-size: 16px;
}

.title-text {
  font-weight: 600;
  font-size: 14px;
}

.monitor-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.monitor-controls :deep(.el-tag) {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: #fff;
}

.monitor-controls :deep(.el-button--danger) {
  background: rgba(245, 108, 108, 0.8);
  border: none;
}

.monitor-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stats-overview {
  display: flex;
  gap: 8px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
}

.stats-card {
  flex: 1;
  text-align: center;
  padding: 8px;
  background: #fff;
  border-radius: 4px;
  border: 1px solid #ebeef5;
}

.stats-card.success {
  border-color: #67c23a;
  background: #f0f9eb;
}

.stats-card.warning {
  border-color: #e6a23c;
  background: #fdf6ec;
}

.stats-card.danger {
  border-color: #f56c6c;
  background: #fef0f0;
}

.stats-label {
  font-size: 11px;
  color: #909399;
  margin-bottom: 4px;
}

.stats-value {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.stats-card.success .stats-value {
  color: #67c23a;
}

.stats-card.warning .stats-value {
  color: #e6a23c;
}

.stats-card.danger .stats-value {
  color: #f56c6c;
}

.history-section {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  overflow: hidden;
}

.history-header {
  padding: 8px 12px;
  background: #f5f7fa;
  font-size: 13px;
  font-weight: 500;
  color: #606266;
  border-bottom: 1px solid #ebeef5;
}

.monitor-empty {
  opacity: 0.7;
}

.placeholder-card {
  border: 1px dashed #c0c4cc;
  border-radius: 6px;
  padding: 12px;
  background: #fafafa;
}

.placeholder-card .monitor-header {
  background: linear-gradient(135deg, #909399 0%, #606266 100%);
}

.cell-danger {
  color: #f56c6c;
  font-weight: 600;
}

:deep(.el-table) {
  font-size: 12px;
}

:deep(.el-table .cell) {
  padding: 4px 8px;
}
</style>
