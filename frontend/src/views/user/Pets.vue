<template>
  <div class="page-container">
    <el-card class="head-card">
      <div class="head">
        <div class="head-text">
          <h2 class="title">🐾 宠物档案</h2>
          <p class="subtitle">
            登记品种、年龄、疫苗免疫证明、性格习性与喂养禁忌。接单员抢单后、上门前会先看到这些内容。
          </p>
        </div>
        <el-button type="primary" @click="openCreate">新增宠物</el-button>
      </div>
    </el-card>

    <el-card v-loading="loading" class="list-card">
      <el-empty v-if="!loading && pets.length === 0" description="还没有宠物档案，先添加一只吧">
        <el-button type="primary" @click="openCreate">新增宠物</el-button>
      </el-empty>

      <div v-else class="pet-grid">
        <article v-for="pet in pets" :key="pet.id" class="pet-card">
          <header class="pet-top">
            <el-image
              v-if="pet.avatar"
              :src="pet.avatar"
              fit="cover"
              class="pet-avatar"
              :preview-src-list="[pet.avatar]"
              preview-teleported
            />
            <div v-else class="pet-avatar pet-avatar-empty">{{ speciesEmoji(pet.species) }}</div>

            <div class="pet-id">
              <h3 class="pet-name">{{ pet.name }}</h3>
              <div class="pet-tags">
                <el-tag size="small" effect="plain">{{ pet.species || '物种未填' }}</el-tag>
                <el-tag v-if="pet.breed" size="small" type="info" effect="plain">{{ pet.breed }}</el-tag>
                <el-tag size="small" type="info" effect="light">{{ GENDER_TEXT[pet.gender] ?? GENDER_TEXT[0] }}</el-tag>
              </div>
            </div>
          </header>

          <dl class="pet-facts">
            <div>
              <dt>年龄</dt>
              <dd>{{ petAgeText(pet.ageMonths) }}</dd>
            </div>
            <div>
              <dt>体重</dt>
              <dd>{{ pet.weightKg == null ? '未填写' : `${pet.weightKg} kg` }}</dd>
            </div>
          </dl>

          <p class="pet-note"><span class="note-label">性格</span>{{ pet.personality || '未填写' }}</p>
          <p class="pet-note"><span class="note-label">禁忌</span>{{ pet.feedingTaboo || '未填写' }}</p>

          <div v-if="pet.vaccineCerts && pet.vaccineCerts.length" class="pet-certs">
            <span class="note-label">疫苗证明</span>
            <el-image
              v-for="(url, i) in pet.vaccineCerts"
              :key="url"
              :src="url"
              fit="cover"
              class="cert-thumb"
              :preview-src-list="pet.vaccineCerts"
              :initial-index="i"
              preview-teleported
            />
          </div>

          <footer class="pet-actions">
            <span class="pet-time">建档 {{ pet.createTime }}</span>
            <span class="pet-btns">
              <el-button link type="primary" @click="openEdit(pet)">编辑</el-button>
              <el-button link type="danger" @click="onDelete(pet)">删除</el-button>
            </span>
          </footer>
        </article>
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑宠物档案' : '新增宠物档案'"
      width="640px"
      top="6vh"
      @closed="onDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="宠物昵称" prop="name">
          <el-input v-model="form.name" placeholder="例如：豆豆" maxlength="50" show-word-limit clearable />
        </el-form-item>

        <el-form-item label="物种" prop="species">
          <el-radio-group v-model="form.species">
            <el-radio-button v-for="s in SPECIES" :key="s" :value="s">{{ s }}</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="品种" prop="breed">
          <el-input v-model="form.breed" placeholder="例如：柯基 / 英国短毛猫" maxlength="50" clearable />
        </el-form-item>

        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="form.gender">
            <el-radio-button :value="0">未知</el-radio-button>
            <el-radio-button :value="1">公</el-radio-button>
            <el-radio-button :value="2">母</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="年龄" prop="ageMonths">
          <div class="age-selects">
            <el-select v-model="ageYears" aria-label="宠物年龄岁数">
              <el-option v-for="year in AGE_YEAR_OPTIONS" :key="year" :label="`${year} 岁`" :value="year" />
            </el-select>
            <el-select v-model="ageRemainderMonths" aria-label="宠物年龄月数">
              <el-option
                v-for="month in availableAgeMonthOptions"
                :key="month"
                :label="`${month} 个月`"
                :value="month"
              />
            </el-select>
          </div>
          <span class="hint">{{ petAgeText(form.ageMonths) }}</span>
        </el-form-item>

        <el-form-item label="体重" prop="weightKg">
          <el-input-number v-model="form.weightKg" :min="0" :max="9999.99" :precision="2" :step="0.5" controls-position="right" />
          <span class="unit">kg</span>
        </el-form-item>

        <el-form-item label="宠物照片">
          <ImageUpload v-model="form.avatar" biz-type="pet" :limit="1" />
        </el-form-item>

        <el-form-item label="疫苗证明">
          <ImageUpload v-model="form.vaccineCerts" biz-type="cert" :limit="5" />
          <div class="form-tip">最多 5 张，支持 jpg / png / webp</div>
        </el-form-item>

        <el-form-item label="性格习性" prop="personality">
          <el-input
            v-model="form.personality"
            type="textarea"
            :rows="2"
            maxlength="255"
            show-word-limit
            placeholder="例如：亲人活泼，见到陌生狗会兴奋吠叫"
          />
        </el-form-item>

        <el-form-item label="喂养禁忌" prop="feedingTaboo">
          <el-input
            v-model="form.feedingTaboo"
            type="textarea"
            :rows="2"
            maxlength="500"
            show-word-limit
            placeholder="例如：禁食巧克力、葡萄、洋葱；每日狗粮定量 150g，分两餐"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ImageUpload from '@/components/ImageUpload.vue'
