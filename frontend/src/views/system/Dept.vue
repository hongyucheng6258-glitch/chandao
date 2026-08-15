<template>
  <el-card>
    <div class="toolbar">
      <el-button type="primary" icon="Plus" @click="openDialog(null, 0)">新增根部门</el-button>
    </div>
    <el-table :data="tree" row-key="id" default-expand-all v-loading="loading">
      <el-table-column prop="deptName" label="部门名称" />
      <el-table-column prop="sort" label="排序" width="100" />
      <el-table-column prop="createdTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(null, row.id)">新增子部门</el-button>
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-popconfirm title="确定删除？" @confirm="handleDelete(row)">
            <template #reference><el-button link type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑部门' : '新增部门'" width="420px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="部门名称" required><el-input v-model="form.deptName" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
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
import { deptApi } from '@/api'

const tree = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const form = reactive({})

async function load() {
  loading.value = true
  try {
    tree.value = (await deptApi.tree()).data
  } finally {
    loading.value = false
  }
}

function openDialog(row, parentId) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row || { parentId: parentId ?? 0, deptName: '', sort: 0 })
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.deptName) return ElMessage.warning('请填写部门名称')
  if (form.id) {
    await deptApi.update(form.id, form)
  } else {
    await deptApi.create(form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function handleDelete(row) {
  await deptApi.remove(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar { margin-bottom: 12px; }
</style>
