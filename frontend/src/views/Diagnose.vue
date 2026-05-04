<template>
  <div class="diagnose-page">
    <div class="main-content">
      <div class="left-panel">
        <el-card class="command-card">
          <template #header>
            <div class="card-header">
              <span>执行诊断命令</span>
            </div>
          </template>

          <el-form :model="form" label-width="100px">
            <el-form-item label="目标服务器">
              <el-select
                v-model="form.serverId"
                placeholder="请选择服务器"
                style="width: 100%"
                @change="handleServerChange"
              >
                <el-option
                  v-for="s in serverStore.list"
                  :key="s.id"
                  :label="s.name + ' (' + s.host + ':' + s.httpPort + ')'"
                  :value="s.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="诊断命令">
              <div class="command-input-wrapper">
                <el-autocomplete
                  ref="commandInputRef"
                  v-model="form.command"
                  :fetch-suggestions="filterCommands"
                  placeholder="输入或选择命令"
                  style="width: 200px"
                  clearable
                  @select="handleCommandSelect"
                  @focus="activeInput = 'command'"
                  @keydown.up.prevent="navigateCommandHistory(-1)"
                  @keydown.down.prevent="navigateCommandHistory(1)"
                  @keydown.enter="handleCommandEnter"
                >
                  <template #default="{ item }">
                    <div class="command-suggestion">
                      <span class="cmd-name">{{ item.name }}</span>
                      <span class="cmd-desc">{{ item.description }}</span>
                    </div>
                  </template>
                </el-autocomplete>
                <el-input
                  ref="paramInputRef"
                  v-model="form.params"
                  placeholder="参数（可选）"
                  style="flex: 1; margin-left: 8px"
                  clearable
                  @focus="activeInput = 'params'"
                  @keydown.up.prevent="navigateParamHistory(-1)"
                  @keydown.down.prevent="navigateParamHistory(1)"
                />
              </div>
            </el-form-item>

            <el-form-item label="指令预览">
              <div class="command-preview">
                <pre class="preview-code">{{ previewCommand || '（请输入命令）' }}</pre>
                <el-button
                  v-if="previewCommand"
                  size="small"
                  type="primary"
                  link
                  @click="copyCommand"
                >
                  <el-icon><CopyDocument /></el-icon>
                  复制
                </el-button>
              </div>
            </el-form-item>

            <el-form-item>
              <el-button
                v-if="userStore.role === 'OPERATOR' || userStore.role === 'ADMIN'"
                type="primary"
                @click="handleExecute"
                :loading="executing"
                :disabled="!form.serverId || !form.command.trim()"
              >
                执行
              </el-button>
              <el-button @click="clearResult">清空结果</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card
          v-if="result"
          class="result-card"
          :style="{ borderLeft: isSuccess ? '4px solid #67c23a' : '4px solid #f56c6c' }"
        >
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span>
                执行结果
                <el-tag
                  v-if="result.state === 'succeeded'"
                  type="success"
                  size="small"
                  style="margin-left: 8px"
                >
                  成功
                </el-tag>
                <el-tag v-else type="danger" size="small" style="margin-left: 8px">失败</el-tag>
              </span>
              <el-button size="small" @click="expandRaw = !expandRaw">
                {{ expandRaw ? '收起原始响应' : '查看原始响应' }}
              </el-button>
            </div>
          </template>

          <div
            v-if="result.error"
            style="color: #f56c6c; margin-bottom: 12px; white-space: pre-wrap"
          >
            {{ result.error }}
          </div>

          <div v-for="(r, i) in result.structuredResults" :key="i">
            <component :is="getRenderer(r.type)" :data="r.data" />
          </div>

          <div
            v-if="result.results && result.results.length > 0 && !result.structuredResults"
            v-for="(r, i) in result.results"
            :key="'raw-' + i"
          >
            <pre
              style="
                background: #f5f7fa;
                padding: 12px;
                border-radius: 4px;
                font-size: 13px;
                line-height: 1.6;
                overflow-x: auto;
              "
              >{{ r }}</pre
            >
          </div>

          <div v-if="result.rawResponse && expandRaw" style="margin-top: 12px">
            <div style="font-weight: 500; margin-bottom: 8px; color: #606266">原始响应</div>
            <pre
              style="
                background: #f5f7fa;
                padding: 12px;
                border-radius: 4px;
                font-size: 12px;
                max-height: 300px;
                overflow: auto;
              "
              >{{ result.rawResponse }}</pre
            >
          </div>
        </el-card>
      </div>

      <div class="right-panel" :class="{ collapsed: helpPanelCollapsed }">
        <div class="help-panel-header">
          <span v-if="!helpPanelCollapsed">帮助</span>
          <el-button
            size="small"
            :icon="helpPanelCollapsed ? ArrowLeft : ArrowRight"
            @click="helpPanelCollapsed = !helpPanelCollapsed"
          />
        </div>
        <div v-if="!helpPanelCollapsed" class="help-panel-content">
          <template v-if="activeInput === 'command'">
            <h4>命令清单</h4>
            <el-input
              v-model="commandSearch"
              placeholder="搜索命令..."
              clearable
              size="small"
              style="margin-bottom: 12px"
            />
            <div class="command-list">
              <div
                v-for="cmd in filteredCommands"
                :key="cmd.name"
                class="command-item"
                @click="selectCommand(cmd.name)"
              >
                <div class="cmd-title">{{ cmd.name }}</div>
                <div class="cmd-brief">{{ cmd.description }}</div>
              </div>
            </div>
          </template>
          <template v-else>
            <h4>{{ form.command || '命令' }} 参数说明</h4>
            <div v-if="currentCommandInfo" class="param-help">
              <div class="param-section">
                <div class="section-title">参数</div>
                <pre class="param-text">{{ currentCommandInfo.params }}</pre>
              </div>
              <div class="param-section">
                <div class="section-title">示例</div>
                <div class="example-list">
                  <div
                    v-for="(example, idx) in currentCommandInfo.examples"
                    :key="idx"
                    class="example-item"
                    @click="applyExample(example)"
                  >
                    <code>{{ example }}</code>
                  </div>
                </div>
              </div>
            </div>
            <el-empty v-else description="请先选择一个命令" :image-size="60" />
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue';
import { useServerStore } from '../stores/servers';
import { useUserStore } from '../stores/user';
import { useCommandCacheStore } from '../stores/commandCache';
import { executeCommand } from '../api';
import { getRenderer } from '../components/ResultRenderer';
import { ElMessage } from 'element-plus';
import { CopyDocument, ArrowLeft, ArrowRight } from '@element-plus/icons-vue';
import arthasCommands from '../data/arthas-commands.json';

