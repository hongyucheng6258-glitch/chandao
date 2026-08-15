<template>
  <el-card>
    <div class="toolbar">
      <el-select v-model="query.productId" placeholder="产品" clearable style="width: 160px" @change="load">
        <el-option v-for="p in productOptions" :key="p.id" :label="p.name" :value="p.id" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px" @change="load">
        <el-option v-for="(v, k) in statusMap" :key="k" :label="v" :value="k" />
      </el-select>
      <el-select v-model="query.priority" placeholder="优先级" clearable style="width: 110px" @change="load">
        <el-option v-for="n in [1, 2, 3, 4]" :key="n" :label="'P' + n" :value="n" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="需求标题" clearable style="width: 180px" @change="load" />
      <el-button v-perm="'story:add'" type="primary" icon="Plus" @click="openDialog()">提需求</el-button>
      <el-button v-perm="'story:add'" type="danger" :disabled="!selectedIds.length" @click="batchDelete">批量删除 ({{ selectedIds.length }})</el-button>
      <el-button v-perm="'story:flow'" type="primary" :disabled="!selectedIds.length" @click="batchAssignVisible = true">批量指派</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" @selection-change="onSelectionChange">
      <el-table-column type="selection" width="45" />
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="需求标题" show-overflow-tooltip />
      <el-table-column label="优先级" width="80">
        <template #default="{ row }"><PriorityTag :level="row.priority" /></template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }"><StatusTag :status="row.status" type="story" /></template>
      </el-table-column>
      <el-table-column prop="estimate" label="预估(h)" width="90" />
      <el-table-column prop="assignedTo" label="指派给" width="100">
        <template #default="{ row }">{{ userName(row.assignedTo) }}</template>
      </el-table-column>
      <el-table-column prop="createdTime" label="创建时间" width="165" />
      <el-table-column label="操作" width="260">
        <template #default="{ row }">
          <el-button v-perm="'story:flow'" v-if="row.status === 'draft' || row.status === 'changed' || row.status === 'closed'"
            link type="success" @click="flow(row, 'activate')">激活</el-button>
          <el-button v-perm="'story:flow'" v-if="row.status !== 'closed'" link type="warning" @click="flow(row, 'close')">关闭</el-button>
          <el-button v-perm="'story:flow'" link type="primary" @click="openAssign(row)">指派</el-button>
          <el-button v-perm="'story:add'" link type="success" @click="openSplit(row)">拆分任务</el-button>
          <el-button link type="primary" @click="openActivity(row)">动态</el-button>
          <el-button v-perm="'story:add'" link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-popconfirm title="确定删除？" @confirm="handleDelete(row)">
            <template #reference><el-button v-perm="'story:add'" link type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
      :total="total" layout="total, prev, pager, next" style="margin-top: 12px" @current-change="load" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑需求' : '提需求'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="所属产品" required>
          <el-select v-model="form.productId" style="width: 100%">
            <el-option v-for="p in productOptions" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="需求标题" required><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="需求描述"><el-input v-model="form.description" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="优先级">
          <el-radio-group v-model="form.priority">
            <el-radio-button v-for="n in [1, 2, 3, 4]" :key="n" :value="n">P{{ n }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="预估工时"><el-input-number v-model="form.estimate" :min="0" :step="1" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignVisible" title="指派需求" width="400px">
      <el-select v-model="assignTo" style="width: 100%" placeholder="选择处理人">
        <el-option v-for="u in userOptions" :key="u.id" :label="u.realName" :value="u.id" />
      </el-select>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAssign">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchAssignVisible" title="批量指派需求" width="400px">
      <el-select v-model="batchAssignTo" style="width: 100%" placeholder="选择处理人">
        <el-option v-for="u in userOptions" :key="u.id" :label="u.realName" :value="u.id" />
      </el-select>
      <template #footer>
        <el-button @click="batchAssignVisible = false">取消</el-button>
        <el-button type="primary" @click="handleBatchAssign">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="splitVisible" :title="`拆分任务 #${splitBiz.id}`" width="540px">
      <el-form :model="splitForm" label-width="90px">
        <el-form-item label="所属项目" required>
          <el-select v-model="splitForm.projectId" style="width: 100%" placeholder="选择项目" @change="onProjectChange">
            <el-option v-for="p in projectOptions" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属迭代" required>
          <el-select v-model="splitForm.sprintId" style="width: 100%" placeholder="选择迭代" :disabled="!splitForm.projectId">
            <el-option v-for="s in sprintOptions" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
          <div v-if="splitForm.projectId && sprintOptions.length === 0" style="color:#e6a23c;font-size:12px;margin-top:4px;line-height:1.5">
            该项目下还没有迭代，请先到「项目管理 → 项目列表」创建迭代，再回来拆分任务。
          </div>
        </el-form-item>
        <el-form-item label="任务名称" required>
          <el-input v-model="splitForm.name" placeholder="默认带需求标题" />
        </el-form-item>
        <el-form-item label="处理人" required>
          <el-select v-model="splitForm.assignedTo" style="width: 100%" placeholder="选择处理人">
            <el-option v-for="u in userOptions" :key="u.id" :label="u.realName" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="预估工时">
          <el-input-number v-model="splitForm.estimate" :min="0" :step="1" />
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker v-model="splitForm.deadline" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="splitVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSplit">生成任务</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="activityVisible" :title="`需求 #${activityBiz.id}`" size="480px">
      <div class="detail-wrap">
        <div class="biz-head">
          <span class="biz-title">{{ activityBiz.title }}</span>
          <StatusTag :status="activityBiz.status" type="story" />
        </div>
        <AttachmentList :key="'att' + activityBiz.id" object-type="story" :object-id="activityBiz.id" />
        <el-divider />
        <div class="section-title">动态</div>
        <ActivityPanel :key="'act' + activityBiz.id" object-type="story" :object-id="activityBiz.id" />
      </div>
    </el-drawer>
  </el-card>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { storyApi, productApi, userApi, projectApi, sprintApi } from '@/api'
import StatusTag from '@/components/StatusTag.vue'
import PriorityTag from '@/components/PriorityTag.vue'
import ActivityPanel from '@/components/ActivityPanel.vue'
import AttachmentList from '@/components/AttachmentList.vue'

const statusMap = { draft: '草稿', active: '已激活', changed: '已变更', closed: '已关闭' }
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10, productId: null, status: '', priority: null, keyword: '' })
const dialogVisible = ref(false)
const form = reactive({})
const productOptions = ref([])
const projectOptions = ref([])
const userOptions = ref([])
const assignVisible = ref(false)
const assignTo = ref(null)
const currentRow = ref(null)
const activityVisible = ref(false)
const activityBiz = reactive({ id: null, title: '', status: '' })
const selectedIds = ref([])
const batchAssignVisible = ref(false)
const batchAssignTo = ref(null)
const splitVisible = ref(false)
const splitBiz = reactive({ id: null })
const splitForm = reactive({ projectId: null, sprintId: null, name: '', assignedTo: null, estimate: 0, deadline: null })
const allSprints = ref([])
const sprintOptions = computed(() =>
  splitForm.projectId == null ? [] : (allSprints.value || []).filter((s) => s.projectId === splitForm.projectId)
)

