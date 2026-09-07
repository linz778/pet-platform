<template>
  <div class="bounty-page">
    <header class="bounty-hero">
      <div><span>BOUNTY REVIEW</span><h1>悬赏任务审核</h1><p>核对接单员完成证明，审核通过后平台担保款自动结算。</p></div>
      <div class="hero-count"><strong>{{ total }}</strong><small>条任务</small></div>
    </header>

    <el-card>
      <el-tabs v-model="activeTab" @tab-change="changeTab">
        <el-tab-pane label="待审核" name="4" />
        <el-tab-pane label="待补证" name="3" />
        <el-tab-pane label="已通过" name="5" />
        <el-tab-pane label="全部" name="all" />
      </el-tabs>
      <div v-loading="loading" class="task-list">
        <el-empty v-if="!loading && tasks.length === 0" description="当前没有悬赏任务" />
        <article v-for="task in tasks" :key="task.id" class="task-card">
          <header>
            <div><el-tag type="warning" effect="dark">悬赏</el-tag><strong>{{ task.taskTitle }}</strong></div>
            <el-tag :type="statusType(task.status)" effect="light">{{ task.statusText }}</el-tag>
          </header>
          <p class="description">{{ task.taskDescription }}</p>
          <div class="task-grid">
            <span><small>发布者</small>{{ task.ownerNickname || '宠物主人' }}</span>
            <span><small>接单员</small>{{ task.sitterName || '尚未接单' }}</span>
            <span><small>宠物</small>{{ task.petName || '未知宠物' }}</span>
            <span><small>悬赏金额</small><b>¥{{ money(task.amount) }}</b></span>
          </div>
          <footer><span>{{ task.serviceStart }} · {{ task.serviceAddress }}</span><el-button type="primary" plain @click="openTask(task)">{{ task.status === 4 ? '开始审核' : '查看详情' }}</el-button></footer>
        </article>
      </div>
      <el-pagination v-if="total" layout="total, prev, pager, next" :total="total" :page-size="query.size" :current-page="query.page" @current-change="changePage" />
    </el-card>

    <el-drawer v-model="drawerVisible" title="悬赏任务详情" size="620px">
      <div v-if="current" v-loading="loadingDetail">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="任务标题">{{ current.taskTitle }}</el-descriptions-item>
          <el-descriptions-item label="任务要求">{{ current.taskDescription }}</el-descriptions-item>
          <el-descriptions-item label="发布者 / 宠物">{{ current.ownerNickname || '宠物主人' }} / {{ current.petName }}</el-descriptions-item>
          <el-descriptions-item label="接单员">{{ current.sitterName || '尚未接单' }}</el-descriptions-item>
          <el-descriptions-item label="时间">{{ current.serviceStart }}</el-descriptions-item>
          <el-descriptions-item label="地址">{{ current.serviceAddress }}</el-descriptions-item>
          <el-descriptions-item label="悬赏金额"><b class="amount">¥{{ money(current.amount) }}</b></el-descriptions-item>
          <el-descriptions-item v-if="current.taskReviewRemark" label="上次审核说明">{{ current.taskReviewRemark }}</el-descriptions-item>
        </el-descriptions>

        <h3>接单员完成证明</h3>
        <EvidenceList :evidences="evidences" />

        <template v-if="current.status === 4">
          <h3>审核决定</h3>
          <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
            <el-form-item label="审核结果" prop="approved">
              <el-radio-group v-model="form.approved">
                <el-radio-button :value="true">通过并结算</el-radio-button>
                <el-radio-button :value="false">驳回并要求补证</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="审核说明" prop="result">
              <el-input v-model="form.result" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="说明照片审核依据，该内容会同步给接单员" />
            </el-form-item>
          </el-form>
          <el-alert v-if="form.approved === true" type="warning" :closable="false" show-icon :title="`通过后将立即结算 ¥${money(current.amount)}，操作不可撤销。`" />
          <div class="drawer-actions"><el-button @click="drawerVisible = false">暂不处理</el-button><el-button type="primary" :loading="reviewing" @click="submitReview">确认审核</el-button></div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageBountyTasks, reviewBountyTask } from '@/api/adminDispatch'
import { getOrder, getOrderEvidence } from '@/api/order'
import EvidenceList from '@/components/EvidenceList.vue'
import { money } from '@/utils/format'

