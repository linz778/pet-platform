<template>
  <div class="page-container">
    <el-card class="head-card">
      <div class="head">
        <div class="head-text">
          <h2 class="title">🎒 我的接单</h2>
          <p class="subtitle">抢单成功后的订单都在这里。上门完成服务并由用户验收后，到手金额才会结算进收益钱包。</p>
        </div>
        <el-button @click="load">刷新</el-button>
      </div>
    </el-card>

    <el-card>
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane v-for="(text, code) in TABS" :key="code" :label="text" :name="String(code)" />
      </el-tabs>

      <div v-loading="loading" class="list-wrap">
        <el-empty v-if="!loading && orders.length === 0" description="这个状态下还没有订单">
          <el-button type="primary" @click="router.push('/sitter/hall')">去接单大厅看看</el-button>
        </el-empty>

        <article v-for="o in orders" :key="o.id" class="order-card">
          <header class="order-top">
            <span class="order-no">{{ o.orderNo }}</span>
            <el-tag :type="STATUS_TAG[o.status] ?? 'info'" effect="light" size="small">
              {{ o.statusText || '未知状态' }}
            </el-tag>
          </header>

          <div class="order-body">
            <div class="order-line">
              <span class="line-label">服务</span>
              <span>{{ o.categoryName || '未知服务' }}<em v-if="o.unit" class="unit">/ {{ o.unit }}</em></span>
            </div>
            <div class="order-line">
              <span class="line-label">宠物</span>
              <span>
                {{ o.petName || '未知宠物' }}
                <el-tag v-if="o.petDeleted" size="small" type="info" effect="plain">档案已删除</el-tag>
              </span>
            </div>
            <div class="order-line">
              <span class="line-label">雇主</span>
              <span>{{ o.ownerNickname || '未知用户' }}</span>
            </div>
            <div class="order-line">
              <span class="line-label">时间</span>
              <span>{{ o.serviceStart }}<template v-if="o.serviceEnd"> ~ {{ o.serviceEnd }}</template></span>
            </div>
            <div class="order-line">
              <span class="line-label">地址</span>
              <span>{{ o.serviceAddress }}</span>
            </div>
          </div>

          <footer class="order-foot">
            <div class="income">
              <span class="income-label">到手</span>
              <span class="amount">¥{{ money(o.sitterIncome) }}</span>
            </div>
            <el-button link @click="openDetail(o.id)">详情</el-button>
          </footer>
        </article>
      </div>

      <el-pagination
        v-if="total > 0"
        class="pager"
        layout="total, prev, pager, next"
        :total="total"
        :current-page="query.page"
        :page-size="query.size"
        @current-change="onPageChange"
      />
    </el-card>

    <el-drawer v-model="detailVisible" title="订单详情" size="480px">
      <div v-loading="loadingDetail">
        <template v-if="detail">
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="STATUS_TAG[detail.status] ?? 'info'" effect="light" size="small">
                {{ detail.statusText || '未知状态' }}
              </el-tag>
              <span class="pay-state">{{ detail.payStatusText }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="服务">{{ detail.categoryName || '未知服务' }}</el-descriptions-item>
            <el-descriptions-item label="宠物">
              {{ detail.petName || '未知宠物' }}
              <span v-if="detail.petSpecies" class="pay-state">{{ detail.petSpecies }} · {{ detail.petBreed || '未填品种' }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="上门时间">
              {{ detail.serviceStart }}<template v-if="detail.serviceEnd"> ~ {{ detail.serviceEnd }}</template>
            </el-descriptions-item>
            <el-descriptions-item label="服务地址">{{ detail.serviceAddress }}</el-descriptions-item>
            <el-descriptions-item label="坐标">{{ detail.addressLng }}, {{ detail.addressLat }}</el-descriptions-item>
            <!-- 备注里通常是门禁密码、猫砂位置这类履约必需信息，抢到单之后才对接单员开放 -->
            <el-descriptions-item v-if="detail.remark" label="雇主备注">{{ detail.remark }}</el-descriptions-item>
            <el-descriptions-item label="订单金额">¥{{ money(detail.amount) }}</el-descriptions-item>
            <el-descriptions-item label="我的到手">
              <span class="amount">¥{{ money(detail.sitterIncome) }}</span>
              <span class="pay-state">平台抽成 ¥{{ money(detail.commission) }}</span>
            </el-descriptions-item>
          </el-descriptions>

          <!-- 未发生的流程节点后端压根不返回该键（Jackson non_null），必须逐个 v-if 守卫 -->
          <h4 class="drawer-sub">履约进度</h4>
          <el-timeline>
            <el-timeline-item :timestamp="detail.createTime" type="primary">用户提交订单</el-timeline-item>
            <el-timeline-item v-if="detail.payTime" :timestamp="detail.payTime" type="primary">
              用户支付，资金进入平台担保
            </el-timeline-item>
            <el-timeline-item v-if="detail.takenTime" :timestamp="detail.takenTime" type="primary">
              我已接单
            </el-timeline-item>
            <el-timeline-item v-if="detail.checkinTime" :timestamp="detail.checkinTime" type="primary">
              到达并定位打卡
            </el-timeline-item>
            <el-timeline-item v-if="detail.finishTime" :timestamp="detail.finishTime" type="primary">
              服务完成，等待用户验收
            </el-timeline-item>
            <el-timeline-item v-if="detail.acceptTime" :timestamp="detail.acceptTime" type="success">
              验收通过，已结算到我的钱包
            </el-timeline-item>
            <el-timeline-item v-if="detail.cancelTime" :timestamp="detail.cancelTime" type="danger">
              订单已取消
            </el-timeline-item>
          </el-timeline>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getOrder } from '@/api/order'
import { pageMyTakenOrders } from '@/api/sitter'
import { money } from '@/utils/format'

const router = useRouter()

// 抢单之后的状态才可能出现在这里，所以不含「待支付 / 待接单」；已取消的单主人取消时我还没抢到
const TABS = { 2: '已接单', 3: '服务中', 4: '待验收', 5: '已完成' }
const STATUS_TAG = { 2: 'primary', 3: 'primary', 4: 'warning', 5: 'success', 6: 'info' }

const activeTab = ref('all')
const orders = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10 })