import { listMyPets, createPet, updatePet, deletePet } from '@/api/pet'
import { petAgeText } from '@/utils/format'

const SPECIES = ['狗', '猫', '其他']
const GENDER_TEXT = { 0: '性别未知', 1: '♂ 公', 2: '♀ 母' }
const AGE_YEAR_OPTIONS = Array.from({ length: 51 }, (_, year) => year)
const AGE_MONTH_OPTIONS = Array.from({ length: 12 }, (_, month) => month)

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const pets = ref([])
const formRef = ref()

const emptyForm = () => ({
  id: null,
  name: '',
  species: '狗',
  breed: '',
  gender: 0,
  ageMonths: 0,
  weightKg: null,
  avatar: '',
  vaccineCerts: [],
  personality: '',
  feedingTaboo: ''
})

const form = reactive(emptyForm())

// 接口和数据库继续使用总月数，界面只负责在“岁 / 月”之间双向换算。
// 后端年龄上限是 600 个月，因此选择 50 岁时月份只能为 0。
const ageYears = computed({
  get: () => Math.floor(normalizeAgeMonths(form.ageMonths) / 12),
  set: (years) => {
    const year = Number(years)
    const month = year >= 50 ? 0 : ageRemainderMonths.value
    form.ageMonths = year * 12 + month
  }
})

const ageRemainderMonths = computed({
  get: () => normalizeAgeMonths(form.ageMonths) % 12,
  set: (months) => {
    form.ageMonths = ageYears.value * 12 + Number(months)
  }
})

const availableAgeMonthOptions = computed(() => (ageYears.value >= 50 ? [0] : AGE_MONTH_OPTIONS))

const rules = {
  name: [
    { required: true, message: '请填写宠物昵称', trigger: 'blur' },
    { max: 50, message: '昵称不能超过 50 字', trigger: 'blur' }
  ],
  species: [{ required: true, message: '请选择物种', trigger: 'change' }]
}

function speciesEmoji(species) {
  if (species === '狗') return '🐶'
  if (species === '猫') return '🐱'
  return '🐾'
}

function normalizeAgeMonths(months) {
  const value = Number(months)
  if (!Number.isFinite(value) || value < 0) return 0
  return Math.min(Math.floor(value), 600)
}

