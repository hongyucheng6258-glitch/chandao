<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="6" v-for="card in cards" :key="card.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-num" :style="{ color: card.color }">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 16px">
      <div class="toolbar">
        <span class="card-title">项目概览</span>
        <el-select v-model="sprintId" placeholder="选择迭代(默认首个)" style="width: 200px" @change="loadCharts">
          <el-option v-for="s in sprintOptions" :key="s.id" :label="`${s.name}（${s.status}）`" :value="s.id" />
        </el-select>
      </div>
      <el-row :gutter="16">
        <el-col :span="12"><div ref="burndownRef" class="chart"></div></el-col>
        <el-col :span="12"><div ref="bugPieRef" class="chart"></div></el-col>
      </el-row>
    </el-card>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card>
          <template #header>我的待办任务</template>
          <el-table :data="myTasks" size="small">
            <el-table-column prop="name" label="任务" show-overflow-tooltip />
            <el-table-column label="状态" width="90">
              <template #default="{ row }"><StatusTag :status="row.status" type="task" /></template>
            </el-table-column>
            <el-table-column prop="deadline" label="截止" width="110" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>待我处理的 Bug</template>
          <el-table :data="myBugs" size="small">
            <el-table-column prop="title" label="Bug" show-overflow-tooltip />
            <el-table-column label="状态" width="90">
              <template #default="{ row }"><StatusTag :status="row.status" type="bug" /></template>
            </el-table-column>
            <el-table-column label="严重程度" width="90">
              <template #default="{ row }"><PriorityTag :level="row.severity" kind="severity" /></template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { dashboardApi, sprintApi, statsApi } from '@/api'
import StatusTag from '@/components/StatusTag.vue'
import PriorityTag from '@/components/PriorityTag.vue'

const summary = ref({})
const myTasks = ref([])
const myBugs = ref([])
const sprintId = ref(null)
const sprintOptions = ref([])
const burndownRef = ref()
const bugPieRef = ref()
let burndownChart, bugPieChart

const cards = ref([
  { label: '我的待办任务', value: 0, color: '#409eff' },
  { label: '待处理 Bug', value: 0, color: '#f56c6c' },
  { label: '我的需求', value: 0, color: '#e6a23c' },
  { label: '进行中项目', value: 0, color: '#67c23a' }
])

async function loadCharts() {
  if (sprintId.value) {
    const burndown = (await statsApi.burndown(sprintId.value)).data
    burndownChart.setOption({
      title: { text: '迭代燃尽图', left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'axis' },
      grid: { top: 48, bottom: 32, left: 48, right: 24 },
      xAxis: { type: 'category', data: burndown.map((d) => d.statDate) },
      yAxis: { type: 'value', name: '剩余工时(h)' },
      series: [{ name: '剩余工时', type: 'line', smooth: true, areaStyle: {}, data: burndown.map((d) => d.leftHours) }]
    }, true)
  }
  const bugDist = (await statsApi.bugDistribution()).data
  bugPieChart.setOption({
    title: { text: 'Bug 状态分布', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{ type: 'pie', radius: '55%', data: bugDist }]
  }, true)
}

onMounted(async () => {
  const res = await dashboardApi.summary()
  summary.value = res.data
  cards.value[0].value = res.data.myTaskCount
  cards.value[1].value = res.data.myBugCount
  cards.value[2].value = res.data.myStoryCount
  cards.value[3].value = res.data.doingProjectCount
  myTasks.value = (await dashboardApi.myTasks()).data
  myBugs.value = (await dashboardApi.myBugs()).data

  sprintOptions.value = (await sprintApi.options()).data
  await nextTick()
  burndownChart = echarts.init(burndownRef.value)
  bugPieChart = echarts.init(bugPieRef.value)
  window.addEventListener('resize', () => {
    burndownChart && burndownChart.resize()
    bugPieChart && bugPieChart.resize()
  })
  if (sprintOptions.value.length) {
    sprintId.value = sprintOptions.value[0].id
  }
  loadCharts()
})
</script>

<style scoped>
.stat-card { text-align: center; }
.stat-num { font-size: 32px; font-weight: 600; }
.stat-label { color: #909399; margin-top: 4px; }
.toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.card-title { font-weight: 600; color: #303133; }
.chart { height: 320px; }
</style>
