<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="角色名称" clearable style="width: 180px" @change="load" />
      <el-button type="primary" icon="Plus" @click="openDialog()">新增角色</el-button>
    </div>

    <el-table :data="rows" v-loading="loading">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="roleCode" label="角色编码" width="130" />
      <el-table-column prop="roleName" label="角色名称" width="140" />
      <el-table-column prop="remark" label="备注" show-overflow-tooltip />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="warning" @click="openGrant(row)">授权</el-button>
          <el-popconfirm title="确定删除？" @confirm="handleDelete(row)">
            <template #reference><el-button link type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
      :total="total" layout="total, prev, pager, next" style="margin-top: 12px" @current-change="load" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑角色' : '新增角色'" width="440px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="角色编码" required><el-input v-model="form.roleCode" :disabled="!!form.id" placeholder="如 DEV" /></el-form-item>
        <el-form-item label="角色名称" required><el-input v-model="form.roleName" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="grantVisible" :title="`角色授权 - ${currentRole?.roleName}`" width="440px">
      <el-tree ref="treeRef" :data="permTree" show-checkbox node-key="id"
        :props="{ label: 'permName', children: 'children' }" default-expand-all />
      <template #footer>
        <el-button @click="grantVisible = false">取消</el-button>
        <el-button type="primary" @click="handleGrant">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { roleApi, permApi } from '@/api'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '' })
const dialogVisible = ref(false)
const form = reactive({})
const grantVisible = ref(false)
const currentRole = ref(null)
const permTree = ref([])
const treeRef = ref()

async function load() {
  loading.value = true
  try {
    const res = await roleApi.page(query)
    rows.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row || { roleCode: '', roleName: '', remark: '' })
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.roleCode || !form.roleName) return ElMessage.warning('请填写编码和名称')
  if (form.id) {
    await roleApi.update(form.id, form)
  } else {
    await roleApi.create(form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function openGrant(row) {
  currentRole.value = row
  permTree.value = (await permApi.tree()).data
  grantVisible.value = true
  await nextTick()
  treeRef.value.setCheckedKeys(row.permIds || [])
}

async function handleGrant() {
  const checked = treeRef.value.getCheckedKeys()
  const half = treeRef.value.getHalfCheckedKeys()
  await roleApi.update(currentRole.value.id, { roleName: currentRole.value.roleName, permIds: [...checked, ...half] })
  ElMessage.success('授权成功')
  grantVisible.value = false
  load()
}

async function handleDelete(row) {
  await roleApi.remove(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; gap: 12px; margin-bottom: 12px; }
</style>
