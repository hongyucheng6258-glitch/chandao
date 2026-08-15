<template>
  <el-card>
    <div class="toolbar">
      <el-select v-model="query.objectType" placeholder="对象类型" clearable style="width: 140px" @change="load">
        <el-option v-for="t in ['story', 'task', 'bug', 'product', 'project', 'sprint', 'release']" :key="t" :label="t" :value="t" />
      </el-select>
      <el-input v-model="query.actorName" placeholder="操作人" clearable style="width: 160px" @change="load" />
    </div>
    <el-table :data="rows" v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="objectType" label="对象类型" width="100" />
      <el-table-column prop="objectId" label="对象ID" width="90" />
      <el-table-column prop="action" label="动作" width="140" />
      <el-table-column prop="actorName" label="操作人" width="110" />
      <el-table-column prop="createdTime" label="时间" width="170" />
      <el-table-column prop="detail" label="明细" show-overflow-tooltip />
    </el-table>
    <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
      :total="total" layout="total, prev, pager, next" style="margin-top: 12px" @current-change="load" />
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { logApi } from '@/api'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ pageNum: 1, pageSize: 15, objectType: '', actorName: '' })

async function load() {
  loading.value = true
  try {
    const res = await logApi.page(query)
    rows.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; gap: 12px; margin-bottom: 12px; }
</style>
