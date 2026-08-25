<template>
  <div class="login-wrapper">
    <div class="login-card">
      <!-- 左侧品牌展示区 -->
      <div class="login-brand">
        <div class="brand-top">
          <div class="brand-logo">
            <div class="brand-logo-icon">C</div>
            <div class="brand-logo-text">橙子项目管理平台</div>
          </div>
          <h1 class="brand-title">敏捷项目管理<br>让协作更高效</h1>
          <p class="brand-desc">产品需求 → 项目迭代 → 任务执行 → 测试发布，全流程可视化管理</p>
          <div class="brand-features">
            <div class="brand-feature">
              <div class="brand-feature-icon">📋</div>
              <span>需求全生命周期管理</span>
            </div>
            <div class="brand-feature">
              <div class="brand-feature-icon">🎯</div>
              <span>迭代看板拖拽式协作</span>
            </div>
            <div class="brand-feature">
              <div class="brand-feature-icon">🐛</div>
              <span>Bug跟踪与测试管理</span>
            </div>
          </div>
        </div>
        <div class="brand-footer">© 2026 橙子项目管理平台 · 毕业设计项目</div>
      </div>

      <!-- 右侧登录表单区 -->
      <div class="login-form-area">
        <h2 class="form-title">欢迎回来 👋</h2>
        <p class="form-subtitle">请登录您的账号以继续</p>

        <el-form ref="formRef" :model="form" :rules="rules" @keyup.enter="handleLogin">
          <el-form-item prop="username">
            <label class="form-label">账号</label>
            <el-input
              v-model="form.username"
              placeholder="请输入账号"
              size="large"
              class="form-input"
            >
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item prop="password">
            <label class="form-label">密码</label>
            <el-input
              v-model="form.password"
              type="password"
              show-password
              placeholder="请输入密码"
              size="large"
              class="form-input"
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-button
            type="primary"
            class="login-btn"
            :loading="loading"
            size="large"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form>

        <div class="quick-login">
          <div class="quick-login-title">快捷登录（演示账号）</div>
          <div class="quick-login-grid">
            <div
              v-for="acc in quickAccounts"
              :key="acc.username"
              class="quick-login-btn"
              :class="{ active: activeAccount === acc.username }"
              @click="quickLogin(acc)"
            >
              <div class="quick-role-icon">{{ acc.icon }}</div>
              <div class="quick-role-name">{{ acc.shortRole }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)
const activeAccount = ref('')
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const quickAccounts = [
  { username: 'admin', role: '系统管理员', shortRole: '管理员', icon: '👑' },
  { username: 'po01', role: '产品经理', shortRole: '产品', icon: '📝' },
  { username: 'pm01', role: '项目经理', shortRole: '项目', icon: '📊' },
  { username: 'dev01', role: '开发工程师', shortRole: '开发', icon: '💻' },
  { username: 'qa01', role: '测试工程师', shortRole: '测试', icon: '🔍' }
]

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    await userStore.fetchInfo()
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}

async function quickLogin(acc) {
  activeAccount.value = acc.username
  form.username = acc.username
  form.password = ''
  ElMessage.info('已填入账号，请输入密码后登录')
  activeAccount.value = ''
  return
}
</script>

<style scoped>
.login-wrapper {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  position: relative;
  overflow: hidden;
  background: var(--bg-gradient);
}

.login-wrapper::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -20%;
  width: 800px;
  height: 800px;
  background: radial-gradient(circle, rgba(141, 240, 197, 0.14) 0%, transparent 70%);
  border-radius: 50%;
  pointer-events: none;
}

.login-wrapper::after {
  content: '';
  position: absolute;
  bottom: -30%;
  left: -10%;
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, rgba(110, 168, 220, 0.08) 0%, transparent 70%);
  border-radius: 50%;
  pointer-events: none;
}

.login-card {
  background: var(--bg-secondary);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-xl);
  width: 100%;
  max-width: 920px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  overflow: hidden;
  position: relative;
  z-index: 1;
}

/* 左侧品牌区 */
.login-brand {
  background: linear-gradient(160deg, #10243f 0%, #173251 55%, #1d3858 100%);
  padding: 44px 36px;
  color: white;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  position: relative;
  overflow: hidden;
}

.login-brand::before {
  content: '';
  position: absolute;
  top: -100px;
  right: -100px;
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(255,255,255,0.08) 0%, transparent 70%);
  border-radius: 50%;
}

.brand-top {
  position: relative;
  z-index: 1;
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 36px;
}

.brand-logo-icon {
  width: 44px;
  height: 44px;
  background: var(--accent-primary);
  color: var(--surface-dark);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 800;
}

.brand-logo-text {
  font-family: Inter, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.brand-title {
  font-family: Inter, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 14px;
  line-height: 1.2;
}

.brand-desc {
  font-size: 14px;
  opacity: 0.75;
  line-height: 1.7;
  margin-bottom: 28px;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.brand-feature {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  opacity: 0.85;
}

.brand-feature-icon {
  width: 28px;
  height: 28px;
  background: rgba(255,255,255,0.12);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.brand-footer {
  font-size: 12px;
  opacity: 0.5;
  position: relative;
  z-index: 1;
}

/* 右侧表单区 */
.login-form-area {
  padding: 48px 44px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.form-title {
  font-family: Inter, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 26px;
  font-weight: 700;
  margin-bottom: 6px;
  color: var(--text-primary);
}

.form-subtitle {
  color: var(--text-secondary);
  font-size: 14px;
  margin-bottom: 28px;
}

.form-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.form-input :deep(.el-input__wrapper) {
  padding: 4px 15px;
  box-shadow: 0 0 0 1.5px var(--border-color) inset !important;
  border-radius: var(--radius-sm);
}

.form-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1.5px var(--accent-success) inset, var(--focus-ring) !important;
}

.login-btn {
  width: 100%;
  margin-top: 8px;
  font-weight: 600;
  font-size: 15px;
  height: 44px;
  border-radius: var(--radius-sm);
}

/* 快捷登录 */
.quick-login {
  margin-top: 28px;
  padding-top: 22px;
  border-top: 1px solid var(--border-light);
}

.quick-login-title {
  font-size: 12px;
  font-weight: 700;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 14px;
}

.quick-login-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
}

.quick-login-btn {
  padding: 12px 6px;
  border: 1.5px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--bg-secondary);
  cursor: pointer;
  transition: all 0.2s;
  text-align: center;
}

.quick-login-btn:hover {
  border-color: var(--accent-success);
  background: var(--accent-primary-bg);
  transform: translateY(-1px);
}

.quick-login-btn.active {
  border-color: var(--accent-success);
  background: var(--accent-primary-bg);
}

.quick-role-icon {
  font-size: 18px;
  margin-bottom: 4px;
}

.quick-role-name {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-secondary);
}

/* 响应式 */
@media (max-width: 768px) {
  .login-card {
    grid-template-columns: 1fr;
    max-width: 420px;
  }
  .login-brand {
    display: none;
  }
  .login-form-area {
    padding: 36px 28px;
  }
  .quick-login-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
</style>
