<template>
  <div class="dispatch-page">
    <header class="dispatch-hero">
      <div>
        <span>ORDER OPERATIONS</span>
        <h1>订单调度中心</h1>
        <p>查看全平台订单流转，并为长时间无人接取的订单人工指派接单员。</p>
      </div>
      <div class="hero-total"><strong>{{ total }}</strong><small>条匹配订单</small></div>
    </header>

    <section class="filter-panel">
      <el-select v-model="query.status" placeholder="全部订单状态" clearable @change="changeFilter">
        <el-option v-for="item in statuses" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-button :loading="loading" @click="loadOrders">刷新订单</el-button>
      <span>只有已支付且待接单的订单可以人工派单</span>
    </section>

    <section class="order-panel">
      <el-table v-loading="loading" :data="orders" empty-text="当前没有订单">
        <el-table-column prop="orderNo" label="订单号" min-width="190" />
        <el-table-column label="客户 / 服务" min-width="170">
          <template #default="{ row }">
            <strong>{{ row.ownerNickname || '宠物主人' }}</strong>
            <small>{{ row.categoryName }} · {{ row.petName || '宠物' }}</small>
          </template>
        </el-table-column>
        <el-table-column label="预约信息" min-width="260">
          <template #default="{ row }">
            <span>{{ row.serviceStart }}</span>
            <small class="address">{{ row.serviceAddress }}</small>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="105">
          <template #default="{ row }"><strong class="amount">¥{{ money(row.amount) }}</strong></template>
        </el-table-column>
        <el-table-column label="状态" width="105">
          <template #default="{ row }"><el-tag :type="statusType(row.status)" effect="light">{{ row.statusText }}</el-tag></template>
        </el-table-column>
        <el-table-column label="接单员" min-width="130">
          <template #default="{ row }">{{ row.sitterName || '尚未接单' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="115" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 1" type="primary" @click="openAssign(row)">人工派单</el-button>
            <el-button v-else link @click="showDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total"
        layout="total, prev, pager, next"
        :total="total"
        :page-size="query.size"
        :current-page="query.page"
        @current-change="changePage"
      />
    </section>

    <el-dialog v-model="assignVisible" title="选择接单员" width="620px" destroy-on-close>
      <div v-if="currentOrder" class="order-brief">
        <strong>{{ currentOrder.categoryName }} · {{ currentOrder.petName }}</strong>
        <span>{{ currentOrder.serviceStart }}</span>
        <small>{{ currentOrder.serviceAddress }}</small>
      </div>
      <div v-loading="sitterLoading" class="sitter-list">
        <el-empty v-if="!sitterLoading && !sitters.length" description="暂无符合条件的接单员" />
        <label v-for="item in sitters" :key="item.userId" class="sitter-card" :class="{ selected: selectedSitter === item.userId }">
          <input v-model="selectedSitter" type="radio" name="sitter" :value="item.userId">
          <span class="avatar">{{ item.displayName?.slice(0, 1) }}</span>
          <span><strong>{{ item.displayName }}</strong><small>{{ item.realName }}</small></span>
          <span><strong>{{ item.creditScore }} 分</strong><small>信誉 {{ item.creditLevel }} 级</small></span>
          <span class="distance">{{ item.distanceKm == null ? '位置未设置' : `${item.distanceKm} km` }}</span>
        </label>
      </div>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!selectedSitter" :loading="assigning" @click="submitAssign">确认派单</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="订单概要" width="560px">
      <el-descriptions v-if="currentOrder" :column="1" border>
        <el-descriptions-item label="订单号">{{ currentOrder.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="服务">{{ currentOrder.categoryName }} / {{ currentOrder.petName }}</el-descriptions-item>
        <el-descriptions-item label="预约时间">{{ currentOrder.serviceStart }}</el-descriptions-item>
        <el-descriptions-item label="服务地址">{{ currentOrder.serviceAddress }}</el-descriptions-item>
        <el-descriptions-item label="当前状态">{{ currentOrder.statusText }}</el-descriptions-item>
        <el-descriptions-item label="接单员">{{ currentOrder.sitterName || '尚未接单' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { assignOrder, listAssignableSitters, pageDispatchOrders } from '@/api/adminDispatch'
import { money } from '@/utils/format'

const statuses = [
  { value: 0, label: '待支付' }, { value: 1, label: '待接单' },
  { value: 2, label: '已接单' }, { value: 3, label: '服务中' },
  { value: 4, label: '待验收' }, { value: 5, label: '已完成' },
  { value: 6, label: '已取消' }, { value: 7, label: '仲裁中' }
]
const query = reactive({ page: 1, size: 10, status: null })
const orders = ref([])
const total = ref(0)
const loading = ref(false)
const assignVisible = ref(false)
const detailVisible = ref(false)
const sitterLoading = ref(false)
const assigning = ref(false)
const currentOrder = ref(null)
const sitters = ref([])
const selectedSitter = ref(null)

async function loadOrders() {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.status != null) params.status = query.status
    const data = await pageDispatchOrders(params)
    orders.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function changeFilter() {
  query.page = 1
  loadOrders()
}

function changePage(page) {
  query.page = page
  loadOrders()
}

function showDetail(order) {
  currentOrder.value = order
  detailVisible.value = true
}

async function openAssign(order) {
  currentOrder.value = order
  selectedSitter.value = null
  sitters.value = []
  assignVisible.value = true
  sitterLoading.value = true
  try {
    sitters.value = await listAssignableSitters(order.id)
  } finally {
    sitterLoading.value = false
  }
}

async function submitAssign() {
  const sitter = sitters.value.find((item) => item.userId === selectedSitter.value)
  const confirmed = await ElMessageBox.confirm(
    `确认将该订单指派给「${sitter?.displayName}」吗？`,
    '确认人工派单',
    { type: 'warning', confirmButtonText: '确认派单', cancelButtonText: '取消' }
  ).catch(() => false)
  if (!confirmed) return
  assigning.value = true
  try {
    await assignOrder(currentOrder.value.id, selectedSitter.value)
    assignVisible.value = false
    await loadOrders()
    ElMessage.success('派单成功，订单已同步到接单员端')
  } finally {
    assigning.value = false
  }
}

function statusType(status) {
  if (status === 5) return 'success'
  if ([0, 1, 4, 7].includes(status)) return 'warning'
  if (status === 6) return 'danger'
  return 'primary'
}

onMounted(loadOrders)
</script>

<style scoped>
.dispatch-page { min-height: calc(100vh - 100px); color: #24332a; }
.dispatch-hero { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; padding: 25px 30px; border-radius: 20px; background: linear-gradient(120deg, #17382b, #477759 70%, #71966b); color: #fff; box-shadow: 0 14px 32px rgb(35 69 49 / 17%); }
.dispatch-hero > div > span { color: #bdd8c3; font-size: 10px; font-weight: 700; letter-spacing: 2px; }
.dispatch-hero h1 { margin: 6px 0 4px; font-size: 27px; }
.dispatch-hero p { margin: 0; color: rgb(255 255 255 / 70%); font-size: 12px; }
.hero-total { display: flex; align-items: center; flex-direction: column; min-width: 92px; padding: 12px 18px; border-radius: 15px; background: rgb(255 255 255 / 12%); }
.hero-total strong { font-size: 27px; }
.hero-total small { color: rgb(255 255 255 / 70%); }
.filter-panel { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; padding: 14px 17px; border: 1px solid #e4ebe6; border-radius: 15px; background: #fff; }
.filter-panel .el-select { width: 180px; }
.filter-panel > span { margin-left: auto; color: #8a958e; font-size: 11px; }
.order-panel { padding: 8px 18px 18px; border: 1px solid #e4ebe6; border-radius: 18px; background: #fff; box-shadow: 0 8px 25px rgb(40 68 51 / 6%); }
.order-panel :deep(.el-table) { --el-table-header-bg-color: #f5f8f6; --el-table-row-hover-bg-color: #f7fbf8; }
.order-panel strong, .order-panel small, .order-panel span { display: block; }
.order-panel small { margin-top: 3px; color: #87938b; font-size: 10px; }
.order-panel .address { overflow: hidden; max-width: 330px; text-overflow: ellipsis; white-space: nowrap; }
.amount { color: #477b57; }
.order-panel :deep(.el-pagination) { justify-content: center; margin-top: 18px; }
.order-brief { display: flex; flex-direction: column; gap: 4px; margin-bottom: 12px; padding: 13px 15px; border-radius: 12px; background: #f3f8f4; }
.order-brief span, .order-brief small { color: #718078; font-size: 11px; }
.sitter-list { min-height: 150px; max-height: 410px; overflow-y: auto; }
.sitter-card { display: grid; align-items: center; grid-template-columns: auto 42px 1fr 95px 90px; gap: 10px; margin-bottom: 8px; padding: 11px 13px; border: 1px solid #e4ebe6; border-radius: 13px; cursor: pointer; transition: border-color .2s, background .2s; }
.sitter-card.selected { border-color: #5c8d6b; background: #f1f8f3; }
.sitter-card input { accent-color: #4f8760; }
.avatar { display: grid; height: 38px; place-items: center; border-radius: 11px; background: #dfeee3; color: #477b57; font-weight: 700; }
.sitter-card > span { display: flex; flex-direction: column; gap: 2px; }
.sitter-card small { color: #8a958e; font-size: 10px; }
.sitter-card .distance { color: #527e60; font-size: 11px; text-align: right; }
@media (max-width: 700px) { .dispatch-hero { align-items: flex-start; flex-direction: column; gap: 18px; } .filter-panel { align-items: stretch; flex-direction: column; } .filter-panel > span { margin-left: 0; } .sitter-card { grid-template-columns: auto 38px 1fr; } .sitter-card > span:nth-last-child(-n+2) { display: none; } }
</style>
