<template>
  <div>
    <h3>执行诊断命令</h3>

    <el-card style="margin-bottom: 16px;">
      <el-form :model="form" label-width="100px">
        <el-form-item label="目标服务器">
          <el-select v-model="form.serverId" placeholder="请选择服务器" style="width: 300px;">
            <el-option
              v-for="s in store.list"
              :key="s.id"
              :label="s.name + ' (' + s.host + ':' + s.httpPort + ')'"
              :value="s.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="诊断命令">
          <el-input
            v-model="form.command"
            type="textarea"
            :rows="4"
            placeholder="输入 Arthas 命令，例如：&#10;dashboard&#10;thread -n 5&#10;jstack&#10;sc -d com.example.MyController"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleExecute" :loading="executing" :disabled="!form.serverId || !form.command.trim()">
            执行
          </el-button>
          <el-button @click="clearResult">清空结果</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="result" :style="{ borderLeft: isSuccess ? '4px solid #67c23a' : '4px solid #f56c6c' }">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span>
            执行结果
            <el-tag v-if="result.state === 'succeeded'" type="success" size="small" style="margin-left: 8px;">成功</el-tag>
            <el-tag v-else type="danger" size="small" style="margin-left: 8px;">失败</el-tag>
          </span>
          <el-button size="small" @click="expandRaw = !expandRaw">
            {{ expandRaw ? '收起原始响应' : '查看原始响应' }}
          </el-button>
        </div>
      </template>

      <div v-if="result.error" style="color: #f56c6c; margin-bottom: 12px; white-space: pre-wrap;">{{ result.error }}</div>

      <div v-for="(r, i) in result.results" :key="i">
        <pre