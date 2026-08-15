<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="项目名称" clearable style="width: 200px" @change="load" />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px" @change="load">
        <el-option label="未开始" value="wait" />
        <el-option label="进行中" value="doing" />
        <el-option label="已暂停" value="suspended" />
        <el-option label="已关闭" value="closed" />
      </el-select>
      <el-button v-perm="'project:add'" type="primary" icon="Plus" @click="openDialog()">新增项目</el-button>
    </div>

    <el-table :data="rows" v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="项目名称">
        <template #default="{ row }">
          <el-link type="primary" @click="$router.push(`/project/detail/${row.id}`)">{{ row.name }}</el-link>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }"><StatusTag :status="row.status" type="project" /></template>
      </el-table-column>
      <el-table-column label="周期" width="210">
        <template #default="{ row }">{{ row.beginDate }} ~ {{ row.endDate }}</template>
      </el-table-column>
      <el-table-column prop="createdTime" label="创建时间" width="165" />
      <el-table-column label="操作" width="300">
        <template #default="{ row }">
          <el-button v-perm="'project:add'" v-if="row.status === 'wait'" link type="success" @click="flow(row, 'start')">启动</el-button>
          <el-button v-perm="'project:add'" v-if="row.status === 'doing'" link type="warning" @click="flow(row, 'suspend')">暂停</el-button>
          <el-button v-perm="'project:add'" v-if="row.status === 'suspended'" link type="success" @click="flow(row, 'resume')">恢复</el-button>
          <el-button v-perm="'project:add'" v-if="row.status !== 'closed'" link type="info" @click="flow(row, 'close')">关闭</el-button>
          <el-button v-perm="'project:add'" link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-popconfirm title="确定删除？" @confirm="handleDelete(row)">
            <template #reference><el-button v-perm="'project:add'" link type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
      :total="total" layout="total, prev, pager, next" style="margin-top: 12px" @current-change="load" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑项目' : '新增项目'" width="540px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="项目名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="关联产品">
          <el-select v-model="form.productId" clearable style="width: 100%">
            <el-option v-for="p in productOptions" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="form.ownerId" clearable style="width: 100%">
            <el-option v-for="u in userOptions" :key="u.id" :label="u.realName" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="起止日期">
          <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { projectApi, productApi, userApi } from '@/api'
import StatusTag from '@/components/StatusTag.vue'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: '' })
const dialogVisible = ref(false)
const form = reactive({})
const range = ref([])
const productOptions = ref([])
const userOptions = ref([])

async function load() {
  loading.value = true
  try {
    const res = await projectApi.page(query)
    rows.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row || { name: '', productId: null, ownerId: null, description: '' })
  range.value = row?.beginDate ? [row.beginDate, row.endDate] : []
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.name) return ElMessage.warning('请填写项目名称')
  form.beginDate = range.value?.[0]
  form.endDate = range.value?.[1]
  if (form.id) {
    await projectApi.update(form.id, form)
  } else {
    await projectApi.create(form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function flow(row, action) {
  await projectApi.flow(row.id, action)
  ElMessage.success('操作成功')
  load()
}

async function handleDelete(row) {
  await projectApi.remove(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(async () => {
  load()
  productOptions.value = (await productApi.options()).data
  userOptions.value = (await userApi.options()).data
})
</script>

<style scoped>
.toolbar { display: flex; gap: 12px; margin-bottom: 12px; }
</style>
