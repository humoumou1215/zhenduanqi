<template>
  <div>
    <h3>审计日志</h3>

    <el-card style="margin-bottom: 16px">
      <el-form :model="filters" inline>
        <el-form-item label="操作人">
          <el-input
            v-model="filters.username"
            placeholder="用户名"
            clearable
            style="width: 140px"
            @clear="search"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select
            v-model="filters.action"
            placeholder="全部"
            clearable
            style="width: 160px"
            @change="search"
          >
            <el-option label="执行命令" value="EXECUTE_COMMAND" />
            <el-option label="登录" value="LOGIN" />
            <el-option label="管理服务器" value="MANAGE_SERVER" />
            <el-option label="管理用户" value="MANAGE_USER" />
            <el-option label="管理场景" value="MANAGE_SCENE" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker
            v-model="timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            value-format="YYYY-MM-DDTHH:mm:ss"
            @change="search"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-table
      :data="logs"
      v-loading="loading"
      stripe
      style="width: 100%"
      @sort-change="onSortChange"
    >
      <el-table-column prop="createdAt" label="时间" width="170" sortable="custom">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column prop="username" label="操作人" width="100" />
      <el-table-column prop="userIp" label="IP" width="130" />
      <el-table-column prop="action" label="操作类型" width="140" />
      <el-table-column prop="target" label="目标" width="140" />
      <el-table-column prop="result" label="结果" width="90">
        <template #default="{ row }">
          <el-tag
            :type="
              row.result === 'SUCCESS' ? 'success' : row.result === 'BLOCKED' ? 'warning' : 'danger'
            "
            size="small"
          >
            {{ row.result }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="durationMs" label="耗时(ms)" width="90" sortable="custom" />
      <el-table-column type="expand" label="详情">
        <template #default="{ row }">
          <div style="padding: 12px">
            <div v-if="row.command">
              <strong>指令：</strong>
              <pre style="background: #f5f7fa; padding: 8px; margin: 4px 0">{{ row.command }}</pre>
            </div>
            <div v-if="row.resultDetail">
              <strong>结果详情：</strong>
              <pre style="background: #f5f7fa; padding: 8px; margin: 4px 0">{{
                row.resultDetail
              }}</pre>
            </div>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div style="margin-top: 16px; display: flex; justify-content: flex-end">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="fetchLogs"
        @current-change="fetchLogs"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { getAuditLogs } from '../api';

const logs = ref([]);
const loading = ref(false);
const page = ref(1);
const pageSize = ref(20);
const total = ref(0);
const sortField = ref('createdAt');
const sortOrder = ref('desc');
const timeRange = ref(null);

const filters = reactive({ username: '', action: '', target: '' });

onMounted(() => fetchLogs());

function formatTime(t) {
  if (!t) return '';
  return t.replace('T', ' ').substring(0, 19);
}

async function fetchLogs() {
  loading.value = true;
  try {
    const params = {
      page: page.value - 1,
      size: pageSize.value,
      sort: sortField.value + ',' + sortOrder.value,
    };
    if (filters.username) params.username = filters.username;
    if (filters.action) params.action = filters.action;
    if (timeRange.value) {
      params.startTime = timeRange.value[0];
      params.endTime = timeRange.value[1];
    }
    const res = await getAuditLogs(params);
    logs.value = res.data.content;
    total.value = res.data.totalElements;
  } finally {
    loading.value = false;
  }
}

function search() {
  page.value = 1;
  fetchLogs();
}

function reset() {
  filters.username = '';
  filters.action = '';
  filters.target = '';
  timeRange.value = null;
  page.value = 1;
  fetchLogs();
}

function onSortChange(col) {
  if (col.prop) {
    sortField.value = col.prop;
    sortOrder.value = col.order === 'ascending' ? 'asc' : 'desc';
    fetchLogs();
  }
}
</script>
