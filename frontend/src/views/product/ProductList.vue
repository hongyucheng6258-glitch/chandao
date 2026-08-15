<template>
  <div class="page-container">
    <div class="page-card">
      <div class="table-toolbar">
        <el-input v-model="query.keyword" placeholder="产品名称" clearable style="width: 220px" @change="loadData" />
        <el-button type="primary" v-perm="'product:add'" @click="openDialog()">
          <el-icon><Plus /></el-icon>新增产品
        </el-button>
      </div>
      <el-table :data="list" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="productName" label="产品名称" min-width="160" />
        <el-table-column prop="productCode" label="产品编码" width="130" />
        <el-table-column prop="ownerName" label="负责人" width="110" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" v-perm="'product:edit'" @click="openDialog(row)">编辑</el-button>
            <el-button link type="danger" v-perm="'product:delete'" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑产品' : '新增产品'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="产品名称" prop="productName">
          <el-input v-model="form.productName" />
        </el-form-item>
        <el-form-item label="产品编码" prop="productCode">
          <el-input v-model="form.productCode" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="form.ownerId" filterable placeholder="请选择负责人" style="width: 100%">
            <el-option v-for="u in users" :key="u.id" :label="u.realName || u.username" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="进行中" value="doing" />
            <el-option label="已完成" value="done" />
            <el-option label="已关闭" value="closed" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
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
import productApi from '@/api/product'
import userApi from '@/api/user'
import StatusTag from '@/components/StatusTag.vue'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '' })
const users = ref([])

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const emptyForm = { id: null, productName: '', productCode: '', ownerId: null, status: 'doing', description: '' }
const form = reactive({ ...emptyForm })
const rules = {
  productName: [{ required: true, message: '请输入产品名称', trigger: 'blur' }],
  productCode: [{ required: true, message: '请输入产品编码', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const data = await productApi.page(query)
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
      productName: row.productName,
      productCode: row.productCode,
      ownerId: row.ownerId,
      status: row.status,
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
      await productApi.update(form.id, form)
    } else {
      await productApi.add(form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除产品「${row.productName}」吗？`, '提示', { type: 'warning' })
  await productApi.remove(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(async () => {
  loadData()
  const data = await userApi.page({ pageNum: 1, pageSize: 200 })
  users.value = data.records || []
})
</script>
