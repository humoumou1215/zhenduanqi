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
      <h3>服务器管理</h3>
      <div>
        <el-button v-if="userStore.role === 'ADMIN'" type="primary" @click="openDialog()">
          添加服务器
        </el-button>
        <el-button type="success" @click="checkAllStatus" :loading="checkingAll">
          检测全部
        </el-button>
      </div>
    </div>

    <el-table :data="store.list" v-loading="store.loading" stripe style="width: 100%" @sort-change="handleSortChange" :default-sort="{ prop: 'id', order: 'ascending' }">
      <el-table-column prop="id" label="ID" width="120" sortable />
      <el-table-column prop="name" label="名称" width="200" sortable />
      <el-table-column prop="host" label="主机" width="150" sortable />
      <el-table-column prop="httpPort" label="端口" width="80" sortable />
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column label="连接状态" width="180">
        <template #default="{ row }">
          <div v-if="statusMap[row.id]?.loading">
            <el-tag type="info" size="small">检测中...</el-tag>
          </div>
          <div v-else-if="statusMap[row.id]">
            <el-tooltip
              :content="statusMap[row.id].error || statusMap[row.id].message"
              placement="top"
            >
              <el-tag :type="statusMap[row.id].connected ? 'success' : 'danger'" size="small">
                {{ statusMap[row.id].connected ? '已连接' : '未连接' }}
              </el-tag>
            </el-tooltip>
          </div>
          <div v-else>
            <el-tag type="info" size="small">未知</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column v-if="userStore.role === 'ADMIN'" label="操作" width="240">
        <template #default="{ row }">
          <el-button
            size="small"
            @click="checkSingleStatus(row.id)"
            :loading="statusMap[row.id]?.loading"
          >
            检测
          </el-button>
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑服务器' : '添加服务器'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="ID" v-if="!isEdit">
          <el-input v-model="form.id" placeholder="唯一标识，如 server-1" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="如 生产环境-应用A" />
        </el-form-item>
        <el-form-item label="主机">
          <el-input v-model="form.host" placeholder="IP 地址或域名" />
        </el-form-item>
        <el-form-item label="端口">
          <el-input-number v-model="form.httpPort" :min="1" :max="65535" />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="Arthas HTTP 认证用户名，默认 arthas" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="Arthas HTTP 认证密码"
            show-password
          />
        </el-form-item>
        <el-divider content-position="left">向后兼容（旧版 Token 认证）</el-divider>
        <el-form-item label="Token">
          <el-input
            v-model="form.token"
            type="password"
            placeholder="Arthas HTTP API Token（已废弃）"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useServerStore } from '../stores/servers';
import { useUserStore } from '../stores/user';
import { createServer, updateServer, deleteServer, getServerStatus } from '../api';

const store = useServerStore();
const userStore = useUserStore();
const dialogVisible = ref(false);
const isEdit = ref(false);
const saving = ref(false);
const checkingAll = ref(false);
const editingId = ref('');
const statusMap = reactive({});
const sortProp = ref('id');
const sortOrder = ref('ascending');

const defaultForm = () => ({
  id: '',
  name: '',
  host: '',
  httpPort: 8563,
  username: 'arthas',
  password: '',
  token: '',
});
const form = ref(defaultForm());

onMounted(async () => {
  await store.fetchServers();
  store.list.forEach((s) => checkStatus(s.id));
});

async function checkStatus(id) {
  statusMap[id] = { loading: true };
  try {
    const res = await getServerStatus(id);
    statusMap[id] = { ...res.data, loading: false };
  } catch (e) {
    statusMap[id] = {
      connected: false,
      error: e.response?.data?.message || '检测失败',
      loading: false,
    };
  }
}

async function checkSingleStatus(id) {
  await checkStatus(id);
  ElMessage.success('检测完成');
}

async function checkAllStatus() {
  checkingAll.value = true;
  try {
    const promises = store.list.map((s) => checkStatus(s.id));
    await Promise.all(promises);
    ElMessage.success('全部检测完成');
  } catch (e) {
    ElMessage.error('检测过程中出现错误');
  } finally {
    checkingAll.value = false;
  }
}

function openDialog(row) {
  if (row) {
    isEdit.value = true;
    editingId.value = row.id;
    form.value = {
      id: row.id,
      name: row.name,
      host: row.host,
      httpPort: row.httpPort,
      username: row.username || '',
      password: '',
      token: '',
    };
  } else {
    isEdit.value = false;
    editingId.value = '';
    form.value = defaultForm();
  }
  dialogVisible.value = true;
}

async function handleSave() {
  saving.value = true;
  try {
    if (isEdit.value) {
      await updateServer(editingId.value, form.value);
      ElMessage.success('更新成功');
    } else {
      await createServer(form.value);
      ElMessage.success('创建成功');
    }
    dialogVisible.value = false;
    await store.fetchServers();
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败');
  } finally {
    saving.value = false;
  }
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定要删除该服务器吗？', '确认');
    await deleteServer(id);
    ElMessage.success('删除成功');
    await store.fetchServers();
  } catch {
    // cancelled
  }
}

function handleSortChange({ prop, order }) {
  sortProp.value = prop;
  sortOrder.value = order;
  if (order) {
    store.list.sort((a, b) => {
      const aVal = a[prop];
      const bVal = b[prop];
      const compare = aVal < bVal ? -1 : aVal > bVal ? 1 : 0;
      return order === 'ascending' ? compare : -compare;
    });
  }
}
</script>
