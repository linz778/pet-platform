<template>
  <div class="arbitration-page">
    <el-card class="head-card">
      <div class="head">
        <div>
          <h2>纠纷仲裁</h2>
          <p>核对用户申诉证据与接单员履约存证，裁定通过后担保款将全额退还用户。</p>
        </div>
        <el-button @click="load">刷新</el-button>
      </div>
    </el-card>

    <el-card>
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane label="待审核" name="0" />
        <el-tab-pane label="已处理" name="2" />
      </el-tabs>

      <div v-loading="loading" class="case-list">
        <el-empty v-if="!loading && cases.length === 0" description="暂无申诉记录" />
        <article v-for="item in cases" :key="item.id" class="case-card">
          <header>
            <div>
              <span class="order-no">{{ item.orderNo }}</span>
              <span class="service-name">{{ item.categoryName || '未知服务' }}</span>
            </div>
            <el-tag :type="statusTag(item)">{{ decisionText(item) }}</el-tag>
          </header>
          <div class="case-grid">
            <p><span>申诉用户</span>{{ item.complainantName || '未知用户' }}</p>
            <p><span>接单员</span>{{ item.sitterName || '未知接单员' }}</p>
            <p><span>订单金额</span><strong>¥{{ money(item.orderAmount) }}</strong></p>
            <p><span>提交时间</span>{{ item.createTime }}</p>
          </div>
          <p class="reason"><span>申诉原因</span>{{ item.reason }}</p>
          <footer>
            <span v-if="item.approved" class="refund">已退款 ¥{{ money(item.refundAmount) }}</span>
            <span v-else-if="item.approved === false" class="rejected">申诉已驳回</span>
            <span v-else class="pending">等待平台审核，订单资金仍在担保中</span>
            <el-button :type="item.status === 0 ? 'danger' : 'primary'" plain @click="openCase(item)">
              {{ item.status === 0 ? '审核处理' : '查看裁定' }}
            </el-button>
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

    <el-drawer v-model="drawerVisible" title="申诉审核详情" size="640px">
      <div v-if="current" v-loading="loadingDetail">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="订单号">{{ current.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="服务">{{ current.categoryName || '未知服务' }}</el-descriptions-item>
          <el-descriptions-item label="申诉用户">{{ current.complainantName || '未知用户' }}</el-descriptions-item>
          <el-descriptions-item label="接单员">{{ current.sitterName || '未知接单员' }}</el-descriptions-item>
          <el-descriptions-item label="订单金额">¥{{ money(current.orderAmount) }}</el-descriptions-item>
          <el-descriptions-item label="申诉时间">{{ current.createTime }}</el-descriptions-item>
        </el-descriptions>

        <h3>用户申诉</h3>
        <div class="complaint-box">
          <p>{{ current.reason }}</p>
          <div class="image-wall">
            <el-image
              v-for="url in current.evidenceUrls"
              :key="url"
              :src="url"
              :preview-src-list="current.evidenceUrls"
              fit="cover"
            />
          </div>
        </div>

        <h3>接单员履约存证</h3>
        <EvidenceList :evidences="fulfillmentEvidence" />

        <template v-if="current.status === 0">
          <h3>平台裁定</h3>
          <el-form ref="decisionFormRef" :model="decisionForm" :rules="rules" label-position="top">
            <el-form-item label="审核结果" prop="approved">
              <el-radio-group v-model="decisionForm.approved">
                <el-radio-button :value="true">通过并全额退款</el-radio-button>
                <el-radio-button :value="false">驳回申诉</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="审核说明" prop="result">
              <el-input
                v-model="decisionForm.result"
                type="textarea"
                :rows="4"
                maxlength="500"
                show-word-limit
                placeholder="说明责任认定和审核依据，该内容会同步给用户与接单员"
              />
            </el-form-item>
          </el-form>
          <el-alert
            v-if="decisionForm.approved === true"
            type="warning"
            :closable="false"
            show-icon
            :title="`审核通过后将立即退还 ¥${money(current.orderAmount)} 至用户余额，此操作不可撤销。`"
          />
          <div class="drawer-actions">
            <el-button @click="drawerVisible = false">暂不处理</el-button>
            <el-button type="danger" :loading="deciding" @click="submitDecision">确认提交裁定</el-button>
          </div>
        </template>

        <section v-else class="result-box" :class="current.approved ? 'approved' : 'rejected'">
          <h3>{{ current.approved ? '申诉通过，已退款' : '申诉已驳回' }}</h3>
          <p>{{ current.result }}</p>
          <strong v-if="current.approved">退款金额：¥{{ money(current.refundAmount) }}</strong>
        </section>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { decideArbitration, pageArbitrations } from '@/api/arbitration'
import { getOrderEvidence } from '@/api/order'
import EvidenceList from '@/components/EvidenceList.vue'
import { money } from '@/utils/format'

const activeTab = ref('0')
const cases = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10 })

