<template>
  <div class="page-container">
    <el-card class="head-card">
      <div class="head">
        <div class="head-text">
          <h2 class="title">🧭 接单大厅</h2>
          <p class="subtitle">按距离由近到远列出附近待接单的订单。抢单成功后资金仍由平台担保，服务验收通过才结算给你。</p>
        </div>
        <div class="head-side">
          <el-tag v-if="profile" :type="AUDIT_TAG[profile.auditStatus] ?? 'info'" effect="light">
            资质{{ profile.auditStatusText }}
          </el-tag>
          <el-button :disabled="!approved" @click="openProfileDialog">资质资料</el-button>
        </div>
      </div>
    </el-card>

    <!-- 未过审就不渲染大厅：光把抢单按钮藏起来没用，后端 requireGrabable 一样会拒（1005） -->
    <el-card v-if="!loadingProfile && !approved" class="section-card">
      <el-alert
        :type="profile?.auditStatus === 2 ? 'error' : 'info'"
        show-icon
        :closable="false"
        :title="profile?.auditStatus === 2 ? '资质审核未通过' : '资质审核中，暂时还不能接单'"
      >
        <template #default>
          <p v-if="profile?.auditStatus === 2 && profile.auditRemark" class="alert-line">
            驳回原因：{{ profile.auditRemark }}
          </p>
          <p class="alert-line">
            {{
              profile?.auditStatus === 2
                ? '按驳回原因修改资料后重新提交，审核通过即可开始抢单。'
                : '平台审核通过后，这里会出现附近的待接单订单。'
            }}
          </p>
          <el-button type="primary" size="small" @click="openProfileDialog">
            {{ profile?.auditStatus === 2 ? '重新提交资料' : '查看 / 补充资料' }}
          </el-button>
        </template>
      </el-alert>

      <el-descriptions v-if="profile?.realName" class="profile-brief" :column="2" border size="small">
        <el-descriptions-item label="真实姓名">{{ profile.realName }}</el-descriptions-item>
        <el-descriptions-item label="身份证号">{{ profile.idCardMasked || '未填写' }}</el-descriptions-item>
        <el-descriptions-item label="经验年限">{{ profile.experienceYears ?? 0 }} 年</el-descriptions-item>
        <el-descriptions-item label="信誉等级">{{ profile.creditLevel ?? '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <template v-else-if="approved">
      <el-card class="section-card">
        <div class="filter-bar">
          <div class="filter-item">
            <span class="filter-label">我的位置</span>
            <span class="filter-value">{{ center.lng.toFixed(4) }}, {{ center.lat.toFixed(4) }}</span>
            <el-tag size="small" effect="plain" type="info">{{ LOCATION_SOURCE_TEXT[locationSource] }}</el-tag>
            <el-button link type="primary" :loading="locating" @click="relocate">重新定位</el-button>
            <el-button link type="primary" @click="openLocationDialog">填写位置</el-button>
          </div>
          <div class="filter-item">
            <span class="filter-label">检索半径</span>
            <el-radio-group v-model="radiusKm" size="small" @change="onFilterChange">
              <el-radio-button v-for="r in RADIUS_OPTIONS" :key="r" :value="r">{{ r }} km</el-radio-button>
            </el-radio-group>
          </div>
          <el-button size="small" :loading="loading" @click="load">刷新</el-button>
        </div>
      </el-card>

      <el-card v-if="showMap" class="section-card map-card">
        <AmapView :center="[center.lng, center.lat]" :zoom="13" height="340px" @loaded="onMapLoaded" @error="onMapError" />
        <p class="map-hint">每个标记是一单待接订单，点下方的订单卡片可以把地图移到它的位置。</p>
      </el-card>
      <el-alert
        v-else
        class="section-card"
        type="info"
        show-icon
        :closable="false"
        :title="mapBroken ? '地图加载失败，已切换为列表模式' : '地图未启用，已切换为列表模式'"
      >
        <template #default>
          {{
            mapBroken
              ? '高德地图加载失败，通常是 key 无效、域名未加白名单或配额用尽。'
              : '未配置高德地图 key（.env.development 里的 VITE_AMAP_KEY）。'
          }}
          抢单完全不受影响，下面的列表已经按距离由近到远排好。
        </template>
      </el-alert>

      <el-card>
        <div v-loading="loading" class="list-wrap">
          <el-empty v-if="!loading && orders.length === 0" :description="`附近 ${radiusKm} 公里内暂时没有待接单的订单`">
            <el-button @click="widenRadius">扩大检索范围</el-button>
          </el-empty>

          <article v-for="o in orders" :key="o.id" class="hall-card" @click="focusOnMap(o)">
            <header class="hall-top">
              <div class="hall-title">
                <span class="hall-emoji">{{ CATEGORY_EMOJI[o.categoryCode] ?? '🐾' }}</span>
                <span class="hall-name">{{ o.categoryName || '未知服务' }}</span>
                <el-tag v-if="o.unit" size="small" effect="plain" type="info">/ {{ o.unit }}</el-tag>
              </div>
              <span class="hall-distance">📍 {{ distanceText(o.distanceKm) || '距离未知' }}</span>
            </header>

            <div class="hall-body">
              <div class="hall-line">
                <span class="line-label">宠物</span>
                <span>{{ o.petName || '未知' }}<em v-if="o.petSpecies" class="muted"> · {{ o.petSpecies }}</em></span>
              </div>
              <div class="hall-line">
                <span class="line-label">时间</span>
                <span>{{ o.serviceStart }}<template v-if="o.serviceEnd"> ~ {{ o.serviceEnd }}</template></span>
              </div>
              <div class="hall-line">
                <span class="line-label">地址</span>
                <span>{{ o.serviceAddress }}</span>
              </div>
            </div>

            <footer class="hall-foot">
              <div class="income">
                <span class="income-label">到手</span>
                <span class="income-value">¥{{ money(o.sitterIncome) }}</span>
                <span class="income-note">订单总额 ¥{{ money(o.amount) }}</span>
              </div>
              <el-button type="primary" :loading="grabbingId === o.id" @click.stop="onGrab(o)">立即抢单</el-button>
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
    </template>

    <el-dialog v-model="locationDialogVisible" title="设置接单搜索位置" width="560px">
      <el-form ref="locationFormRef" :model="locationForm" :rules="locationRules" label-width="86px">
        <el-form-item label="位置名称">
          <el-autocomplete
            v-model="locationForm.address"
            class="location-search"
            placeholder="输入小区、学校、商场或详细地址"
            :fetch-suggestions="searchLocationSuggestions"
            :trigger-on-focus="false"
            :debounce="300"
            value-key="value"
            clearable
            @select="selectLocationSuggestion"
          >
            <template #default="{ item }">
              <div class="location-option">
                <strong>{{ item.name }}</strong>
                <span>{{ item.detail || '暂无详细地址' }}</span>
              </div>
            </template>
          </el-autocomplete>
          <div class="form-tip">输入关键词后请选择一个候选地点，系统会自动填入经纬度。</div>
          <div v-if="locationSearchError" class="form-tip search-error">{{ locationSearchError }}</div>
        </el-form-item>
        <el-form-item label="位置坐标" prop="lng">
          <div class="coordinate-row">
            <el-input-number
              v-model="locationForm.lng"
              :controls="false"
              :precision="7"
              :min="-180"
              :max="180"
              placeholder="经度"
            />
            <el-input-number
              v-model="locationForm.lat"
              :controls="false"
              :precision="7"
              :min="-90"
              :max="90"
              placeholder="纬度"
            />
          </div>
          <div class="form-tip">也可以直接填写经纬度；保存后立即以此位置刷新附近订单。</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="locationDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingLocation" @click="saveManualLocation">保存并搜索</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="profileDialogVisible" title="接单员资质" width="560px">
      <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="96px">
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="profileForm.realName" maxlength="50" placeholder="与身份证一致" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="profileForm.idCard" maxlength="18" placeholder="18 位身份证号" />
          <div v-if="profile?.idCardFilled" class="form-tip">
            已登记：{{ profile.idCardMasked }}。平台只回传脱敏值，变更时请重新填写完整号码。
          </div>
        </el-form-item>
        <el-form-item label="经验年限" prop="experienceYears">
          <el-input-number v-model="profileForm.experienceYears" :min="0" :max="60" :step="1" />
        </el-form-item>
        <el-form-item label="身份证照片">
          <ImageUpload v-model="profileForm.idCardImg" biz-type="cert" :limit="1" />
        </el-form-item>
        <el-form-item label="健康证明">
          <ImageUpload v-model="profileForm.healthCert" biz-type="cert" :limit="1" />
        </el-form-item>
        <el-form-item label="资质证书">
          <ImageUpload v-model="profileForm.qualification" biz-type="cert" :limit="1" />
        </el-form-item>
      </el-form>
      <p class="dialog-note">提交后进入待审核状态，由平台管理员审核；被驳回后可按原因修改并重新提交。</p>
      <template #footer>
        <el-button @click="profileDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingProfile" @click="onSubmitProfile">提交审核</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AmapView from '@/components/AmapView.vue'
import ImageUpload from '@/components/ImageUpload.vue'
import { getMySitterProfile, grabOrder, pageHallOrders, submitSitterProfile, updateMyLocation } from '@/api/sitter'
import { distanceText, money } from '@/utils/format'
import { getCurrentPosition, searchPois } from '@/utils/amap'

const router = useRouter()

const AUDIT_TAG = { 0: 'warning', 1: 'success', 2: 'danger' }
const RADIUS_OPTIONS = [1, 3, 5, 10, 20]
const LOCATION_SOURCE_TEXT = { geo: '浏览器定位', manual: '手动位置', profile: '备用位置', default: '演示默认坐标' }
// 与种子接单员、演示订单一致的坐标：定位不可用时用它兜底，大厅仍能检索到演示订单
const DEFAULT_CENTER = { lng: 121.4737, lat: 31.2304 }
const CATEGORY_EMOJI = { FEEDING: '🍚', GROOMING: '🛁', WALKING: '🦮', COMPANION: '🧸' }

const hasAmapKey = computed(() => !!import.meta.env.VITE_AMAP_KEY)
// key 配了但无效 / 超配额 / 断网时 AmapView 会 emit error，此时退回纯列表模式，
// 否则页面上只剩一个 340px 的灰色空盒子，除了控制台没有任何提示。
const mapBroken = ref(false)
const showMap = computed(() => hasAmapKey.value && !mapBroken.value)

const profile = ref(null)
const loadingProfile = ref(true)
const approved = computed(() => profile.value?.auditStatus === 1)

const center = reactive({ ...DEFAULT_CENTER })
const locationSource = ref('default')
const locating = ref(false)
const radiusKm = ref(5)

const locationDialogVisible = ref(false)
const savingLocation = ref(false)
const locationFormRef = ref(null)
const locationSearchError = ref('')
const locationForm = reactive({ address: '', lng: null, lat: null })
const locationRules = {
  lng: [
    { required: true, message: '请搜索并选择位置，或填写经纬度', trigger: 'change' },
    {
      validator: (_rule, _value, callback) => {
        const lng = Number(locationForm.lng)
        const lat = Number(locationForm.lat)
        if (!Number.isFinite(lng) || lng < -180 || lng > 180 || !Number.isFinite(lat) || lat < -90 || lat > 90) {
          callback(new Error('经纬度取值不合法'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ]
}
let locationSearchSequence = 0

const orders = ref([])
const total = ref(0)
const loading = ref(false)
const grabbingId = ref(null)
const query = reactive({ page: 1, size: 10 })

const profileDialogVisible = ref(false)
const submittingProfile = ref(false)
const profileFormRef = ref(null)
const profileForm = reactive({
  realName: '',
  idCard: '',
  idCardImg: '',
  healthCert: '',
  qualification: '',
  experienceYears: 0
})
const profileRules = {
  realName: [{ required: true, message: '请填写真实姓名', trigger: 'blur' }],
  idCard: [
    { required: true, message: '请填写身份证号', trigger: 'blur' },
    { pattern: /^\d{17}[\dXx]$/, message: '身份证号格式不正确', trigger: 'blur' }
  ]
}

// 地图实例与标记一律用普通变量而不是 ref：AMap 对象内部有大量循环引用与私有状态，
// 被 Vue 的 reactive 代理包过之后会直接崩在 setter 上。
let amapRef = null
let mapInstance = null
let markers = []

async function load() {
  loading.value = true
  try {
    const res = await pageHallOrders({
      lng: center.lng,
      lat: center.lat,
      radiusKm: radiusKm.value,
      page: query.page,
      size: query.size
    })
    orders.value = res.records ?? []
    total.value = res.total ?? 0
    renderMarkers()
  } catch {
    // 错误提示已由 request.js 拦截器统一弹过
  } finally {
    loading.value = false
  }
}

function applyCenter(lng, lat, source) {
  center.lng = Number(Number(lng).toFixed(7))
  center.lat = Number(Number(lat).toFixed(7))
  locationSource.value = source
  mapInstance?.setCenter([center.lng, center.lat])
}

/** 浏览器定位只在 HTTPS 或 localhost 下可用，被拒 / 超时时退回档案坐标，再退回演示默认值。 */
async function relocate() {
  locating.value = true
  try {
    const pos = await getCurrentPosition()
    applyCenter(pos.lng, pos.lat, 'geo')
  } catch {
    if (profile.value?.currentLng && profile.value?.currentLat) {
      applyCenter(profile.value.currentLng, profile.value.currentLat, 'profile')
    } else {
      applyCenter(DEFAULT_CENTER.lng, DEFAULT_CENTER.lat, 'default')
    }
    ElMessage.warning('浏览器定位不可用，已改用备用坐标检索')
  } finally {
    locating.value = false
  }
  await load()
}

function openLocationDialog() {
  Object.assign(locationForm, {
    address: '',
    lng: center.lng,
    lat: center.lat
  })
  locationSearchError.value = ''
  locationDialogVisible.value = true
}

async function searchLocationSuggestions(keyword, callback) {
  const query = keyword?.trim()
  const sequence = ++locationSearchSequence
  locationSearchError.value = ''
  if (!query) {
    callback([])
    return
  }
  try {
    const pois = await searchPois(query)
    if (sequence !== locationSearchSequence) return
    callback(pois.map((poi) => ({
      ...poi,
      detail: `${poi.district}${poi.address}`,
      value: `${poi.name}${poi.district || poi.address ? ` · ${poi.district}${poi.address}` : ''}`
    })))
  } catch {
    if (sequence !== locationSearchSequence) return
    locationSearchError.value = '地点搜索失败，请检查高德地图配置，或直接填写经纬度。'
    callback([])
  }
}

function selectLocationSuggestion(item) {
  locationForm.address = item.value
  locationForm.lng = item.lng
  locationForm.lat = item.lat
  locationFormRef.value?.clearValidate('lng')
}

async function saveManualLocation() {
  const ok = await locationFormRef.value.validate().catch(() => false)
  if (!ok) return
  savingLocation.value = true
  try {
    profile.value = await updateMyLocation({ lng: locationForm.lng, lat: locationForm.lat })
    applyCenter(locationForm.lng, locationForm.lat, 'manual')
    query.page = 1
    locationDialogVisible.value = false
    await load()
    ElMessage.success('备用位置已保存，并已刷新附近订单')
  } catch {
    // 拦截器已提示
  } finally {
    savingLocation.value = false
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

function widenRadius() {
  const next = RADIUS_OPTIONS.find((r) => r > radiusKm.value)
  if (!next) {
    ElMessage.info('已经是最大检索范围了')
    return
  }
  radiusKm.value = next
  onFilterChange()
}

function onMapLoaded({ AMap, map }) {
  amapRef = AMap
  mapInstance = map
  map.setCenter([center.lng, center.lat])
  renderMarkers()
}

function onMapError() {
  // AmapView 内部已 console.error，这里切到列表模式，别留一个空白灰盒子
  mapBroken.value = true
  amapRef = null
  mapInstance = null
  markers = []
}

function renderMarkers() {
  if (!mapInstance || !amapRef) return
  if (markers.length) {
    mapInstance.remove(markers)
    markers = []
  }
  markers = orders.value
    .filter((o) => o.addressLng != null && o.addressLat != null)
    .map((o) => {
      const marker = new amapRef.Marker({
        position: [Number(o.addressLng), Number(o.addressLat)],
        title: `${o.categoryName || '服务'} · ${distanceText(o.distanceKm)} · 到手 ¥${money(o.sitterIncome)}`
      })
      mapInstance.add(marker)
      return marker
    })
}

function focusOnMap(order) {
  if (!mapInstance || order.addressLng == null || order.addressLat == null) return
  mapInstance.setZoomAndCenter(15, [Number(order.addressLng), Number(order.addressLat)])
}

async function onGrab(order) {
  try {
    await ElMessageBox.confirm(
      `${order.categoryName || '服务'} · ${order.serviceStart}\n${order.serviceAddress}\n验收通过后到手 ¥${money(order.sitterIncome)}`,
      '确认抢单',
      { type: 'warning', confirmButtonText: '确认接单', cancelButtonText: '再看看' }
    )
  } catch {
    return
  }

  grabbingId.value = order.id
  let grabbed = false
  try {
    await grabOrder(order.id)
    grabbed = true
    ElMessage.success('抢单成功，已加入「我的接单」')
  } catch {
    // 2002（被人抢先）等原因拦截器已提示，这里再弹一次就是双重提示
  } finally {
    grabbingId.value = null
  }

  if (grabbed) {
    router.push('/sitter/orders')
  } else {
    // 失败说明这一单已被别人抢走或已取消，列表已经和服务端不一致了，必须刷新
    await load()
  }
}

function openProfileDialog() {
  Object.assign(profileForm, {
    realName: profile.value?.realName ?? '',
    // 后端只回脱敏值，绝不能拿 idCardMasked 回填：那会把 310101********1234 当成新号码提交上去
    idCard: '',
    idCardImg: profile.value?.idCardImg ?? '',
    healthCert: profile.value?.healthCert ?? '',
    qualification: profile.value?.qualification ?? '',
    experienceYears: profile.value?.experienceYears ?? 0
  })
  profileDialogVisible.value = true
}

async function onSubmitProfile() {
  const ok = await profileFormRef.value.validate().catch(() => false)
  if (!ok) return
  submittingProfile.value = true
  try {
    profile.value = await submitSitterProfile({ ...profileForm })
    profileDialogVisible.value = false
    ElMessage.success('资质已提交，等待平台审核')
    if (approved.value) await load()
  } catch {
    // 拦截器已提示
  } finally {
    submittingProfile.value = false
  }
}

onBeforeUnmount(() => {
  markers = []
  mapInstance = null
  amapRef = null
})

onMounted(async () => {
  try {
    profile.value = await getMySitterProfile()
  } catch {
    // 拦截器已提示
  } finally {
    loadingProfile.value = false
  }
  if (!approved.value) return

  // 先用档案坐标或演示默认值出一屏，再异步去要浏览器定位：
  // 定位可能弹权限框卡住、也可能直接被拒，不该让整个大厅干等着。
  if (profile.value?.currentLng && profile.value?.currentLat) {
    applyCenter(profile.value.currentLng, profile.value.currentLat, 'profile')
  }
  await load()
  relocate()
})
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
  flex-wrap: wrap;
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

.head-side {
  display: flex;
  align-items: center;
  gap: 10px;
}

.section-card {
  margin-bottom: 16px;
}

.alert-line {
  margin: 0 0 6px;
  font-size: 13px;
  line-height: 1.6;
}

.profile-brief {
  margin-top: 16px;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  font-size: 13px;
  color: var(--pp-muted);
}

.filter-value {
  font-family: monospace;
  font-size: 13px;
}

.location-search {
  width: 100%;
}

.location-option {
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: 4px 0;
  line-height: 1.5;
}

.location-option strong,
.location-option span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.location-option span {
  color: var(--pp-muted);
  font-size: 12px;
}

.coordinate-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  width: 100%;
}

.coordinate-row .el-input-number {
  width: 100%;
}

.search-error {
  color: var(--el-color-danger);
}

.map-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: var(--pp-muted);
}

.list-wrap {
  min-height: 220px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hall-card {
  padding: 16px;
  border: 1px solid var(--pp-tint-2);
  border-radius: var(--pp-radius);
  background: #fff;
  cursor: pointer;
  transition: box-shadow 0.2s ease, border-color 0.2s ease;
}

.hall-card:hover {
  box-shadow: var(--pp-shadow-hover);
  border-color: var(--pp-primary);
}

.hall-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 10px;
  border-bottom: 1px dashed var(--pp-tint-2);
}

.hall-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.hall-emoji {
  font-size: 20px;
}

.hall-name {
  font-weight: 600;
}

.hall-distance {
  font-size: 13px;
  color: var(--pp-primary);
  font-weight: 600;
  white-space: nowrap;
}

.hall-body {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 8px 24px;
  padding: 12px 0;
}

.hall-line {
  display: flex;
  gap: 8px;
  font-size: 13px;
  align-items: baseline;
}

.line-label {
  flex: 0 0 34px;
  color: var(--pp-muted);
}

.muted {
  font-style: normal;
  color: var(--pp-muted);
}

.hall-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 10px;
  border-top: 1px dashed var(--pp-tint-2);
}

.income {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.income-label {
  font-size: 12px;
  color: var(--pp-muted);
}

.income-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--pp-primary);
}

.income-note {
  font-size: 12px;
  color: var(--pp-muted);
}

.pager {
  margin-top: 16px;
  justify-content: center;
}

.form-tip {
  font-size: 12px;
  color: var(--pp-muted);
  line-height: 1.6;
}

.dialog-note {
  margin: 0;
  font-size: 12px;
  color: var(--pp-muted);
}
</style>
