<template>
  <el-card>
    <div class="toolbar">
      <el-button type="primary" icon="Plus" @click="openDialog(null, 0)">新增根节点</el-button>
    </div>
    <el-table :data="tree" row-key="id" default-expand-all v-loading="loading">
      <el-table-column prop="permName" label="名称" width="200" />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag size="small" :type="{ 1: 'warning', 2: 'primary', 3: 'info' }[row.permType]">
            {{ { 1: '目录', 2: '菜单', 3: '按钮' }[row.permType] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="permKey" label="权限标识" width="180" />
      <el-table-column prop="path" label="路由路径" width="180" />
      <el-table-column prop="icon" label="图标" width="120" />
      <el-table-column prop="sort" label="排序" width="70" />
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(null, row.id)">新增子级</el-button>
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-popconfirm title="确定删除？子节点会一并删除" @confirm="handleDelete(row)">
            <template #reference><el-button link type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑权限' : '新增权限'" width="460px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="类型">
          <el-radio-group v-model="form.permType">
            <el-radio-button :value="1">目录</el-radio-button>
            <el-radio-button :value="2">菜单</el-radio-button>
            <el-radio-button :value="3">按钮</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="名称" required><el-input v-model="form.permName" /></el-form-item>
        <el-form-item label="权限标识"><el-input v-model="form.permKey" placeholder="如 task:assign" /></el-form-item>
        <el-form-item label="路由路径"><el-input v-model="form.path" placeholder="如 /task" /></el-form-item>
        <el-form-item label="图标"><el-input v-model="form.icon" placeholder="element-plus 图标名" /></el-form-item>
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
import { permApi } from '@/api'

const tree = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const form = reactive({})

async function load() {
  loading.value = true
  try {
    tree.value = (await permApi.tree()).data
  } finally {
    loading.value = false
  }
}

function openDialog(row, parentId) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row || { parentId: parentId ?? 0, permType: 2, permName: '', permKey: '', path: '', icon: '', sort: 0 })
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.permName) return ElMessage.warning('请填写名称')
  if (form.id) {
    await permApi.update(form.id, form)
  } else {
    await permApi.create(form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function handleDelete(row) {
  await permApi.remove(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar { margin-bottom: 12px; }
</style>
