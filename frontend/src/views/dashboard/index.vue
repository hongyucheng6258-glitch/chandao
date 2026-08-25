<template>
  <div class="dashboard-page">
    <!-- 欢迎横幅 -->
    <div class="welcome-banner">
      <div class="welcome-content">
        <h2 class="welcome-title">{{ greeting }}，{{ userStore.realName || '用户' }} 👋</h2>
        <p class="welcome-desc">今天是 {{ todayText }}，您有 {{ summary.myTaskCount || 0 }} 个待办任务和 {{ summary.myBugCount || 0 }} 个待处理Bug</p>
        <div class="welcome-stats">
          <div class="welcome-stat">
            <div class="welcome-stat-num">{{ summary.doingProjectCount || 0 }}</div>
            <div class="welcome-stat-label">进行中项目</div>
          </div>
          <div class="welcome-stat">
            <div class="welcome-stat-num">{{ summary.myStoryCount || 0 }}</div>
            <div class="welcome-stat-label">我的需求</div>
          </div>
          <div class="welcome-stat">
            <div class="welcome-stat-num">{{ summary.myTaskCount || 0 }}</div>
            <div class="welcome-stat-label">待办任务</div>
          </div>
          <div class="welcome-stat">
            <div class="welcome-stat-num">{{ summary.myBugCount || 0 }}</div>
            <div class="welcome-stat-label">活跃Bug</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card" v-for="card in statCards" :key="card.label">
        <div class="stat-card-header">
          <div class="stat-card-icon" :class="card.iconClass">{{ card.icon }}</div>
          <div class="stat-card-trend" :class="card.trendClass">{{ card.trend }}</div>
        </div>
        <div class="stat-card-value">{{ card.value }}</div>
        <div class="stat-card-label">{{ card.label }}</div>
      </div>
    </div>

    <!-- 项目概览图表 -->
    <div class="panel chart-panel">
      <div class="panel-header">
        <div class="panel-title"><span class="panel-kicker">OVERVIEW</span>项目概览</div>
        <el-select v-model="sprintId" placeholder="选择迭代" size="default" style="width: 220px" @change="loadCharts">
          <el-option v-for="s in sprintOptions" :key="s.id" :label="`${s.name}（${s.status}）`" :value="s.id" />
        </el-select>
      </div>
      <div class="panel-body chart-body">
        <div class="chart-item">
          <div ref="burndownRef" class="chart"></div>
        </div>
        <div class="chart-item">
          <div ref="bugPieRef" class="chart"></div>
        </div>
      </div>
    </div>

    <!-- 待办列表 -->
    <div class="dashboard-grid">
      <div class="panel">
        <div class="panel-header">
          <div class="panel-title"><span class="panel-kicker">TASKS</span>我的待办任务</div>
          <a class="panel-link" @click="goBoard">更多 →</a>
        </div>
        <div class="panel-body">
          <div
            v-for="task in myTasks.slice(0, 6)"
            :key="task.id"
            class="task-list-item"
            @click="goTaskBoard"
          >
            <div class="task-priority" :class="getPriorityClass(task.priority)"></div>
            <div class="task-info">
              <div class="task-title">{{ task.name }}</div>
              <div class="task-meta">
                <span v-if="task.projectName">📁 {{ task.projectName }}</span>
                <span v-if="task.deadline">📅 {{ task.deadline }}</span>
                <span v-if="task.assigneeName">👤 {{ task.assigneeName }}</span>
              </div>
            </div>
            <StatusTag :status="task.status" type="task" />
          </div>
          <div v-if="!myTasks.length" class="empty-state">
            <div class="empty-icon">🎉</div>
            <div class="empty-text">暂无待办任务</div>
          </div>
        </div>
      </div>

      <div class="panel">
        <div class="panel-header">
          <div class="panel-title"><span class="panel-kicker">QUALITY</span>待我处理的 Bug</div>
          <a class="panel-link" @click="goBug">更多 →</a>
        </div>
        <div class="panel-body">
          <div
            v-for="bug in myBugs.slice(0, 6)"
            :key="bug.id"
            class="task-list-item"
            @click="goBug"
          >
            <div class="task-priority" :class="getSeverityClass(bug.severity)"></div>
            <div class="task-info">
              <div class="task-title">{{ bug.title }}</div>
              <div class="task-meta">
                <PriorityTag :level="bug.severity" kind="severity" />
                <span v-if="bug.createdTime">📅 {{ bug.createdTime }}</span>
              </div>
            </div>
            <StatusTag :status="bug.status" type="bug" />
          </div>
          <div v-if="!myBugs.length" class="empty-state">
            <div class="empty-icon">✅</div>
            <div class="empty-text">暂无待处理Bug</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { dashboardApi, sprintApi, statsApi } from '@/api'