const serverStore = useServerStore();
const userStore = useUserStore();
const commandCacheStore = useCommandCacheStore();

const result = ref(null);
const executing = ref(false);
const expandRaw = ref(false);
const form = ref({ serverId: '', command: '', params: '' });
const activeInput = ref('command');
const helpPanelCollapsed = ref(false);
const commandSearch = ref('');
const commandInputRef = ref(null);
const paramInputRef = ref(null);
const commandHistoryIndex = ref(-1);
const paramHistoryIndex = ref(-1);

const isSuccess = computed(() => result.value?.state === 'succeeded');

const previewCommand = computed(() => {
  if (!form.value.command.trim()) return '';
  const cmd = form.value.command.trim();
  const params = form.value.params.trim();
  return params ? `${cmd} ${params}` : cmd;
});

const currentCommandInfo = computed(() => {
  if (!form.value.command) return null;
  return arthasCommands.find((c) => c.name.toLowerCase() === form.value.command.toLowerCase());
});

const filteredCommands = computed(() => {
  if (!commandSearch.value) return arthasCommands;
  const keyword = commandSearch.value.toLowerCase();
  return arthasCommands.filter(
    (c) => c.name.toLowerCase().includes(keyword) || c.description.toLowerCase().includes(keyword)
  );
});

onMounted(() => {
  serverStore.fetchServers();
  restoreSelectedServer();
});

watch(
  () => form.value.command,
  (newCmd) => {
    if (newCmd) {
      const latestParam = commandCacheStore.getLatestParam(newCmd);
      if (latestParam && !form.value.params) {
        form.value.params = latestParam;
      }
    }
  }
);

function restoreSelectedServer() {
  serverStore.restoreCurrentServer();
  if (serverStore.currentServerId) {
    form.value.serverId = serverStore.currentServerId;
  }
}

function handleServerChange(serverId) {
  serverStore.setCurrentServer(serverId);
}

function filterCommands(queryString, cb) {
  const results = queryString
    ? arthasCommands.filter(
        (c) =>
          c.name.toLowerCase().includes(queryString.toLowerCase()) ||
          c.description.toLowerCase().includes(queryString.toLowerCase())
      )
    : arthasCommands;
  cb(results);
}

function handleCommandSelect(item) {
  form.value.command = item.name;
  const latestParam = commandCacheStore.getLatestParam(item.name);
  if (latestParam) {
    form.value.params = latestParam;
  }
  nextTick(() => {
    paramInputRef.value?.focus();
  });
}

function handleCommandEnter() {
  if (form.value.command) {
    paramInputRef.value?.focus();
  }
}

