<template>
  <div class="audit-page">
    <header class="audit-hero">
      <div>
        <span class="eyebrow">QUALIFICATION REVIEW</span>
        <h1>人员资质审核</h1>
        <p>核验接单员实名资料、健康证明与专业资质，审核结果会立即同步到接单员端。</p>
      </div>
      <div class="hero-stat"><strong>{{ total }}</strong><span>{{ statusLabel }}申请</span></div>
    </header>

    <section class="filter-panel">
      <el-radio-group v-model="query.status" @change="onFilterChange">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button :value="0">待审核</el-radio-button>
        <el-radio-button :value="1">已通过</el-radio-button>
        <el-radio-button :value="2">已驳回</el-radio-button>
      </el-radio-group>
      <div class="filter-actions">
        <el-input v-model="query.keyword" clearable placeholder="搜索真实姓名" prefix-icon="Search" @keyup.enter="onFilterChange" />
        <el-button :loading="loading" @click="onFilterChange">查询</el-button>
      </div>
    </section>

    <section v-loading="loading" class="application-list">
      <el-empty v-if="!loading && applications.length === 0" description="当前筛选条件下没有资质申请" />
      <article v-for="item in applications" :key="item.profileId" class="application-card">
        <div class="applicant">
          <el-avatar :size="52" class="applicant-avatar">{{ item.realName?.slice(0, 1) || '接' }}</el-avatar>
          <div>
            <div class="name-row"><strong>{{ item.realName }}</strong><el-tag :type="statusType(item.auditStatus)" effect="light">{{ item.auditStatusText }}</el-tag></div>
            <span>{{ item.nickname || item.username }} · {{ item.phoneMasked || '未留手机号' }}</span>
          </div>
        </div>

        <div class="profile-summary">
          <div><span>身份证号</span><strong>{{ item.idCardMasked || '未填写' }}</strong></div>
          <div><span>从业经验</span><strong>{{ item.experienceYears || 0 }} 年</strong></div>
          <div><span>信誉等级</span><strong>{{ item.creditLevel || 3 }} 级 / {{ item.creditScore || 100 }} 分</strong></div>
          <div><span>提交时间</span><strong>{{ item.submitTime || '--' }}</strong></div>
        </div>

        <div class="certificate-strip">
          <div v-for="cert in certificates(item)" :key="cert.label" class="cert-thumb" :class="{ empty: !cert.url }">
            <el-image v-if="cert.url" :src="cert.url" :preview-src-list="certificateUrls(item)" fit="cover" preview-teleported />
            <span v-else>未上传</span>
            <small>{{ cert.label }}</small>
          </div>
        </div>

        <div class="card-actions">
          <p v-if="item.auditStatus === 2"><span>驳回原因</span>{{ item.auditRemark }}</p>
          <el-button @click="openDetail(item)">查看完整资料</el-button>
          <el-button v-if="item.auditStatus === 0" type="primary" @click="openDetail(item)">开始审核</el-button>
        </div>
      </article>
    </section>

    <el-pagination
      v-if="total > 0"
      class="pager"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="query.size"
      :current-page="query.page"
      @current-change="onPageChange"
    />

    <el-dialog v-model="detailVisible" width="860px" top="4vh" class="audit-dialog" destroy-on-close>
      <template #header>
        <div class="dialog-title"><span>资质申请详情</span><el-tag :type="statusType(current?.auditStatus)">{{ current?.auditStatusText }}</el-tag></div>
      </template>

      <template v-if="current">
        <section class="identity-card">
          <el-avatar :size="62" class="applicant-avatar">{{ current.realName?.slice(0, 1) }}</el-avatar>
          <div><h2>{{ current.realName }}</h2><p>{{ current.nickname || current.username }} · {{ current.phoneMasked }}</p></div>
          <div class="identity-data"><span>身份证号<strong>{{ current.idCardMasked }}</strong></span><span>从业经验<strong>{{ current.experienceYears || 0 }} 年</strong></span></div>
        </section>

        <section class="document-section">
          <div class="section-title"><span>01</span><div><h3>证明材料</h3><p>点击图片可查看原图，请核对姓名、有效期与证件清晰度。</p></div></div>
          <div class="document-grid">
            <article v-for="cert in certificates(current)" :key="cert.label" class="document-card" :class="{ missing: !cert.url }">
              <el-image v-if="cert.url" :src="cert.url" :preview-src-list="certificateUrls(current)" fit="contain" preview-teleported />
              <div v-else class="missing-doc">📄<span>申请人未上传</span></div>
              <footer><strong>{{ cert.label }}</strong><el-tag :type="cert.url ? 'success' : 'info'" size="small" effect="plain">{{ cert.url ? '已上传' : '缺少' }}</el-tag></footer>
            </article>
          </div>
        </section>

        <section v-if="current.auditStatus === 0" class="decision-section">
          <div class="section-title"><span>02</span><div><h3>审核结论</h3><p>通过后该接单员立即获得接单资格；驳回时需说明需要补充或修改的内容。</p></div></div>
          <el-form label-position="top">
            <el-form-item label="初始信誉等级">
              <el-rate v-model="decision.creditLevel" :max="5" show-score score-template="{value} 级" />
            </el-form-item>
            <el-form-item label="审核说明 / 驳回原因">
              <el-input v-model="decision.remark" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="通过时可不填；驳回时请写清楚需要重新提交的材料" />
            </el-form-item>
          </el-form>
          <div class="decision-actions">
            <el-button type="danger" plain :loading="deciding" @click="submitDecision(false)">驳回申请</el-button>
            <el-button type="success" :loading="deciding" @click="submitDecision(true)">审核通过并启用接单</el-button>
          </div>
        </section>

        <el-alert v-else :type="current.auditStatus === 1 ? 'success' : 'error'" :closable="false" show-icon>
          <template #title>{{ current.auditStatus === 1 ? '该申请已审核通过，接单员当前具备接单资格。' : `该申请已驳回：${current.auditRemark}` }}</template>
        </el-alert>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { decideSitterAudit, pageSitterAudits } from '@/api/sitterAudit'

