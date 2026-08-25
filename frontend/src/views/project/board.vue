<template>
  <div class="board-page">
    <div class="page-header">
      <h2 class="page-title">迭代看板</h2>
      <div class="page-actions">
        <el-button type="primary" icon="Plus" :disabled="!sprintId" @click="openTaskDialog()">新建任务</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-select v-model="projectId" placeholder="选择项目" style="width: 180px" @change="onProjectChange">
        <el-option v-for="p in projectOptions" :key="p.id" :label="p.name" :value="p.id" />
      </el-select>
      <el-select v-model="sprintId" placeholder="选择迭代" style="width: 180px" @change="loadBoard">
        <el-option v-for="s in sprintOptions" :key="s.id" :label="s.name" :value="s.id" />
      </el-select>
    </div>

    <div class="board-container" v-if="sprintId">
      <div class="board-column" v-for="col in columns" :key="col.status" :class="{ 'drag-over': dragOverStatus === col.status }">
        <div class="board-column-header">
          <div class="board-column-title">
            <StatusTag :status="col.status" type="task" />
          </div>
          <span class="board-column-count">{{ tasksByStatus(col.status).length }}</span>
        </div>
        <div class="board-column-body"
             @dragover.prevent="onDragOver(col.status)"
             @dragleave="onDragLeave(col.status)"
             @drop="onDrop(col.status)">
          <div v-for="task in tasksByStatus(col.status)" :key="task.id" class="board-card" :class="task.status"
                   draggable="true" @dragstart="onDragStart(task)" @dragend="onDragEnd"
                   @click="openTaskDrawer(task)">
            <div class="board-card-title">{{ task.name }}</div>
            <div class="board-card-meta">
              <div class="board-card-tags">
                <PriorityTag :level="task.priority" />
              </div>
              <span class="board-card-assignee">{{ userName(task.assignedTo) }}</span>
            </div>
            <div class="board-card-hours">估 {{ task.estimate }}h / 耗 {{ task.consumed }}h / 剩 {{ task.left }}h</div>
            <div class="board-card-actions" @click.stop>
              <el-button v-if="task.status === 'wait' || task.status === 'pause'" link type="success" size="small" @click="flow(task, 'start')">开始</el-button>
              <el-button v-if="task.status === 'doing'" link type="primary" size="small" @click="flow(task, 'finish')">完成</el-button>
              <el-button v-if="task.status === 'doing'" link type="warning" size="small" @click="openHours(task)">工时</el-button>
              <el-button v-if="task.status === 'done'" link type="info" size="small" @click="flow(task, 'close')">关闭</el-button>
              <el-button v-perm="'task:assign'" link size="small" @click="openAssign(task)">指派</el-button>
            </div>
          </div>
          <el-empty v-if="!tasksByStatus(col.status).length" description="暂无任务" :image-size="40" />
        </div>
      </div>
    </div>
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
  </div>
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
.board-page {
  padding: 24px 28px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}
.page-title {
  font-family: Inter, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
}
.page-actions {
  display: flex;
  gap: 10px;
}
.filter-bar {
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  padding: 16px 20px;
  margin-bottom: 16px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-light);
  display: flex;
  gap: 12px;
  align-items: center;
}
.board-container {
  display: flex;
  gap: 14px;
  overflow-x: auto;
  padding-bottom: 16px;
}
.board-column {
  min-width: 280px;
  flex: 1;
  background: var(--surface-soft);
  border-radius: var(--radius-md);
  padding: 14px;
  transition: all 0.15s;
}
.board-column.drag-over {
  outline: 2px dashed var(--accent-success);
  outline-offset: -2px;
  background: var(--accent-primary-bg);
}
.board-column-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  padding: 0 4px;
}
.board-column-title {
  display: flex;
  align-items: center;
  gap: 8px;
}
.board-column-count {
  background: var(--bg-secondary);
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 700;
  color: var(--text-secondary);
}
.board-column-body {
  min-height: 200px;
}
.board-card {
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
  padding: 14px;
  margin-bottom: 10px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-light);
  cursor: grab;
  transition: all 0.15s;
  border-left: 3px solid transparent;
}
.board-card:active {
  cursor: grabbing;
}
.board-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}
.board-card.wait { border-left-color: var(--text-muted); }
.board-card.doing { border-left-color: var(--accent-primary); }
.board-card.done { border-left-color: var(--accent-success); }
.board-card.pause { border-left-color: var(--accent-warning); }
.board-card.cancel { border-left-color: var(--accent-danger); }
.board-card.closed { border-left-color: var(--text-muted); }
.board-card-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 10px;
  line-height: 1.4;
}
.board-card-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.board-card-tags {
  display: flex;
  gap: 6px;
}
.board-card-assignee {
  font-size: 12px;
  color: var(--text-muted);
}
.board-card-hours {
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--border-light);
}
.board-card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.detail-wrap { padding: 2px 4px; }
.section-title { font-size: 13px; font-weight: 600; color: var(--text-primary); margin: 4px 0 8px; }
.biz-head { display: flex; align-items: center; gap: 8px; padding-bottom: 10px; margin-bottom: 4px; border-bottom: 1px solid var(--border-light); }
.biz-title { font-size: 14px; font-weight: 600; color: var(--text-primary); }
</style>
