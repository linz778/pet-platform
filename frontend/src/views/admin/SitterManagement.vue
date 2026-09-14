<template>
  <div class="sitter-page">
    <header class="sitter-hero">
      <div><span>SERVICE TEAM</span><h1>接单员管理</h1><p>统一查看资质、信誉与接单状态，处理账号和接单权限。</p></div>
      <div class="hero-total"><strong>{{ total }}</strong><small>位接单员</small></div>
    </header>

    <section class="filter-panel">
      <el-select v-model="query.userStatus" clearable placeholder="账号状态" @change="changeFilter"><el-option label="正常" :value="1" /><el-option label="已禁用" :value="0" /></el-select>
      <el-select v-model="query.auditStatus" clearable placeholder="资质状态" @change="changeFilter"><el-option label="待审核" :value="0" /><el-option label="已通过" :value="1" /><el-option label="已驳回" :value="2" /></el-select>
      <el-select v-model="query.available" clearable placeholder="接单状态" @change="changeFilter"><el-option label="可接单" :value="1" /><el-option label="暂停接单" :value="0" /></el-select>
      <el-input v-model="query.keyword" clearable placeholder="搜索账号、姓名或手机号" @keyup.enter="changeFilter" />
      <el-button :loading="loading" @click="changeFilter">查询</el-button>
    </section>

    <section class="table-panel">
      <el-table v-loading="loading" :data="sitters" empty-text="当前没有接单员">
        <el-table-column label="接单员" min-width="210"><template #default="{ row }"><div class="identity"><el-avatar :src="row.avatar">{{ (row.nickname || row.username || '?').slice(0, 1) }}</el-avatar><span><strong>{{ row.nickname || row.username }}</strong><small>{{ row.realName || '尚未提交实名' }} · {{ row.phoneMasked || '未留手机号' }}</small></span></div></template></el-table-column>
        <el-table-column label="资质" width="105"><template #default="{ row }"><el-tag :type="auditType(row.auditStatus)">{{ row.auditStatusText || '未提交' }}</el-tag></template></el-table-column>
        <el-table-column label="信誉" width="130"><template #default="{ row }"><strong>{{ row.creditScore ?? 100 }} 分</strong><small>{{ row.creditLevel || 1 }} 级 · {{ row.experienceYears || 0 }} 年经验</small></template></el-table-column>
        <el-table-column label="账号" width="95"><template #default="{ row }"><el-tag :type="row.userStatus === 1 ? 'success' : 'danger'">{{ row.userStatus === 1 ? '正常' : '禁用' }}</el-tag></template></el-table-column>
        <el-table-column label="接单" width="110"><template #default="{ row }"><el-switch :model-value="row.available === 1" :disabled="row.auditStatus !== 1 || row.userStatus !== 1" inline-prompt active-text="开启" inactive-text="暂停" @change="toggleAvailable(row, $event)" /></template></el-table-column>
        <el-table-column label="注册时间" prop="createTime" min-width="170" />
        <el-table-column label="操作" width="115" fixed="right"><template #default="{ row }"><el-button link :type="row.userStatus === 1 ? 'danger' : 'success'" @click="toggleStatus(row)">{{ row.userStatus === 1 ? '禁用账号' : '恢复账号' }}</el-button></template></el-table-column>
      </el-table>
      <el-pagination v-if="total" layout="total, prev, pager, next" :total="total" :page-size="query.size" :current-page="query.page" @current-change="changePage" />
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageSitters, setSitterAvailable, setSitterStatus } from '@/api/adminAccount'

const query = reactive({ page: 1, size: 10, userStatus: null, auditStatus: null, available: null, keyword: '' })
const sitters = ref([])
const total = ref(0)
const loading = ref(false)

async function loadSitters() {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size, keyword: query.keyword.trim() || undefined }
    for (const key of ['userStatus', 'auditStatus', 'available']) if (query[key] != null) params[key] = query[key]
    const data = await pageSitters(params)
    sitters.value = data.records || []
    total.value = data.total || 0
  } finally { loading.value = false }
}

function auditType(status) { return status === 1 ? 'success' : status === 2 ? 'danger' : 'warning' }
function changeFilter() { query.page = 1; loadSitters() }
function changePage(page) { query.page = page; loadSitters() }

async function toggleStatus(sitter) {
  const status = sitter.userStatus === 1 ? 0 : 1
  const action = status === 0 ? '禁用' : '恢复'
  const confirmed = await ElMessageBox.confirm(`确认${action}接单员「${sitter.nickname || sitter.username}」吗？`, `${action}接单员账号`, { type: status === 0 ? 'warning' : 'success' }).catch(() => false)
  if (!confirmed) return
  await setSitterStatus(sitter.userId, status)
  sitter.userStatus = status
  if (status === 0) sitter.available = 0
  ElMessage.success(`账号已${action}`)
}

async function toggleAvailable(sitter, enabled) {
  await setSitterAvailable(sitter.userId, enabled ? 1 : 0)
  sitter.available = enabled ? 1 : 0
  ElMessage.success(enabled ? '已允许接单' : '已暂停接单')
}

onMounted(loadSitters)
</script>

<style scoped>
.sitter-page { min-height: calc(100vh - 100px); color: #29332e; }.sitter-hero { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; padding: 25px 30px; border-radius: 20px; background: linear-gradient(120deg, #4a3529, #8a644d 72%, #bd9679); color: #fff; box-shadow: 0 14px 32px rgb(99 67 48 / 18%); }.sitter-hero span { color: #ead8ca; font-size: 10px; font-weight: 700; letter-spacing: 2px; }.sitter-hero h1 { margin: 6px 0 4px; font-size: 27px; }.sitter-hero p { margin: 0; color: rgb(255 255 255 / 72%); font-size: 12px; }.hero-total { display: flex; align-items: center; flex-direction: column; padding: 12px 18px; border-radius: 15px; background: rgb(255 255 255 / 12%); }.hero-total strong { font-size: 27px; }.hero-total small { color: rgb(255 255 255 / 70%); }.filter-panel { display: flex; flex-wrap: wrap; gap: 9px; margin-bottom: 14px; padding: 14px 17px; border: 1px solid #e7e3df; border-radius: 15px; background: #fff; }.filter-panel .el-select { width: 145px; }.filter-panel .el-input { width: 245px; }.table-panel { padding: 8px 18px 18px; border: 1px solid #e7e3df; border-radius: 18px; background: #fff; box-shadow: 0 8px 25px rgb(70 53 42 / 6%); }.identity { display: flex; align-items: center; gap: 11px; }.identity span,.identity strong,.identity small,.table-panel td small { display: block; }.identity small,.table-panel td small { margin-top: 3px; color: #92958f; }.table-panel :deep(.el-pagination) { justify-content: center; margin-top: 18px; }@media (max-width: 760px) { .sitter-hero { align-items: stretch; flex-direction: column; }.filter-panel .el-select,.filter-panel .el-input { width: 100%; } }
</style>