const drawerVisible = ref(false)
const loadingDetail = ref(false)
const current = ref(null)
const fulfillmentEvidence = ref([])
const decisionFormRef = ref(null)
const decisionForm = reactive({ approved: null, result: '' })
const deciding = ref(false)
const rules = {
  approved: [{ required: true, message: '请选择审核结果', trigger: 'change' }],
  result: [
    { required: true, message: '请填写审核说明', trigger: 'blur' },
    { max: 500, message: '审核说明不能超过 500 字', trigger: 'blur' }
  ]
}

function decisionText(item) {
  if (item.approved === true) return '申诉通过 · 已退款'
  if (item.approved === false) return '申诉驳回'
  return item.statusText || '待审核'
}

function statusTag(item) {
  if (item.approved === true) return 'success'
  if (item.approved === false) return 'info'
  return 'danger'
}

async function load() {
  loading.value = true
  try {
    const res = await pageArbitrations({
      page: query.page,
      size: query.size,
      status: activeTab.value === 'all' ? undefined : Number(activeTab.value)
    })
    cases.value = res.records ?? []
    total.value = res.total ?? 0
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

async function openCase(item) {
  current.value = item
  fulfillmentEvidence.value = []
  decisionForm.approved = null
  decisionForm.result = ''
  drawerVisible.value = true
  loadingDetail.value = true
  fulfillmentEvidence.value = await getOrderEvidence(item.orderId).catch(() => [])
  loadingDetail.value = false
}

async function submitDecision() {
  try {
    await decisionFormRef.value?.validate()
  } catch {
    return
  }

  const approved = decisionForm.approved === true
  try {
    await ElMessageBox.confirm(
      approved
        ? `确认申诉通过并立即全额退款 ¥${money(current.value.orderAmount)}？该操作不可撤销。`
        : '确认驳回本次申诉？订单将恢复为待验收，用户可继续确认验收。',
      approved ? '确认退款裁定' : '确认驳回裁定',
      { type: 'warning', confirmButtonText: '确认提交', cancelButtonText: '再检查一下' }
    )
  } catch {
    return
  }

  deciding.value = true
  try {
    await decideArbitration(current.value.id, {
      approved,
      result: decisionForm.result.trim()
    })
    ElMessage.success(approved ? '裁定完成，订单款项已全额退还用户' : '裁定完成，订单已恢复待验收')
    drawerVisible.value = false
    await load()
  } finally {
    deciding.value = false
  }
}

load()
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

.head h2 {
  margin: 0 0 6px;
  font-size: 20px;
}

.head p {
  margin: 0;
  color: var(--pp-muted);
  font-size: 13px;
}

.case-list {
  min-height: 260px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.case-card {
  padding: 16px;
  border: 1px solid var(--pp-tint-2);
  border-radius: var(--pp-radius);
}

.case-card header,
.case-card footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.case-card header {
  padding-bottom: 10px;
  border-bottom: 1px dashed var(--pp-tint-2);
}

.case-card footer {
  padding-top: 10px;
  border-top: 1px dashed var(--pp-tint-2);
}

.order-no {
  font-family: monospace;
  color: var(--pp-muted);
}

.service-name {
  margin-left: 12px;
  font-weight: 600;
}

.case-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 8px 20px;
  padding-top: 10px;
}

.case-grid p,
.reason {
  margin: 0;
  line-height: 1.6;
}

.case-grid span,
.reason span {
  display: inline-block;
  min-width: 72px;
  color: var(--pp-muted);
}

.reason {
  margin: 8px 0 12px;
}

.refund,
.pending {
  color: var(--pp-primary);
}

.rejected {
  color: var(--pp-muted);
}

.pager {
  justify-content: center;
  margin-top: 16px;
}

h3 {
  margin: 20px 0 12px;
  font-size: 15px;
}

.complaint-box,
.result-box {
  padding: 14px;
  border-radius: var(--pp-radius);
}

.complaint-box {
  border: 1px solid #f3c7c7;
  background: #fff8f8;
}

.complaint-box p {
  margin: 0 0 12px;
  line-height: 1.7;
}

.image-wall {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.image-wall :deep(.el-image) {
  width: 100px;
  height: 100px;
  border-radius: 8px;
}

.drawer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}

.result-box {
  margin-top: 20px;
  border: 1px solid var(--pp-tint-2);
}

.result-box.approved {
  background: var(--el-color-success-light-9);
}

.result-box.rejected {
  background: var(--el-fill-color-light);
}

.result-box h3 {
  margin-top: 0;
}
</style>
