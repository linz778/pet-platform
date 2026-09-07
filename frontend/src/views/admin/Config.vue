<template>
  <div class="rule-page">
    <header class="rule-hero">
      <div>
        <span>SERVICE RULES</span>
        <h1>服务规则配置</h1>
        <p>规则保存后立即用于新订单计价，已创建订单继续使用下单时的价格快照。</p>
      </div>
      <div class="hero-stat"><strong>{{ onlineCount }}/{{ rules.length }}</strong><small>服务已上架</small></div>
    </header>

    <section v-loading="loading" class="rule-grid">
      <el-empty v-if="!loading && !rules.length" description="暂无服务规则" />
      <article v-for="item in rules" :key="item.id" class="rule-card" :class="{ offline: item.status !== 1 }">
        <header>
          <span class="service-icon">{{ meta(item.code).icon }}</span>
          <div><small>{{ item.code }}</small><h2>{{ item.name }}</h2></div>
          <el-tag :type="item.status === 1 ? 'success' : 'info'" effect="light">{{ item.status === 1 ? '已上架' : '已下架' }}</el-tag>
        </header>
        <div class="price-row">
          <span>基础价<strong>¥{{ money(item.basePrice) }}<small>/ {{ item.unit }}</small></strong></span>
          <span>周末价<strong>¥{{ weekendPrice(item) }}</strong></span>
        </div>
        <div class="rate-row">
          <span><i>周末倍率</i><b>× {{ Number(item.holidayRate).toFixed(2) }}</b></span>
          <span><i>平台抽成</i><b>{{ percent(item.commissionRate) }}%</b></span>
          <span><i>接单员基础到手</i><b>¥{{ sitterIncome(item) }}</b></span>
        </div>
        <div class="checklist">
          <small>标准作业清单</small>
          <div><el-tag v-for="task in item.checklist" :key="task" size="small" effect="plain">{{ task }}</el-tag><span v-if="!item.checklist?.length">暂未配置</span></div>
        </div>
        <button type="button" @click="openEdit(item)">编辑计价与服务规则 <span>→</span></button>
      </article>
    </section>

    <el-dialog v-model="editVisible" width="640px" destroy-on-close>
      <template #header><div class="dialog-title"><span>{{ current && meta(current.code).icon }}</span><div><small>编辑服务规则</small><strong>{{ current?.name }}</strong></div></div></template>
      <el-form v-if="current" label-position="top">
        <div class="form-grid">
          <el-form-item label="基础单价">
            <el-input-number v-model="form.basePrice" :min="0.01" :max="99999.99" :precision="2" :step="5" controls-position="right" />
          </el-form-item>
          <el-form-item label="周末溢价倍数">
            <el-input-number v-model="form.holidayRate" :min="1" :max="5" :precision="2" :step="0.1" controls-position="right" />
          </el-form-item>
          <el-form-item label="平台抽成比例">
            <el-input-number v-model="form.commissionPercent" :min="0" :max="100" :precision="1" :step="1" controls-position="right" />
            <span class="field-suffix">%</span>
          </el-form-item>
          <el-form-item label="服务状态">
            <el-switch v-model="form.onShelf" inline-prompt active-text="上架" inactive-text="下架" />
          </el-form-item>
        </div>
        <el-form-item label="标准作业清单">
          <el-select v-model="form.checklist" multiple filterable allow-create default-first-option placeholder="输入清单项后按回车，可添加多项" />
        </el-form-item>
        <div class="preview-row">
          <span>工作日用户支付<strong>¥{{ preview.base }}</strong></span>
          <span>周末用户支付<strong>¥{{ preview.weekend }}</strong></span>
          <span>工作日接单员到手<strong>¥{{ preview.income }}</strong></span>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRule">保存并立即生效</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listServiceRules, updateServiceRule } from '@/api/adminServiceRule'
import { money } from '@/utils/format'

const icons = { FEEDING: '🥣', GROOMING: '🛁', WALKING: '🦮', COMPANION: '🧸' }
const loading = ref(false)
const saving = ref(false)
const rules = ref([])
const current = ref(null)
const editVisible = ref(false)
const form = reactive({ basePrice: 0, holidayRate: 1, commissionPercent: 0, checklist: [], onShelf: true })
const onlineCount = computed(() => rules.value.filter((item) => item.status === 1).length)
const preview = computed(() => {
  const base = Number(form.basePrice || 0)
  const rate = Number(form.holidayRate || 1)
  const commission = Number(form.commissionPercent || 0) / 100
  return { base: money(base), weekend: money(base * rate), income: money(base - base * commission) }
})

function meta(code) {
  return { icon: icons[code] || '🐾' }
}

function percent(rate) {
  return (Number(rate || 0) * 100).toFixed(1).replace('.0', '')
}

function weekendPrice(item) {
  return money(Number(item.basePrice) * Number(item.holidayRate))
}

function sitterIncome(item) {
  const base = Number(item.basePrice)
  return money(base - base * Number(item.commissionRate))
}

async function loadRules() {
  loading.value = true
  try {
    rules.value = await listServiceRules()
  } finally {
    loading.value = false
  }
}

