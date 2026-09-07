<template>
  <div v-loading="loading" class="dashboard-page">
    <header class="dashboard-hero">
      <div>
        <span class="eyebrow">OPERATIONS OVERVIEW</span>
        <h1>数据分析看板</h1>
        <p>掌握平台经营、订单履约与服务供给情况，数据来自当前业务数据库。</p>
      </div>
      <div class="hero-actions">
        <el-radio-group v-model="days" size="small" @change="loadDashboard">
          <el-radio-button :value="7">近 7 天</el-radio-button>
          <el-radio-button :value="14">近 14 天</el-radio-button>
          <el-radio-button :value="30">近 30 天</el-radio-button>
        </el-radio-group>
        <el-button :loading="loading" @click="loadDashboard">刷新数据</el-button>
        <small>更新于 {{ dashboard.generatedAt || '--' }}</small>
      </div>
      <div class="hero-orbit" aria-hidden="true"><span>🐾</span><span>✦</span><span>🐾</span></div>
    </header>

    <section class="metric-grid">
      <article v-for="metric in metrics" :key="metric.label" class="metric-card" :class="metric.tone">
        <div class="metric-icon">{{ metric.icon }}</div>
        <div class="metric-main">
          <span>{{ metric.label }}</span>
          <strong>{{ metric.value }}</strong>
          <small>{{ metric.note }}</small>
        </div>
      </article>
    </section>

    <section class="chart-grid">
      <article class="panel trend-panel">
        <div class="panel-head">
          <div><span>经营趋势</span><h2>订单量与成交额</h2></div>
          <div class="legend"><span class="orders">订单量</span><span class="turnover">成交额</span></div>
        </div>
        <div ref="trendChartEl" class="chart trend-chart" />
      </article>

      <article class="panel">
        <div class="panel-head">
          <div><span>服务偏好</span><h2>热门服务占比</h2></div>
        </div>
        <div ref="categoryChartEl" class="chart donut-chart" />
      </article>
    </section>

    <section class="lower-grid">
      <article class="panel status-panel">
        <div class="panel-head">
          <div><span>履约健康度</span><h2>订单状态分布</h2></div>
          <strong class="completion-rate">{{ overview.completionRate || 0 }}%</strong>
        </div>
        <div ref="statusChartEl" class="chart status-chart" />
      </article>

      <article class="panel todo-panel">
        <div class="panel-head">
          <div><span>运营待办</span><h2>需要你的关注</h2></div>
        </div>
        <button type="button" class="todo-item audit" @click="router.push('/admin/audit')">
          <span class="todo-icon">🪪</span>
          <span><strong>{{ overview.pendingAuditCount || 0 }} 项资质待审核</strong><small>核验接单员身份与服务资质</small></span>
          <b>→</b>
        </button>
        <button type="button" class="todo-item arbitration" @click="router.push('/admin/arbitration')">
          <span class="todo-icon">⚖️</span>
          <span><strong>{{ overview.pendingArbitrationCount || 0 }} 项纠纷待处理</strong><small>查看双方说明与证据并作出裁定</small></span>
          <b>→</b>
        </button>
        <div class="supply-row">
          <span>在线服务供给</span>
          <strong>{{ overview.activeSitterCount || 0 }} / {{ overview.approvedSitterCount || 0 }}</strong>
          <el-progress :percentage="activeSitterRate" :stroke-width="8" :show-text="false" />
        </div>
      </article>
    </section>

    <article class="panel recent-panel">
      <div class="panel-head">
        <div><span>实时业务</span><h2>最近订单动态</h2></div>
        <el-button link type="primary" @click="router.push('/admin/dispatch')">进入订单调度 →</el-button>
      </div>
      <el-table :data="dashboard.recentOrders || []" stripe class="recent-table" empty-text="暂时没有订单">
        <el-table-column prop="orderNo" label="订单号" min-width="190" />
        <el-table-column label="服务与宠物" min-width="180">
          <template #default="{ row }"><strong>{{ row.categoryName }}</strong><span class="pet-name">{{ row.petName }}</span></template>
        </el-table-column>
        <el-table-column label="订单金额" width="130"><template #default="{ row }">¥{{ money(row.amount) }}</template></el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }"><el-tag :type="statusTagType(row.status)" effect="light">{{ row.statusText }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" min-width="170" />
      </el-table>
    </article>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { init, use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { getAdminDashboard } from '@/api/adminDashboard'
import { money } from '@/utils/format'

use([BarChart, LineChart, PieChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const router = useRouter()
const loading = ref(false)
const days = ref(7)
const dashboard = reactive({ overview: {}, dailyTrend: [], categoryMetrics: [], statusMetrics: [], recentOrders: [] })
const overview = computed(() => dashboard.overview || {})

const metrics = computed(() => [
  { label: '累计成交额', value: `¥${money(overview.value.settledAmount)}`, note: '已验收订单总额', icon: '¥', tone: 'green' },
  { label: '平台佣金收入', value: `¥${money(overview.value.platformRevenue)}`, note: '已结算平台收入', icon: '↗', tone: 'gold' },
  { label: '今日新增订单', value: overview.value.todayOrderCount || 0, note: `累计 ${overview.value.totalOrderCount || 0} 单`, icon: '📋', tone: 'blue' },
  { label: '宠物主人', value: overview.value.ownerCount || 0, note: '平台注册用户', icon: '👤', tone: 'purple' },
  { label: '认证接单员', value: overview.value.approvedSitterCount || 0, note: `${overview.value.activeSitterCount || 0} 人当前可接单`, icon: '🐾', tone: 'mint' },
  { label: '订单完成率', value: `${overview.value.completionRate || 0}%`, note: '累计已完成 / 全部订单', icon: '✓', tone: 'coral' }
])

const activeSitterRate = computed(() => {
  const total = Number(overview.value.approvedSitterCount || 0)
  return total ? Math.round(Number(overview.value.activeSitterCount || 0) * 100 / total) : 0
})

const trendChartEl = ref(null)
const categoryChartEl = ref(null)
const statusChartEl = ref(null)
let trendChart
let categoryChart
let statusChart

async function loadDashboard() {
  loading.value = true
  try {
    const data = await getAdminDashboard(days.value)
    Object.assign(dashboard, data || {})
    await nextTick()
    renderCharts()
  } finally {
    loading.value = false
  }
}

function renderCharts() {
  trendChart ||= init(trendChartEl.value)
  categoryChart ||= init(categoryChartEl.value)
  statusChart ||= init(statusChartEl.value)
  const dates = dashboard.dailyTrend.map((item) => item.statDate.slice(5).replace('-', '/'))

  trendChart.setOption({
    animationDuration: 650,
    color: ['#5c8e6b', '#e2a74c'],
    grid: { left: 42, right: 52, top: 24, bottom: 32 },
    tooltip: { trigger: 'axis', backgroundColor: '#1e2d25', borderWidth: 0, textStyle: { color: '#fff' } },
    xAxis: { type: 'category', data: dates, axisLine: { lineStyle: { color: '#dce5df' } }, axisTick: { show: false }, axisLabel: { color: '#8a968f' } },
    yAxis: [
      { type: 'value', minInterval: 1, axisLabel: { color: '#8a968f' }, splitLine: { lineStyle: { color: '#edf2ee' } } },
      { type: 'value', axisLabel: { color: '#8a968f', formatter: '¥{value}' }, splitLine: { show: false } }
    ],
    series: [
      { name: '订单量', type: 'bar', barMaxWidth: 24, data: dashboard.dailyTrend.map((item) => item.orderCount), itemStyle: { borderRadius: [6, 6, 0, 0] } },
      { name: '成交额', type: 'line', yAxisIndex: 1, smooth: true, symbol: 'circle', symbolSize: 7, data: dashboard.dailyTrend.map((item) => Number(item.turnover)), lineStyle: { width: 3 }, areaStyle: { opacity: 0.08 } }
    ]
  }, true)

  const categories = dashboard.categoryMetrics.length
    ? dashboard.categoryMetrics.map((item) => ({ value: item.orderCount, name: item.categoryName }))
    : [{ value: 1, name: '暂无订单', itemStyle: { color: '#edf2ee' } }]
  categoryChart.setOption({
    color: ['#5c8e6b', '#efb858', '#74a8c7', '#a88bc1', '#e98972'],
    tooltip: { trigger: 'item', formatter: '{b}<br/>{c} 单 · {d}%' },
    legend: { bottom: 0, icon: 'circle', itemWidth: 8, textStyle: { color: '#66736b' } },
    series: [{ type: 'pie', radius: ['48%', '70%'], center: ['50%', '43%'], avoidLabelOverlap: true, itemStyle: { borderColor: '#fff', borderWidth: 4, borderRadius: 8 }, label: { formatter: '{d}%', color: '#48554d' }, data: categories }]
  }, true)

  statusChart.setOption({
    color: ['#6e9b78'],
    grid: { left: 72, right: 28, top: 10, bottom: 24 },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#edf2ee' } }, axisLabel: { color: '#8a968f' } },
    yAxis: { type: 'category', data: dashboard.statusMetrics.map((item) => item.statusText), axisTick: { show: false }, axisLine: { show: false }, axisLabel: { color: '#536159' } },
    series: [{ type: 'bar', barMaxWidth: 15, data: dashboard.statusMetrics.map((item) => item.orderCount), itemStyle: { borderRadius: 8, color: (params) => ['#9ab8a2', '#72a080', '#e8ba64', '#77a6bf', '#a78dbe', '#4f8760', '#de8571', '#dcaa58'][params.dataIndex % 8] } }]
  }, true)
}

function resizeCharts() {
  trendChart?.resize()
  categoryChart?.resize()
  statusChart?.resize()
}

function statusTagType(status) {
  if (status === 5) return 'success'
  if (status === 6) return 'danger'
  if ([3, 4].includes(status)) return 'warning'
  return 'info'
}

onMounted(() => {
  loadDashboard()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  trendChart?.dispose()
  categoryChart?.dispose()
  statusChart?.dispose()
})
</script>

<style scoped>
.dashboard-page { --dash-green: #4f8760; min-height: calc(100vh - 100px); color: #203128; }
.dashboard-hero { position: relative; overflow: hidden; display: flex; align-items: center; justify-content: space-between; gap: 24px; margin-bottom: 18px; padding: 26px 30px; border-radius: 20px; background: linear-gradient(120deg, #17382b, #3f7354 65%, #769b6b); color: #fff; box-shadow: 0 16px 38px rgb(31 68 48 / 18%); }
.eyebrow { color: #b9d8bf; font-size: 10px; font-weight: 700; letter-spacing: 2px; }
.dashboard-hero h1 { margin: 6px 0 4px; font-size: 27px; }
.dashboard-hero p { margin: 0; color: rgb(255 255 255 / 72%); font-size: 12px; }
.hero-actions { z-index: 1; display: flex; align-items: center; justify-content: flex-end; gap: 10px; flex-wrap: wrap; }
.hero-actions small { width: 100%; color: rgb(255 255 255 / 58%); text-align: right; font-size: 10px; }
.hero-actions :deep(.el-radio-button__inner), .hero-actions :deep(.el-button) { border-color: rgb(255 255 255 / 20%); background: rgb(255 255 255 / 10%); color: #fff; box-shadow: none; }
.hero-actions :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) { border-color: #fff; background: #fff; color: #37684a; }
.hero-orbit { position: absolute; inset: auto 33% -50px auto; opacity: 0.08; transform: rotate(-12deg); }
.hero-orbit span { display: inline-block; margin: 10px; font-size: 70px; }

.metric-grid { display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); gap: 12px; margin-bottom: 16px; }
.metric-card { display: flex; min-width: 0; align-items: center; gap: 12px; padding: 17px 16px; border: 1px solid #e7ede9; border-radius: 16px; background: #fff; box-shadow: 0 7px 20px rgb(41 69 53 / 6%); }
.metric-icon { display: grid; flex: 0 0 38px; height: 38px; place-items: center; border-radius: 12px; font-size: 17px; font-weight: 700; }
.metric-main { display: flex; min-width: 0; flex-direction: column; gap: 2px; }
.metric-main > span { color: #7a877f; font-size: 11px; }
.metric-main strong { overflow: hidden; font-size: clamp(17px, 1.5vw, 23px); text-overflow: ellipsis; white-space: nowrap; }
.metric-main small { color: #9aa49e; font-size: 9px; }
.metric-card.green .metric-icon, .metric-card.mint .metric-icon { background: #e8f4eb; color: #4f8760; }
.metric-card.gold .metric-icon { background: #fff4dc; color: #c78a29; }
.metric-card.blue .metric-icon { background: #e9f3f8; color: #568aa9; }
.metric-card.purple .metric-icon { background: #f1eaf7; color: #8a6aa3; }
.metric-card.coral .metric-icon { background: #fbece8; color: #cf725d; }

.chart-grid, .lower-grid { display: grid; gap: 16px; margin-bottom: 16px; }
.chart-grid { grid-template-columns: minmax(0, 1.75fr) minmax(300px, 0.75fr); }
.lower-grid { grid-template-columns: minmax(0, 1.25fr) minmax(330px, 0.75fr); }
.panel { min-width: 0; padding: 20px 22px; border: 1px solid #e5ece7; border-radius: 18px; background: #fff; box-shadow: 0 8px 24px rgb(38 65 49 / 6%); }
.panel-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 8px; }
.panel-head span { color: var(--dash-green); font-size: 10px; font-weight: 700; letter-spacing: 0.6px; }
.panel-head h2 { margin: 4px 0 0; font-size: 17px; }
.legend { display: flex; gap: 16px; padding-top: 8px; }
.legend span::before { display: inline-block; width: 8px; height: 8px; margin-right: 5px; border-radius: 50%; content: ''; }
.legend .orders::before { background: #5c8e6b; }
.legend .turnover::before { background: #e2a74c; }
.chart { width: 100%; height: 290px; }
.donut-chart { height: 290px; }
.status-chart { height: 230px; }
.completion-rate { color: var(--dash-green); font-size: 22px; }

.todo-panel { display: flex; flex-direction: column; }
.todo-item { display: flex; align-items: center; gap: 11px; width: 100%; margin-top: 10px; padding: 13px; border: 1px solid #edf1ee; border-radius: 13px; background: #fafcfb; color: inherit; cursor: pointer; font: inherit; text-align: left; transition: transform .2s, border-color .2s; }
.todo-item:hover { border-color: #9db9a5; transform: translateX(3px); }
.todo-icon { display: grid; flex: 0 0 38px; height: 38px; place-items: center; border-radius: 10px; background: #edf6ef; font-size: 19px; }
.todo-item > span:nth-child(2) { display: flex; min-width: 0; flex: 1; flex-direction: column; gap: 3px; }
.todo-item strong { font-size: 13px; }
.todo-item small { color: #879188; font-size: 10px; }
.todo-item b { color: var(--dash-green); }
.supply-row { display: grid; grid-template-columns: 1fr auto; gap: 8px 16px; margin-top: auto; padding-top: 18px; color: #77847b; font-size: 11px; }
.supply-row strong { color: #31433a; }
.supply-row :deep(.el-progress) { grid-column: 1 / -1; }
.recent-panel { margin-bottom: 20px; }
.recent-table { margin-top: 8px; }
.recent-table strong, .recent-table .pet-name { display: block; }
.pet-name { margin-top: 2px; color: #87938b; font-size: 10px; }

@media (max-width: 1400px) { .metric-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); } }
@media (max-width: 980px) { .chart-grid, .lower-grid { grid-template-columns: 1fr; } .dashboard-hero { align-items: flex-start; flex-direction: column; } .hero-actions { justify-content: flex-start; } .hero-actions small { text-align: left; } }
@media (max-width: 680px) { .metric-grid { grid-template-columns: 1fr 1fr; } .dashboard-hero { padding: 22px 18px; } .panel { padding: 17px 14px; } .legend { display: none; } }
</style>
