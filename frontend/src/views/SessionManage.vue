<template>
  <div>
    <div
      style="
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 16px;
      "
    >
      <h3>会话管理</h3>
      <div style="display: flex; gap: 12px; align-items: center">
        <el-select
          v-model="filterServerId"
          placeholder="选择服务器"
          clearable
          style="width: 200px"
          @change="fetchSessions"
        >
          <el-option
            v-for="server in servers"
            :key="server.id"
            :label="server.name"
            :value="server.id"
          />
        </el-select>
        <el-input
          v-model="filterUsername"
          placeholder="输入用户名筛选"
          clearable
          style="width: 200px"
          @clear="fetchSessions"
          @keyup.enter="fetchSessions"
        />
        <el-button type="primary" @click="fetchSessions">刷新</el-button>
      </div>
    </div>

    <el-table :data="sessions" v-loading="loading" stripe style="width: 100%">
      <el-table-column prop="id" label="会话ID" width="80" />
      <el-table-column prop="serverId" label="服务器" width="120" />
      <el-table-column prop="arthasSessionId" label="Arthas会话ID" width="200">
        <template #default="{ row }">
          <el-tooltip :content="row.arthasSessionId || '-'" placement="top">
            <span class="truncate-text">{{ row.arthasSessionId || '-' }}</span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column prop="username" label="用户" width="100" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag
            :type="
              row.status === 'ACTIVE' ? 'success' : row.status === 'RESET' ? 'warning' : 'info'
            "
            size="small"
          >
            {{ statusTextMap[row.status] || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="command" label="命令" min-width="200">
        <template #default="{ row }">
          <el-tooltip :content="row.command || '-'" placement="top">
            <span class="truncate-text">{{ row.command || '-' }}</span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="160">
        <template #default="{ row }">
          {{ formatDateTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column prop="lastActiveAt" label="最后活跃" width="160">
        <template #default="{ row }">
          {{ formatDateTime(row.lastActiveAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'ACTIVE'"
            size="small"
            type="warning"
            @click="handleInterrupt(row)"
            :loading="interruptingId === row.id"
          >
            中断
          </el-button>
          <el-button
            v-if="row.status === 'ACTIVE'"
            size="small"
            type="danger"
            @click="handleClose(row)"
            :loading="closingId === row.id"
          >
            关闭
          </el-button>
          <span v-if="row.status !== 'ACTIVE'" class="no-action">-</span>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="sessions.length === 0 && !loading" class="empty-tip">暂无活跃会话</div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getActiveSessions, interruptSessionJob, closeSession, getServers } from '../api';

const sessions = ref([]);
const servers = ref([]);
const loading = ref(false);
const interruptingId = ref(null);
const closingId = ref(null);
const filterServerId = ref('');
const filterUsername = ref('');
let refreshInterval = null;

const statusTextMap = {
  ACTIVE: '活跃',
  RESET: '已重置',
  CLOSED: '已关闭',
};

onMounted(() => {
  fetchServers();
  fetchSessions();
  refreshInterval = setInterval(fetchSessions, 30000);
});

onUnmounted(() => {
  if (refreshInterval) {
    clearInterval(refreshInterval);
  }
});

async function fetchServers() {
  try {
    const res = await getServers();
    servers.value = res.data || [];
  } catch (e) {
    console.error('获取服务器列表失败', e);
  }
}

async function fetchSessions() {
  loading.value = true;
  try {
    const params = {};
    if (filterServerId.value) {
      params.serverId = filterServerId.value;
    }
    if (filterUsername.value) {
      params.username = filterUsername.value;
    }
    const res = await getActiveSessions(params);
    sessions.value = res.data || [];
  } catch (e) {
    ElMessage.error('获取会话列表失败: ' + (e.response?.data?.error || e.message));
  } finally {
    loading.value = false;
  }
}

async function handleInterrupt(row) {
  try {
    await ElMessageBox.confirm(`确定要中断会话 ${row.id} 吗？`, '确认中断');
  } catch {
    return;
  }

  interruptingId.value = row.id;
  try {
    const res = await interruptSessionJob(row.id);
    if (res.data.success) {
      ElMessage.success('中断成功');
      await fetchSessions();
    } else {
      ElMessage.error('中断失败');
    }
  } catch (e) {
    ElMessage.error('中断失败: ' + (e.response?.data?.error || e.message));
  } finally {
    interruptingId.value = null;
  }
}

async function handleClose(row) {
  try {
    await ElMessageBox.confirm(
      `确定要关闭会话 ${row.id} 吗？这将执行 reset 并关闭会话。`,
      '确认关闭'
    );
  } catch {
    return;
  }

  closingId.value = row.id;
  try {
    const res = await closeSession(row.id);
    if (res.data.success) {
      ElMessage.success('关闭成功');
      await fetchSessions();
    } else {
      ElMessage.error('关闭失败');
    }
  } catch (e) {
    ElMessage.error('关闭失败: ' + (e.response?.data?.error || e.message));
  } finally {
    closingId.value = null;
  }
}

function formatDateTime(dateStr) {
  if (!dateStr) return '-';
  try {
    const date = new Date(dateStr);
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  } catch {
    return dateStr;
  }
}
</script>

<style scoped>
.truncate-text {
  display: inline-block;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}

.no-action {
  color: #999;
  font-size: 12px;
}

.empty-tip {
  text-align: center;
  padding: 40px;
  color: #999;
}
</style>
