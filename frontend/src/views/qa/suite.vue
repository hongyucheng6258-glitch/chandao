<template>
  <el-card>
    <div class="toolbar">
      <el-select v-model="query.productId" placeholder="产品" clearable style="width: 180px" @change="load">
        <el-option v-for="p in productOptions" :key="p.id" :label="p.name" :value="p.id" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px" @change="load">
        <el-option label="待执行" value="planned" />
        <el-option label="执行中" value="running" />
        <el-option label="已完成" value="done" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="测试单名称" clearable style="width: 200px" @change="load" />
      <el-button type="primary" icon="Plus" @click="openCreate">新建测试单</el-button>
    </div>

    <el-table :data="rows" v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="测试单名称" show-overflow-tooltip />
      <el-table-column prop="productId" label="产品" width="120">
        <template #default="{ row }">{{ productName(row.productId) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusName(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">执行/查看</el-button>
          <el-popconfirm title="确定删除该测试单？" @confirm="handleDelete(row)">
            <template #reference><el-button link type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
      :total="total" layout="total, prev, pager, next" style="margin-top: 12px" @current-change="load" />

    <!-- 新建测试单 -->
    <el-dialog v-model="createVisible" title="新建测试单" width="640px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="测试单名称" required>
          <el-input v-model="form.name" placeholder="如：商城一期回归测试" />
        </el-form-item>
        <el-form-item label="所属产品" required>
          <el-select v-model="form.productId" style="width: 100%" @change="onProductChange">
            <el-option v-for="p in productOptions" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联迭代">
          <el-select v-model="form.sprintId" clearable style="width: 100%" placeholder="可选">
            <el-option v-for="s in sprintOptions" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="选择用例" required>
          <el-select v-model="form.caseIds" multiple filterable style="width: 100%" placeholder="从该产品用例中选择">
            <el-option v-for="c in caseOptions" :key="c.id" :label="`#${c.id} ${c.title}`" :value="c.id" />
          </el-select>
          <div class="tip">已选 {{ form.caseIds.length }} 条用例</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- 详情：执行 -->
    <el-drawer v-model="detailVisible" :title="`测试单详情 - ${current.name || ''}`" size="62%" :destroy-on-close="true">
      <template v-if="detail">
        <el-descriptions :column="2" border size="small" style="margin-bottom: 12px">
          <el-descriptions-item label="产品">{{ productName(detail.suite.productId) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(detail.suite.status)">{{ statusName(detail.suite.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detail.suite.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 通过率概览 -->
        <el-card shadow="never" style="margin-bottom: 12px">
          <div class="summary">
            <div class="stat"><span class="num">{{ detail.summary.total }}</span><span class="lbl">总用例</span></div>
            <div class="stat pass"><span class="num">{{ detail.summary.pass }}</span><span class="lbl">通过</span></div>
            <div class="stat fail"><span class="num">{{ detail.summary.fail }}</span><span class="lbl">失败</span></div>
            <div class="stat blocked"><span class="num">{{ detail.summary.blocked }}</span><span class="lbl">阻塞</span></div>
            <div class="stat unexec"><span class="num">{{ detail.summary.unexecuted }}</span><span class="lbl">未执行</span></div>
            <div class="stat rate"><span class="num">{{ detail.summary.passRate }}%</span><span class="lbl">通过率</span></div>
          </div>
          <el-progress :percentage="detail.summary.passRate" :stroke-width="14"
            :status="detail.summary.passRate === 100 ? 'success' : ''" style="margin-top: 8px" />
        </el-card>

        <el-table :data="detail.cases" border>
          <el-table-column prop="caseId" label="用例ID" width="80" />
          <el-table-column prop="title" label="用例标题" show-overflow-tooltip />
          <el-table-column prop="type" label="类型" width="80">
            <template #default="{ row }">{{ typeMap[row.type] || row.type }}</template>
          </el-table-column>
          <el-table-column label="结果" width="210">
            <template #default="{ row }">
              <el-select v-model="row.result" size="small" placeholder="未执行" @change="() => onResultChange(row)">
                <el-option label="通过" value="pass" />
                <el-option label="失败" value="fail" />
                <el-option label="阻塞" value="blocked" />
                <el-option label="未执行" value="" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="执行人" width="100">
            <template #default="{ row }">{{ row.executorId ? userName(row.executorId) : '-' }}</template>
          </el-table-column>
          <el-table-column label="备注" min-width="120">
            <template #default="{ row }">
              <el-input v-model="row.remark" size="small" placeholder="备注" @blur="() => onResultChange(row)" />
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-drawer>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { testsuiteApi, productApi, sprintApi, testcaseApi, userApi } from '@/api'

const typeMap = { feature: '功能', ui: '界面', performance: '性能' }
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10, productId: null, status: '', keyword: '' })

