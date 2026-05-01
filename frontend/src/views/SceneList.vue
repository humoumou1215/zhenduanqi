<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px">
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
        <el-select v-model="selectedCategory" placeholder="全部分类" clearable style="width: 160px" @change="handleCategoryChange">
          <el-option label="线程问题" value="THREAD" />
          <el-option label="内存问题" value="MEMORY" />
          <el-option label="JVM 基础" value="JVM" />
          <el-option label="方法调试" value="METHOD" />
          <el-option label="类加载问题" value="CLASSLOADER" />
        </el-select>
      </div>
    </div>

    <div v-loading="loading">
      <div v-if="filteredScenes.length === 0 && !loading" style="text-align: center; padding: 60px 0; color: #909399">
        <el-icon :size="48"><Folder /></el-icon>
        <p style="margin-top: 16px">暂无场景数据</p>
      </div>

      <el-row :gutter="20" v-else>
        <el-col
          v-for="scene in filteredScenes"
          :key="scene.id"
          :xs="24"
          :sm="12"
          :md="8"
          :lg="6"
          style="margin-bottom: 20px"
        >
          <el-card
            class="scene-card"
            :body-style="{ padding: '0' }"
            shadow="hover"
            @click="enterScene(scene)"
          >
            <div class="scene-card-header" :style="{ backgroundColor: getCategoryColor(scene.category) }">
              <el-icon :size="28" style="color: white">
                <component :is="getCategoryIcon(scene.category)" />
              </el-icon>
            </div>
            <div class="scene-card-body">
              <h3 class="scene-title">{{ scene.name }}</h3>
              <p class="scene-desc">{{ scene.description || '暂无描述' }}</p>
              <div class="scene-meta">
                <el-tag size="small" :type="getCategoryTagType(scene.category)">
                  {{ getCategoryName(scene.category) }}
                </el-tag>
                <span class="step-count">{{ scene.stepCount || 0 }} 个步骤</span>
              </div>
              <p v-if="scene.businessScenario" class="scene-business">
                <el-icon><Location /></el-icon>
                {{ scene.businessScenario }}
              </p>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <el-dialog v-model="showServerDialog" title="选择目标服务器" width="500px" :close-on-click-modal="false">
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
import { Search, Folder, Location, Odometer, DataAnalysis, Box, Connection, Monitor } from '@element-plus/icons-vue';
import { getScenes, getSceneSteps } from '../api';
import { useServerStore } from '../stores/servers';
import { useDiagnoseStore } from '../stores/diagnose';

const router = useRouter();
const serverStore = useServerStore();
const diagnoseStore = useDiagnoseStore();

const loading = ref(false);
const scenes = ref([]);
const searchKeyword = ref('');
const selectedCategory = ref('');
const showServerDialog = ref(false);
const selectedServerId = ref('');
const pendingScene = ref(null);

const categoryMap = {
  THREAD: { name: '线程问题', color: '#409EFF', tagType: '' },
  MEMORY: { name: '内存问题', color: '#67C23A', tagType: 'success' },
  JVM: { name: 'JVM 基础', color: '#E6A23C', tagType: 'warning' },
  METHOD: { name: '方法调试', color: '#9B59B6', tagType: 'info' },
  CLASSLOADER: { name: '类加载问题', color: '#F56C6C', tagType: 'danger' }
};

const filteredScenes = computed(() => {
  let result = scenes.value;

  if (selectedCategory.value) {
    result = result.filter(s => s.category === selectedCategory.value);
  }

  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase();
    result = result.filter(s =>
      s.name.toLowerCase().includes(keyword) ||
      (s.businessScenario && s.businessScenario.toLowerCase().includes(keyword)) ||
      (s.description && s.description.toLowerCase().includes(keyword))
    );
  }

  return result;
});

function getCategoryName(category) {
  return categoryMap[category]?.name || category;
}

function getCategoryColor(category) {
  return categoryMap[category]?.color || '#909399';
}

function getCategoryTagType(category) {
  return categoryMap[category]?.tagType || '';
}

function getCategoryIcon(category) {
  const iconMap = {
    THREAD: Odometer,
    MEMORY: DataAnalysis,
    JVM: Box,
    METHOD: Monitor,
    CLASSLOADER: Connection
  };
  return iconMap[category] || Folder;
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
    scenes.value = scenesWithSteps;
  } catch (e) {
    ElMessage.error('加载场景列表失败');
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
}

function handleCategoryChange() {
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
      steps: stepsRes.data
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
.scene-card {
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  height: 100%;
}

.scene-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.15);
}

.scene-card-header {
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.scene-card-body {
  padding: 16px;
}

.scene-title {
  margin: 0 0 8px 0;
  font-size: 16px;
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
</style>
