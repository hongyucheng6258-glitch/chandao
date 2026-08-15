<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="账号/姓名" clearable style="width: 180px" @change="load" />
      <el-tree-select v-model="query.deptId" :data="deptTree" :props="{ label: 'deptName', value: 'id', children: 'children' }"
        placeholder="部门" clearable check-strictly style="width: 160px" @change="load" />
      <el-button type="primary" icon="Plus" @click="openDialog()">新增用户</el-button>
    </div>

    <el-table :data="rows" v-loading="loading">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="username" label="账号" width="120" />
      <el-table-column prop="realName" label="姓名" width="110" />
      <el-table-column prop="email" label="邮箱" show-overflow-tooltip />
      <el-table-column label="角色" width="180">
        <template #default="{ row }">
          <el-tag v-for="rid in row.roleIds" :key="rid" size="small" style="margin-right: 4px">
            {{ roleName(rid) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-switch :model-value="row.status === 1" @change="toggleStatus(row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-popconfirm title="确定删除该用户？" @confirm="handleDelete(row)">
            <template #reference><el-button link type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
      :total="total" layout="total, prev, pager, next" style="margin-top: 12px" @current-change="load" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用户' : '新增用户'" width="500px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="账号" required><el-input v-model="form.username" :disabled="!!form.id" /></el-form-item>
        <el-form-item :label="form.id ? '重置密码' : '密码'">
          <el-input v-model="form.password" type="password" show-password :placeholder="form.id ? '留空则不修改' : '默认 123456'" />
        </el-form-item>
        <el-form-item label="姓名" required><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="部门">
          <el-tree-select v-model="form.deptId" :data="deptTree" :props="{ label: 'deptName', value: 'id', children: 'children' }"
            clearable check-strictly style="width: 100%" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple style="width: 100%">
            <el-option v-for="r in roleOptions" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
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
import { userApi, roleApi, deptApi } from '@/api'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', deptId: null })
const dialogVisible = ref(false)
const form = reactive({})
const roleOptions = ref([])
const deptTree = ref([])

const roleName = (id) => roleOptions.value.find((r) => r.id === id)?.roleName || id

async function load() {
  loading.value = true
  try {
    const res = await userApi.page(query)
    rows.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row ? { ...row, password: '' } : { username: '', password: '', realName: '', email: '', deptId: null, roleIds: [] })
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.username || !form.realName) return ElMessage.warning('请填写账号和姓名')
  if (form.id) {
    await userApi.update(form.id, form)
  } else {
    await userApi.create(form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function toggleStatus(row) {
  await userApi.changeStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success('已更新')
  load()
}

async function handleDelete(row) {
  await userApi.remove(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(async () => {
  load()
  roleOptions.value = (await roleApi.options()).data
  deptTree.value = (await deptApi.tree()).data
})
</script>

<style scoped>
.toolbar { display: flex; gap: 12px; margin-bottom: 12px; }
</style>
