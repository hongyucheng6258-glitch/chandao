<template>
  <el-card>
    <div class="toolbar">
      <el-select v-model="projectId" placeholder="选择项目" style="width: 180px" @change="onProjectChange">
        <el-option v-for="p in projectOptions" :key="p.id" :label="p.name" :value="p.id" />
      </el-select>
      <el-select v-model="sprintId" placeholder="选择迭代" style="width: 180px" @change="loadBoard">
        <el-option v-for="s in sprintOptions" :key="s.id" :label="s.name" :value="s.id" />
      </el-select>
      <el-button type="primary" icon="Plus" :disabled="!sprintId" @click="openTaskDialog()">新建任务</el-button>
    </div>

    <el-row :gutter="12" v-if="sprintId">
      <el-col :span="6" v-for="col in columns" :key="col.status">
        <div class="board-col" :class="{ 'drag-over': dragOverStatus === col.status }">
          <div class="board-col-header">
            <StatusTag :status="col.status" type="task" />
            <span class="count">{{ tasksByStatus(col.status).length }}</span>
          </div>
          <div class="board-col-body"
               @dragover.prevent="onDragOver(col.status)"
               @dragleave="onDragLeave(col.status)"
               @drop="onDrop(col.status)">
            <el-card v-for="task in tasksByStatus(col.status)" :key="task.id" shadow="hover" class="task-card"
                     draggable="true" @dragstart="onDragStart(task)" @dragend="onDragEnd"
                     @click="openTaskDrawer(task)">
              <div class="task-name">{{ task.name }}</div>
              <div class="task-meta">
                <PriorityTag :level="task.priority" />
                <span>{{ userName(task.assignedTo) }}</span>
              </div>
              <div class="task-hours">估 {{ task.estimate }}h / 耗 {{ task.consumed }}h / 剩 {{ task.left }}h</div>
              <div class="task-actions" @click.stop>
                <el-button v-if="task.status === 'wait' || task.status === 'pause'" link type="success" size="small" @click="flow(task, 'start')">开始</el-button>
                <el-button v-if="task.status === 'doing'" link type="primary" size="small" @click="flow(task, 'finish')">完成</el-button>
                <el-button v-if="task.status === 'doing'" link type="warning" size="small" @click="openHours(task)">工时</el-button>
                <el-button v-if="task.status === 'done'" link type="info" size="small" @click="flow(task, 'close')">关闭</el-button>
                <el-button v-perm="'task:assign'" link size="small" @click="openAssign(task)">指派</el-button>
              </div>
            </el-card>
            <el-empty v-if="!tasksByStatus(col.status).length" description="无任务" :image-size="40" />
          </div>
        </div>
      </el-col>
    </el-row>
    <el-empty v-else description="请先选择项目和迭代" />

    <el-dialog v-model="taskVisible" title="新建任务" width="520px">
      <el-form :model="taskForm" label-width="90px">
        <el-form-item label="任务名称" required><el-input v-model="taskForm.name" /></el-form-item>
        <el-form-item label="关联需求">
          <el-select v-model="taskForm.storyId" clearable style="width: 100%">
            <el-option v-for="s in sprintStories" :key="s.id" :label="`#${s.id} ${s.title}`" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="taskForm.type" style="width: 100%">
            <el-option label="开发" value="dev" />
            <el-option label="测试" value="test" />
            <el-option label="设计" value="design" />
            <el-option label="研究" value="study" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-radio-group v-model="taskForm.priority">
            <el-radio-button v-for="n in [1, 2, 3, 4]" :key="n" :value="n">P{{ n }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="预估工时"><el-input-number v-model="taskForm.estimate" :min="0" /></el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker v-model="taskForm.deadline" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="taskVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTask">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="hoursVisible" title="登记工时" width="400px">
      <el-form label-width="90px">
        <el-form-item label="本次消耗"><el-input-number v-model="hoursForm.consumed" :min="0.5" :step="0.5" /></el-form-item>
        <el-form-item label="剩余工时"><el-input-number v-model="hoursForm.left" :min="0" :step="0.5" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="hoursVisible = false">取消</el-button>
        <el-button type="primary" @click="saveHours">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignVisible" title="指派任务" width="400px">
      <el-select v-model="assignTo" style="width: 100%">
        <el-option v-for="u in userOptions" :key="u.id" :label="u.realName" :value="u.id" />
      </el-select>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAssign">确定</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="taskDrawerVisible" :title="`任务 #${currentTask?.id ?? ''}`" size="480px">
      <div class="detail-wrap" v-if="currentTask">
        <div class="biz-head">
          <span class="biz-title">{{ currentTask.name }}</span>
          <StatusTag :status="currentTask.status" type="task" />
        </div>
        <AttachmentList :key="'att' + currentTask.id" object-type="task" :object-id="currentTask.id" />
        <el-divider />
        <div class="section-title">动态</div>
        <ActivityPanel :key="'act' + currentTask.id" object-type="task" :object-id="currentTask.id" />
      </div>
    </el-drawer>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { projectApi, sprintApi, taskApi, userApi } from '@/api'
import StatusTag from '@/components/StatusTag.vue'
import PriorityTag from '@/components/PriorityTag.vue'
import ActivityPanel from '@/components/ActivityPanel.vue'
import AttachmentList from '@/components/AttachmentList.vue'

const route = useRoute()

const columns = [
  { status: 'wait' }, { status: 'doing' }, { status: 'done' }, { status: 'closed' }
]
const projectId = ref(null)
const sprintId = ref(null)
const projectOptions = ref([])
const sprintOptions = ref([])
const sprintStories = ref([])
const tasks = ref([])
const userOptions = ref([])
const taskVisible = ref(false)
const taskForm = reactive({})
const hoursVisible = ref(false)
const hoursForm = reactive({ consumed: 1, left: 0 })
const assignVisible = ref(false)
const assignTo = ref(null)
const currentTask = ref(null)
const taskDrawerVisible = ref(false)
const draggingId = ref(null)
const dragOverStatus = ref(null)

function onDragStart(task) {
  draggingId.value = task.id
}
function onDragEnd() {
  draggingId.value = null
  dragOverStatus.value = null
}
function onDragOver(status) {
  dragOverStatus.value = status
}
function onDragLeave(status) {
  if (dragOverStatus.value === status) dragOverStatus.value = null
}
async function onDrop(status) {
  const id = draggingId.value
  dragOverStatus.value = null
  if (!id) return
  const task = tasks.value.find((t) => t.id === id)
  if (!task || task.status === status) return
  try {
    await taskApi.move(id, status)
    task.status = status
    ElMessage.success('已更新状态')
  } catch (e) {
    /* 状态机校验失败等: 保持原位, 拦截器已提示 */
  }
}

const userName = (id) => userOptions.value.find((u) => u.id === id)?.realName || '未指派'
const tasksByStatus = (status) => tasks.value.filter((t) => t.status === status)

async function onProjectChange() {
  sprintId.value = null
  sprintOptions.value = (await sprintApi.list(projectId.value)).data
}

async function loadBoard() {
  if (!sprintId.value) return
  tasks.value = (await taskApi.board(sprintId.value)).data
  sprintStories.value = (await sprintApi.stories(sprintId.value)).data
}

function openTaskDialog() {
  Object.keys(taskForm).forEach((k) => delete taskForm[k])
  Object.assign(taskForm, { name: '', storyId: null, type: 'dev', priority: 3, estimate: 8, deadline: null, sprintId: sprintId.value })
  taskVisible.value = true
}

async function saveTask() {
  if (!taskForm.name) return ElMessage.warning('请填写任务名称')
  await taskApi.create(taskForm)
  ElMessage.success('创建成功')
  taskVisible.value = false
  loadBoard()
}

async function flow(task, action) {
  await taskApi.flow(task.id, action)
  ElMessage.success('操作成功')
  loadBoard()
}

function openHours(task) {
  currentTask.value = task
  hoursForm.consumed = 1
  hoursForm.left = task.left
  hoursVisible.value = true
}

async function saveHours() {
  await taskApi.logHours(currentTask.value.id, hoursForm)
  ElMessage.success('已登记')
  hoursVisible.value = false
  loadBoard()
}

function openTaskDrawer(task) {
  currentTask.value = task
  taskDrawerVisible.value = true
}

function openAssign(task) {
  currentTask.value = task
  assignTo.value = task.assignedTo
  assignVisible.value = true
}

async function saveAssign() {
  await taskApi.assign(currentTask.value.id, assignTo.value)
  ElMessage.success('已指派')
  assignVisible.value = false
  loadBoard()
}

onMounted(async () => {
  projectOptions.value = (await projectApi.options()).data
  userOptions.value = (await userApi.options()).data
  const qp = route.query
  if (qp.projectId) {
    projectId.value = Number(qp.projectId)
    sprintOptions.value = (await sprintApi.list(projectId.value)).data
    if (qp.sprintId) {
      sprintId.value = Number(qp.sprintId)
      loadBoard()
    }
  }
})
</script>

<style scoped>
.toolbar { display: flex; gap: 12px; margin-bottom: 12px; }
.detail-wrap { padding: 2px 4px; }
.section-title { font-size: 13px; font-weight: 600; color: #303133; margin: 4px 0 8px; }
.biz-head { display: flex; align-items: center; gap: 8px; padding-bottom: 10px; margin-bottom: 4px; border-bottom: 1px solid #ebeef5; }
.biz-title { font-size: 14px; font-weight: 600; color: #303133; }
.board-col { background: #f5f7fa; border-radius: 8px; min-height: 400px; transition: outline .15s; }
.board-col.drag-over { outline: 2px dashed #409eff; outline-offset: -2px; background: #ecf5ff; }
.task-card { margin-bottom: 8px; cursor: grab; }
.task-card:active { cursor: grabbing; }
.board-col-header { display: flex; justify-content: space-between; align-items: center; padding: 10px 12px; }
.count { color: #909399; font-size: 13px; }
.board-col-body { padding: 0 8px 8px; }
.task-card { margin-bottom: 8px; }
.task-name { font-size: 13px; font-weight: 500; margin-bottom: 6px; }
.task-meta { display: flex; justify-content: space-between; font-size: 12px; color: #909399; }
.task-hours { font-size: 12px; color: #c0c4cc; margin-top: 4px; }
.task-actions { margin-top: 4px; }
</style>
