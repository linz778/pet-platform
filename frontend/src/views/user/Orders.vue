<template>
  <div class="page-container">
    <el-card class="head-card">
      <div class="head">
        <div class="head-text">
          <h2 class="title">📋 我的订单</h2>
          <p class="subtitle">支付后订单进入接单大厅等待抢单；接单员上门服务并提交存证后，由你验收放款。</p>
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
        <el-empty v-if="!loading && orders.length === 0" description="这个状态下还没有订单" />

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
              <span class="line-label">时间</span>
              <span>{{ o.serviceStart }}<template v-if="o.serviceEnd"> ~ {{ o.serviceEnd }}</template></span>
            </div>
            <div class="order-line">
              <span class="line-label">地址</span>
              <span>{{ o.serviceAddress }}</span>
            </div>
          </div>

          <footer class="order-foot">
            <span class="amount">¥{{ money(o.amount) }}</span>
            <div class="actions">
              <el-button link @click="openDetail(o.id)">详情</el-button>
              <el-button v-if="o.status === 0" type="primary" size="small" @click="onPay(o)">立即支付</el-button>
              <el-button v-if="o.status === 0 || o.status === 1" size="small" @click="onCancel(o)">取消订单</el-button>
              <el-button
                v-if="o.status === 4"
                type="success"
                size="small"
                :loading="acceptingId === o.id"
                @click="onAccept(o)"
              >
                确认验收
              </el-button>
            </div>
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
              <el-tag v-if="detail.petDeleted" size="small" type="info" effect="plain">档案已删除</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="上门时间">
              {{ detail.serviceStart }}<template v-if="detail.serviceEnd"> ~ {{ detail.serviceEnd }}</template>
            </el-descriptions-item>
            <el-descriptions-item label="服务地址">{{ detail.serviceAddress }}</el-descriptions-item>
            <el-descriptions-item label="坐标">{{ detail.addressLng }}, {{ detail.addressLat }}</el-descriptions-item>
            <el-descriptions-item v-if="detail.remark" label="备注">{{ detail.remark }}</el-descriptions-item>
            <el-descriptions-item label="订单金额">
              <span class="amount">¥{{ money(detail.amount) }}</span>
            </el-descriptions-item>
            <el-descriptions-item v-if="detail.cancelReason" label="取消原因">
              {{ detail.cancelReason }}
            </el-descriptions-item>
          </el-descriptions>

          <!-- 未发生的流程节点后端压根不返回该键（Jackson non_null），必须逐个 v-if 守卫，
               否则时间轴上会排出一串 undefined。createTime 一定有值，所以它不用守卫。 -->
          <h4 class="drawer-sub">履约进度</h4>
          <el-timeline>
            <el-timeline-item :timestamp="detail.createTime" type="primary">提交订单</el-timeline-item>
            <el-timeline-item v-if="detail.payTime" :timestamp="detail.payTime" type="primary">
              支付成功，资金进入平台担保
            </el-timeline-item>
            <el-timeline-item v-if="detail.takenTime" :timestamp="detail.takenTime" type="primary">
              接单员已接单
            </el-timeline-item>
            <el-timeline-item v-if="detail.checkinTime" :timestamp="detail.checkinTime" type="primary">
              接单员到达并打卡
            </el-timeline-item>
            <el-timeline-item v-if="detail.finishTime" :timestamp="detail.finishTime" type="primary">
              服务完成，等待验收
            </el-timeline-item>
            <el-timeline-item v-if="detail.acceptTime" :timestamp="detail.acceptTime" type="success">
              验收通过，已结算
            </el-timeline-item>
            <el-timeline-item v-if="detail.cancelTime" :timestamp="detail.cancelTime" type="danger">
              订单已取消
            </el-timeline-item>
          </el-timeline>

          <!-- 验收前用户必须能看到接单员留下的凭证。已取消的单不渲染：
               取消只可能发生在待支付/待接单，那时压根没有存证，只会多一个空态 -->
          <template v-if="[3, 4, 5].includes(detail.status)">
            <h4 class="drawer-sub">履约存证</h4>
            <EvidenceList :evidences="evidences" />
          </template>

          <div class="drawer-actions">
            <el-button v-if="detail.status === 0" type="primary" @click="onPay(detail)">立即支付</el-button>
            <el-button v-if="detail.status === 0 || detail.status === 1" @click="onCancel(detail)">取消订单</el-button>
            <el-button
              v-if="detail.status === 4"
              type="success"
              :loading="acceptingId === detail.id"
              @click="onAccept(detail)"
            >
              确认验收并结算
            </el-button>
          </div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { acceptOrder, cancelOrder, getOrder, getOrderEvidence, pageMyOrders, payOrder } from '@/api/order'
