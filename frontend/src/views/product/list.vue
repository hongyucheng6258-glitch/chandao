<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="产品名称" clearable style="width: 200px" @change="load" />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px" @change="load">
        <el-option label="正常" value="normal" />
        <el-option label="已关闭" value="closed" />
      </el-select>
      <el-button v-perm="'product:add'" type="primary" icon="Plus" @click="openDialog()">新增产品</el-button>
    </div>

    <el-table :data="rows" v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="产品名称" />
      <el-table-column prop="code" label="代号" width="120" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'normal' ? 'success' : 'info'" size="small">
            {{ row.status === 'normal' ? '正常' : '已关闭' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button link type="primary" @click="openPlans(row)">计划</el-button>
          <el-button v-perm="'product:add'" link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-popconfirm title="确定删除该产品？" @confirm="handleDelete(row)">
            <template #reference>
              <el-button v-perm="'product:add'" link type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
      :total="total" layout="total, prev, pager, next" style="margin-top: 12px" @current-change="load" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑产品' : '新增产品'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="产品名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="产品代号"><el-input v-model="form.code" /></el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="form.ownerId" clearable style="width: 100%">
            <el-option v-for="u in userOptions" :key="u.id" :label="u.realName" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="planVisible" :title="`产品计划 - ${currentProduct?.name || ''}`" size="480px">
      <el-button v-perm="'product:add'" type="primary" size="small" @click="planDialogVisible = true">新增计划</el-button>
      <el-table :data="plans" size="small" style="margin-top: 12px">
        <el-table-column prop="title" label="计划" />
        <el-table-column label="周期" width="200">
          <template #default="{ row }">{{ row.beginDate }} ~ {{ row.endDate }}</template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <el-dialog v-model="planDialogVisible" title="新增计划" width="480px" append-to-body>
      <el-form :model="planForm" label-width="90px">
        <el-form-item label="计划名称" required><el-input v-model="planForm.title" /></el-form-item>
        <el-form-item label="起止日期">
          <el-date-picker v-model="planRange" type="daterange" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="目标"><el-input v-model="planForm.goal" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="planDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSavePlan">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { productApi, userApi } from '@/api'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: '' })
const dialogVisible = ref(false)
const form = reactive({})
const userOptions = ref([])
const planVisible = ref(false)
const planDialogVisible = ref(false)
const plans = ref([])
const currentProduct = ref(null)
const planForm = reactive({})
const planRange = ref([])

async function load() {
  loading.value = true
  try {
    const res = await productApi.page(query)
    rows.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row || { name: '', code: '', ownerId: null, description: '' })
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.name) return ElMessage.warning('请填写产品名称')
  if (form.id) {
    await productApi.update(form.id, form)
  } else {
    await productApi.create(form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function handleDelete(row) {
  await productApi.remove(row.id)
  ElMessage.success('删除成功')
  load()
}

async function openPlans(row) {
  currentProduct.value = row
  plans.value = (await productApi.plans(row.id)).data
  planVisible.value = true
}

async function handleSavePlan() {
  if (!planForm.title) return ElMessage.warning('请填写计划名称')
  planForm.beginDate = planRange.value?.[0]
  planForm.endDate = planRange.value?.[1]
  await productApi.createPlan(currentProduct.value.id, planForm)
  ElMessage.success('保存成功')
  planDialogVisible.value = false
  plans.value = (await productApi.plans(currentProduct.value.id)).data
}

onMounted(async () => {
  load()
  userOptions.value = (await userApi.options()).data
})
</script>

<style scoped>
.toolbar { display: flex; gap: 12px; margin-bottom: 12px; }
</style>
