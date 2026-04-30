<template>
  <el-container style="min-height: 100vh">
    <el-header style="background: #409eff; color: white; display: flex; align-items: center; padding: 0 20px;">
      <h2 style="margin: 0; font-size: 18px;">Arthas 远程诊断工具</h2>
      <div style="flex: 1" />
      <el-menu
        mode="horizontal"
        :ellipsis="false"
        :default-active="route.path"
        router
        style="background: transparent; border-bottom: none;"
        active-text-color="#fff"
      >
        <el-menu-item index="/servers" style="color: white;">服务器管理</el-menu-item>
        <el-menu-item index="/diagnose" style="color: white;">执行诊断</el-menu-item>
      </el-menu>
    </el-header>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { useRoute } from 'vue-router'
const route = useRoute()
</script>

<style>
body { margin: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
</style>