import EvidenceList from '@/components/EvidenceList.vue'
import { money } from '@/utils/format'

const TABS = { 0: '待支付', 1: '待接单', 2: '已接单', 3: '服务中', 4: '待验收', 5: '已完成', 6: '已取消' }
const STATUS_TAG = { 0: 'warning', 1: 'primary', 2: 'primary', 3: 'primary', 4: 'warning', 5: 'success', 6: 'info' }

const activeTab = ref('all')
const orders = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10 })

const detailVisible = ref(false)
const loadingDetail = ref(false)
const detail = ref(null)
const evidences = ref([])
const acceptingId = ref(null)

async function load() {
  loading.value = true
  try {
    const res = await pageMyOrders({
      page: query.page,
      size: query.size,
      status: activeTab.value === 'all' ? undefined : Number(activeTab.value)
    })
    orders.value = res.records ?? []
    total.value = res.total ?? 0
  } catch {
    // 错误提示已由 request.js 拦截器统一弹过，这里再弹一次就是双重提示
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
  evidences.value = []
  try {
    detail.value = await getOrder(id)
    evidences.value = [3, 4, 5].includes(detail.value.status)
      ? await getOrderEvidence(id).catch(() => [])
      : []
  } catch {
    detailVisible.value = false
  } finally {
    loadingDetail.value = false
  }
}

async function onPay(order) {
  try {
    await ElMessageBox.confirm(
      `确认支付 ¥${money(order.amount)}？支付后资金进入平台担保，验收通过才结算给接单员。`,
      '确认支付',
      { type: 'warning', confirmButtonText: '确认支付', cancelButtonText: '再想想' }
    )
  } catch {
    return
  }

  try {
    await payOrder(order.id)
    ElMessage.success('支付成功，订单已进入接单大厅')
    detailVisible.value = false
    await load()
  } catch {
    // 余额不足等原因拦截器已提示；仍刷新一次，让列表与服务端状态对齐
    await load()
  }
}

async function onCancel(order) {
  let reason
  try {
    const res = await ElMessageBox.prompt('取消后若已支付会全额退回余额。请填写取消原因（可留空）：', '取消订单', {
      confirmButtonText: '确认取消订单',
      cancelButtonText: '不取消了',
      inputType: 'textarea',
      inputPlaceholder: '例如：临时有事，改天再约',
      inputValidator: (v) => (v && v.length > 255 ? '取消原因不能超过 255 字' : true)
    })
    reason = res.value
  } catch {
    return
  }

  try {
    await cancelOrder(order.id, reason)
    ElMessage.success('订单已取消')
    detailVisible.value = false
    await load()
  } catch {
    await load()
  }
}

async function onAccept(order) {
  try {
    await ElMessageBox.confirm(
      `确认服务已经完成并通过验收？确认后 ¥${money(order.amount)} 担保资金将结算给接单员，操作不可撤销。`,
      '确认验收',
      { type: 'warning', confirmButtonText: '验收通过并结算', cancelButtonText: '继续检查' }
    )
  } catch {
    return
  }

  acceptingId.value = order.id
  try {
    await acceptOrder(order.id)
    ElMessage.success('验收完成，担保资金已结算')
    detailVisible.value = false
    await load()
  } catch {
    // 服务端用条件更新保证幂等；失败后刷新，避免页面保留过期状态
    await load()
  } finally {
    acceptingId.value = null
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

.amount {
  font-size: 18px;
  font-weight: 700;
  color: var(--pp-primary);
}

.actions {
  display: flex;
  align-items: center;
  gap: 8px;
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

.drawer-actions {
  display: flex;
  gap: 8px;
  margin-top: 20px;
}
</style>