function onSelectionChange(rows) {
  selectedIds.value = rows.map((r) => r.id)
}

function openActivity(row) {
  Object.assign(activityBiz, { id: row.id, title: row.title, status: row.status })
  activityVisible.value = true
}

const userName = (id) => userOptions.value.find((u) => u.id === id)?.realName || '-'

async function load() {
  loading.value = true
  try {
    const res = await storyApi.page(query)
    rows.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row || { productId: query.productId, title: '', description: '', priority: 3, estimate: 0 })
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.title || !form.productId) return ElMessage.warning('请填写产品和标题')
  if (form.id) {
    await storyApi.update(form.id, form)
  } else {
    await storyApi.create(form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function flow(row, action) {
  await storyApi.flow(row.id, action)
  ElMessage.success('操作成功')
  load()
}

function openAssign(row) {
  currentRow.value = row
  assignTo.value = row.assignedTo
  assignVisible.value = true
}

async function handleAssign() {
  await storyApi.assign(currentRow.value.id, assignTo.value)
  ElMessage.success('指派成功')
  assignVisible.value = false
  load()
}

async function handleDelete(row) {
  await storyApi.remove(row.id)
  ElMessage.success('删除成功')
  load()
}

async function batchDelete() {
  if (!selectedIds.value.length) return
  try {
    await ElMessageBox.confirm(`确定批量删除选中的 ${selectedIds.value.length} 条需求？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  await storyApi.batchDelete(selectedIds.value)
  ElMessage.success('批量删除成功')
  selectedIds.value = []
  load()
}

async function handleBatchAssign() {
  if (!batchAssignTo.value) return ElMessage.warning('请选择处理人')
  await storyApi.batchAssign(selectedIds.value, batchAssignTo.value)
  ElMessage.success('批量指派成功')
  batchAssignVisible.value = false
  selectedIds.value = []
  load()
}

function openSplit(row) {
  splitBiz.id = row.id
  Object.assign(splitForm, {
    projectId: null, sprintId: null,
    name: '实现：' + row.title,
    assignedTo: row.assignedTo,
    estimate: row.estimate || 0,
    deadline: null
  })
  // 打开弹窗时一次性拉取全部迭代（含 projectId），后续按项目在前端过滤，
  // 避免“选完项目才异步去查、查不到就静默空着”导致迭代选不了的问题
  sprintApi.options().then((res) => { allSprints.value = res.data || [] }).catch(() => { allSprints.value = [] })
  splitVisible.value = true
}

function onProjectChange() {
  // 切换项目时清空已选迭代，sprintOptions 由 computed 自动按项目过滤
  splitForm.sprintId = null
}

async function handleSplit() {
  if (!splitForm.projectId) return ElMessage.warning('请选择项目')
  if (!splitForm.sprintId) return ElMessage.warning('请选择迭代')
  if (!splitForm.name) return ElMessage.warning('请填写任务名称')
  if (!splitForm.assignedTo) return ElMessage.warning('请选择处理人')
  await storyApi.createTask(splitBiz.id, { ...splitForm })
  ElMessage.success('已生成任务，可在迭代看板/我的任务查看')
  splitVisible.value = false
  load()
}

onMounted(async () => {
  load()
  productOptions.value = (await productApi.options()).data
  projectOptions.value = (await projectApi.options()).data
  userOptions.value = (await userApi.options()).data
})
</script>

<style scoped>
.toolbar { display: flex; gap: 12px; margin-bottom: 12px; flex-wrap: wrap; }
.detail-wrap { padding: 2px 4px; }
.section-title { font-size: 13px; font-weight: 600; color: #303133; margin: 4px 0 8px; }
.biz-head { display: flex; align-items: center; gap: 8px; padding-bottom: 10px; margin-bottom: 4px; border-bottom: 1px solid #ebeef5; }
.biz-title { font-size: 14px; font-weight: 600; color: #303133; }
</style>
