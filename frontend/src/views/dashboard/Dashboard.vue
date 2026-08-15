<template>
  <div class="page-container">
    <el-row :gutter="16">
      <el-col :span="6" v-for="card in cards" :key="card.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-card-body">
            <div>
              <div class="stat-value">{{ card.value }}</div>
              <div class="stat-label">{{ card.label }}</div>
            </div>
            <el-icon :size="40" :color="card.color"><component :is="card.icon" /></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <div class="chart-header">
              <span>迭代燃尽图</span>
              <el-select v-model="sprintId" placeholder="选择迭代" size="small" style="width: 200px" @change="loadBurndown">
                <el-option v-for="s in sprints" :key="s.id" :label="s.name" :value="s.id" />
              </el-select>
            </div>
          </template>
          <div ref="burndownRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="never">
          <template #header><span>Bug 状态分布</span></template>
          <div ref="bugRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, onBeforeUnmount, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import { getSummary } from '@/api/dashboard'
import { getBurndown, getBugDistribution } from '@/api/stats'
import sprintApi from '@/api/sprint'

const cards = reactive([
  { label: '我的任务', value: 0, icon: 'List', color: '#409eff' },
  { label: '我的 Bug', value: 0, icon: 'Warning', color: '#f56c6c' },
  { label: '我的需求', value: 0, icon: 'Document', color: '#e6a23c' },
  { label: '进行中项目', value: 0, icon: 'FolderOpened', color: '#67c23a' }
])

const sprints = ref([])
const sprintId = ref(null)
const burndownRef = ref()
const bugRef = ref()
let burndownChart = null
let bugChart = null

const loadSummary = async () => {
  const data = await getSummary()
  cards[0].value = data.myTaskCount
  cards[1].value = data.myBugCount
  cards[2].value = data.myStoryCount
  cards[3].value = data.doingProjectCount
}

const loadSprints = async () => {
  const data = await sprintApi.page({ pageNum: 1, pageSize: 50 })
  sprints.value = data.records || []
  if (sprints.value.length) {
    sprintId.value = sprints.value[0].id
    loadBurndown()
  }
}

const loadBurndown = async () => {
  if (!sprintId.value) return
  const data = await getBurndown(sprintId.value)
  const dates = (data || []).map((d) => d.statDate)
  burndownChart?.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['剩余工时', '任务总数', '已完成任务'] },
    grid: { left: 40, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value' },
    series: [
      { name: '剩余工时', type: 'line', smooth: true, data: (data || []).map((d) => d.leftHours), areaStyle: { opacity: 0.1 } },
      { name: '任务总数', type: 'line', data: (data || []).map((d) => d.taskTotal) },
      { name: '已完成任务', type: 'line', data: (data || []).map((d) => d.taskDone) }
    ]
  })
}

const loadBugDist = async () => {
  const data = await getBugDistribution()
  bugChart?.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['40%', '65%'],
        center: ['50%', '45%'],
        data: data || [],
        label: { formatter: '{b}: {c}' }
      }
    ]
  })
}

const resize = () => {
  burndownChart?.resize()
  bugChart?.resize()
}

onMounted(async () => {
  burndownChart = echarts.init(burndownRef.value)
  bugChart = echarts.init(bugRef.value)
  window.addEventListener('resize', resize)
  loadSummary()
  loadSprints()
  loadBugDist()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  burndownChart?.dispose()
  bugChart?.dispose()
})
</script>

<style scoped>
.stat-card-body {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.chart {
  height: 320px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
