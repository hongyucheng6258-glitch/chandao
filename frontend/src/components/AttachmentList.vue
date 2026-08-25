<template>
  <div class="attachment-list">
    <div class="att-head">
      <span class="att-title">附件</span>
      <el-upload
        :action="uploadUrl"
        :headers="headers"
        :data="{ objectType, objectId }"
        :show-file-list="false"
        :before-upload="beforeUpload"
        :on-success="onSuccess"
        :on-error="onError"
        accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.png,.jpg,.jpeg,.gif,.webp,.zip,.rar,.txt,.md">
        <el-button type="primary" size="small" icon="Upload" :loading="uploading">上传附件</el-button>
      </el-upload>
    </div>

    <el-empty v-if="!loading && !list.length" description="暂无附件" :image-size="40" />
    <ul v-else class="att-items">
      <li v-for="a in list" :key="a.id" class="att-item">
        <el-icon class="att-icon"><Document /></el-icon>
        <span class="att-name" :title="a.fileName">{{ a.fileName }}</span>
        <span class="att-size">{{ formatSize(a.fileSize) }}</span>
        <span class="att-meta">{{ a.uploaderName }} · {{ fmt(a.createdTime) }}</span>
        <span class="att-ops">
          <el-button v-if="isImage(a.fileExt)" link type="primary" size="small" @click="preview(a)">预览</el-button>
          <el-button link type="primary" size="small" @click="download(a)">下载</el-button>
          <el-button v-if="canDelete(a)" link type="danger" size="small" @click="remove(a)">删除</el-button>
        </span>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Document } from '@element-plus/icons-vue'
import { attachmentApi } from '@/api'
import { useUserStore } from '@/store/user'

const props = defineProps({
  objectType: { type: String, required: true },
  objectId: { type: [Number, String], required: true }
})

const store = useUserStore()
const list = ref([])
const loading = ref(false)
const uploading = ref(false)

const token = localStorage.getItem('token')
const uploadUrl = '/api/attachments/upload'
const headers = { Authorization: `Bearer ${token}` }

const IMAGE_EXT = ['png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp']

function isImage(ext) {
  return !!ext && IMAGE_EXT.includes(String(ext).toLowerCase())
}
function fmt(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').slice(0, 16)
}
function formatSize(bytes) {
  if (!bytes && bytes !== 0) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}
function canDelete(a) {
  return String(a.uploaderId) === String(store.id)
}

async function load() {
  loading.value = true
  try {
    const res = await attachmentApi.list(props.objectType, props.objectId)
    list.value = res.data || []
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

function beforeUpload(file) {
  const max = 10 * 1024 * 1024
  if (file.size > max) {
    ElMessage.warning('文件不能超过 10MB')
    return false
  }
  uploading.value = true
  return true
}

function onSuccess(response) {
  uploading.value = false
  if (response && response.code === 200) {
    ElMessage.success('上传成功')
    load()
  } else {
    ElMessage.error(response?.message || '上传失败')
  }
}

function onError() {
  uploading.value = false
  ElMessage.error('上传失败, 请重试')
}

async function download(att) {
  try {
    const res = await fetch(`/api/attachments/${att.id}/download`, {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
    })
    if (!res.ok) throw new Error('download failed')
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = att.fileName
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error('下载失败')
  }
}

async function preview(att) {
  try {
    const res = await fetch(`/api/attachments/${att.id}/download`, {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
    })
    if (!res.ok) throw new Error('preview failed')
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    window.open(url, '_blank')
    setTimeout(() => URL.revokeObjectURL(url), 60000)
  } catch (e) {
    ElMessage.error('预览失败')
  }
}

async function remove(att) {
  try {
    await attachmentApi.remove(att.id)
    ElMessage.success('已删除')
    load()
  } catch (e) {
    /* 拦截器已提示 */
  }
}

onMounted(load)
</script>

<style scoped>
.attachment-list { display: flex; flex-direction: column; gap: 8px; }
.att-head { display: flex; align-items: center; justify-content: space-between; }
.att-title { font-size: 13px; font-weight: 650; color: var(--text-primary); }
.att-items { list-style: none; margin: 0; padding: 0; max-height: 220px; overflow: auto; }
.att-item {
  display: flex; align-items: center; gap: 8px; padding: 6px 8px;
  border: 1px solid var(--border-light); border-radius: var(--radius-sm); margin-bottom: 6px; background: var(--surface-soft);
}
.att-icon { color: var(--accent-info); flex: none; }
.att-name { flex: 1; font-size: 13px; color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.att-size { font-size: 12px; color: var(--text-secondary); flex: none; }
.att-meta { font-size: 12px; color: var(--text-muted); flex: none; }
.att-ops { flex: none; display: flex; gap: 2px; }
</style>
