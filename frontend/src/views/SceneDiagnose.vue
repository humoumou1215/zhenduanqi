<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px">
      <div>
        <el-button text @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回场景列表
        </el-button>
        <h2 style="margin: 8px 0 0 0">{{ diagnoseStore.currentSceneName }}</h2>
      </div>
      <div style="display: flex; gap: 12px; align-items: center">
        <el-tag v-if="diagnoseStore.selectedServerId" type="info">
          {{ getServerName(diagnoseStore.selectedServerId) }}
        </el-tag>
        <el-button type="danger" @click="handleReset">
          开始新诊断
        </el-button>
      </div>
    </div>

    <div v-if="diagnoseStore.steps.length === 0" style="text-align: center; padding: 60px 0; color: #909399">
      <p>暂无诊断步骤</p>
    </div>

    <div v-else class="steps-container">
      <el-card
        v-for="(step, index) in diagnoseStore.steps"
        :key="step.id"
        class="step-card"
        :class="{
          'step-completed': stepStates[index]?.state === 'completed',
          'step-executing': stepStates[index]?.state === 'executing',
          'step-error': stepStates[index]?.state === 'error'
        }"
        style="margin-bottom: 16px"
      >
        <template #header>
          <div style="display: flex; justify-content: space-between; align-items: center">
            <div style="display: flex; align-items: center; gap: 12px">
              <el-tag :type="getStepTagType(stepStates[index]?.state)" size="small">
                步骤 {{ index + 1 }}
              </el-tag>
              <span style="font-weight: 600">{{ step.title || '步骤 ' + (index + 1) }}</span>
            </div>
            <div v-if="stepStates[index]?.state === 'completed'" style="color: #67c23a">
              <el-icon><CircleCheck /></el-icon>
              已完成
            </div>
            <div v-else-if="stepStates[index]?.state === 'executing'" style="color: #409eff">
              <el-icon class="is-loading"><Loading /></el-icon>
              执行中...
            </div>
          </div>
        </template>

        <div class="step-content">
          <p v-if="step.description" class="step-description">
            <el-icon><InfoFilled /></el-icon>
            {{ step.description }}
          </p>

          <div class="command-input-area">
            <el-input
              v-model="stepCommands[index]"
              :placeholder="step.command"
              :disabled="stepStates[index]?.state === 'executing'"
              @keyup.enter="handleExecute(index)"
            >
              <template #append>
                <el-button
                  :loading="stepStates[index]?.state === 'executing'"
                  :disabled="!diagnoseStore.selectedServerId || stepStates[index]?.state === 'executing'"
                  @click="handleExecute(index)"
                >
                  <el-icon v-if="!stepStates[index]?.state"><VideoPlay /></el-icon>
                  执行
                </el-button>
              </template>
            </el-input>
            <div v-if="hasPlaceholder(step.command) && stepStates[index]?.state !== 'completed'" class="placeholder-hint">
              <el-icon><Warning /></el-icon>
              命令包含占位符 { {{ getUnfilledPlaceholders(step.command) }} }，将从上一步结果自动提取
            </div>
          </div>

          <div v-if="stepStates[index]?.result" class="step-result" style="margin-top: 16px">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px">
              <span style="font-weight: 500">执行结果</span>
              <el-tag v-if="stepStates[index]?.result.state === 'succeeded'" type="success" size="small">成功</el-tag>
              <el-tag v-else-if="stepStates[index]?.result.state === 'failed'" type="danger" size="small">失败</el-tag>
              <el-tag v-else type="info" size="small">{{ stepStates[index]?.result.state }}</el-tag>
            </div>

            <div v-if="stepStates[index]?.result.error" style="color: #f56c6c; margin-bottom: 8px">
              {{ stepStates[index]?.result.error }}
            </div>

            <div v-for="(r, i) in stepStates[index]?.result.structuredResults" :key="i">
              <component :is="getRenderer(r.type)" :data="r.data" />
            </div>

            <div v-if="stepStates[index]?.result.results && stepStates[index]?.result.results.length > 0 && !stepStates[index]?.result.structuredResults">
              <div v-for="(r, i) in stepStates[index]?.result.results" :key="'raw-' + i">
                <pre class="result-raw">{{ typeof r === 'string' ? r : JSON.stringify(r, null, 2) }}</pre>
              </div>
            </div>

            <div v-if="stepStates[index]?.result.results && stepStates[index]?.result.results.length > 0" style="margin-top: 12px">
              <el-button size="small" text @click="expandRaw[index] = !expandRaw[index]">
                {{ expandRaw[index] ? '收起' : '查看' }}原始响应
              </el-button>
              <pre v-if="expandRaw[index]" class="result-raw" style="margin-top: 8px">{{ stepStates[index]?.result.rawResponse }}</pre>
            </div>
          </div>

          <div v-if="step.expectedHint" class="step-hint" style="margin-top: 12px">
            <el-icon style="color: #e6a23c"><InfoFilled /></el-icon>
            {{ step.expectedHint }}
          </div>

          <div v-if="stepStates[index]?.state === 'completed' && step.extract_rules" class="variables-extracted" style="margin-top: 12px">
            <span style="color: #67c23a; font-size: 13px">
              <el-icon><Check /></el-icon>
              已提取变量:
            </span>
            <el-tag
              v-for="[key, value] in getExtractedVariables(step.id)"
              :key="key"
              size="small"
              style="margin-left: 8px"
            >
              {{ key }} = {{ value }}
            </el-tag>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  ArrowLeft, VideoPlay, CircleCheck, Loading, InfoFilled, Warning, Check
} from '@element-plus/icons-vue';
import { executeCommand, getServerById } from '../api';
import { useDiagnoseStore } from '../stores/diagnose';
import { useServerStore } from '../stores/servers';
import { getRenderer } from '../components/ResultRenderer';

