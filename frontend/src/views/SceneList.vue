<template>
  <div>
    <div
      style="
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 20px;
      "
    >
      <div>
        <h2 style="margin: 0">诊断场景</h2>
        <p style="color: #909399; margin: 4px 0 0 0; font-size: 14px">
          选择一个场景，按步骤引导进行诊断
        </p>
      </div>
      <div style="display: flex; gap: 12px">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索业务场景..."
          style="width: 240px"
          clearable
          @input="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
    </div>

    <div v-loading="loading">
      <div
        v-if="filteredScenes.length === 0 && !loading"
        style="text-align: center; padding: 60px 0; color: #909399"
      >
        <el-icon :size="48"><Folder /></el-icon>
        <p style="margin-top: 16px">
          {{ searchKeyword ? `未找到与"${searchKeyword}"相关的场景` : '暂无场景数据' }}
        </p>
        <p v-if="searchKeyword" style="margin-top: 8px; font-size: 13px">请尝试其他关键词</p>
      </div>

      <div v-else>
        <div v-for="category in visibleCategories" :key="category.code" class="category-section">
          <div class="category-header">
            <div class="category-icon" :style="{ backgroundColor: category.color }">
              <el-icon :size="24" style="color: white">
                <component :is="category.icon" />
              </el-icon>
            </div>
            <div class="category-info">
              <h3 class="category-name">{{ category.name }}</h3>
              <p class="category-desc">{{ category.description }}</p>
            </div>
          </div>
          <el-row :gutter="16">
            <el-col
              v-for="scene in getScenesByCategory(category.code)"
              :key="scene.id"
              :xs="24"
              :sm="12"
              :md="8"
              :lg="6"
              style="margin-bottom: 16px"
            >
              <el-card
                class="scene-card"
                :body-style="{ padding: '16px' }"
                shadow="hover"
                @click="enterScene(scene)"
              >
                <h4 class="scene-title" v-html="highlightText(scene.name)"></h4>
                <p class="scene-desc" v-html="highlightText(scene.description || '暂无描述')"></p>
                <div class="scene-meta">
                  <span class="step-count">{{ scene.stepCount || 0 }} 个步骤</span>
                </div>
                <p v-if="scene.businessScenario" class="scene-business">
                  <el-icon><Location /></el-icon>
                  <span v-html="highlightText(scene.businessScenario)"></span>
                </p>
              </el-card>
            </el-col>
          </el-row>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="showServerDialog"
      title="选择目标服务器"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form label-width="100px">
        <el-form-item label="目标服务器" required>
          <el-select v-model="selectedServerId" placeholder="请选择服务器" style="width: 100%">
            <el-option
              v-for="s in serverStore.list"
              :key="s.id"
              :label="s.name + ' (' + s.host + ':' + s.httpPort + ')'"
              :value="s.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showServerDialog = false">取消</el-button>
        <el-button type="primary" :disabled="!selectedServerId" @click="confirmEnterScene">
          开始诊断
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  Search,
  Folder,
  Location,
  Lock,
  Database,
  Info,
  Clock,
  BarChart,
} from '@element-plus/icons-vue';
import { getScenes, getSceneSteps } from '../api';
import { useServerStore } from '../stores/servers';
import { useDiagnoseStore } from '../stores/diagnose';

const router = useRouter();
const serverStore = useServerStore();
const diagnoseStore = useDiagnoseStore();

const loading = ref(false);
const scenes = ref([]);
const searchKeyword = ref('');
const showServerDialog = ref(false);
const selectedServerId = ref('');
const pendingScene = ref(null);

const categoryDefinitions = [
  {
    code: 'THREAD',
    name: '线程问题',
    description: '死锁、CPU 飙高等',
    color: '#F56C6C',
    icon: Lock,
  },
  {
    code: 'MEMORY',
    name: '内存问题',
    description: 'OOM、内存泄漏、GC 频繁等',
    color: '#9B59B6',
    icon: Database,
  },
  {
    code: 'JVM',
    name: 'JVM 基础',
    description: '配置确认、环境信息等',
    color: '#409EFF',
    icon: Info,
  },
  {
    code: 'METHOD',
    name: '方法调试',
    description: '耗时追踪、调用监控等',
    color: '#67C23A',
    icon: Clock,
  },
  {
    code: 'CLASSLOADER',
    name: '类加载问题',
    description: '类冲突、ClassNotFoundException等',
    color: '#909399',
    icon: Search,
  },
];

