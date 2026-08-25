<template>
  <div class="config-page">
    <div class="page-header">
      <h2 class="page-title">系统配置</h2>
      <div class="page-actions">
        <el-button :icon="Refresh" @click="handleRefresh" :loading="refreshing">刷新缓存</el-button>
        <el-button type="primary" :icon="Check" @click="handleSave" :loading="saving">保存配置</el-button>
      </div>
    </div>

    <div class="config-tip">
      <el-alert type="info" :closable="false" show-icon>
        配置保存后自动清除缓存，下次请求即时生效，无需重启服务。部分配置（如系统名称）刷新页面后可见。
      </el-alert>
    </div>

    <div class="config-grid">
      <div class="config-card" v-for="group in groupedConfigs" :key="group.key">
        <div class="config-card-header">
          <div class="config-card-icon">{{ group.icon }}</div>
          <div>
            <div class="config-card-title">{{ group.name }}</div>
            <div class="config-card-desc">{{ group.desc }}</div>
          </div>
        </div>
        <div class="config-card-body">
          <div class="config-field" v-for="item in group.items" :key="item.configKey">
            <label class="config-field-label">{{ item.configName }}</label>
            <el-input
              v-if="item.configType === 'string'"
              v-model="formData[item.configKey]"
              :placeholder="item.description || '请输入'"
              clearable
              class="config-field-input"
            />
            <el-input-number
              v-else-if="item.configType === 'number'"
              v-model="formData[item.configKey]"
              :min="0"
              controls-position="right"
              class="config-field-input"
            />
            <el-switch
              v-else-if="item.configType === 'boolean'"
              v-model="formData[item.configKey]"
              active-value="true"
              inactive-value="false"
            />
            <el-input
              v-else-if="item.configType === 'textarea'"
              v-model="formData[item.configKey]"
              type="textarea"
              :rows="3"
              :placeholder="item.description || '请输入'"
              class="config-field-input"
            />
            <div v-if="item.description" class="config-field-desc">{{ item.description }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="config-save-bar">
      <el-button @click="handleReset">重置</el-button>
      <el-button type="primary" :icon="Check" @click="handleSave" :loading="saving">保存所有配置</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Check } from '@element-plus/icons-vue'
import { getConfigList, updateConfig, refreshConfig } from '@/api/config'

const configList = ref([])
const formData = reactive({})
const saving = ref(false)
const refreshing = ref(false)

const groupConfig = {
  'system': { name: '基础设置', desc: '系统名称与基础参数', icon: '🏷️' },
  'dashboard': { name: '工作台设置', desc: '工作台展示配置', icon: '📊' },
  'upload': { name: '文件上传', desc: '上传大小与类型限制', icon: '📤' },
  'jwt': { name: '安全设置', desc: 'JWT与密码策略', icon: '🔐' },
  'mail': { name: '通知设置', desc: '邮件与站内通知', icon: '📧' },
  'default': { name: '其他设置', desc: '其他系统参数', icon: '⚙️' }
}

const groupedConfigs = computed(() => {
  const groups = {}
  configList.value.forEach(item => {
    const prefix = item.configKey.split('.')[0]
    const groupKey = groupConfig[prefix] ? prefix : 'default'
    if (!groups[groupKey]) {
      groups[groupKey] = {
        key: groupKey,
        name: groupConfig[groupKey].name,
        desc: groupConfig[groupKey].desc,
        icon: groupConfig[groupKey].icon,
        items: []
      }
    }
    groups[groupKey].items.push(item)
  })
  return Object.values(groups)
})

async function loadData() {
  const res = await getConfigList()
  configList.value = res.data || []
  configList.value.forEach(item => {
    formData[item.configKey] = item.configValue ?? ''
  })
}

async function handleSave() {
  saving.value = true
  try {
    const payload = {}
    configList.value.forEach(item => {
      payload[item.configKey] = String(formData[item.configKey] ?? '')
    })
    await updateConfig(payload)
    ElMessage.success('配置保存成功，已自动刷新缓存')
    await loadData()
  } finally {
    saving.value = false
  }
}

function handleReset() {
  loadData()
  ElMessage.info('已重置为当前配置')
}

async function handleRefresh() {
  refreshing.value = true
  try {
    await refreshConfig()
    ElMessage.success('缓存已刷新')
    await loadData()
  } finally {
    refreshing.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.config-page {
  padding: 24px 28px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}

.page-title {
  font-family: 'Outfit', sans-serif;
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
}

.page-actions {
  display: flex;
  gap: 10px;
}

.config-tip {
  margin-bottom: 20px;
}

.config-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 18px;
}

.config-card {
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  padding: 22px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-light);
}

.config-card-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-light);
}

.config-card-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: var(--accent-primary-bg);
  color: var(--accent-success);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.config-card-title {
  font-family: Inter, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}

.config-card-desc {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
}

.config-card-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.config-field {
  display: flex;
  flex-direction: column;
}

.config-field-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.config-field-input {
  width: 100%;
}

.config-field-input :deep(.el-input__wrapper) {
  border-radius: var(--radius-sm);
}

.config-field-desc {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 6px;
}

.config-save-bar {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 20px;
  border-top: 1px solid var(--border-light);
}

@media (max-width: 1024px) {
  .config-grid {
    grid-template-columns: 1fr;
  }
}
</style>
