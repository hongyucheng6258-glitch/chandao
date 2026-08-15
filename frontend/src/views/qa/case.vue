<template>
  <el-card>
    <div class="toolbar">
      <el-select v-model="query.productId" placeholder="产品" clearable style="width: 180px" @change="load">
        <el-option v-for="p in productOptions" :key="p.id" :label="p.name" :value="p.id" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="用例标题" clearable style="width: 200px" @change="load" />
      <el-button type="primary" icon="Plus" @click="openDialog()">新建用例</el-button>
      <el-button type="success" icon="Download" :loading="exporting" @click="handleExport">导出Excel</el-button>
    </div>

    <el-table :data="rows" v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="用例标题" show-overflow-tooltip />
      <el-table-column prop="type" label="类型" width="90">
        <template #default="{ row }">{{ typeMap[row.type] || row.type }}</template>
      </el-table-column>
      <el-table-column prop="precondition" label="前置条件" show-overflow-tooltip />
      <el-table-column prop="createdTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-popconfirm title="确定删除？" @confirm="handleDelete(row)">
            <template #reference><el-button link type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
      :total="total" layout="total, prev, pager, next" style="margin-top: 12px" @current-change="load" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用例' : '新建用例'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="所属产品" required>
          <el-select v-model="form.productId" style="width: 100%">
            <el-option v-for="p in productOptions" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="用例标题" required><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="功能" value="feature" />
            <el-option label="界面" value="ui" />
            <el-option label="性能" value="performance" />
          </el-select>
        </el-form-item>
        <el-form-item label="前置条件"><el-input v-model="form.precondition" /></el-form-item>
        <el-form-item label="步骤预期"><el-input v-model="form.steps" type="textarea" :rows="4"
          placeholder='如: [{"step":"输入账号密码","expect":"登录成功"}]' /></el-form-item>
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
import { testcaseApi, productApi } from '@/api'
import { exportToExcel } from '@/utils/excel'

const typeMap = { feature: '功能', ui: '界面', performance: '性能' }
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const exporting = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10, productId: null, keyword: '' })
const dialogVisible = ref(false)
const form = reactive({})
const productOptions = ref([])

const exportColumns = [
  { title: 'ID', key: 'id' },
  { title: '用例标题', key: 'title' },
  { title: '类型', key: 'type', formatter: (r) => typeMap[r.type] || r.type },
  { title: '前置条件', key: 'precondition' },
  { title: '步骤与预期', key: 'steps', formatter: formatSteps },
  { title: '创建时间', key: 'createdTime' }
]

function formatSteps(row) {
  try {
    const arr = JSON.parse(row.steps || '[]')
    if (!Array.isArray(arr) || !arr.length) return ''
    return arr.map((s, i) => `步骤${i + 1}: ${s.step || ''} -> 预期: ${s.expect || ''}`).join('；')
  } catch {
    return row.steps || ''
  }
}

async function handleExport() {
  exporting.value = true
  try {
    const res = await testcaseApi.page({ ...query, pageNum: 1, pageSize: 10000 })
    const list = res.data.records || []
    if (!list.length) return ElMessage.warning('暂无数据可导出')
    exportToExcel(exportColumns, list, `测试用例_${new Date().toISOString().slice(0, 10)}`)
    ElMessage.success('导出成功')
  } finally {
    exporting.value = false
  }
}

async function load() {
  loading.value = true
  try {
    const res = await testcaseApi.page(query)
    rows.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row || { productId: query.productId, title: '', type: 'feature', precondition: '', steps: '' })
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.title || !form.productId) return ElMessage.warning('请填写产品和标题')
  if (form.id) {
    await testcaseApi.update(form.id, form)
  } else {
    await testcaseApi.create(form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function handleDelete(row) {
  await testcaseApi.remove(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(async () => {
  load()
  productOptions.value = (await productApi.options()).data
})
</script>

<style scoped>
.toolbar { display: flex; gap: 12px; margin-bottom: 12px; }
</style>
