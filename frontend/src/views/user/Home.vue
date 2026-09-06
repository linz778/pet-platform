<template>
  <div class="page-container">
    <el-card class="head-card">
      <div class="head">
        <div class="head-text">
          <h2 class="title">🏠 预约上门服务</h2>
          <p class="subtitle">选一项服务、一只宠物、一个上门时间，地图上标好地址就能下单。支付后进入接单大厅等待接单员抢单。</p>
        </div>
        <div class="wallet-strip">
          <div class="wallet-item">
            <span class="wallet-label">可用余额</span>
            <span class="wallet-value">¥{{ money(wallet.balance) }}</span>
          </div>
          <div class="wallet-item">
            <span class="wallet-label">担保中</span>
            <span class="wallet-value muted">¥{{ money(wallet.frozen) }}</span>
          </div>
          <el-button type="primary" plain @click="openRecharge">充值</el-button>
        </div>
      </div>
    </el-card>

    <el-alert
      v-if="shortOfBalance"
      class="balance-alert"
      type="warning"
      show-icon
      :closable="false"
      title="余额不足以支付这一单"
    >
      <template #default>
        当前可用 ¥{{ money(wallet.balance) }}，本单需 ¥{{ money(price.amount) }}。可以先下单再去「我的订单」充值后支付。
        <el-button link type="primary" @click="openRecharge">立即充值</el-button>
      </template>
    </el-alert>

    <el-card v-loading="loadingCategories" class="section-card">
      <h3 class="section-title">1. 选择服务</h3>
      <el-empty v-if="!loadingCategories && categories.length === 0" description="暂无上架服务，请联系平台" />
      <div v-else class="cat-grid">
        <button
          v-for="c in categories"
          :key="c.id"
          type="button"
          class="cat-card"
          :class="{ active: form.categoryId === c.id }"
          @click="pickCategory(c.id)"
        >
          <span class="cat-emoji">{{ CATEGORY_EMOJI[c.code] ?? '🐾' }}</span>
          <span class="cat-name">{{ c.name }}</span>
          <span class="cat-price">¥{{ money(c.basePrice) }}<em>/{{ c.unit || '次' }}</em></span>
          <span class="cat-hint">周末 ×{{ c.holidayRate }}</span>
        </button>
      </div>
    </el-card>

    <el-card class="section-card">
      <h3 class="section-title">2. 填写预约信息</h3>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px" label-position="right">
        <el-form-item label="服务宠物" prop="petId">
          <el-select v-model="form.petId" placeholder="请选择宠物" class="full" :disabled="pets.length === 0">
            <el-option v-for="p in pets" :key="p.id" :label="p.name" :value="p.id">
              <span>{{ p.name }}</span>
              <span class="opt-hint">{{ p.species || '未填物种' }} · {{ petAgeText(p.ageMonths) }}</span>
            </el-option>
          </el-select>
          <div v-if="pets.length === 0" class="form-tip">
            还没有宠物档案，
            <el-button link type="primary" @click="router.push('/user/pets')">先去添加一只</el-button>
          </div>
          <div v-else-if="selectedPet && selectedPet.feedingTaboo" class="form-tip">
            喂养禁忌：{{ selectedPet.feedingTaboo }}
          </div>
        </el-form-item>

        <el-form-item label="开始时间" prop="serviceStart">
          <el-date-picker
            v-model="form.serviceStart"
            type="datetime"
            placeholder="选择上门时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            :disabled-date="disabledDate"
            class="full"
          />
        </el-form-item>

        <el-form-item label="结束时间" prop="serviceEnd">
          <el-date-picker
            v-model="form.serviceEnd"
            type="datetime"
            placeholder="可不填"
            value-format="YYYY-MM-DD HH:mm:ss"
            :disabled-date="disabledDate"
            class="full"
          />
        </el-form-item>

        <el-form-item label="服务地址" prop="serviceAddress">
          <el-autocomplete
            v-model="form.serviceAddress"
            class="full"
            placeholder="输入地点名称，例如：明珠中学"
            maxlength="255"
            show-word-limit
            :fetch-suggestions="searchAddressSuggestions"
            :trigger-on-focus="false"
            :debounce="300"
            value-key="value"
            @select="selectAddress"
          >
            <template #default="{ item }">
              <div class="address-option">
                <div class="address-option-main">
                  <strong>{{ item.name }}</strong>
                  <el-tag v-if="item.distanceText" size="small" type="info" effect="plain">
                    {{ item.distanceText }}
                  </el-tag>
                </div>
                <span>{{ item.addressText || '暂无详细地址' }}</span>
              </div>
            </template>
          </el-autocomplete>
          <div class="form-tip">
            输入第一个字即搜索当前坐标 1 公里内的地点；选择候选项会自动更新地址坐标和地图标记。
          </div>
          <div v-if="addressSearchError" class="form-tip search-error">{{ addressSearchError }}</div>
        </el-form-item>

        <el-form-item label="地址坐标" prop="addressLng">
          <div class="geo-row">
            <el-input-number v-model="form.addressLng" :precision="7" :step="0.001" :min="-180" :max="180" :controls="false" placeholder="经度" />
            <el-input-number v-model="form.addressLat" :precision="7" :step="0.001" :min="-90" :max="90" :controls="false" placeholder="纬度" />
            <el-button :loading="locating" @click="locateMe">用我的位置</el-button>
          </div>
          <div class="form-tip">
            坐标用于接单大厅的附近检索与接单员到达打卡的距离校验，务必标准。
          </div>
        </el-form-item>

        <el-form-item v-if="showMap" label="地图选点">
          <div class="map-wrap">
            <AmapView :center="mapCenter" :zoom="14" height="320px" @loaded="onMapLoaded" @error="onMapError" />
            <div class="map-hint">点击地图或拖动标记即可设置坐标</div>
          </div>
        </el-form-item>
        <el-form-item v-else label="地图选点">
          <el-alert type="info" show-icon :closable="false" :title="mapBroken ? '地图加载失败' : '地图未启用'">
            <template #default>
              <template v-if="mapBroken">
                高德地图加载失败，通常是 key 无效、域名未加白名单或配额用尽。
              </template>
              <template v-else>
                未配置高德地图 key（<code>.env.development</code> 里的 <code>VITE_AMAP_KEY</code>）。
              </template>
              请直接在上方手填经纬度，或点「用我的位置」。填好坐标后下单流程完全不受影响。
            </template>
          </el-alert>
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="门禁密码、猫砂位置、特殊注意事项等" />
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="section-card summary-card">
      <h3 class="section-title">3. 费用确认</h3>
      <div v-if="price" class="summary">
        <div class="sum-row">
          <span>{{ price.categoryName }}</span>
          <span class="sum-val">¥{{ money(price.amount) }}</span>
        </div>
        <div class="sum-row">
          <span>上门时间</span>
          <span class="sum-val">
            {{ form.serviceStart || '未选择' }}
            <el-tag v-if="price.holiday" size="small" type="warning" effect="light">周末溢价</el-tag>
          </span>
        </div>
        <div class="sum-row total">
          <span>应付总额</span>
          <span class="sum-val">¥{{ money(price.amount) }}</span>
        </div>
        <p class="sum-note">支付后资金进入平台担保，验收通过才结算给接单员。取消订单全额退回余额。</p>
      </div>
      <el-empty v-else :image-size="60" description="选好服务与上门时间后显示价格" />

      <el-button type="primary" size="large" class="submit-btn" :loading="submitting" @click="onSubmit">
        立即下单
      </el-button>
    </el-card>

    <el-dialog v-model="rechargeVisible" title="钱包充值" width="420px">
      <el-form label-width="80px">
        <el-form-item label="充值金额">
          <el-input-number v-model="rechargeAmount" :min="1" :max="10000" :precision="2" :step="100" class="full" />
        </el-form-item>
        <el-form-item label="快捷选择">
          <el-radio-group v-model="rechargeAmount">
            <el-radio-button :value="100">100</el-radio-button>
            <el-radio-button :value="500">500</el-radio-button>
            <el-radio-button :value="1000">1000</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <p class="dialog-note">本期为模拟支付通道，不接真实资金，充值即时到账。单次限额 1 - 10000 元。</p>
      <template #footer>
        <el-button @click="rechargeVisible = false">取消</el-button>
        <el-button type="primary" :loading="recharging" @click="onRecharge">确认充值</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AmapView from '@/components/AmapView.vue'