const productOptions = ref([])
const sprintOptions = ref([])
const caseOptions = ref([])
const userOptions = ref([])

// 新建
const createVisible = ref(false)
const saving = ref(false)
const form = reactive({ name: '', productId: null, sprintId: null, remark: '', caseIds: [] })

// 详情
const detailVisible = ref(false)
const detail = ref(null)
const current = ref({})

async function load() {
  loading.value = true
  try {
    const res = await testsuiteApi.page(query)
    rows.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function statusName(s) {
  return { planned: '待执行', running: '执行中', done: '已完成' }[s] || s
}
function statusType(s) {
  return { planned: 'info', running: 'warning', done: 'success' }[s] || ''
}
function productName(id) {
  return productOptions.value.find(p => p.id === id)?.name || id
}
function userName(id) {
  return userOptions.value.find(u => u.id === id)?.realName || id
}

async function openCreate() {
  Object.assign(form, { name: '', productId: null, sprintId: null, remark: '', caseIds: [] })
  caseOptions.value = []
  sprintOptions.value = []
  createVisible.value = true
}

async function onProductChange(pid) {
  form.caseIds = []
  if (!pid) { caseOptions.value = []; sprintOptions.value = []; return }
  const c = await testcaseApi.page({ pageNum: 1, pageSize: 1000, productId: pid })
  caseOptions.value = c.data.records
  const s = await sprintApi.options()
  sprintOptions.value = s.data
}

async function handleCreate() {
  if (!form.name || !form.productId) { ElMessage.warning('请填写名称与产品'); return }
  if (!form.caseIds.length) { ElMessage.warning('请至少选择一条用例'); return }
  saving.value = true
  try {
    await testsuiteApi.create({ ...form })
    ElMessage.success('测试单创建成功')
    createVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function openDetail(row) {
  current.value = row
  detailVisible.value = true
  const res = await testsuiteApi.get(row.id)
  res.data.cases.forEach(c => { if (c.result == null) c.result = '' })
  detail.value = res.data
}

async function onResultChange(row) {
  if (!detail.value) return
  await testsuiteApi.run(detail.value.suite.id, {
    caseId: row.caseId,
    result: row.result || null,
    remark: row.remark || '',
    spentMinutes: 0
  })
  // 刷新概览
  const res = await testsuiteApi.get(detail.value.suite.id)
  res.data.cases.forEach(c => { if (c.result == null) c.result = '' })
  detail.value = res.data
}

async function handleDelete(row) {
  await testsuiteApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  productOptions.value = (await productApi.options()).data
  userOptions.value = (await userApi.options()).data
  load()
})
</script>

<style scoped>
.toolbar { display: flex; gap: 12px; margin-bottom: 12px; flex-wrap: wrap; }
.tip { color: #909399; font-size: 12px; margin-top: 4px; }
.summary { display: flex; gap: 18px; text-align: center; }
.stat { display: flex; flex-direction: column; min-width: 56px; }
.stat .num { font-size: 22px; font-weight: 600; color: #303133; }
.stat .lbl { font-size: 12px; color: #909399; margin-top: 2px; }
.stat.pass .num { color: #67c23a; }
.stat.fail .num { color: #f56c6c; }
.stat.blocked .num { color: #e6a23c; }
.stat.unexec .num { color: #909399; }
.stat.rate .num { color: #409eff; }
</style>