const loading = ref(false)
const deciding = ref(false)
const applications = ref([])
const total = ref(0)
const detailVisible = ref(false)
const current = ref(null)
const query = reactive({ page: 1, size: 8, status: 0, keyword: '' })
const decision = reactive({ creditLevel: 3, remark: '' })
const statusLabel = computed(() => ({ 0: '待审核', 1: '已通过', 2: '已驳回' }[query.status] || '全部'))

async function load() {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size, keyword: query.keyword.trim() || undefined }
    if (query.status !== '') params.status = query.status
    const data = await pageSitterAudits(params)
    applications.value = data.records ?? []
    total.value = data.total ?? 0
  } finally {
    loading.value = false
  }
}

function onFilterChange() {
  query.page = 1
  load()
}

function onPageChange(page) {
  query.page = page
  load()
}

function openDetail(item) {
  current.value = item
  decision.creditLevel = item.creditLevel || 3
  decision.remark = ''
  detailVisible.value = true
}

function certificates(item) {
  return [
    { label: '身份证照片', url: item?.idCardImg },
    { label: '健康证明', url: item?.healthCert },
    { label: '专业资质证书', url: item?.qualification }
  ]
}

function certificateUrls(item) {
  return certificates(item).map((cert) => cert.url).filter(Boolean)
}

function statusType(status) {
  return ({ 0: 'warning', 1: 'success', 2: 'danger' })[status] || 'info'
}

