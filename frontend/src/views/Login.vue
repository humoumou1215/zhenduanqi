<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <h2 class="login-title">Arthas 远程诊断工具</h2>
        <p class="login-subtitle">v1.0.0</p>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
            size="large"
            clearable
            autocomplete="username"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            size="large"
            show-password
            clearable
            autocomplete="current-password"
            @input="checkPasswordStrength"
          />
          <div
            v-if="form.password"
            class="password-strength"
            :class="'strength-' + passwordStrength.level"
          >
            <div class="strength-bar">
              <div class="strength-level" :style="{ width: passwordStrength.width + '%' }"></div>
            </div>
            <span class="strength-text">{{ passwordStrength.text }}</span>
          </div>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="rememberMe">记住我</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-button"
            :loading="loading"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-tips">
        <p>
          <el-icon><InfoFilled /></el-icon>
          默认账号：admin / admin123
        </p>
        <p>
          <el-icon><Key /></el-icon>
          按 Enter 键快速登录
        </p>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { User, Lock, InfoFilled, Key } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { useUserStore } from '../stores/user';

const router = useRouter();
const userStore = useUserStore();
const formRef = ref(null);
const loading = ref(false);
const rememberMe = ref(false);

const form = reactive({ username: '', password: '' });
const passwordStrength = reactive({ level: 0, text: '', width: 0 });
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
};

function checkPasswordStrength() {
  const password = form.password;
  let strength = 0;
  let text = '';
  let level = 0;
  let width = 0;

  if (password.length >= 6) strength += 1;
  if (password.length >= 10) strength += 1;
  if (/[A-Z]/.test(password)) strength += 1;
  if (/[a-z]/.test(password)) strength += 1;
  if (/[0-9]/.test(password)) strength += 1;
  if (/[^A-Za-z0-9]/.test(password)) strength += 1;

  if (strength <= 2) {
    text = '弱';
    level = 1;
    width = 33;
  } else if (strength <= 4) {
    text = '中';
    level = 2;
    width = 66;
  } else {
    text = '强';
    level = 3;
    width = 100;
  }

  passwordStrength.level = level;
  passwordStrength.text = text;
  passwordStrength.width = width;
}

onMounted(() => {
  const savedUsername = localStorage.getItem('rememberedUsername');
  if (savedUsername) {
    form.username = savedUsername;
    rememberMe.value = true;
  }
});

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;

  loading.value = true;
  try {
    // 对用户名和密码进行 trim 处理
    const trimmedUsername = form.username.trim();
    const trimmedPassword = form.password.trim();

    // 更新表单值为 trim 后的值
    form.username = trimmedUsername;
    form.password = trimmedPassword;

    await userStore.login(trimmedUsername, trimmedPassword);

    // 保存登录状态，包括记住我功能
    if (rememberMe.value) {
      localStorage.setItem('rememberedUsername', trimmedUsername);
      localStorage.setItem('loginRemembered', 'true');
    } else {
      localStorage.removeItem('rememberedUsername');
      localStorage.removeItem('loginRemembered');
    }

    // 保存登录时间以便后续分析
    localStorage.setItem('lastLoginTime', new Date().toISOString());

    ElMessage.success('登录成功');
    router.push('/scenes');
  } catch (e) {
    ElMessage.error(e.response?.data?.error || '登录失败');
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 400px;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.login-title {
  text-align: center;
  margin: 0;
  color: #303133;
}

.login-subtitle {
  text-align: center;
  margin: 8px 0 0 0;
  color: #909399;
  font-size: 14px;
}

.login-button {
  width: 100%;
  border-radius: 8px;
}

.login-tips {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
  font-size: 13px;
  color: #909399;
}

.login-tips p {
  margin: 8px 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

.login-tips .el-icon {
  color: #409eff;
}

.password-strength {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.strength-bar {
  flex: 1;
  height: 6px;
  background-color: #ebeef5;
  border-radius: 3px;
  overflow: hidden;
}

.strength-level {
  height: 100%;
  border-radius: 3px;
  transition: all 0.3s ease;
}

.strength-1 .strength-level {
  background-color: #f56c6c;
}

.strength-2 .strength-level {
  background-color: #e6a23c;
}

.strength-3 .strength-level {
  background-color: #67c23a;
}

.strength-text {
  font-size: 12px;
  min-width: 24px;
}

.strength-1 .strength-text {
  color: #f56c6c;
}

.strength-2 .strength-text {
  color: #e6a23c;
}

.strength-3 .strength-text {
  color: #67c23a;
}

/* 添加淡入动画效果 */
.login-card {
  animation: fadeInUp 0.5s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 添加按钮悬停效果 */
.login-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
}

.login-button:active {
  transform: translateY(0);
}
</style>
