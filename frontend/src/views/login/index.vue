<template>
  <div class="login-page">
    <el-card class="login-card">
      <div class="title">PMS 敏捷项目管理系统</div>
      <div class="subtitle">参考禅道的需求-任务-缺陷全流程管理平台</div>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="'User'" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="密码" :prefix-icon="'Lock'" />
        </el-form-item>
        <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">
          登 录
        </el-button>
      </el-form>
      <div class="tip">初始账号：admin / 123456</div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

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
</script>

<style scoped>
.login-page {
  height: 100%; display: flex; justify-content: center; align-items: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #e6f1fb 100%);
}
.login-card { width: 400px; padding: 12px 16px; }
.title { font-size: 22px; font-weight: 600; text-align: center; color: #303133; }
.subtitle { font-size: 13px; color: #909399; text-align: center; margin: 8px 0 24px; }
.login-btn { width: 100%; }
.tip { text-align: center; color: #c0c4cc; font-size: 12px; margin-top: 12px; }
</style>