async function loadPets() {
  loading.value = true
  try {
    pets.value = await listMyPets()
  } catch {
    // 失败原因已由 request.js 的响应拦截器弹出，页面不再重复提示
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

function openEdit(pet) {
  // 逐字段映射而不是 {...pet}：pet 里还有 createTime 等只读出参，
  // 混进 form 后会随保存请求一起发给后端。
  Object.assign(form, {
    id: pet.id,
    name: pet.name ?? '',
    species: pet.species ?? '狗',
    breed: pet.breed ?? '',
    gender: pet.gender ?? 0,
    ageMonths: normalizeAgeMonths(pet.ageMonths),
    weightKg: pet.weightKg ?? null,
    avatar: pet.avatar ?? '',
    // 拷一份：ImageUpload 内部会改写这个数组，直接引用会污染列表里的原始数据
    vaccineCerts: [...(pet.vaccineCerts ?? [])],
    personality: pet.personality ?? '',
    feedingTaboo: pet.feedingTaboo ?? ''
  })
  dialogVisible.value = true
}

function onDialogClosed() {
  formRef.value?.clearValidate()
  Object.assign(form, emptyForm())
}

async function onSubmit() {
  // validate() 校验不过时是 reject，不接住会在控制台留下未处理的 Promise 异常
  const ok = await formRef.value.validate().catch(() => false)
  if (!ok) return

  submitting.value = true
  try {
    const { id, ...payload } = form
    if (id) {
      await updatePet(id, payload)
      ElMessage.success('已保存')
    } else {
      await createPet(payload)
      ElMessage.success('已添加')
    }
    dialogVisible.value = false
    await loadPets()
  } catch {
    // 同上，拦截器已提示
  } finally {
    submitting.value = false
  }
}

async function onDelete(pet) {
  const confirmed = await ElMessageBox.confirm(`确定删除「${pet.name}」的档案吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  }).catch(() => false)
  if (!confirmed) return

  try {
    await deletePet(pet.id)
    ElMessage.success('已删除')
    await loadPets()
  } catch {
    // 同上，拦截器已提示
  }
}

onMounted(loadPets)
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

.list-card {
  min-height: 200px;
}

.pet-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.pet-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 16px;
  border: 1px solid var(--pp-tint-2);
  border-radius: var(--pp-radius);
  background: #fff;
  transition: box-shadow 0.2s ease, transform 0.2s ease;
}

.pet-card:hover {
  box-shadow: var(--pp-shadow-hover);
  transform: translateY(-2px);
}

.pet-top {
  display: flex;
  gap: 12px;
  align-items: center;
}

.pet-avatar {
  width: 64px;
  height: 64px;
  flex: 0 0 64px;
  border-radius: 12px;
  background: var(--pp-tint);
}

.pet-avatar-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
}

.pet-id {
  min-width: 0;
}

.pet-name {
  margin: 0 0 6px;
  font-size: 17px;
}

.pet-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.pet-facts {
  display: flex;
  gap: 24px;
  margin: 0;
  padding: 10px 0;
  border-top: 1px dashed var(--pp-tint-2);
  border-bottom: 1px dashed var(--pp-tint-2);
}

.pet-facts dt {
  font-size: 12px;
  color: var(--pp-muted);
}

.pet-facts dd {
  margin: 2px 0 0;
  font-size: 14px;
  font-weight: 600;
}

.pet-note {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--pp-ink);
}

.note-label {
  display: inline-block;
  min-width: 32px;
  margin-right: 8px;
  font-size: 12px;
  color: var(--pp-muted);
}

.pet-certs {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.cert-thumb {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  border: 1px solid var(--pp-tint-2);
}

.pet-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
  padding-top: 8px;
}

.pet-time {
  font-size: 12px;
  color: var(--pp-muted);
}

.unit {
  margin-left: 8px;
  color: var(--pp-muted);
  font-size: 13px;
}

.age-selects {
  display: grid;
  grid-template-columns: repeat(2, 132px);
  gap: 10px;
}

.age-selects .el-select {
  width: 100%;
}

.hint {
  margin-left: 12px;
  font-size: 12px;
  color: var(--pp-primary);
}

@media (max-width: 520px) {
  .age-selects {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    width: 100%;
  }

  .hint {
    width: 100%;
    margin-left: 0;
  }
}

.form-tip {
  width: 100%;
  font-size: 12px;
  color: var(--pp-muted);
  line-height: 1.6;
}
</style>
