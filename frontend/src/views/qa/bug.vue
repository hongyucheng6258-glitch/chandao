<template>
  <el-card>
    <div class="toolbar">
      <el-select v-model="query.productId" placeholder="产品" clearable style="width: 150px" @change="load">
        <el-option v-for="p in productOptions" :key="p.id" :label="p.name" :value="p.id" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="load">
        <el-option label="激活" value="active" />
        <el-option label="已解决" value="resolved" />
        <el-option label="已关闭" value="closed" />
      </el-select>
      <el-select v-model="query.severity" placeholder="严重程度" clearable style="width: 120px" @change="load">
        <el-option label="致命" :value="1" /><el-option label="严重" :value="2" />
        <el-option label="一般" :value="3" /><el-option label="轻微" :value="4" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="Bug标题" clearable style="width: 170px" @change="load" />
      <el-button v-perm="'bug:add'" type="primary" icon="Plus" @click="openDialog()">提Bug</el-button>
      <el-button type="success" icon="Download" :loading="exporting" @click="handleExport">导出Excel</el-button>
      <el-button v-perm="'bug:add'" type="danger" :disabled="!selectedIds.length" @click="batchDelete">批量删除 ({{ selectedIds.length }})</el-button>
      <el-button v-perm="'bug:handle'" type="primary" :disabled="!selectedIds.length" @click="batchAssignVisible = true">批量指派</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" @selection-change="onSelectionChange">
      <el-table-column type="selection" width="45" />
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="Bug标题" show-overflow-tooltip />
      <el-table-column label="严重程度" width="85">
        <template #default="{ row }"><PriorityTag :level="row.severity" kind="severity" /></template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }"><StatusTag :status="row.status" type="bug" /></template>
      </el-table-column>
      <el-table-column label="指派给" width="90">
        <template #default="{ row }">{{ userName(row.assignedTo) }}</template>
      </el-table-column>
      <el-table-column prop="resolution" label="解决方案" width="95">
        <template #default="{ row }">{{ resolutionMap[row.resolution] || '-' }}</template>
      </el-table-column>
      <el-table-column prop="createdTime" label="创建时间" width="165" />
      <el-table-column label="操作" width="270">
        <template #default="{ row }">
          <el-button v-perm="'bug:handle'" link type="primary" @click="openAssign(row)">指派</el-button>
          <el-button v-perm="'bug:handle'" v-if="row.status === 'active'" link type="success" @click="openResolve(row)">解决</el-button>
          <el-button v-perm="'bug:handle'" v-if="row.status === 'resolved'" link type="success" @click="flow(row, 'close')">关闭</el-button>
          <el-button v-perm="'bug:handle'" v-if="row.status === 'resolved' || row.status === 'closed'" link type="warning" @click="flow(row, 'reopen')">打回</el-button>
          <el-button link type="primary" @click="openActivity(row)">动态</el-button>
          <el-button v-perm="'bug:add'" link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-popconfirm title="确定删除？" @confirm="handleDelete(row)">
            <template #reference><el-button v-perm="'bug:add'" link type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
      :total="total" layout="total, prev, pager, next" style="margin-top: 12px" @current-change="load" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑Bug' : '提Bug'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="所属产品" required>
          <el-select v-model="form.productId" style="width: 100%">
            <el-option v-for="p in productOptions" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="Bug标题" required><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="重现步骤"><el-input v-model="form.steps" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="严重程度">
          <el-radio-group v-model="form.severity">
            <el-radio-button :value="1">致命</el-radio-button><el-radio-button :value="2">严重</el-radio-button>
            <el-radio-button :value="3">一般</el-radio-button><el-radio-button :value="4">轻微</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="关联需求">
          <el-select v-model="form.storyId" clearable style="width: 100%">
            <el-option v-for="s in storyOptions" :key="s.id" :label="`#${s.id} ${s.title}`" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="指派给">
          <el-select v-model="form.assignedTo" clearable style="width: 100%">
            <el-option v-for="u in userOptions" :key="u.id" :label="u.realName" :value="u.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignVisible" title="指派Bug" width="400px">
      <el-select v-model="assignTo" style="width: 100%">
        <el-option v-for="u in userOptions" :key="u.id" :label="u.realName" :value="u.id" />
      </el-select>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAssign">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchAssignVisible" title="批量指派Bug" width="400px">
      <el-select v-model="batchAssignTo" style="width: 100%" placeholder="选择处理人">
        <el-option v-for="u in userOptions" :key="u.id" :label="u.realName" :value="u.id" />
      </el-select>
      <template #footer>
        <el-button @click="batchAssignVisible = false">取消</el-button>
        <el-button type="primary" @click="handleBatchAssign">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resolveVisible" title="解决Bug" width="420px">
      <el-select v-model="resolution" style="width: 100%" placeholder="选择解决方案">
        <el-option label="已修复" value="fixed" />
        <el-option label="不是Bug" value="notbug" />
        <el-option label="重复Bug" value="duplicate" />
        <el-option label="设计如此" value="bydesign" />
        <el-option label="不予修复" value="wontfix" />
      </el-select>
      <template #footer>
        <el-button @click="resolveVisible = false">取消</el-button>
        <el-button type="primary" @click="handleResolve">确定</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="activityVisible" :title="`Bug #${activityBiz.id}`" size="480px">
      <div class="detail-wrap">
        <div class="biz-head">
          <span class="biz-title">{{ activityBiz.title }}</span>
          <StatusTag :status="activityBiz.status" type="bug" />
        </div>
        <AttachmentList :key="'att' + activityBiz.id" object-type="bug" :object-id="activityBiz.id" />
        <el-divider />
        <div class="section-title">动态</div>
        <ActivityPanel :key="'act' + activityBiz.id" object-type="bug" :object-id="activityBiz.id" />
      </div>
    </el-drawer>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { bugApi, productApi, storyApi, userApi } from '@/api'