const router = useRouter();
const route = useRoute();
const diagnoseStore = useDiagnoseStore();
const serverStore = useServerStore();

const stepCommands = ref([]);
const stepStates = ref([]);
const expandRaw = ref([]);
const serverNames = ref({});

function getStepTagType(state) {
  if (state === 'completed') return 'success';
  if (state === 'executing') return 'warning';
  if (state === 'error') return 'danger';
  return 'info';
}

function hasPlaceholder(command) {
  if (!command) return false;
  const matches = command.match(/\{(\w+)\}/g);
  return matches && matches.length > 0;
}

function getUnfilledPlaceholders(command) {
  if (!command) return '';
  const matches = command.match(/\{(\w+)\}/g) || [];
  const unfilled = matches
    .map(m => m.slice(1, -1))
    .filter(key => !diagnoseStore.variables.has(key));
  return [...new Set(unfilled)].join(', ');
}

function getExtractedVariables(stepId) {
  const variables = [];
  for (const [key, value] of diagnoseStore.variables.entries()) {
    variables.push([key, value]);
  }
  return variables;
}

function getServerName(serverId) {
  return serverNames.value[serverId] || serverId;
}

async function loadServerNames() {
  if (diagnoseStore.selectedServerId) {
    try {
      const res = await getServerById(diagnoseStore.selectedServerId);
      serverNames.value[diagnoseStore.selectedServerId] = res.data.name;
    } catch {
      serverNames.value[diagnoseStore.selectedServerId] = diagnoseStore.selectedServerId;
    }
  }
}

function initializeStepCommands() {
  stepCommands.value = diagnoseStore.steps.map((step, index) => {
    return diagnoseStore.getPreFilledCommand(step.id) || step.command || '';
  });
  stepStates.value = diagnoseStore.steps.map(() => ({}));
  expandRaw.value = diagnoseStore.steps.map(() => false);
}

async function handleExecute(index) {
  const step = diagnoseStore.steps[index];
  const command = stepCommands.value[index] || step.command;

  if (!command || !command.trim()) {
    ElMessage.warning('请输入命令');
    return;
  }

  stepStates.value[index] = { state: 'executing', result: null };

  try {
    const res = await executeCommand(diagnoseStore.selectedServerId, command);
    const result = res.data;

    stepStates.value[index] = { state: 'completed', result };

    diagnoseStore.saveStepResult(step.id, result);

    for (let i = index + 1; i < diagnoseStore.steps.length; i++) {
      const nextStep = diagnoseStore.steps[i];
      stepCommands.value[i] = diagnoseStore.getPreFilledCommand(nextStep.id) || nextStep.command || '';
    }
  } catch (e) {
    stepStates.value[index] = {
      state: 'error',
      result: {
        state: 'failed',
        error: e.response?.data?.error || e.message || '执行失败',
        results: []
      }
    };
    ElMessage.error(e.response?.data?.error || '命令执行失败');
  }
}

async function handleReset() {
  try {
    await ElMessageBox.confirm(
      '确定要开始新的诊断吗？当前诊断进度将丢失。',
      '确认重置',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    );
    diagnoseStore.reset();
    router.push('/scenes');
  } catch {
  }
}

function goBack() {
  router.push('/scenes');
}

onMounted(async () => {
  if (!diagnoseStore.currentSceneId) {
    router.push('/scenes');
    return;
  }

  await loadServerNames();
  initializeStepCommands();
});
</script>

<style scoped>
.step-card {
  border-left: 4px solid #dcdfe6;
  transition: border-color 0.3s;
}

.step-card.step-completed {
  border-left-color: #67c23a;
}

.step-card.step-executing {
  border-left-color: #409eff;
}

.step-card.step-error {
  border-left-color: #f56c6c;
}

.step-description {
  color: #606266;
  font-size: 14px;
  margin: 0 0 12px 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

.command-input-area {
  margin-bottom: 8px;
}

.placeholder-hint {
  color: #e6a23c;
  font-size: 12px;
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.step-result {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
}

.step-hint {
  background: #fdf6ec;
  color: #e6a23c;
  font-size: 13px;
  padding: 8px 12px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.variables-extracted {
  background: #f0f9eb;
  padding: 8px 12px;
  border-radius: 4px;
}

.result-raw {
  background: #fff;
  padding: 12px;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.6;
  overflow-x: auto;
  margin: 0;
}
</style>