const filteredScenes = computed(() => {
  let result = scenes.value;

  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase();
    result = result.filter(
      (s) =>
        s.name.toLowerCase().includes(keyword) ||
        (s.businessScenario && s.businessScenario.toLowerCase().includes(keyword)) ||
        (s.description && s.description.toLowerCase().includes(keyword))
    );
  }

  return result;
});

const visibleCategories = computed(() => {
  return categoryDefinitions.filter((cat) => {
    const scenesInCategory = filteredScenes.value.filter((s) => s.category === cat.code);
    return scenesInCategory.length > 0;
  });
});

function getScenesByCategory(categoryCode) {
  return filteredScenes.value
    .filter((s) => s.category === categoryCode)
    .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0));
}

async function fetchScenes() {
  loading.value = true;
  try {
    const res = await getScenes();
    const scenesWithSteps = await Promise.all(
      res.data.map(async (scene) => {
        try {
          const stepsRes = await getSceneSteps(scene.id);
          return { ...scene, stepCount: stepsRes.data.length };
        } catch {
          return { ...scene, stepCount: 0 };
        }
      })
    );
    scenes.value = scenesWithSteps.sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0));
  } catch (e) {
    ElMessage.error('加载场景列表失败');
  } finally {
    loading.value = false;
  }
}

function handleSearch() {}

function highlightText(text) {
  if (!text) return '';
  if (!searchKeyword.value) return text;

  const keyword = searchKeyword.value;
  const regex = new RegExp(`(${escapeRegExp(keyword)})`, 'gi');
  return text.replace(regex, '<mark class="highlight">$1</mark>');
}

function escapeRegExp(string) {
  return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function enterScene(scene) {
  pendingScene.value = scene;
  selectedServerId.value = diagnoseStore.selectedServerId || '';
  showServerDialog.value = true;
}

async function confirmEnterScene() {
  if (!selectedServerId.value || !pendingScene.value) return;

  try {
    const stepsRes = await getSceneSteps(pendingScene.value.id);
    const sceneWithSteps = {
      ...pendingScene.value,
      steps: stepsRes.data,
    };

    diagnoseStore.initScene(sceneWithSteps, selectedServerId.value);

    showServerDialog.value = false;
    router.push(`/scenes/${pendingScene.value.id}/diagnose`);
  } catch (e) {
    ElMessage.error('加载场景步骤失败');
  }
}

onMounted(() => {
  fetchScenes();
  serverStore.fetchServers();
});
</script>

<style scoped>
.category-section {
  margin-bottom: 32px;
}

.category-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.category-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.category-info {
  flex: 1;
}

.category-name {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.category-desc {
  margin: 4px 0 0 0;
  font-size: 13px;
  color: #909399;
}

.scene-card {
  cursor: pointer;
  transition:
    transform 0.2s,
    box-shadow 0.2s;
  height: 100%;
  border: 1px solid #ebeef5;
}

.scene-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-color: #409eff;
}

.scene-title {
  margin: 0 0 8px 0;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  line-height: 1.4;
}

.scene-desc {
  margin: 0 0 12px 0;
  font-size: 13px;
  color: #909399;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 40px;
}

.scene-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.step-count {
  font-size: 12px;
  color: #909399;
}

.scene-business {
  margin: 0;
  font-size: 12px;
  color: #606266;
  display: flex;
  align-items: center;
  gap: 4px;
}

.scene-business .el-icon {
  flex-shrink: 0;
}

:deep(.highlight) {
  background-color: #fef0f0;
  color: #f56c6c;
  padding: 0 2px;
  border-radius: 2px;
}
</style>