import { listCategories, previewPrice } from '@/api/serviceCategory'
import { listMyPets } from '@/api/pet'
import { getMyWallet, recharge } from '@/api/wallet'
import { createOrder } from '@/api/order'
import { money, petAgeText, formatDateTime } from '@/utils/format'
import { getCurrentPosition, searchNearbyPois } from '@/utils/amap'

const router = useRouter()

const CATEGORY_EMOJI = { FEEDING: '🍚', GROOMING: '🛁', WALKING: '🦮', COMPANION: '🧸' }
// 与种子数据里的接单员坐标一致：降级成手填模式时，用这个默认值仍能在大厅检索到演示订单
const DEFAULT_CENTER = [121.4737, 31.2304]

const hasAmapKey = computed(() => !!import.meta.env.VITE_AMAP_KEY)
// key 配了但无效 / 超配额 / 断网时 AmapView 会 emit error，此时也要退回手填坐标模式，
// 否则页面上只剩一个 320px 的灰色空盒子，除了控制台没有任何提示。
const mapBroken = ref(false)
const showMap = computed(() => hasAmapKey.value && !mapBroken.value)

const categories = ref([])
const pets = ref([])
const wallet = reactive({ balance: 0, frozen: 0, totalIncome: 0 })
const price = ref(null)
const loadingCategories = ref(false)
const submitting = ref(false)
const locating = ref(false)
const rechargeVisible = ref(false)
const recharging = ref(false)
const rechargeAmount = ref(1000)
const addressSearchError = ref('')