const detailVisible = ref(false)
const loadingDetail = ref(false)
const detail = ref(null)

async function load() {
  loading.value = true
  try {
    const res = await pageMyTakenOrders({
      page: query.page,
      size: query.size,
      status: activeTab.value === 'all' ? undefined : Number(activeTab.value)
    })
    orders.value = res.records ?? []
    total.value = res.total ?? 0
  } catch {
    // 错误提示已由 request.js 拦截器统一弹过
  } finally {
    loading.value = false
  }
}

function onTabChange() {
  query.page = 1
  load()
}

function onPageChange(page) {
  query.page = page
  load()
}

async function openDetail(id) {
  detailVisible.value = true
  loadingDetail.value = true
  detail.value = null
  try {
    detail.value = await getOrder(id)
  } catch {
    detailVisible.value = false
  } finally {
    loadingDetail.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.head-card {
  margin-bottom: 16px;
}

.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.title {
  margin: 0 0 6px;
  font-size: 20px;
}

.subtitle {
  margin: 0;
  font-size: 13px;
  color: var(--pp-muted);
}

.list-wrap {
  min-height: 220px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-card {
  padding: 16px;
  border: 1px solid var(--pp-tint-2);
  border-radius: var(--pp-radius);
  background: #fff;
  transition: box-shadow 0.2s ease;
}

.order-card:hover {
  box-shadow: var(--pp-shadow);
}

.order-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 10px;
  border-bottom: 1px dashed var(--pp-tint-2);
}

.order-no {
  font-family: monospace;
  font-size: 13px;
  color: var(--pp-muted);
}

.order-body {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 8px 24px;
  padding: 12px 0;
}

.order-line {
  display: flex;
  gap: 8px;
  font-size: 13px;
  align-items: baseline;
}

.line-label {
  flex: 0 0 34px;
  color: var(--pp-muted);
}

.unit {
  font-style: normal;
  color: var(--pp-muted);
  margin-left: 6px;
  font-size: 12px;
}

.order-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 10px;
  border-top: 1px dashed var(--pp-tint-2);
}

.income {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.income-label {
  font-size: 12px;
  color: var(--pp-muted);
}

.amount {
  font-size: 18px;
  font-weight: 700;
  color: var(--pp-primary);
}

.pager {
  margin-top: 16px;
  justify-content: center;
}

.drawer-sub {
  margin: 20px 0 12px;
  font-size: 14px;
}

.pay-state {
  margin-left: 8px;
  font-size: 12px;
  color: var(--pp-muted);
}
</style>