function openEdit(item) {
  current.value = item
  form.basePrice = Number(item.basePrice)
  form.holidayRate = Number(item.holidayRate)
  form.commissionPercent = Number(item.commissionRate) * 100
  form.checklist = [...(item.checklist || [])]
  form.onShelf = item.status === 1
  editVisible.value = true
}

async function saveRule() {
  saving.value = true
  try {
    const updated = await updateServiceRule(current.value.id, {
      basePrice: form.basePrice,
      holidayRate: form.holidayRate,
      commissionRate: Number((form.commissionPercent / 100).toFixed(3)),
      checklist: form.checklist.map((item) => item.trim()).filter(Boolean),
      status: form.onShelf ? 1 : 0
    })
    const index = rules.value.findIndex((item) => item.id === updated.id)
    if (index >= 0) rules.value[index] = updated
    editVisible.value = false
    ElMessage.success('服务规则已更新，新订单立即生效')
  } finally {
    saving.value = false
  }
}

onMounted(loadRules)
</script>

<style scoped>
.rule-page { min-height: calc(100vh - 100px); color: #24332a; }
.rule-hero { display: flex; align-items: center; justify-content: space-between; margin-bottom: 18px; padding: 25px 30px; border-radius: 20px; background: linear-gradient(120deg, #17382b, #497959 70%, #799a6d); color: #fff; box-shadow: 0 14px 34px rgb(35 69 49 / 17%); }
.rule-hero > div > span { color: #bdd8c3; font-size: 10px; font-weight: 700; letter-spacing: 2px; }
.rule-hero h1 { margin: 6px 0 4px; font-size: 27px; }
.rule-hero p { margin: 0; color: rgb(255 255 255 / 70%); font-size: 12px; }
.hero-stat { display: flex; align-items: center; flex-direction: column; min-width: 100px; padding: 12px 18px; border-radius: 15px; background: rgb(255 255 255 / 12%); }
.hero-stat strong { font-size: 25px; }.hero-stat small { color: rgb(255 255 255 / 68%); }
.rule-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; min-height: 260px; }
.rule-grid > .el-empty { grid-column: 1 / -1; }
.rule-card { overflow: hidden; padding: 20px; border: 1px solid #e2ebe5; border-radius: 18px; background: #fff; box-shadow: 0 8px 24px rgb(39 67 50 / 6%); }
.rule-card.offline { opacity: .68; }
.rule-card > header { display: flex; align-items: center; gap: 11px; }
.service-icon { display: grid; width: 45px; height: 45px; place-items: center; border-radius: 13px; background: #eaf4ed; font-size: 23px; }
.rule-card header div { flex: 1; }.rule-card header small { color: #92a097; font-size: 9px; letter-spacing: 1px; }.rule-card h2 { margin: 2px 0 0; font-size: 18px; }
.price-row { display: grid; grid-template-columns: 1fr 1fr; margin: 17px 0 13px; padding: 14px 16px; border-radius: 13px; background: #f5f9f6; }
.price-row > span { display: flex; flex-direction: column; color: #819087; font-size: 10px; }.price-row > span + span { padding-left: 18px; border-left: 1px solid #dfe8e2; }
.price-row strong { margin-top: 4px; color: #3f7650; font-size: 21px; }.price-row strong small { color: #7d8d83; font-size: 10px; font-weight: 400; }
.rate-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }.rate-row span { display: flex; flex-direction: column; padding: 9px 11px; border: 1px solid #edf1ee; border-radius: 10px; }.rate-row i { color: #919d95; font-size: 9px; font-style: normal; }.rate-row b { margin-top: 3px; font-size: 12px; }
.checklist { margin-top: 14px; }.checklist > small { color: #829087; font-size: 10px; }.checklist > div { display: flex; gap: 6px; min-height: 24px; margin-top: 7px; flex-wrap: wrap; }.checklist > div > span { color: #9aa49e; font-size: 10px; }
.rule-card > button { width: calc(100% + 40px); margin: 17px -20px -20px; padding: 13px 20px; border: 0; border-top: 1px solid #edf1ee; background: #fbfcfb; color: #4f7f5d; cursor: pointer; text-align: left; }.rule-card > button span { float: right; }.rule-card > button:hover { background: #f1f7f3; }
.dialog-title { display: flex; align-items: center; gap: 10px; }.dialog-title > span { font-size: 26px; }.dialog-title div { display: flex; flex-direction: column; }.dialog-title small { color: #89958d; }.dialog-title strong { margin-top: 2px; font-size: 17px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 18px; }.form-grid :deep(.el-input-number) { width: 100%; }.field-suffix { position: absolute; right: 42px; color: #89958d; }
.rule-page :deep(.el-form > .el-form-item .el-select) { width: 100%; }
.preview-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; padding: 13px; border-radius: 12px; background: #f3f8f4; }.preview-row span { display: flex; flex-direction: column; color: #839087; font-size: 9px; }.preview-row strong { margin-top: 3px; color: #477957; font-size: 14px; }
@media (max-width: 900px) { .rule-grid { grid-template-columns: 1fr; } .rule-hero { align-items: flex-start; flex-direction: column; gap: 18px; } }
@media (max-width: 620px) { .form-grid, .preview-row { grid-template-columns: 1fr; } }
</style>
