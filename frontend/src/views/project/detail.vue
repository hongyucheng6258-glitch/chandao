<template>
  <div>
    <el-card style="margin-bottom: 16px">
      <template #header>
        <div class="header">
          <span>{{ project?.name }}</span>
          <StatusTag v-if="project" :status="project.status" type="project" />
        </div>
      </template>
      <el-descriptions :column="3" size="small">
        <el-descriptions-item label="周期">{{ project?.beginDate }} ~ {{ project?.endDate }}</el-descriptions-item>
        <el-descriptions-item label="负责人">{{ userName(project?.ownerId) }}</el-descriptions-item>
        <el-descriptions-item label="描述">{{ project?.description || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-row :gutter="16">
      <el-col :span="14">
        <el-card>
          <template #header>
            <div class="header">
              <span>迭代列表 <span style="color:#909399;font-size:12px;font-weight:normal">（点击行选中迭代，下方管理需求）</span></span>
              <el-button v-perm="'project:add'" type="primary" size="small" icon="Plus" @click="openSprintDialog()">新增迭代</el-button>
            </div>
          </template>
          <el-table :data="sprints" size="small" highlight-current-row @current-change="selectSprint">
            <el-table-column label="迭代">
              <template #default="{ row }">{{ row.name || '未命名迭代' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }"><StatusTag :status="row.status" type="sprint" /></template>
            </el-table-column>
            <el-table-column label="周期" width="200">
              <template #default="{ row }">
                <span v-if="row.beginDate && row.endDate">{{ row.beginDate }} ~ {{ row.endDate }}</span>
                <span v-else style="color:#c0c4cc">未设置</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="220">
              <template #default="{ row }">
                <el-button v-perm="'project:add'" v-if="row.status === 'wait'" link type="success" @click.stop="sprintFlow(row, 'start')">开始</el-button>
                <el-button v-perm="'project:add'" v-if="row.status === 'doing'" link type="warning" @click.stop="sprintFlow(row, 'close')">关闭</el-button>
                <el-button v-perm="'project:add'" link type="primary" @click.stop="openSprintDialog(row)">编辑</el-button>
                <el-popconfirm title="确定删除该迭代？迭代下的需求关联和任务不会被删除。" @confirm="deleteSprint(row)">
                  <template #reference><el-button v-perm="'project:add'" link type="danger" @click.stop>删除</el-button></template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card v-if="currentSprint" style="margin-top: 16px">
          <template #header>
            <div class="header">
              <span>「{{ currentSprint.name }}」的需求</span>
              <el-button v-perm="'project:add'" size="small" type="primary" @click="openLinkStory">拉入需求</el-button>
            </div>
          </template>
          <el-table :data="sprintStories" size="small">
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="title" label="需求" show-overflow-tooltip />
            <el-table-column label="状态" width="90">
              <template #default="{ row }"><StatusTag :status="row.status" type="story" /></template>
            </el-table-column>
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button v-perm="'project:add'" link type="danger" @click="unlinkStory(row)">移出</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="10">
        <el-card>
          <template #header>
            <div class="header">
              <span>项目成员</span>
              <el-button v-perm="'project:add'" size="small" type="primary" @click="memberVisible = true">添加成员</el-button>
            </div>
          </template>
          <el-table :data="members" size="small">
            <el-table-column label="成员">
              <template #default="{ row }">{{ userName(row.userId) }}</template>
            </el-table-column>
            <el-table-column prop="role" label="角色" width="80" />
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button v-perm="'project:add'" link type="danger" @click="removeMember(row)">移除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="sprintVisible" :title="sprintForm.id ? '编辑迭代' : '新增迭代'" width="480px">
      <el-form :model="sprintForm" label-width="90px">
        <el-form-item label="迭代名称" required><el-input v-model="sprintForm.name" /></el-form-item>
        <el-form-item label="起止日期">
          <el-date-picker v-model="sprintRange" type="daterange" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="迭代目标"><el-input v-model="sprintForm.goal" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sprintVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSprint">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="linkVisible" title="拉入需求到迭代" width="520px">
      <el-form label-width="80px">
        <el-form-item label="目标迭代" required>
          <el-select v-model="linkSprintId" style="width: 100%" placeholder="选择要拉入的迭代">
            <el-option v-for="s in sprints" :key="s.id" :label="`${s.name}（${s.status === 'doing' ? '进行中' : s.status === 'wait' ? '未开始' : '已关闭'}）`" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择需求">
          <el-select v-model="linkStoryIds" multiple filterable style="width: 100%" placeholder="从需求池中选择（已被其他迭代拉入的会标注）">
            <el-option v-for="s in storyOptions" :key="s.id" :label="`#${s.id} ${s.title}`" :value="s.id">
              <span>{{ s.title }}</span>
              <span v-if="s.sprintName" style="float:right;color:#e6a23c;font-size:12px">
                [{{ s.sprintName }}]
              </span>
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="linkVisible = false">取消</el-button>
        <el-button type="primary" @click="linkStories">确定拉入</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="memberVisible" title="添加成员" width="420px">
      <el-form label-width="80px">
        <el-form-item label="成员">
          <el-select v-model="memberForm.userId" style="width: 100%">
            <el-option v-for="u in userOptions" :key="u.id" :label="u.realName" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="memberForm.role" style="width: 100%">
            <el-option label="项目经理" value="pm" />
            <el-option label="开发" value="dev" />
            <el-option label="测试" value="qa" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="memberVisible = false">取消</el-button>
        <el-button type="primary" @click="addMember">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { projectApi, sprintApi, storyApi, userApi } from '@/api'
import StatusTag from '@/components/StatusTag.vue'

const route = useRoute()
const projectId = Number(route.params.id)
const project = ref(null)
const sprints = ref([])
const members = ref([])
const sprintStories = ref([])
const currentSprint = ref(null)
const sprintVisible = ref(false)
const sprintForm = reactive({})
const sprintRange = ref([])
const linkVisible = ref(false)
const linkStoryIds = ref([])
const linkSprintId = ref(null)
const storyOptions = ref([])
const memberVisible = ref(false)
const memberForm = reactive({ userId: null, role: 'dev' })
const userOptions = ref([])

const userName = (id) => userOptions.value.find((u) => u.id === id)?.realName || '-'

async function loadAll() {
  project.value = (await projectApi.detail(projectId)).data
  sprints.value = (await sprintApi.list(projectId)).data
  members.value = (await projectApi.members(projectId)).data
  if (sprints.value.length && !currentSprint.value) {
    selectSprint(sprints.value[0])
  }
}

async function selectSprint(row) {
  if (!row) return
  currentSprint.value = row
  sprintStories.value = (await sprintApi.stories(row.id)).data
}

function openSprintDialog(row) {
  Object.keys(sprintForm).forEach((k) => delete sprintForm[k])
  Object.assign(sprintForm, row || { name: '', goal: '', projectId })
  sprintRange.value = row?.beginDate ? [row.beginDate, row.endDate] : []
  sprintVisible.value = true
}

async function saveSprint() {
  if (!sprintForm.name) return ElMessage.warning('请填写迭代名称')
  sprintForm.beginDate = sprintRange.value?.[0]
  sprintForm.endDate = sprintRange.value?.[1]
  sprintForm.projectId = projectId
  if (sprintForm.id) {
    await sprintApi.update(sprintForm.id, sprintForm)
  } else {
    await sprintApi.create(sprintForm)
  }
  ElMessage.success('保存成功')
  sprintVisible.value = false
  loadAll()
}

async function sprintFlow(row, action) {
  await sprintApi.flow(row.id, action)
  ElMessage.success('操作成功')
  loadAll()
}

async function openLinkStory() {
  storyOptions.value = (await storyApi.options(project.value?.productId)).data
  linkStoryIds.value = []
  linkSprintId.value = currentSprint.value?.id
  linkVisible.value = true
}

async function linkStories() {
  if (!linkSprintId.value) return ElMessage.warning('请选择目标迭代')
  if (!linkStoryIds.value.length) return ElMessage.warning('请选择需求')
  await sprintApi.linkStories(linkSprintId.value, linkStoryIds.value)
  ElMessage.success('已拉入迭代')
  linkVisible.value = false
  // 切换到目标迭代查看结果
  const target = sprints.value.find((s) => s.id === linkSprintId.value)
  if (target) selectSprint(target)
}

async function unlinkStory(row) {
  await sprintApi.unlinkStory(currentSprint.value.id, row.id)
  ElMessage.success('已移出')
  selectSprint(currentSprint.value)
}

async function deleteSprint(row) {
  await sprintApi.remove(row.id)
  ElMessage.success('迭代已删除')
  // 如果删除的是当前选中的迭代，清空选中状态
  if (currentSprint.value?.id === row.id) {
    currentSprint.value = null
    sprintStories.value = []
  }
  loadAll()
}

async function addMember() {
  if (!memberForm.userId) return ElMessage.warning('请选择成员')
  await projectApi.addMember(projectId, memberForm)
  ElMessage.success('已添加')
  memberVisible.value = false
  members.value = (await projectApi.members(projectId)).data
}

async function removeMember(row) {
  await projectApi.removeMember(row.id)
  members.value = (await projectApi.members(projectId)).data
}

onMounted(async () => {
  userOptions.value = (await userApi.options()).data
  loadAll()
})
</script>

<style scoped>
.header { display: flex; justify-content: space-between; align-items: center; }
/* 选中迭代行高亮增强 */
:deep(.el-table__body tr.current-row > td) {
  background-color: #ecf5ff !important;
  font-weight: 600;
}
:deep(.el-table__body tr.current-row:hover > td) {
  background-color: #d9ecff !important;
}
</style>