const formRef = ref(null)
const form = reactive({
  categoryId: null,
  petId: null,
  serviceStart: defaultStart(),
  serviceEnd: null,
  serviceAddress: '',
  addressLat: DEFAULT_CENTER[1],
  addressLng: DEFAULT_CENTER[0],
  remark: ''
})

const rules = {
  petId: [{ required: true, message: '请选择服务宠物', trigger: 'change' }],
  serviceStart: [{ required: true, message: '请选择上门时间', trigger: 'change' }],
  serviceAddress: [{ required: true, message: '请填写服务地址', trigger: 'blur' }],
  addressLng: [{ required: true, message: '请设置服务地址坐标', trigger: 'change' }]
}

const mapCenter = computed(() => [form.addressLng || DEFAULT_CENTER[0], form.addressLat || DEFAULT_CENTER[1]])
const selectedPet = computed(() => pets.value.find((p) => p.id === form.petId))
const shortOfBalance = computed(
  () => !!price.value && Number(wallet.balance ?? 0) < Number(price.value.amount ?? 0)
)

// 地图实例与标记一律用普通变量而不是 ref：AMap 对象内部有大量循环引用与私有状态，
// 被 Vue 的 reactive 代理包过之后会直接崩在 setter 上。
let mapInstance = null
let markerInstance = null
let addressSearchSequence = 0

function defaultStart() {
  const d = new Date()
  d.setDate(d.getDate() + 1)
  d.setHours(10, 0, 0, 0)
  return formatDateTime(d)
}

function disabledDate(date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date.getTime() < today.getTime()
}

function pickCategory(id) {
  form.categoryId = id
  refreshPrice()
}

// 周末溢价与日期直接相关，改时间必须重算，否则用户会拿着工作日的价格去确认周末的订单
watch(() => form.serviceStart, refreshPrice)

async function refreshPrice() {
  if (!form.categoryId || !form.serviceStart) {
    price.value = null
    return
  }
  try {
    price.value = await previewPrice(form.categoryId, form.serviceStart)
  } catch {
    // 失败原因拦截器已弹过；这里把旧价格清掉，避免拿一个过期的金额去下单
    price.value = null
  }
}