import { exportToExcel } from '@/utils/excel'
import StatusTag from '@/components/StatusTag.vue'
import PriorityTag from '@/components/PriorityTag.vue'
import ActivityPanel from '@/components/ActivityPanel.vue'
import AttachmentList from '@/components/AttachmentList.vue'

const resolutionMap = { fixed: '已修复', notbug: '不是Bug', duplicate: '重复', bydesign: '设计如此', wontfix: '不予修复' }
const severityMap = { 1: '致命', 2: '严重', 3: '一般', 4: '轻微' }
const bugStatusMap = { active: '激活', resolved: '已解决', closed: '已关闭' }
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const exporting = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10, productId: null, status: '', severity: null, keyword: '' })
const dialogVisible = ref(false)
const form = reactive({})
const productOptions = ref([])
const storyOptions = ref([])
const userOptions = ref([])
const assignVisible = ref(false)
const assignTo = ref(null)
const resolveVisible = ref(false)
const resolution = ref('fixed')
const currentRow = ref(null)
const activityVisible = ref(false)
const activityBiz = reactive({ id: null, title: '', status: '' })
const selectedIds = ref([])
const batchAssignVisible = ref(false)
const batchAssignTo = ref(null)

function onSelectionChange(rows) {
  selectedIds.value = rows.map((r) => r.id)
}

function openActivity(row) {
  Object.assign(activityBiz, { id: row.id, title: row.title, status: row.status })
  activityVisible.value = true
}

const userName = (id) => userOptions.value.find((u) => u.id === id)?.realName || '-'

const exportColumns = [
  { title: 'ID', key: 'id' },
  { title: 'Bug标题', key: 'title' },
  { title: '严重程度', key: 'severity', formatter: (r) => severityMap[r.severity] || r.severity },
  { title: '状态', key: 'status', formatter: (r) => bugStatusMap[r.status] || r.status },
  { title: '指派给', key: 'assignedTo', formatter: (r) => userName(r.assignedTo) },
  { title: '解决方案', key: 'resolution', formatter: (r) => resolutionMap[r.resolution] || '-' },
  { title: '创建时间', key: 'createdTime' }
]

async function handleExport() {
  exporting.value = true
  try {
    const res = await bugApi.page({ ...query, pageNum: 1, pageSize: 10000 })
    const list = res.data.records || []
    if (!list.length) return ElMessage.warning('暂无数据可导出')
    exportToExcel(exportColumns, list, `缺陷列表_${new Date().toISOString().slice(0, 10)}`)
    ElMessage.success('导出成功')
  } finally {
    exporting.value = false
  }
}

async function load() {
  loading.value = true
  try {
    const res = await bugApi.page(query)
    rows.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function openDialog(row) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row || { productId: query.productId, title: '', steps: '', severity: 3, priority: 3, storyId: null, assignedTo: null })
  if (form.productId) {
    storyOptions.value = (await storyApi.options(form.productId)).data
  }
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.title || !form.productId) return ElMessage.warning('请填写产品和标题')
  if (form.id) {
    await bugApi.update(form.id, form)
  } else {
    await bugApi.create(form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

function openAssign(row) {
  currentRow.value = row
  assignTo.value = row.assignedTo
  assignVisible.value = true
}

async function handleAssign() {
  await bugApi.assign(currentRow.value.id, assignTo.value)
  ElMessage.success('指派成功')
  assignVisible.value = false
  load()
}

function openResolve(row) {
  currentRow.value = row
  resolution.value = 'fixed'
  resolveVisible.value = true
}

async function handleResolve() {
  await bugApi.flow(currentRow.value.id, 'resolve', resolution.value)
  ElMessage.success('已解决')
  resolveVisible.value = false
  load()
}

async function flow(row, action) {
  await bugApi.flow(row.id, action)
  ElMessage.success('操作成功')
  load()
}

async function handleDelete(row) {
  await bugApi.remove(row.id)
  ElMessage.success('删除成功')
  load()
}

async function batchDelete() {
  if (!selectedIds.value.length) return
  try {
    await ElMessageBox.confirm(`确定批量删除选中的 ${selectedIds.value.length} 个Bug？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  await bugApi.batchDelete(selectedIds.value)
  ElMessage.success('批量删除成功')
  selectedIds.value = []
  load()
}

async function handleBatchAssign() {
  if (!batchAssignTo.value) return ElMessage.warning('请选择处理人')
  await bugApi.batchAssign(selectedIds.value, batchAssignTo.value)
  ElMessage.success('批量指派成功')
  batchAssignVisible.value = false
  selectedIds.value = []
  load()
}

onMounted(async () => {
  load()
  productOptions.value = (await productApi.options()).data
  userOptions.value = (await userApi.options()).data
})
</script>

<style scoped>
.toolbar { display: flex; gap: 12px; margin-bottom: 12px; flex-wrap: wrap; }
.detail-wrap { padding: 2px 4px; }
.section-title { font-size: 13px; font-weight: 650; color: var(--text-primary); margin: 4px 0 8px; }
.biz-head { display: flex; align-items: center; gap: 8px; padding-bottom: 10px; margin-bottom: 4px; border-bottom: 1px solid var(--border-light); }
.biz-title { font-size: 14px; font-weight: 650; color: var(--text-primary); }
</style>
