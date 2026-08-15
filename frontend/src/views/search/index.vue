<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索需求 / Bug / 任务标题..." clearable style="width: 380px"
        @keyup.enter="doSearch" @clear="doSearch">
        <template #append><el-button icon="Search" @click="doSearch">搜索</el-button></template>
      </el-input>
    </div>

    <el-empty v-if="!loading && !searched" description="输入关键字开始搜索" />
    <div v-else v-loading="loading">
      <template v-if="searched">
        <div class="result-block" v-if="stories.length">
          <div class="block-head"><span class="dot story"></span>需求 ({{ stories.length }})</div>
          <el-table :data="stories" @row-click="(r) => go('story', r)" class="click-row">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="title" label="标题" show-overflow-tooltip />
            <el-table-column label="状态" width="100"><template #default="{ row }"><StatusTag :status="row.status" type="story" /></template></el-table-column>
            <el-table-column label="指派给" width="100"><template #default="{ row }">{{ userName(row.assignedTo) }}</template></el-table-column>
          </el-table>
        </div>
        <div class="result-block" v-if="bugs.length">
          <div class="block-head"><span class="dot bug"></span>Bug ({{ bugs.length }})</div>
          <el-table :data="bugs" @row-click="(r) => go('bug', r)" class="click-row">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="title" label="标题" show-overflow-tooltip />
            <el-table-column label="状态" width="100"><template #default="{ row }"><StatusTag :status="row.status" type="bug" /></template></el-table-column>
            <el-table-column label="指派给" width="100"><template #default="{ row }">{{ userName(row.assignedTo) }}</template></el-table-column>
          </el-table>
        </div>
        <div class="result-block" v-if="tasks.length">
          <div class="block-head"><span class="dot task"></span>任务 ({{ tasks.length }})</div>
          <el-table :data="tasks" @row-click="(r) => go('task', r)" class="click-row">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="name" label="标题" show-overflow-tooltip />
            <el-table-column label="状态" width="100"><template #default="{ row }"><StatusTag :status="row.status" type="task" /></template></el-table-column>
            <el-table-column label="指派给" width="100"><template #default="{ row }">{{ userName(row.assignedTo) }}</template></el-table-column>
          </el-table>
        </div>
        <el-empty v-if="!stories.length && !bugs.length && !tasks.length" description="未找到匹配结果" />
      </template>
    </div>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { searchApi, userApi } from '@/api'
import StatusTag from '@/components/StatusTag.vue'

const route = useRoute()
const router = useRouter()
const keyword = ref('')
const searched = ref(false)
const loading = ref(false)
const stories = ref([])
const bugs = ref([])
const tasks = ref([])
const userOptions = ref([])

const userName = (id) => userOptions.value.find((u) => u.id === id)?.realName || '-'

async function doSearch() {
  const kw = keyword.value.trim()
  if (!kw) {
    searched.value = false
    return
  }
  loading.value = true
  try {
    const res = await searchApi.search(kw)
    const data = res.data || {}
    stories.value = data.stories || []
    bugs.value = data.bugs || []
    tasks.value = data.tasks || []
    searched.value = true
  } finally {
    loading.value = false
  }
}

function go(type) {
  if (type === 'story') router.push('/product/story')
  else if (type === 'bug') router.push('/qa/bug')
  else router.push('/project/board')
}

onMounted(async () => {
  userOptions.value = (await userApi.options()).data
  if (route.query.keyword) {
    keyword.value = route.query.keyword
    doSearch()
  }
})
</script>

<style scoped>
.toolbar { display: flex; gap: 12px; margin-bottom: 16px; }
.result-block { margin-bottom: 18px; }
.block-head { font-size: 14px; font-weight: 600; color: #303133; margin-bottom: 8px; display: flex; align-items: center; gap: 8px; }
.dot { width: 9px; height: 9px; border-radius: 50%; display: inline-block; }
.dot.story { background: #409eff; }
.dot.bug { background: #f56c6c; }
.dot.task { background: #67c23a; }
.click-row :deep(.el-table__row) { cursor: pointer; }
</style>