function onMapLoaded({ AMap, map }) {
  mapInstance = map
  markerInstance = new AMap.Marker({
    position: [form.addressLng, form.addressLat],
    draggable: true
  })
  map.add(markerInstance)
  markerInstance.on('dragend', (e) => {
    const [lng, lat] = e.target.getPosition()
    applyGeo(lng, lat)
  })
  map.on('click', (e) => {
    markerInstance.setPosition([e.lnglat.getLng(), e.lnglat.getLat()])
    applyGeo(e.lnglat.getLng(), e.lnglat.getLat())
  })
}

function onMapError() {
  // AmapView 内部已经 console.error，这里切到手填坐标模式，别留一个空白灰盒子
  mapBroken.value = true
  mapInstance = null
  markerInstance = null
}

function applyGeo(lng, lat) {
  form.addressLng = Number(lng.toFixed(7))
  form.addressLat = Number(lat.toFixed(7))
}

function poiDistanceText(distance) {
  if (!Number.isFinite(distance) || distance < 0) return ''
  return distance < 1000 ? `${Math.round(distance)} m` : `${(distance / 1000).toFixed(1)} km`
}

async function searchAddressSuggestions(keyword, callback) {
  const query = keyword.trim()
  const sequence = ++addressSearchSequence
  addressSearchError.value = ''
  if (!query) {
    callback([])
    return
  }

  try {
    const pois = await searchNearbyPois(query, [form.addressLng, form.addressLat], 1000)
    // 用户已经继续输入时，丢弃较早请求的结果，避免旧关键词覆盖新下拉栏
    if (sequence !== addressSearchSequence) return
    callback(
      pois.map((poi) => {
        const addressText = `${poi.district}${poi.address}`
        return {
          ...poi,
          addressText,
          distanceText: poiDistanceText(poi.distance),
          value: addressText ? `${poi.name} · ${addressText}` : poi.name
        }
      })
    )
  } catch {
    if (sequence !== addressSearchSequence) return
    addressSearchError.value = '附近地址检索失败，请检查高德 Key、域名白名单或网络后重试。'
    callback([])
  }
}

function selectAddress(item) {
  form.serviceAddress = item.value
  applyGeo(item.lng, item.lat)
  markerInstance?.setPosition([item.lng, item.lat])
  mapInstance?.setZoomAndCenter(17, [item.lng, item.lat])
  formRef.value?.clearValidate('serviceAddress')
}

async function locateMe() {
  locating.value = true
  try {
    const { lng, lat } = await getCurrentPosition()
    applyGeo(lng, lat)
    markerInstance?.setPosition([lng, lat])
    mapInstance?.setCenter([lng, lat])
    ElMessage.success('已填入当前定位')
  } catch {
    // 浏览器定位只在 HTTPS 或 localhost 下可用，局域网 IP + HTTP 会被静默拒绝
    ElMessage.warning('浏览器定位不可用，请手动填写经纬度或在地图上选点')
  } finally {
    locating.value = false
  }
}

function openRecharge() {
  rechargeVisible.value = true
}

async function onRecharge() {
  recharging.value = true
  try {
    const updated = await recharge(rechargeAmount.value)
    Object.assign(wallet, updated)
    rechargeVisible.value = false
    ElMessage.success(`充值成功，当前余额 ¥${money(wallet.balance)}`)
  } catch {
    // 拦截器已提示
  } finally {
    recharging.value = false
  }
}

