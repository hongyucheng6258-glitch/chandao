<template>
  <div class="page-container">
    <div class="page-card">
      <div class="table-toolbar">
        <div class="filters">
          <el-select v-model="query.productId" placeholder="产品" clearable style="width: 180px" @change="loadData">
            <el-option v-for="p in products" :key="p.id" :label="p.productName" :value="p.id" />
          </el-select>
          <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px" @change="loadData">
            <el-option label="草稿" value="draft" />
            <el-option label="评审中" value="reviewing" />
            <el-option label="已通过" value="approved" />
            <el-option label="开发中" value="developing" />
            <el-option label="测试中" value="testing" />
            <el-option label="已发布" value="released" />
            <el-option label="已关闭" value="closed" />
          </el-select>
          <el-select v-model="query.priority" placeholder="优先级" clearable style="width: 120px" @change="loadData">
            <el-option label="紧急" :value="1" />
            <el-option label="高" :value="2" />
            <el-option label="中" :value="3" />
            <el-option label="低" :value="4" />
          </el-select>
        </div>
        <el-button type="primary" v-perm="'story:add'" @click="openDialog()">
          <el-icon><Plus /></el-icon>新增需求
        </el-button>
      </div>
      <el-table :data="list" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="需求标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="productName" label="所属产品" width="140" />
        <el-table-column label="优先级" width="90">
          <template #default="{ row }"><PriorityTag :priority="row.priority" /></template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column prop="assigneeName" label="负责人" width="110" />
        <el-table-column prop="storyPoint" label="故事点" width="90" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" v-perm="'story:edit'" @click="openDialog(row)">编辑</el-button>
            <el-button link type="danger" v-perm="'story:delete'" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @change="loadData"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑需求' : '新增需求'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="所属产品" prop="productId">
          <el-select v-model="form.productId" style="width: 100%">
            <el-option v-for="p in products" :key="p.id" :label="p.productName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="需求标题" prop="title">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="form.priority" style="width: 100%">
            <el-option label="紧急" :value="1" />
            <el-option label="高" :value="2" />
            <el-option label="中" :value="3" />
            <el-option label="低" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="草稿" value="draft" />
            <el-option label="评审中" value="reviewing" />
            <el-option label="已通过" value="approved" />
            <el-option label="开发中" value="developing" />
            <el-option label="测试中" value="testing" />
            <el-option label="已发布" value="released" />
            <el-option label="已关闭" value="closed" />
          </el-select>
        </el-form-item>
        <el-form-item label="故事点">
          <el-input-number v-model="form.storyPoint" :min="0" :max="100" />
        </el-form-item>
        <el-form-item label="需求描述">
          <el-input v-model="form.description" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import storyApi from '@/api/story'
import productApi from '@/api/product'
import StatusTag from '@/components/StatusTag.vue'
import PriorityTag from '@/components/PriorityTag.vue'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const products = ref([])
const query = reactive({ pageNum: 1, pageSize: 10, productId: null, status: '', priority: null })

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const emptyForm = { id: null, productId: null, title: '', priority: 3, status: 'draft', storyPoint: 0, description: '' }
const form = reactive({ ...emptyForm })
const rules = {
  productId: [{ required: true, message: '请选择产品', trigger: 'change' }],
  title: [{ required: true, message: '请输入需求标题', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const data = await storyApi.page(query)
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

const openDialog = (row) => {
  Object.assign(form, emptyForm)
  if (row) {
    Object.assign(form, {
      id: row.id,
      productId: row.productId,
      title: row.title,
      priority: row.priority,
      status: row.status,
      storyPoint: row.storyPoint,
      description: row.description
    })
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await storyApi.update(form.id, form)
    } else {
      await storyApi.add(form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除需求「${row.title}」吗？`, '提示', { type: 'warning' })
  await storyApi.remove(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(async () => {
  loadData()
  const data = await productApi.page({ pageNum: 1, pageSize: 100 })
  products.value = data.records || []
})
</script>

<style scoped>
.filters {
  display: flex;
  gap: 8px;
}
</style>
