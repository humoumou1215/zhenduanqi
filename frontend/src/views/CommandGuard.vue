<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
      <h3>高危命令规则管理</h3>
      <el-button type="primary" @click="openDialog()">新增规则</el-button>
    </div>

    <el-table :data="rules" v-loading="loading" stripe style="width: 100%">
      <el-table-column label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="row.ruleType === 'BLACKLIST' ? 'danger' : 'success'" size="small">{{ row.ruleType === 'BLACKLIST' ? '黑名单' : '白名单' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="pattern" label="正则表达式" min-width="250" />
      <el-table-column prop="description" label="说明" min-width="180" />
      <el-table-column label="启用" width="80">
        <template #default="{ row }">
          <el-switch v-model="row.enabled" @change="toggleRule(row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑规则' : '新增规则'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="类型">
          <el-select v-model="form.ruleType">
            <el-option label="黑名单" value="BLACKLIST" />
            <el-option label="白名单" value="WHITELIST" />
          </el-select>
        </el-form-item>
        <el-form-item label="正则表达式">
          <el-input v-model="form.pattern" placeholder="如 ^ognl\b" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" placeholder="规则说明" />
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
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getGuardRules, createGuardRule, updateGuardRule, deleteGuardRule } from '../api'

const rules = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const editingId = ref(null)
const form = ref({ ruleType: 'BLACKLIST', pattern: '', description: '' })

onMounted(() => fetchRules())

async function fetchRules() {
  loading.value = true
  try { const res = await getGuardRules(); rules.value = res.data } finally { loading.value = false }
}

function openDialog(row) {
  if (row) {
    isEdit.value = true; editingId.value = row.id
    form.value = { ruleType: row.ruleType, pattern: row.pattern, description: row.description || '' }
  } else {
    isEdit.value = false; editingId.value = null
    form.value = { ruleType: 'BLACKLIST', pattern: '', description: '' }
  }
  dialogVisible.value = true
}

async function handleSave() {
  saving.value = true
  try {
    if (isEdit.value) {
      await updateGuardRule(editingId.value, form.value)
      ElMessage.success('更新成功')
    } else {
      await createGuardRule(form.value)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false; await fetchRules()
  } catch (e) { ElMessage.error(e.response?.data?.error || '操作失败') } finally { saving.value = false }
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定要删除该规则吗？', '确认')
    await deleteGuardRule(id)
    ElMessage.success('删除成功')
    await fetchRules()
  } catch {}
}

async function toggleRule(row) {
  try {
    await updateGuardRule(row.id, { ruleType: row.ruleType, pattern: row.pattern, description: row.description, enabled: row.enabled })
  } catch { row.enabled = !row.enabled }
}
</script>