function navigateCommandHistory(direction) {
  const history = commandCacheStore.getCommandHistory();
  if (history.length === 0) return;

  if (direction === -1) {
    if (commandHistoryIndex.value < history.length - 1) {
      commandHistoryIndex.value++;
    }
  } else {
    if (commandHistoryIndex.value > 0) {
      commandHistoryIndex.value--;
    } else if (commandHistoryIndex.value === 0) {
      commandHistoryIndex.value = -1;
      form.value.command = '';
      return;
    }
  }

  if (commandHistoryIndex.value >= 0 && commandHistoryIndex.value < history.length) {
    form.value.command = history[commandHistoryIndex.value];
  }
}

function navigateParamHistory(direction) {
  if (!form.value.command) return;

  const history = commandCacheStore.getParamHistory(form.value.command);
  if (history.length === 0) return;

  if (direction === -1) {
    if (paramHistoryIndex.value < history.length - 1) {
      paramHistoryIndex.value++;
    }
  } else {
    if (paramHistoryIndex.value > 0) {
      paramHistoryIndex.value--;
    } else if (paramHistoryIndex.value === 0) {
      paramHistoryIndex.value = -1;
      form.value.params = '';
      return;
    }
  }

  if (paramHistoryIndex.value >= 0 && paramHistoryIndex.value < history.length) {
    form.value.params = history[paramHistoryIndex.value];
  }
}

function selectCommand(name) {
  form.value.command = name;
  const latestParam = commandCacheStore.getLatestParam(name);
  if (latestParam) {
    form.value.params = latestParam;
  }
  commandInputRef.value?.focus();
}

function applyExample(example) {
  const parts = example.split(/\s+/);
  if (parts.length > 0) {
    form.value.command = parts[0];
    form.value.params = parts.slice(1).join(' ');
  }
  paramInputRef.value?.focus();
}

async function handleExecute() {
  if (!previewCommand.value) return;

  executing.value = true;
  expandRaw.value = false;

  commandCacheStore.addCommandToHistory(form.value.command);
  if (form.value.params.trim()) {
    commandCacheStore.addParamToHistory(form.value.command, form.value.params);
  }

  commandHistoryIndex.value = -1;
  paramHistoryIndex.value = -1;

  try {
    const res = await executeCommand(form.value.serverId, previewCommand.value);
    result.value = res.data;
  } catch (e) {
    result.value = {
      state: 'failed',
      error: e.response?.data?.error || e.message || '请求失败',
      results: [],
      rawResponse: null,
    };
  } finally {
    executing.value = false;
  }
}

function clearResult() {
  result.value = null;
  expandRaw.value = false;
}

async function copyCommand() {
  if (!previewCommand.value) return;
  try {
    await navigator.clipboard.writeText(previewCommand.value);
    ElMessage.success('已复制到剪贴板');
  } catch {
    ElMessage.error('复制失败');
  }
}
</script>

<style scoped>
.diagnose-page {
  height: 100%;
}

.main-content {
  display: flex;
  gap: 16px;
  height: calc(100vh - 120px);
}

.left-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.right-panel {
  width: 300px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 4px;
  border: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
  transition: width 0.3s;
}

.right-panel.collapsed {
  width: 40px;
}

.help-panel-header {
  padding: 12px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 500;
}

.help-panel-content {
  flex: 1;
  overflow: auto;
  padding: 12px;
}

.command-card {
  flex-shrink: 0;
}

.result-card {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.command-input-wrapper {
  display: flex;
  gap: 8px;
}

.command-suggestion {
  display: flex;
  flex-direction: column;
  padding: 4px 0;
}

.cmd-name {
  font-weight: 500;
  color: #409eff;
}

.cmd-desc {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.command-preview {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.preview-code {
  flex: 1;
  background: #f5f7fa;
  padding: 8px 12px;
  border-radius: 4px;
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 13px;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}

.command-list {
  max-height: calc(100vh - 350px);
  overflow-y: auto;
}

.command-item {
  padding: 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.2s;
}

.command-item:hover {
  background: #f5f7fa;
}

.cmd-title {
  font-weight: 500;
  color: #409eff;
  margin-bottom: 2px;
}

.cmd-brief {
  font-size: 12px;
  color: #606266;
  line-height: 1.4;
}

.param-help {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.param-section {
  margin-bottom: 8px;
}

.section-title {
  font-weight: 500;
  margin-bottom: 8px;
  color: #303133;
}

.param-text {
  background: #f5f7fa;
  padding: 8px;
  border-radius: 4px;
  font-size: 12px;
  white-space: pre-wrap;
  margin: 0;
}

.example-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.example-item {
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background 0.2s;
}

.example-item:hover {
  background: #ecf5ff;
}

.example-item code {
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 12px;
  color: #409eff;
}

h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #303133;
}
</style>