import StatusTag from '@/components/StatusTag.vue'
import PriorityTag from '@/components/PriorityTag.vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const summary = ref({})
const myTasks = ref([])
const myBugs = ref([])
const sprintId = ref(null)
const sprintOptions = ref([])
const burndownRef = ref()
const bugPieRef = ref()
let burndownChart, bugPieChart

const todayText = computed(() => {
  const now = new Date()
  const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日 ${weekDays[now.getDay()]}`
})

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '凌晨好'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const statCards = computed(() => [
  { label: '我的待办任务', value: summary.value.myTaskCount || 0, icon: 'T', iconClass: 'blue', trend: '↑ 12%', trendClass: 'up' },
  { label: '待处理 Bug', value: summary.value.myBugCount || 0, icon: 'B', iconClass: 'orange', trend: '↓ 15%', trendClass: 'down' },
  { label: '我的需求', value: summary.value.myStoryCount || 0, icon: 'S', iconClass: 'green', trend: '↑ 8%', trendClass: 'up' },
  { label: '进行中项目', value: summary.value.doingProjectCount || 0, icon: 'P', iconClass: 'purple', trend: '↑ 3%', trendClass: 'up' }
])

function getPriorityClass(priority) {
  if (priority === 1 || priority === 'high') return 'high'
  if (priority === 2 || priority === 'medium') return 'medium'
  return 'low'
}

function getSeverityClass(severity) {
  if (severity === 1 || severity === 'critical' || severity === 'blocker') return 'high'
  if (severity === 2 || severity === 'major' || severity === 'normal') return 'medium'
  return 'low'
}

async function loadCharts() {
  if (sprintId.value && burndownChart) {
    const burndown = (await statsApi.burndown(sprintId.value)).data
    burndownChart.setOption({
      title: { text: '迭代燃尽图', left: 'center', textStyle: { fontSize: 14, fontWeight: 600, fontFamily: 'Inter' } },
      tooltip: { trigger: 'axis' },
      grid: { top: 48, bottom: 32, left: 48, right: 24 },
      xAxis: { type: 'category', data: burndown.map((d) => d.statDate), axisLine: { lineStyle: { color: '#e2e8f0' } } },
      yAxis: { type: 'value', name: '剩余工时(h)', splitLine: { lineStyle: { color: '#f1f5f9' } } },
      series: [{
        name: '剩余工时',
        type: 'line',
        smooth: true,
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(141,240,197,0.30)' }, { offset: 1, color: 'rgba(141,240,197,0.03)' }]) },
        lineStyle: { color: '#27b984', width: 2 },
        itemStyle: { color: '#27b984' },
        data: burndown.map((d) => d.leftHours)
      }]
    }, true)
  }
  if (bugPieChart) {
    const bugDist = (await statsApi.bugDistribution()).data
    bugPieChart.setOption({
      title: { text: 'Bug 状态分布', left: 'center', textStyle: { fontSize: 14, fontWeight: 600, fontFamily: 'Inter' } },
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      color: ['#27b984', '#6c8fc4', '#a6b4c5', '#d99a3d', '#6ea8dc'],
      series: [{ type: 'pie', radius: ['40%', '65%'], center: ['50%', '45%'], data: bugDist, label: { formatter: '{b}: {c}' } }]
    }, true)
  }
}

function goBoard() {
  router.push('/project/board')
}

function goTaskBoard() {
  router.push('/project/board')
}

function goBug() {
  router.push('/qa/bug')
}

onMounted(async () => {
  try {
    const res = await dashboardApi.summary()
    summary.value = res.data
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
  } catch (e) {
    // API请求失败（如401），响应拦截器会处理跳转
    console.warn('加载工作台数据失败', e)
  }
})
</script>

<style scoped>
.dashboard-page {
  padding: 24px 28px;
}

/* 欢迎横幅 */
.welcome-banner {
  background: linear-gradient(135deg, var(--surface-dark) 0%, var(--surface-dark-raised) 58%, var(--surface-dark-active) 100%);
  border-radius: var(--radius-lg);
  padding: 28px 32px;
  color: white;
  margin-bottom: 20px;
  position: relative;
  overflow: hidden;
}

.welcome-banner::before {
  content: '';
  position: absolute;
  top: -60px;
  right: -60px;
  width: 240px;
  height: 240px;
  background: radial-gradient(circle, rgba(255,255,255,0.1) 0%, transparent 70%);
  border-radius: 50%;
}

.welcome-content {
  position: relative;
  z-index: 1;
}

.welcome-title {
  font-family: Inter, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 6px;
  color: white;
}

.welcome-desc {
  font-size: 14px;
  opacity: 0.8;
  margin-bottom: 20px;
}

.welcome-stats {
  display: flex;
  gap: 40px;
}

.welcome-stat-num {
  font-family: Inter, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 28px;
  font-weight: 700;
  line-height: 1;
}

.welcome-stat-label {
  font-size: 12px;
  opacity: 0.7;
  margin-top: 4px;
}

/* 统计卡片 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  padding: 20px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-light);
  transition: all 0.2s;
}

.stat-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.stat-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.stat-card-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.stat-card-icon.blue { background: #eff6ff; color: #3b82f6; }
.stat-card-icon.orange { background: var(--accent-danger-bg); color: var(--accent-danger); }
.stat-card-icon.green { background: var(--accent-success-bg); color: var(--accent-success); }
.stat-card-icon.purple { background: #eef3fb; color: var(--accent-info); }

.stat-card-trend {
  font-size: 12px;
  font-weight: 600;
}

.stat-card-trend.up { color: var(--accent-success); }
.stat-card-trend.down { color: var(--accent-danger); }

.stat-card-value {
  font-family: Inter, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 28px;
  font-weight: 700;
  line-height: 1;
  margin-bottom: 6px;
}

.stat-card-label {
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 500;
}

/* 面板通用 */
.panel {
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-light);
  overflow: hidden;
  margin-bottom: 20px;
}

.panel-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-light);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: Inter, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 15px;
  font-weight: 750;
}

.panel-kicker {
  color: var(--accent-success);
  font-size: 10px;
  letter-spacing: .12em;
  font-weight: 800;
}

.panel-link {
  font-size: 13px;
  color: var(--accent-success);
  font-weight: 600;
  cursor: pointer;
}

.panel-link:hover {
  text-decoration: underline;
}

.panel-body {
  padding: 4px 0;
}

/* 图表面板 */
.chart-body {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  padding: 20px;
}

.chart-item {
  min-height: 300px;
}

.chart {
  height: 300px;
  width: 100%;
}

/* 待办列表双栏 */
.dashboard-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.task-list-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 20px;
  cursor: pointer;
  transition: background 0.15s;
  border-bottom: 1px solid var(--border-light);
}

.task-list-item:last-child {
  border-bottom: none;
}

.task-list-item:hover {
  background: #f4fbf8;
}

.task-priority {
  width: 4px;
  height: 36px;
  border-radius: 2px;
  flex-shrink: 0;
}

.task-priority.high { background: var(--accent-danger); }
.task-priority.medium { background: var(--accent-warning); }
.task-priority.low { background: var(--accent-success); }

.task-info {
  flex: 1;
  min-width: 0;
}

.task-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.task-meta {
  font-size: 12px;
  color: var(--text-muted);
  display: flex;
  gap: 12px;
  align-items: center;
}

/* 空状态 */
.empty-state {
  padding: 40px 20px;
  min-height: 160px;
  display: grid;
  place-items: center;
  text-align: center;
}

.empty-icon {
  font-size: 40px;
  margin-bottom: 10px;
}

.empty-text {
  font-size: 14px;
  color: var(--text-muted);
}

/* 响应式 */
@media (max-width: 1024px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
  .dashboard-grid { grid-template-columns: 1fr; }
  .chart-body { grid-template-columns: 1fr; }
}

@media (max-width: 640px) {
  .dashboard-page { padding: 16px; }
  .stats-grid { grid-template-columns: 1fr; }
  .welcome-stats { gap: 20px; flex-wrap: wrap; }
}
</style>
