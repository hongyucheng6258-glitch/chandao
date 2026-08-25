<template>
  <div class="activity-panel">
    <el-timeline v-loading="loading" class="activity">
      <el-timeline-item v-for="log in list" :key="log.id" :timestamp="fmt(log.createdTime)" placement="top">
        <!-- 评论 -->
        <div v-if="log.action === 'comment'" class="comment">
          <div class="comment-head">
            <el-avatar :size="22" class="avatar">{{ (log.actorName || '?').slice(0, 1) }}</el-avatar>
            <span class="name">{{ log.actorName }}</span>
            <el-button v-if="canDelete(log)" link type="danger" size="small" class="del" @click="remove(log.id)">删除</el-button>
          </div>
          <div class="comment-body">{{ log.detail }}</div>
        </div>
        <!-- 系统动态 -->
        <div v-else class="event">
          <span class="dot" />
          <span class="event-text"><b>{{ log.actorName }}</b> {{ log.action }}</span>
        </div>
      </el-timeline-item>
    </el-timeline>

    <el-empty v-if="!loading && !list.length" description="暂无动态" :image-size="48" />

    <div class="composer">
      <el-input v-model="content" type="textarea" :rows="2" resize="none" maxlength="2000"
        show-word-limit placeholder="发表评论, 支持对当前需求/Bug/任务展开讨论..." />
      <el-button type="primary" :disabled="!content.trim()" :loading="submitting" @click="submit">发送</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { activityApi } from '@/api'
import { useUserStore } from '@/store/user'

const props = defineProps({
  objectType: { type: String, required: true },
  objectId: { type: [Number, String], required: true }
})

const store = useUserStore()
const list = ref([])
const loading = ref(false)
const content = ref('')
const submitting = ref(false)

function fmt(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').slice(0, 16)
}

function canDelete(log) {
  return String(log.actorId) === String(store.id)
}

async function load() {
  loading.value = true
  try {
    const res = await activityApi.timeline(props.objectType, props.objectId)
    list.value = res.data.records || []
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

async function submit() {
  const text = content.value.trim()
  if (!text) return
  submitting.value = true
  try {
    await activityApi.comment({ objectType: props.objectType, objectId: props.objectId, content: text })
    content.value = ''
    ElMessage.success('评论成功')
    await load()
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    submitting.value = false
  }
}

async function remove(id) {
  try {
    await activityApi.remove(id)
    ElMessage.success('已删除')
    await load()
  } catch (e) {
    /* 拦截器已提示 */
  }
}

onMounted(load)
</script>

<style scoped>
.activity-panel { display: flex; flex-direction: column; min-height: 240px; }
.activity { padding: 8px 4px 0; max-height: 420px; overflow: auto; }
.comment {
  background: var(--surface-soft);
  border: 1px solid var(--border-light);
  border-radius: 8px;
  padding: 8px 10px;
}
.comment-head { display: flex; align-items: center; gap: 6px; margin-bottom: 4px; }
.avatar { background: var(--accent-primary-bg); color: var(--surface-dark); font-size: 13px; }
.name { font-size: 13px; font-weight: 650; color: var(--text-primary); }
.del { margin-left: auto; }
.comment-body {
  font-size: 13px; color: var(--text-primary); white-space: pre-wrap; word-break: break-word; line-height: 1.5;
}
.event { display: flex; align-items: center; gap: 6px; }
.dot { width: 8px; height: 8px; border-radius: 50%; background: #c0c4cc; flex: none; }
.event-text { font-size: 13px; color: var(--text-secondary); }
.composer {
  border-top: 1px solid var(--border-light); padding-top: 10px; margin-top: 8px;
  display: flex; flex-direction: column; gap: 8px;
}
.composer .el-button { align-self: flex-end; }
</style>
