<template>
  <div class="account-page">
    <header class="account-hero">
      <div><span>CUSTOMER ACCOUNTS</span><h1>用户管理</h1><p>查询宠物主人账号，并处理异常账号的启用与禁用。</p></div>
      <div class="hero-total"><strong>{{ total }}</strong><small>位宠物主人</small></div>
    </header>

    <section class="filter-panel">
      <el-select v-model="query.status" clearable placeholder="全部账号状态" @change="changeFilter">
        <el-option label="正常" :value="1" /><el-option label="已禁用" :value="0" />
      </el-select>
      <el-input v-model="query.keyword" clearable placeholder="搜索用户名、昵称或手机号" @keyup.enter="changeFilter" />
      <el-button :loading="loading" @click="changeFilter">查询</el-button>
    </section>

    <section class="table-panel">
      <el-table v-loading="loading" :data="users" empty-text="当前没有用户">
        <el-table-column label="用户" min-width="210">
          <template #default="{ row }"><div class="identity"><el-avatar :src="row.avatar">{{ (row.nickname || row.username || '?').slice(0, 1) }}</el-avatar><span><strong>{{ row.nickname || '未设置昵称' }}</strong><small>@{{ row.username }}</small></span></div></template>
        </el-table-column>
        <el-table-column prop="phoneMasked" label="手机号" min-width="150"><template #default="{ row }">{{ row.phoneMasked || '未填写' }}</template></el-table-column>
        <el-table-column prop="createTime" label="注册时间" min-width="180" />
        <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '已禁用' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }"><el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggleStatus(row)">{{ row.status === 1 ? '禁用账号' : '恢复账号' }}</el-button></template>
        </el-table-column>
      </el-table>
      <el-pagination v-if="total" layout="total, prev, pager, next" :total="total" :page-size="query.size" :current-page="query.page" @current-change="changePage" />
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageUsers, setUserStatus } from '@/api/adminAccount'

const query = reactive({ page: 1, size: 10, status: null, keyword: '' })
const users = ref([])
const total = ref(0)
const loading = ref(false)

async function loadUsers() {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size, keyword: query.keyword.trim() || undefined }
    if (query.status != null) params.status = query.status
    const data = await pageUsers(params)
    users.value = data.records || []
    total.value = data.total || 0
  } finally { loading.value = false }
}

function changeFilter() { query.page = 1; loadUsers() }
function changePage(page) { query.page = page; loadUsers() }

async function toggleStatus(user) {
  const status = user.status === 1 ? 0 : 1
  const action = status === 0 ? '禁用' : '恢复'
  const confirmed = await ElMessageBox.confirm(`确认${action}账号「${user.nickname || user.username}」吗？`, `${action}用户账号`, { type: status === 0 ? 'warning' : 'success' }).catch(() => false)
  if (!confirmed) return
  await setUserStatus(user.id, status)
  user.status = status
  ElMessage.success(`账号已${action}`)
}

onMounted(loadUsers)
</script>

<style scoped>
.account-page { min-height: calc(100vh - 100px); color: #27342d; }.account-hero { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; padding: 25px 30px; border-radius: 20px; background: linear-gradient(120deg, #24483a, #4f8064 72%, #7ba285); color: #fff; box-shadow: 0 14px 32px rgb(41 91 64 / 18%); }.account-hero span { color: #cce0d2; font-size: 10px; font-weight: 700; letter-spacing: 2px; }.account-hero h1 { margin: 6px 0 4px; font-size: 27px; }.account-hero p { margin: 0; color: rgb(255 255 255 / 72%); font-size: 12px; }.hero-total { display: flex; align-items: center; flex-direction: column; padding: 12px 18px; border-radius: 15px; background: rgb(255 255 255 / 12%); }.hero-total strong { font-size: 27px; }.hero-total small { color: rgb(255 255 255 / 70%); }.filter-panel { display: flex; gap: 10px; margin-bottom: 14px; padding: 14px 17px; border: 1px solid #e4e7e5; border-radius: 15px; background: #fff; }.filter-panel .el-select { width: 180px; }.filter-panel .el-input { width: 290px; }.table-panel { padding: 8px 18px 18px; border: 1px solid #e4e7e5; border-radius: 18px; background: #fff; box-shadow: 0 8px 25px rgb(40 68 51 / 6%); }.identity { display: flex; align-items: center; gap: 11px; }.identity span,.identity strong,.identity small { display: block; }.identity small { margin-top: 3px; color: #929b96; }.table-panel :deep(.el-pagination) { justify-content: center; margin-top: 18px; }@media (max-width: 680px) { .account-hero,.filter-panel { align-items: stretch; flex-direction: column; }.filter-panel .el-select,.filter-panel .el-input { width: 100%; } }
</style>
