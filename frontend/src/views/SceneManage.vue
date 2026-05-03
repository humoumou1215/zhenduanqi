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
      <h3>场景管理</h3>
      <el-button type="primary" @click="openSceneDialog()">添加场景</el-button>
    </div>

    <el-table :data="scenes" v-loading="loading" stripe style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="场景名称" min-width="200" />
      <el-table-column prop="category" label="分类" width="120">
        <template #default="{ row }">
          <el-tag size="small">{{ getCategoryLabel(row.category) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column prop="businessScenario" label="业务场景" width="150" show-overflow-tooltip />
      <el-table-column label="启用状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
            {{ row.enabled ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="manageSteps(row)">管理步骤</el-button>
          <el-button size="small" @click="openSceneDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDeleteScene(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="sceneDialogVisible"
      :title="isEditingScene ? '编辑场景' : '添加场景'"
      width="600px"
    >
      <el-form :model="sceneForm" label-width="100px">
        <el-form-item label="场景名称" required>
          <el-input v-model="sceneForm.name" placeholder="请输入场景名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="sceneForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入场景描述"
          />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="sceneForm.category" placeholder="请选择分类" style="width: 100%">
            <el-option
              v-for="cat in categoryDefinitions"
              :key="cat.code"
              :label="cat.name"
              :value="cat.code"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="业务场景">
          <el-input v-model="sceneForm.businessScenario" placeholder="如 订单服务、支付服务" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="sceneForm.icon" placeholder="Element Plus 图标名" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="sceneForm.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="sceneForm.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sceneDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveScene" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="stepsDialogVisible"
      :title="`管理步骤 - ${currentScene?.name}`"
      width="900px"
      :close-on-click-modal="false"
    >
      <div style="margin-bottom: 16px">
        <el-button type="primary" @click="openStepDialog()">添加步骤</el-button>
      </div>

      <el-table :data="steps" stripe style="width: 100%" v-loading="stepsLoading">
        <el-table-column label="顺序" width="80">
          <template #default="{ row }">
            <el-tag size="small">{{ row.stepOrder }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="150" />
        <el-table-column prop="command" label="命令" min-width="200" show-overflow-tooltip />
        <el-table-column prop="expectedHint" label="预期提示" min-width="150" show-overflow-tooltip />
        <el-table-column label="连续执行" width="100">
          <template #default="{ row }">
            <el-tag :type="row.continuous ? 'success' : 'info'" size="small">
              {{ row.continuous ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="超时时间" width="100">
          <template #default="{ row }">
            {{ row.maxExecTime / 1000 }}s
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="moveStep(row, -1)" :disabled="row.stepOrder === 1">
              上移
            </el-button>
            <el-button size="small" @click="openStepDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteStep(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="stepsDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="stepDialogVisible"
      :title="isEditingStep ? '编辑步骤' : '添加步骤'"
      width="700px"
    >
      <el-form :model="stepForm" label-width="100px">
        <el-form-item label="顺序">
          <el-input-number v-model="stepForm.stepOrder" :min="1" />
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="stepForm.title" placeholder="请输入步骤标题" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="stepForm.description"
            type="textarea"
            :rows="2"
            placeholder="请输入步骤描述"
          />
        </el-form-item>
        <el-form-item label="命令" required>
          <el-input
            v-model="stepForm.command"
            type="textarea"
            :rows="3"
            placeholder="请输入 Arthas 命令"
          />
        </el-form-item>
        <el-form-item label="预期提示">
          <el-input
            v-model="stepForm.expectedHint"
            type="textarea"
            :rows="2"
            placeholder="请输入预期提示信息"
          />
        </el-form-item>
        <el-form-item label="连续执行">
          <el-switch v-model="stepForm.continuous" />
        </el-form-item>
        <el-form-item label="超时时间(s)">
          <el-input-number v-model="stepForm.maxExecTime" :min="1" :step="5" />
        </el-form-item>
        <el-form-item label="提取规则">
          <el-input
            v-model="stepForm.extractRules"
            type="textarea"
            :rows="2"
            placeholder="JSON 格式的提取规则"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stepDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveStep" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  getScenes,
  createScene,
  updateScene,
  deleteScene as deleteSceneApi,
  getSceneSteps,
  addSceneStep,
  updateSceneStep,
  deleteSceneStep,
  reorderSceneSteps,
} from '../api';

const categoryDefinitions = [
  { code: 'THREAD', name: '线程问题' },
  { code: 'MEMORY', name: '内存问题' },
  { code: 'JVM', name: 'JVM 基础' },
  { code: 'METHOD', name: '方法调试' },
  { code: 'CLASSLOADER', name: '类加载问题' },
];

const loading = ref(false);
const scenes = ref([]);
const sceneDialogVisible = ref(false);
const isEditingScene = ref(false);
const editingSceneId = ref(null);
const saving = ref(false);

const defaultSceneForm = () => ({
  name: '',
  description: '',
  category: '',
  businessScenario: '',
  icon: '',
  sortOrder: 0,
  enabled: true,
});
const sceneForm = ref(defaultSceneForm());

const stepsDialogVisible = ref(false);
const stepsLoading = ref(false);
const currentScene = ref(null);
const steps = ref([]);
const stepDialogVisible = ref(false);
const isEditingStep = ref(false);
const editingStepId = ref(null);

const defaultStepForm = () => ({
  stepOrder: 1,
  title: '',
  description: '',
  command: '',
  expectedHint: '',
  continuous: false,
  maxExecTime: 30000,
  extractRules: '',
});
const stepForm = ref(defaultStepForm());

function getCategoryLabel(code) {
  const cat = categoryDefinitions.find((c) => c.code === code);
  return cat ? cat.name : code || '-';
}

async function fetchScenes() {
  loading.value = true;
  try {
    const res = await getScenes();
    scenes.value = res.data;
  } catch (e) {
    ElMessage.error('加载场景列表失败');
  } finally {
    loading.value = false;
  }
}

function openSceneDialog(row) {
  if (row) {
    isEditingScene.value = true;
    editingSceneId.value = row.id;
    sceneForm.value = {
      name: row.name,
      description: row.description || '',
      category: row.category || '',
      businessScenario: row.businessScenario || '',
      icon: row.icon || '',
      sortOrder: row.sortOrder || 0,
      enabled: row.enabled,
    };
  } else {
    isEditingScene.value = false;
    editingSceneId.value = null;
    sceneForm.value = defaultSceneForm();
  }
  sceneDialogVisible.value = true;
}

async function saveScene() {
  if (!sceneForm.value.name || !sceneForm.value.name.trim()) {
    ElMessage.error('场景名称不能为空');
    return;
  }
  saving.value = true;
  try {
    if (isEditingScene.value) {
      await updateScene(editingSceneId.value, sceneForm.value);
      ElMessage.success('更新成功');
    } else {
      await createScene(sceneForm.value);
      ElMessage.success('创建成功');
    }
    sceneDialogVisible.value = false;
    await fetchScenes();
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败');
  } finally {
    saving.value = false;
  }
}

async function handleDeleteScene(id) {
  try {
    await ElMessageBox.confirm('确定要删除该场景吗？关联的步骤也会被删除。', '确认删除', {
      type: 'warning',
    });
    await deleteSceneApi(id);
    ElMessage.success('删除成功');
    await fetchScenes();
  } catch {
    // cancelled
  }
}

async function manageSteps(scene) {
  currentScene.value = scene;
  stepsDialogVisible.value = true;
  await fetchSteps(scene.id);
}

async function fetchSteps(sceneId) {
  stepsLoading.value = true;
  try {
    const res = await getSceneSteps(sceneId);
    steps.value = res.data.sort((a, b) => a.stepOrder - b.stepOrder);
  } catch (e) {
    ElMessage.error('加载步骤失败');
  } finally {
    stepsLoading.value = false;
  }
}

function openStepDialog(row) {
  if (row) {
    isEditingStep.value = true;
    editingStepId.value = row.id;
    stepForm.value = {
      stepOrder: row.stepOrder,
      title: row.title || '',
      description: row.description || '',
      command: row.command,
      expectedHint: row.expectedHint || '',
      continuous: row.continuous,
      maxExecTime: row.maxExecTime || 30000,
      extractRules: row.extractRules || '',
    };
  } else {
    isEditingStep.value = false;
    editingStepId.value = null;
    const maxOrder = steps.value.length > 0 ? Math.max(...steps.value.map((s) => s.stepOrder)) : 0;
    stepForm.value = { ...defaultStepForm(), stepOrder: maxOrder + 1 };
  }
  stepDialogVisible.value = true;
}

async function saveStep() {
  if (!stepForm.value.command || !stepForm.value.command.trim()) {
    ElMessage.error('步骤命令不能为空');
    return;
  }
  saving.value = true;
  try {
    if (isEditingStep.value) {
      await updateSceneStep(editingStepId.value, stepForm.value);
      ElMessage.success('更新成功');
    } else {
      await addSceneStep(currentScene.value.id, stepForm.value);
      ElMessage.success('添加成功');
    }
    stepDialogVisible.value = false;
    await fetchSteps(currentScene.value.id);
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败');
  } finally {
    saving.value = false;
  }
}

async function deleteStep(id) {
  try {
    await ElMessageBox.confirm('确定要删除该步骤吗？', '确认删除', { type: 'warning' });
    await deleteSceneStep(id);
    ElMessage.success('删除成功');
    await fetchSteps(currentScene.value.id);
  } catch {
    // cancelled
  }
}

async function moveStep(row, direction) {
  const currentIndex = steps.value.findIndex((s) => s.id === row.id);
  const targetIndex = currentIndex + direction;
  if (targetIndex < 0 || targetIndex >= steps.value.length) return;

  const newSteps = [...steps.value];
  [newSteps[currentIndex], newSteps[targetIndex]] = [newSteps[targetIndex], newSteps[currentIndex]];

  const stepIds = newSteps.map((s) => s.id);
  try {
    await reorderSceneSteps(currentScene.value.id, stepIds);
    ElMessage.success('排序已更新');
    await fetchSteps(currentScene.value.id);
  } catch (e) {
    ElMessage.error('排序更新失败');
  }
}

onMounted(() => {
  fetchScenes();
});
</script>
