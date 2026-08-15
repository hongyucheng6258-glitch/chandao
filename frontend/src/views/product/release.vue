<template>
  <el-card>
    <div class="toolbar">
      <el-select v-model="query.productId" placeholder="产品" clearable style="width: 180px" @change="load">
        <el-option v-for="p in productOptions" :key="p.id" :label="p.name" :value="p.id" />
      </el-select>
      <el-button type="primary" icon="Plus" @click="openDialog()">创建发布</el-button>
    </div>

    <el-table :data="rows" v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="版本号" width="130" />
      <el-table-column prop="releaseDate" label="发布日期" width="120" />
      <el-table-column label="完成需求" width="100">
        <template #default="{ row }">{{ row.storyIds?.length || 0 }} 个</template>
      </el-table-column>
      <el-table-column label="修复Bug" width="100">
        <template #default="{ row }">{{ row.bugIds?.length || 0 }} 个</template>
      </el-table-column>
      <el-table-column prop="description" label="说明" show-overflow-tooltip />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-popconfirm title="确定删除该发布？" @confirm="handleDelete(row)">
            <template #reference><el-button link type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
      :total="total" layout="total, prev, pager, next" style="margin-top: 12px" @current-change="load" />

    <el-dialog v-model="dialogVisible" title="创建发布" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="所属产品" required>
          <el-select v-model="form.productId" style="width: 100%">
            <el-option v-for="p in productOptions" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="版本号" required><el-input v-model="form.name" placeholder="如 v1.0.0" /></el-form-item>
        <el-form-item label="发布日期">
          <el-date-picker v-model="form.releaseDate" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="说明"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
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
import { releaseApi, productApi } from '@/api'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10, productId: null })
const dialogVisible = ref(false)
const form = reactive({})
const productOptions = ref([])

async function load() {
  loading.value = true
  try {
    const res = await releaseApi.page(query)
    rows.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function openDialog() {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, { productId: query.productId, name: '', releaseDate: '', description: '', storyIds: [], bugIds: [] })
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.name || !form.productId) return ElMessage.warning('请填写产品和版本号')
  await releaseApi.create(form)
  ElMessage.success('创建成功')
  dialogVisible.value = false
  load()
}

async function handleDelete(row) {
  await releaseApi.remove(row.id)
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
