<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
      <h3>用户管理</h3>
      <el-button type="primary" @click="openCreateDialog">新建用户</el-button>
    </div>

    <el-table :data="users" v-loading="loading" stripe style="width: 100%">
      <el-table-column prop="username" label="用户名" width="150" />
      <el-table-column prop="realName" label="姓名" width="150" />
      <el-table-column label="角色" width="160">
        <template #default="{ row }">
          <el-tag v-for="r in row.roles" :key="r.code" :type="r.code === 'ADMIN' ? 'danger' : r.code === 'OPERATOR' ? 'warning' : 'info'" size="small" style="margin-right: 4px;">{{ r.name }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">{{ row.status === 'ACTIVE' ? '正常' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
          <el-button size="small" @click="openResetPwdDialog(row)">重置密码</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新建用户'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="用户名" v-if="!isEdit"><el-input v-model="form.username" placeholder="登录用户名" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" placeholder="真实姓名" /></el-form-item>
        <el-form-item label="密码" v-if="!isEdit"><el-input v-model="form.password" type="password" placeholder="至少8位" show-password /></el-form-item>
        <el-form-item label="角色">
          <el-checkbox-group v-model="form.roleCodes">
            <el-checkbox v-for="r in roleOptions" :key="r.roleCode" :label="r.roleCode">{{ r.roleName }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="状态" v-if="isEdit">
          <el-select v-model="form.status"><el-option label="正常" value="ACTIVE" /><el-option label="禁用" value="DISABLED" /></el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="pwdDialogVisible" title="重置密码" width="400px">
      <el-form><el-form-item label="新密码"><el-input v-model="pwdForm.newPassword" type="password" placeholder="至少8位" show-password /></el-form-item></el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleResetPwd" :loading="savingPwd">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getUsers, createUser, updateUser, resetPassword, getRoles } from '../api'

const users = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const editingId = ref(null)
const roleOptions = ref([])
const form = ref({ username: '', password: '', realName: '', roleCodes: [], status: 'ACTIVE' })
const pwdDialogVisible = ref(false)
const pwdUserId = ref(null)
const pwdForm = ref({ newPassword: '' })
const savingPwd = ref(false)

onMounted(async () => { await Promise.all([fetchUsers(), fetchRoles()]) })

async function fetchUsers() {
  loading.value = true
  try { const res = await getUsers(); users.value = res.data } finally { loading.value = false }
}

async function fetchRoles() {
  try { const res = await getRoles(); roleOptions.value = res.data } catch { }
}

function openCreateDialog() {
  isEdit.value = false; editingId.value = null
  form.value = { username: '', password: '', realName: '', roleCodes: [], status: 'ACTIVE' }
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true; editingId.value = row.id
  form.value = { username: row.username, realName: row.realName, password: '', roleCodes: (row.roles || []).map(r => r.code), status: row.status }
  dialogVisible.value = true
}

async function handleSave() {
  saving.value = true
  try {
    if (isEdit.value) {
      await updateUser(editingId.value, { realName: form.value.realName, status: form.value.status, roleCodes: form.value.roleCodes })
      ElMessage.success('更新成功')
    } else {
      await createUser(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false; await fetchUsers()
  } catch (e) { ElMessage.error(e.response?.data?.error || '操作失败') } finally { saving.value = false }
}

function openResetPwdDialog(row) {
  pwdUserId.value = row.id; pwdForm.value = { newPassword: '' }; pwdDialogVisible.value = true
}

async function handleResetPwd() {
  savingPwd.value = true
  try { await resetPassword(pwdUserId.value, pwdForm.value.newPassword); ElMessage.success('密码已重置'); pwdDialogVisible.value = false }
  catch (e) { ElMessage.error(e.response?.data?.error || '重置失败') } finally { savingPwd.value = false }
}
</script>