async function onSubmit() {
  const ok = await formRef.value.validate().catch(() => false)
  if (!ok) return
  if (!form.categoryId) {
    ElMessage.warning('请先选择一项服务')
    return
  }
  if (!price.value) {
    ElMessage.warning('价格还没算出来，请稍等或重新选择上门时间')
    return
  }

  submitting.value = true
  try {
    const order = await createOrder({
      petId: form.petId,
      categoryId: form.categoryId,
      serviceAddress: form.serviceAddress,
      addressLat: form.addressLat,
      addressLng: form.addressLng,
      serviceStart: form.serviceStart,
      serviceEnd: form.serviceEnd || null,
      remark: form.remark || null
    })
    ElMessage.success(`下单成功，订单号 ${order.orderNo}`)
    router.push('/user/orders')
  } catch {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  loadingCategories.value = true
  try {
    // 三个请求互不依赖，并发拉；钱包失败不该挡住下单页渲染，所以单独兜
    const [cats, myPets, myWallet] = await Promise.all([
      listCategories(),
      listMyPets(),
      getMyWallet().catch(() => null)
    ])
    categories.value = cats ?? []
    pets.value = myPets ?? []
    if (myWallet) Object.assign(wallet, myWallet)
    if (pets.value.length === 1) form.petId = pets.value[0].id
  } catch {
    // 拦截器已提示
  } finally {
    loadingCategories.value = false
  }
  refreshPrice()
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

.wallet-strip {
  display: flex;
  align-items: center;
  gap: 20px;
}

.wallet-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.wallet-label {
  font-size: 12px;
  color: var(--pp-muted);
}

.wallet-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--pp-primary);
}

.wallet-value.muted {
  color: var(--pp-ink);
  font-weight: 600;
}

.balance-alert {
  margin-bottom: 16px;
}

.section-card {
  margin-bottom: 16px;
}

.section-title {
  margin: 0 0 16px;
  font-size: 16px;
}

.cat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
}

.cat-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  padding: 16px;
  border: 1px solid var(--pp-tint-2);
  border-radius: var(--pp-radius);
  background: #fff;
  cursor: pointer;
  text-align: left;
  font: inherit;
  color: inherit;
  transition: box-shadow 0.2s ease, border-color 0.2s ease, transform 0.2s ease;
}

.cat-card:hover {
  box-shadow: var(--pp-shadow-hover);
  transform: translateY(-2px);
}

.cat-card.active {
  border-color: var(--pp-primary);
  background: var(--pp-tint);
}

.cat-emoji {
  font-size: 26px;
}

.cat-name {
  font-weight: 600;
}

.cat-price {
  color: var(--pp-primary);
  font-weight: 700;
}

.cat-price em {
  font-style: normal;
  font-size: 12px;
  color: var(--pp-muted);
  font-weight: 400;
}

.cat-hint {
  font-size: 12px;
  color: var(--pp-muted);
}

.full {
  width: 100%;
}

.opt-hint {
  float: right;
  color: var(--pp-muted);
  font-size: 12px;
}

.form-tip {
  font-size: 12px;
  color: var(--pp-muted);
  line-height: 1.6;
}

.form-tip.search-error {
  color: var(--el-color-danger);
}

.address-option {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 5px 0;
  line-height: 1.4;
}

.address-option-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.address-option-main strong {
  overflow: hidden;
  text-overflow: ellipsis;
}

.address-option > span {
  overflow: hidden;
  color: var(--pp-muted);
  font-size: 12px;
  text-overflow: ellipsis;
}

.geo-row {
  display: flex;
  gap: 8px;
  width: 100%;
  flex-wrap: wrap;
}

.map-wrap {
  width: 100%;
}

.map-hint {
  margin-top: 6px;
  font-size: 12px;
  color: var(--pp-muted);
}

.summary {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.sum-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: var(--pp-muted);
}

.sum-val {
  color: var(--pp-ink);
  font-weight: 600;
}

.sum-row.total {
  padding-top: 10px;
  border-top: 1px dashed var(--pp-tint-2);
  font-size: 15px;
  color: var(--pp-ink);
}

.sum-row.total .sum-val {
  color: var(--pp-primary);
  font-size: 22px;
}

.sum-note {
  margin: 0;
  font-size: 12px;
  color: var(--pp-muted);
}

.submit-btn {
  width: 100%;
  margin-top: 16px;
}

.dialog-note {
  margin: 0;
  font-size: 12px;
  color: var(--pp-muted);
}

code {
  background: var(--pp-tint);
  padding: 1px 4px;
  border-radius: 4px;
}
</style>
