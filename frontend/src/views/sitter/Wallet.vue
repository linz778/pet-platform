<template>
  <div class="page-container">
    <el-card class="head-card">
      <div class="head">
        <div>
          <h2 class="title">💰 收益钱包</h2>
          <p class="subtitle">用户验收后佣金自动进入可提现余额，所有收支都保留流水。</p>
        </div>
        <div class="head-actions">
          <el-button :loading="loadingWallet" @click="loadWallet">刷新余额</el-button>
          <el-button type="primary" :disabled="Number(wallet.balance) <= 0" @click="openWithdraw">申请提现</el-button>
        </div>
      </div>
    </el-card>

    <div v-loading="loadingWallet" class="summary-grid">
      <el-card shadow="hover" class="summary-card balance-card">
        <span class="summary-label">可提现余额</span>
        <strong>¥{{ money(wallet.balance) }}</strong>
        <small>模拟通道即时到账</small>
      </el-card>
      <el-card shadow="hover" class="summary-card">
        <span class="summary-label">累计服务收入</span>
        <strong>¥{{ money(wallet.totalIncome) }}</strong>
        <small>提现不会减少累计收入</small>
      </el-card>
      <el-card shadow="hover" class="summary-card">
        <span class="summary-label">本人钱包冻结</span>
        <strong>¥{{ money(wallet.frozen) }}</strong>
        <small>未验收订单的担保资金仍在用户钱包</small>
      </el-card>
    </div>

    <el-card>
      <div class="table-head">
        <div>
          <h3>资金流水</h3>
          <p>佣金入账和提现记录可逐笔核对变动后余额。</p>
        </div>
        <el-select v-model="query.type" clearable placeholder="全部类型" class="type-filter" @change="onFilterChange">
          <el-option label="佣金入账" :value="3" />
          <el-option label="提现" :value="4" />
        </el-select>
      </div>

      <el-table v-loading="loadingTransactions" :data="transactions" stripe empty-text="暂无资金流水">
        <el-table-column prop="createTime" label="时间" width="172" />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag :type="row.type === 3 ? 'success' : 'info'" effect="plain">{{ row.typeDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="130" align="right">
          <template #default="{ row }">
            <span :class="['tx-amount', Number(row.amount) >= 0 ? 'income' : 'expense']">
              {{ signedMoney(row.amount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="变动后余额" width="130" align="right">
          <template #default="{ row }">¥{{ money(row.balanceAfter) }}</template>
        </el-table-column>
        <el-table-column label="关联订单" min-width="120">
          <template #default="{ row }">{{ row.orderId || '—' }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="说明" min-width="220" show-overflow-tooltip />
      </el-table>

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

    <el-dialog v-model="withdrawVisible" title="申请提现" width="420px">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="当前为演示环境，提现会即时从余额扣除，不会调用真实银行通道。"
      />
      <el-form label-width="90px" class="withdraw-form">
        <el-form-item label="可提现">
          <strong>¥{{ money(wallet.balance) }}</strong>
        </el-form-item>
        <el-form-item label="提现金额">
          <el-input-number
            v-model="withdrawAmount"
            :min="1"
            :max="Math.max(1, Number(wallet.balance || 0))"
            :precision="2"
            :step="100"
            controls-position="right"
            class="amount-input"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="withdrawVisible = false">取消</el-button>
        <el-button type="primary" :loading="withdrawing" @click="onWithdraw">确认提现</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyWallet, pageTransactions, withdraw } from '@/api/wallet'
import { money } from '@/utils/format'

const wallet = reactive({ balance: 0, frozen: 0, totalIncome: 0 })
const loadingWallet = ref(false)
const loadingTransactions = ref(false)
const transactions = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, type: undefined })

const withdrawVisible = ref(false)
const withdrawing = ref(false)
const withdrawAmount = ref(100)

async function loadWallet() {
  loadingWallet.value = true
  try {
    Object.assign(wallet, await getMyWallet())
  } catch {
    // 统一响应拦截器已提示
  } finally {
    loadingWallet.value = false
  }
}

async function loadTransactions() {
  loadingTransactions.value = true
  try {
    const res = await pageTransactions({
      page: query.page,
      size: query.size,
      type: query.type
    })
    transactions.value = res.records ?? []
    total.value = res.total ?? 0
  } catch {
    transactions.value = []
    total.value = 0
  } finally {
    loadingTransactions.value = false
  }
}

function onFilterChange() {
  query.page = 1
  loadTransactions()
}

function onPageChange(page) {
  query.page = page
  loadTransactions()
}

function signedMoney(value) {
  const amount = Number(value)
  if (!Number.isFinite(amount)) return '¥0.00'
  return `${amount >= 0 ? '+' : '-'}¥${money(Math.abs(amount))}`
}

function openWithdraw() {
  const balance = Number(wallet.balance)
  if (!Number.isFinite(balance) || balance <= 0) {
    ElMessage.warning('当前没有可提现余额')
    return
  }
  withdrawAmount.value = Math.min(100, balance)
  withdrawVisible.value = true
}

async function onWithdraw() {
  const amount = Number(withdrawAmount.value)
  const balance = Number(wallet.balance)
  if (!Number.isFinite(amount) || amount <= 0 || amount > balance) {
    ElMessage.warning('请输入不超过可用余额的提现金额')
    return
  }

  try {
    await ElMessageBox.confirm(`确认提现 ¥${money(amount)}？演示通道将即时处理。`, '确认提现', {
      type: 'warning',
      confirmButtonText: '确认提现',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  withdrawing.value = true
  try {
    Object.assign(wallet, await withdraw(amount))
    withdrawVisible.value = false
    ElMessage.success('提现成功')
    query.page = 1
    await loadTransactions()
  } catch {
    await loadWallet()
  } finally {
    withdrawing.value = false
  }
}

onMounted(() => Promise.all([loadWallet(), loadTransactions()]))
</script>

<style scoped>
.head-card {
  margin-bottom: 16px;
}

.head,
.head-actions,
.table-head {
  display: flex;
  align-items: center;
}

.head,
.table-head {
  justify-content: space-between;
  gap: 16px;
}

.head-actions {
  gap: 8px;
}

.title,
.table-head h3 {
  margin: 0 0 6px;
}

.title {
  font-size: 20px;
}

.subtitle,
.table-head p {
  margin: 0;
  font-size: 13px;
  color: var(--pp-muted);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.summary-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.summary-card strong {
  font-size: 28px;
  color: var(--pp-ink);
}

.balance-card strong {
  color: var(--pp-primary);
}

.summary-label,
.summary-card small {
  color: var(--pp-muted);
}

.summary-card small {
  line-height: 1.5;
}

.table-head {
  margin-bottom: 16px;
}

.type-filter {
  width: 150px;
}

.tx-amount {
  font-weight: 700;
}

.tx-amount.income {
  color: var(--el-color-success);
}

.tx-amount.expense {
  color: var(--el-color-danger);
}

.pager {
  margin-top: 16px;
  justify-content: center;
}

.withdraw-form {
  margin-top: 20px;
}

.amount-input {
  width: 100%;
}

@media (max-width: 760px) {
  .head,
  .table-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