const activeTab = ref('4')
const query = reactive({ page: 1, size: 10 })
const tasks = ref([])
const total = ref(0)
const loading = ref(false)
const drawerVisible = ref(false)
const loadingDetail = ref(false)
const current = ref(null)
const evidences = ref([])
const reviewing = ref(false)
const formRef = ref(null)
const form = reactive({ approved: null, result: '' })
const rules = {
  approved: [{ required: true, message: '请选择审核结果', trigger: 'change' }],
  result: [{ required: true, message: '请填写审核说明', trigger: 'blur' }, { max: 500, message: '审核说明不能超过 500 字', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    const params = { ...query, status: activeTab.value === 'all' ? undefined : Number(activeTab.value) }
    const data = await pageBountyTasks(params)
    tasks.value = data.records || []
    total.value = data.total || 0
  } finally { loading.value = false }
}

function changeTab() { query.page = 1; load() }
function changePage(page) { query.page = page; load() }
function statusType(status) { return status === 5 ? 'success' : status === 4 ? 'warning' : status === 3 ? 'danger' : 'info' }

async function openTask(task) {
  drawerVisible.value = true
  loadingDetail.value = true
  form.approved = null
  form.result = ''
  try {
    const [detail, proof] = await Promise.all([getOrder(task.id), getOrderEvidence(task.id)])
    current.value = { ...task, ...detail }
    evidences.value = proof || []
  } finally { loadingDetail.value = false }
}

async function submitReview() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  const approved = form.approved === true
  const confirmed = await ElMessageBox.confirm(
    approved ? `确认审核通过并立即结算 ¥${money(current.value.amount)}？` : '确认驳回？任务将退回接单员补充证明。',
    '确认审核结果', { type: 'warning', confirmButtonText: '确认提交', cancelButtonText: '再检查一下' }
  ).catch(() => false)
  if (!confirmed) return
  reviewing.value = true
  try {
    await reviewBountyTask(current.value.id, { approved, result: form.result.trim() })
    ElMessage.success(approved ? '审核通过，悬赏款已结算' : '已驳回并同步给接单员补证')
    drawerVisible.value = false
    await load()
  } finally { reviewing.value = false }
}

load()
</script>

<style scoped>
.bounty-page { min-height: calc(100vh - 100px); color: #26352b; }
.bounty-hero { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; padding: 24px 30px; border-radius: 20px; background: linear-gradient(120deg, #49351d, #9c6d2e 70%, #c39a52); color: #fff; box-shadow: 0 14px 32px rgb(90 62 24 / 16%); }
.bounty-hero span { color: #f2d9ad; font-size: 10px; font-weight: 700; letter-spacing: 2px; }
.bounty-hero h1 { margin: 6px 0 4px; font-size: 27px; }
.bounty-hero p { margin: 0; color: rgb(255 255 255 / 74%); font-size: 12px; }
.hero-count { display: flex; flex-direction: column; min-width: 88px; padding: 12px; border-radius: 14px; background: rgb(255 255 255 / 12%); text-align: center; }
.hero-count strong { font-size: 27px; }.hero-count small { color: #f5e7cf; }
.task-list { display: flex; min-height: 260px; flex-direction: column; gap: 12px; }
.task-card { padding: 16px; border: 1px solid #ebe4d7; border-radius: 15px; background: #fffdf9; }
.task-card header,.task-card footer { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.task-card header > div { display: flex; align-items: center; gap: 10px; }.task-card header strong { font-size: 16px; }
.description { margin: 12px 0; color: #56645b; line-height: 1.7; }
.task-grid { display: grid; grid-template-columns: repeat(4, minmax(120px, 1fr)); gap: 12px; padding: 12px 0; border-block: 1px dashed #e6ddcd; }
.task-grid span { display: flex; flex-direction: column; gap: 3px; }.task-grid small { color: var(--pp-muted); }.task-grid b,.amount { color: #9a681f; }
.task-card footer { padding-top: 12px; }.task-card footer > span { overflow: hidden; color: var(--pp-muted); font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
:deep(.el-pagination) { justify-content: center; margin-top: 18px; }
h3 { margin: 20px 0 12px; font-size: 15px; }.drawer-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 16px; }
@media (max-width: 800px) { .task-grid { grid-template-columns: repeat(2, 1fr); }.bounty-hero { align-items: flex-start; flex-direction: column; gap: 14px; } }
</style>
