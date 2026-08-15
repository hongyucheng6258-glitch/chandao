<template>
  <div>
    <el-card style="margin-bottom: 16px">
      <div class="toolbar">
        <el-select v-model="projectId" placeholder="选择项目" style="width: 180px" @change="onProjectChange">
          <el-option v-for="p in projectOptions" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
        <el-select v-model="sprintId" placeholder="选择迭代" style="width: 180px" @change="loadCharts">
          <el-option v-for="s in sprintOptions" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
      </div>
      <el-row :gutter="16">
        <el-col :span="12"><div ref="burndownRef" class="chart"></div></el-col>
        <el-col :span="12"><div ref="taskPieRef" class="chart"></div></el-col>
      </el-row>
    </el-card>

    <el-card style="margin-bottom: 16px">
      <div class="block-title">成员工时汇总（基于所选迭代）</div>
      <div ref="workhourRef" class="chart"></div>
    </el-card>

    <el-card>
      <div class="toolbar">
        <el-select v-model="statProductId" placeholder="选择产品(全部)" clearable style="width: 180px" @change="loadBugPie">
          <el-option v-for="p in productOptions" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
      </div>
      <div ref="bugPieRef" class="chart"></div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import * as echarts from 'echarts'
import { projectApi, sprintApi, productApi, statsApi, taskApi } from '@/api'

const projectId = ref(null)
const sprintId = ref(null)
const statProductId = ref(null)
const projectOptions = ref([])
const sprintOptions = ref([])
const productOptions = ref([])
const burndownRef = ref()
const taskPieRef = ref()
const bugPieRef = ref()
const workhourRef = ref()
let burndownChart, taskPieChart, bugPieChart, workhourChart

async function onProjectChange() {
  sprintId.value = null
  sprintOptions.value = (await sprintApi.list(projectId.value)).data
}

async function loadCharts() {
  if (!sprintId.value) return
  const burndown = (await statsApi.burndown(sprintId.value)).data
  burndownChart.setOption({
    title: { text: '迭代燃尽图', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: burndown.map((d) => d.statDate) },
    yAxis: { type: 'value', name: '剩余工时(h)' },
    series: [{ name: '剩余工时', type: 'line', smooth: true, areaStyle: {}, data: burndown.map((d) => d.leftHours) }]
  }, true)

  const taskDist = (await statsApi.taskDistribution(sprintId.value)).data
  taskPieChart.setOption({
    title: { text: '任务状态分布', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'item' },
    series: [{ type: 'pie', radius: '55%', data: taskDist }]
  }, true)

  await loadWorkhour()
}

async function loadWorkhour() {
  if (!sprintId.value) return
  const data = (await taskApi.workhourSummary({ sprintId: sprintId.value })).data
  workhourChart.setOption({
    title: { text: '成员工时汇总 (h)', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { bottom: 0, data: ['预估', '已消耗', '剩余'] },
    grid: { top: 50, bottom: 50, left: 50, right: 20 },
    xAxis: { type: 'category', data: data.map((d) => d.assigneeName) },
    yAxis: { type: 'value', name: '工时' },
    series: [
      { name: '预估', type: 'bar', data: data.map((d) => d.estimateTotal) },
      { name: '已消耗', type: 'bar', data: data.map((d) => d.consumedTotal) },
      { name: '剩余', type: 'bar', data: data.map((d) => d.leftTotal) }
    ]
  }, true)
}

async function loadBugPie() {
  const data = (await statsApi.bugDistribution(statProductId.value)).data
  bugPieChart.setOption({
    title: { text: 'Bug 状态分布', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'item' },
    series: [{ type: 'pie', radius: '55%', data }]
  }, true)
}

onMounted(async () => {
  burndownChart = echarts.init(burndownRef.value)
  taskPieChart = echarts.init(taskPieRef.value)
  bugPieChart = echarts.init(bugPieRef.value)
  workhourChart = echarts.init(workhourRef.value)
  projectOptions.value = (await projectApi.options()).data
  productOptions.value = (await productApi.options()).data
  loadBugPie()
  window.addEventListener('resize', () => {
    burndownChart.resize(); taskPieChart.resize(); bugPieChart.resize(); workhourChart.resize()
  })
})
</script>

<style scoped>
.toolbar { display: flex; gap: 12px; margin-bottom: 12px; }
.block-title { font-size: 14px; font-weight: 600; color: #303133; margin-bottom: 8px; }
.chart { height: 320px; }
</style>