async function submitDecision(approved) {
  if (!approved && !decision.remark.trim()) {
    ElMessage.warning('驳回申请时请填写具体原因')
    return
  }
  if (approved) {
    const confirmed = await ElMessageBox.confirm(
      `确认通过「${current.value.realName}」的资质申请并立即开放接单权限吗？`,
      '确认审核通过',
      { type: 'success', confirmButtonText: '确认通过', cancelButtonText: '再核对一下' }
    ).catch(() => false)
    if (!confirmed) return
  }

  deciding.value = true
  try {
    const updated = await decideSitterAudit(current.value.profileId, {
      approved,
      remark: decision.remark,
      creditLevel: decision.creditLevel
    })
    current.value = updated
    detailVisible.value = false
    await load()
    ElMessage.success(approved ? '审核已通过，接单员状态已更新为可接单' : '申请已驳回，原因已同步给接单员')
  } finally {
    deciding.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.audit-page { min-height: calc(100vh - 100px); color: #24332a; }
.audit-hero { display: flex; align-items: center; justify-content: space-between; gap: 24px; margin-bottom: 16px; padding: 25px 30px; border-radius: 20px; background: linear-gradient(120deg, #f0f8f2, #fff 65%, #fff8e8); border: 1px solid #e0ebe3; }
.eyebrow { color: #568265; font-size: 10px; font-weight: 700; letter-spacing: 2px; }
.audit-hero h1 { margin: 6px 0 4px; font-size: 26px; }
.audit-hero p { margin: 0; color: #78857d; font-size: 12px; }
.hero-stat { display: flex; flex-direction: column; align-items: center; min-width: 90px; padding: 12px 20px; border-radius: 15px; background: #fff; box-shadow: 0 8px 24px rgb(56 91 68 / 8%); }
.hero-stat strong { color: #4f8760; font-size: 26px; }
.hero-stat span { color: #8a958e; font-size: 10px; }
.filter-panel { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 14px; padding: 14px 16px; border: 1px solid #e7ede9; border-radius: 15px; background: #fff; }
.filter-actions { display: flex; gap: 8px; }
.filter-actions .el-input { width: 220px; }
.application-list { min-height: 280px; }
.application-card { display: grid; grid-template-columns: minmax(190px, .8fr) minmax(410px, 1.8fr) 250px auto; align-items: center; gap: 22px; margin-bottom: 12px; padding: 18px 20px; border: 1px solid #e3ebe5; border-radius: 17px; background: #fff; box-shadow: 0 6px 20px rgb(40 69 51 / 5%); transition: border-color .2s, transform .2s; }
.application-card:hover { border-color: #9cb9a4; transform: translateY(-2px); }
.applicant { display: flex; min-width: 0; align-items: center; gap: 12px; }
.applicant-avatar { flex: 0 0 auto; background: linear-gradient(145deg, #75a582, #3e7350); color: #fff; font-weight: 700; }
.applicant > div { min-width: 0; }
.name-row { display: flex; align-items: center; gap: 8px; }
.name-row strong { font-size: 16px; }
.applicant > div > span { color: #8b958f; font-size: 10px; }
.profile-summary { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px 18px; }
.profile-summary div { display: flex; min-width: 0; flex-direction: column; gap: 2px; }
.profile-summary span { color: #929c96; font-size: 9px; }
.profile-summary strong { overflow: hidden; font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }
.certificate-strip { display: flex; gap: 7px; }
.cert-thumb { position: relative; overflow: hidden; width: 72px; height: 58px; border-radius: 9px; background: #eef3ef; }
.cert-thumb :deep(.el-image) { width: 100%; height: 100%; }
.cert-thumb > span { display: grid; height: 100%; place-items: center; color: #a6afa9; font-size: 9px; }
.cert-thumb small { position: absolute; right: 0; bottom: 0; left: 0; padding: 2px; background: rgb(26 45 33 / 66%); color: #fff; text-align: center; font-size: 8px; }
.card-actions { display: flex; align-items: flex-end; justify-content: center; flex-direction: column; gap: 7px; }
.card-actions p { max-width: 210px; margin: 0; color: #b75d50; font-size: 10px; text-align: right; }
.card-actions p span { display: block; color: #999; }
.pager { justify-content: center; margin-top: 18px; }
.dialog-title { display: flex; align-items: center; gap: 10px; font-size: 18px; font-weight: 700; }
.identity-card { display: flex; align-items: center; gap: 14px; margin-bottom: 22px; padding: 18px; border-radius: 15px; background: linear-gradient(120deg, #eef7f0, #fafcf8); }
.identity-card h2 { margin: 0 0 4px; font-size: 19px; }
.identity-card p { margin: 0; color: #829087; font-size: 11px; }
.identity-data { display: flex; margin-left: auto; gap: 30px; }
.identity-data span { display: flex; flex-direction: column; color: #8d9991; font-size: 9px; }
.identity-data strong { margin-top: 4px; color: #314238; font-size: 12px; }
.section-title { display: flex; align-items: center; gap: 10px; margin-bottom: 13px; }
.section-title > span { display: grid; width: 31px; height: 31px; place-items: center; border-radius: 9px; background: #e9f3eb; color: #508261; font-size: 11px; font-weight: 700; }
.section-title h3, .section-title p { margin: 0; }
.section-title h3 { font-size: 15px; }
.section-title p { margin-top: 2px; color: #8a958e; font-size: 10px; }
.document-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; margin-bottom: 22px; }
.document-card { overflow: hidden; border: 1px solid #e3ebe5; border-radius: 13px; background: #fafcfb; }
.document-card > :deep(.el-image), .missing-doc { width: 100%; height: 175px; }
.missing-doc { display: flex; align-items: center; justify-content: center; flex-direction: column; gap: 5px; color: #a5aea8; font-size: 30px; }
.missing-doc span { font-size: 10px; }
.document-card footer { display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; }
.document-card footer strong { font-size: 11px; }
.decision-section { padding: 17px; border: 1px solid #e1ebe3; border-radius: 15px; background: #fbfdfb; }
.decision-actions { display: flex; justify-content: flex-end; gap: 8px; }

@media (max-width: 1500px) { .application-card { grid-template-columns: 1fr 2fr auto; } .certificate-strip { display: none; } }
@media (max-width: 820px) { .application-card { align-items: start; grid-template-columns: 1fr; } .card-actions { align-items: flex-start; } .profile-summary { grid-template-columns: 1fr 1fr; } .filter-panel { align-items: stretch; flex-direction: column; } .filter-actions .el-input { width: 100%; } .identity-data { display: none; } .document-grid { grid-template-columns: 1fr; } }
</style>
