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
            <span class="filter-value">{{ activeLocationText }}</span>
            <el-tag size="small" effect="plain" type="info">{{ LOCATION_SOURCE_TEXT[locationSource] }}</el-tag>
            <el-button link type="primary" :loading="locating" @click="relocate">重新定位</el-button>
            <el-button link type="primary" @click="openAddressBook">地址簿</el-button>
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

    <el-dialog v-model="addressBookVisible" title="接单搜索地址簿" width="680px" top="5vh">
      <template v-if="!addressEditorVisible">
        <div class="address-book-head">
          <p>定位不可用时，系统会自动使用默认地址搜索附近订单。</p>
          <el-button type="primary" @click="openAddressEditor()">新增地址</el-button>
        </div>
        <el-empty v-if="!loadingAddresses && addresses.length === 0" description="还没有保存地址">
          <el-button type="primary" @click="openAddressEditor()">添加第一个地址</el-button>
        </el-empty>
        <div v-else v-loading="loadingAddresses" class="address-list">
          <article v-for="address in addresses" :key="address.id" class="address-card">
            <div class="address-main">
              <div class="address-title">
                <el-tag effect="plain">{{ address.label }}</el-tag>
                <el-tag v-if="address.defaultAddress" type="success">默认地址</el-tag>
              </div>
              <p>{{ fullAddress(address) }}</p>
            </div>
            <div class="address-actions">
              <el-button type="primary" size="small" @click="useAddress(address)">使用</el-button>
              <el-button link type="primary" @click="openAddressEditor(address)">编辑</el-button>
              <el-button v-if="!address.defaultAddress" link type="success" @click="makeDefaultAddress(address)">
                设为默认
              </el-button>
              <el-button link type="danger" @click="removeAddress(address)">删除</el-button>
            </div>
          </article>
        </div>
      </template>

      <template v-else>
        <div class="editor-head">
          <el-button link type="primary" @click="closeAddressEditor">← 返回地址簿</el-button>
          <strong>{{ addressForm.id ? '编辑地址' : '新增地址' }}</strong>
        </div>
        <el-form ref="addressFormRef" :model="addressForm" :rules="addressRules" label-width="86px">
          <el-form-item label="地址标签" prop="label">
            <el-radio-group v-model="addressForm.label">
              <el-radio-button v-for="label in ADDRESS_LABELS" :key="label" :value="label">{{ label }}</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="所在地区" required>
            <div class="region-row">
              <el-select
                v-model="addressForm.provinceAdcode"
                placeholder="选择省份"
                :loading="loadingProvinces"
                @change="onProvinceChange"
              >
                <el-option v-for="item in provinces" :key="item.adcode" :label="item.name" :value="item.adcode" />
              </el-select>
              <el-select
                v-model="addressForm.cityAdcode"
                placeholder="选择城市"
                :disabled="!addressForm.provinceAdcode"
                :loading="loadingCities"
                @change="onCityChange"
              >
                <el-option v-for="item in cities" :key="item.adcode" :label="item.name" :value="item.adcode" />
              </el-select>
              <el-select
                v-model="addressForm.districtAdcode"
                placeholder="选择地区"
                :disabled="!addressForm.cityAdcode"
                :loading="loadingDistricts"
                @change="onDistrictChange"
              >
                <el-option v-for="item in districts" :key="item.adcode" :label="item.name" :value="item.adcode" />
              </el-select>
            </div>
          </el-form-item>
          <el-form-item label="详细位置" prop="detailAddress">
            <el-autocomplete
              v-model="addressForm.detailAddress"
              class="location-search"
              placeholder="输入小区、学校、商场或门牌地址"
              :disabled="!addressForm.districtAdcode"
              :fetch-suggestions="searchAddressSuggestions"
              :trigger-on-focus="false"
              :debounce="300"
              value-key="value"
              clearable
              @input="onDetailAddressInput"
              @select="selectAddressSuggestion"
            >
              <template #default="{ item }">
                <div class="location-option">
                  <strong>{{ item.name }}</strong>
                  <span>{{ item.detail || '暂无详细地址' }}</span>
                </div>
              </template>
            </el-autocomplete>
            <div class="form-tip">选择候选地点后，系统会在后台记录准确位置，页面不会显示坐标。</div>
            <div v-if="addressSearchError" class="form-tip search-error">{{ addressSearchError }}</div>
          </el-form-item>
          <el-form-item>
            <el-checkbox v-model="addressForm.defaultAddress" :disabled="Boolean(addressForm.id) && addressForm.defaultAddress">
              设为默认搜索地址
            </el-checkbox>
          </el-form-item>
        </el-form>
      </template>

      <template #footer>
        <template v-if="addressEditorVisible">
          <el-button @click="closeAddressEditor">取消</el-button>
          <el-button type="primary" :loading="savingAddress" @click="saveAddress">保存地址</el-button>
        </template>
        <el-button v-else @click="addressBookVisible = false">关闭</el-button>
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
import {
  createAddress as createSitterAddress,
  deleteAddress as deleteSitterAddress,
  getMySitterProfile,
  grabOrder,
  listMyAddresses,
  pageHallOrders,
  setDefaultAddress,
  submitSitterProfile,
  updateAddress as updateSitterAddress
} from '@/api/sitter'
import { distanceText, money } from '@/utils/format'
import { getCurrentPosition, searchAdministrativeChildren, searchPois } from '@/utils/amap'

const router = useRouter()

const AUDIT_TAG = { 0: 'warning', 1: 'success', 2: 'danger' }
const RADIUS_OPTIONS = [1, 3, 5, 10, 20]
const ADDRESS_LABELS = ['家', '学校', '公司', '其他']
const LOCATION_SOURCE_TEXT = { geo: '浏览器定位', address: '地址簿', profile: '备用位置', default: '演示默认位置' }
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

const addresses = ref([])
const loadingAddresses = ref(false)
const addressBookVisible = ref(false)
const addressEditorVisible = ref(false)
const savingAddress = ref(false)
const activeAddressId = ref(null)
const addressFormRef = ref(null)
const addressSearchError = ref('')
const provinces = ref([])
const cities = ref([])
const districts = ref([])
const loadingProvinces = ref(false)
const loadingCities = ref(false)
const loadingDistricts = ref(false)
const addressForm = reactive(emptyAddressForm())
const addressRules = {
  label: [{ required: true, message: '请选择地址标签', trigger: 'change' }],
  detailAddress: [
    { required: true, message: '请填写详细位置', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== selectedDetailValue || !Number.isFinite(Number(addressForm.lng)) || !Number.isFinite(Number(addressForm.lat))) {
          callback(new Error('请从地点候选中选择准确位置'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}
let selectedDetailValue = ''
let addressSearchSequence = 0

const activeLocationText = computed(() => {
  if (locationSource.value === 'geo') return '浏览器当前位置'
  const address = addresses.value.find((item) => item.id === activeAddressId.value)
  if (address) return `${address.label} · ${fullAddress(address)}`
  if (locationSource.value === 'profile') return '已保存的备用位置'
  return '平台演示位置'
})

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
    activeAddressId.value = null
    applyCenter(pos.lng, pos.lat, 'geo')
  } catch {
    const defaultAddress = addresses.value.find((item) => item.defaultAddress)
    if (defaultAddress) {
      activeAddressId.value = defaultAddress.id
      applyCenter(defaultAddress.lng, defaultAddress.lat, 'address')
    } else if (profile.value?.currentLng && profile.value?.currentLat) {
      activeAddressId.value = null
      applyCenter(profile.value.currentLng, profile.value.currentLat, 'profile')
    } else {
      activeAddressId.value = null
      applyCenter(DEFAULT_CENTER.lng, DEFAULT_CENTER.lat, 'default')
    }
    ElMessage.warning(defaultAddress ? `浏览器定位不可用，已使用默认地址「${defaultAddress.label}」` : '浏览器定位不可用，已改用备用位置检索')
  } finally {
    locating.value = false
  }
  await load()
}

function emptyAddressForm() {
  return {
    id: null,
    label: '家',
    provinceAdcode: '',
    province: '',
    cityAdcode: '',
    city: '',
    districtAdcode: '',
    district: '',
    detailAddress: '',
    lng: null,
    lat: null,
    defaultAddress: false
  }
}

function fullAddress(address) {
  return `${address.province || ''}${address.city || ''}${address.district || ''}${address.detailAddress || ''}`
}

async function loadAddresses() {
  loadingAddresses.value = true
  try {
    addresses.value = await listMyAddresses()
  } catch {
    // 拦截器已提示
  } finally {
    loadingAddresses.value = false
  }
}

async function openAddressBook() {
  addressBookVisible.value = true
  addressEditorVisible.value = false
  await Promise.all([loadAddresses(), loadProvinces()])
}

async function openAddressEditor(address = null) {
  Object.assign(addressForm, emptyAddressForm())
  cities.value = []
  districts.value = []
  selectedDetailValue = ''
  addressSearchError.value = ''
  addressFormRef.value?.clearValidate()
  addressEditorVisible.value = true
  await loadProvinces()
  if (!address) {
    addressForm.defaultAddress = addresses.value.length === 0
    return
  }

  Object.assign(addressForm, {
    id: address.id,
    label: address.label,
    province: address.province,
    city: address.city,
    district: address.district,
    detailAddress: address.detailAddress,
    lng: Number(address.lng),
    lat: Number(address.lat),
    defaultAddress: address.defaultAddress
  })
  selectedDetailValue = address.detailAddress

  const province = findRegion(provinces.value, address.province)
  if (!province) return
  addressForm.provinceAdcode = province.adcode
  await loadCities(province.adcode)
  const city = findRegion(cities.value, address.city)
  if (!city) return
  addressForm.cityAdcode = city.adcode
  await loadDistricts(city.adcode)
  const district = findRegion(districts.value, address.district)
  if (district) addressForm.districtAdcode = district.adcode
}

function closeAddressEditor() {
  addressEditorVisible.value = false
  addressFormRef.value?.clearValidate()
}

function findRegion(items, name) {
  return items.find((item) => item.name === name)
    || items.find((item) => item.name.includes(name) || name.includes(item.name))
}

async function loadProvinces() {
  if (provinces.value.length || loadingProvinces.value) return
  loadingProvinces.value = true
  try {
    provinces.value = await searchAdministrativeChildren('中国', 'country')
  } catch {
    ElMessage.error('省份列表加载失败，请检查高德地图配置')
  } finally {
    loadingProvinces.value = false
  }
}

async function loadCities(provinceAdcode) {
  loadingCities.value = true
  try {
    cities.value = await searchAdministrativeChildren(provinceAdcode, 'province')
  } catch {
    cities.value = []
    ElMessage.error('城市列表加载失败')
  } finally {
    loadingCities.value = false
  }
}

async function loadDistricts(cityAdcode) {
  loadingDistricts.value = true
  try {
    districts.value = await searchAdministrativeChildren(cityAdcode, 'city')
  } catch {
    districts.value = []
    ElMessage.error('地区列表加载失败')
  } finally {
    loadingDistricts.value = false
  }
}

async function onProvinceChange(adcode) {
  const province = provinces.value.find((item) => item.adcode === adcode)
  addressForm.province = province?.name || ''
  addressForm.cityAdcode = ''
  addressForm.city = ''
  addressForm.districtAdcode = ''
  addressForm.district = ''
  resetDetailAddress()
  cities.value = []
  districts.value = []
  if (adcode) await loadCities(adcode)
}

async function onCityChange(adcode) {
  const city = cities.value.find((item) => item.adcode === adcode)
  addressForm.city = city?.name || ''
  addressForm.districtAdcode = ''
  addressForm.district = ''
  resetDetailAddress()
  districts.value = []
  if (adcode) await loadDistricts(adcode)
}

function onDistrictChange(adcode) {
  const district = districts.value.find((item) => item.adcode === adcode)
  addressForm.district = district?.name || ''
  resetDetailAddress()
  addressFormRef.value?.clearValidate('provinceAdcode')
}

function resetDetailAddress() {
  addressForm.detailAddress = ''
  addressForm.lng = null
  addressForm.lat = null
  selectedDetailValue = ''
}

async function searchAddressSuggestions(keyword, callback) {
  const queryText = keyword?.trim()
  const sequence = ++addressSearchSequence
  addressSearchError.value = ''
  if (!queryText || !addressForm.cityAdcode || !addressForm.district) {
    callback([])
    return
  }
  try {
    const pois = await searchPois(`${addressForm.district}${queryText}`, addressForm.cityAdcode)
    if (sequence !== addressSearchSequence) return
    callback(pois.map((poi) => ({
      ...poi,
      detail: `${poi.district}${poi.address}`,
      value: `${poi.name}${poi.address ? ` · ${poi.address}` : ''}`
    })))
  } catch {
    if (sequence !== addressSearchSequence) return
    addressSearchError.value = '详细位置搜索失败，请检查高德地图配置后重试。'
    callback([])
  }
}

function onDetailAddressInput(value) {
  if (value === selectedDetailValue) return
  addressForm.lng = null
  addressForm.lat = null
}

function selectAddressSuggestion(item) {
  selectedDetailValue = item.value
  addressForm.detailAddress = item.value
  addressForm.lng = item.lng
  addressForm.lat = item.lat
  addressFormRef.value?.clearValidate('detailAddress')
}

async function saveAddress() {
  if (!addressForm.provinceAdcode || !addressForm.cityAdcode || !addressForm.districtAdcode) {
    ElMessage.warning('请依次选择省份、城市和地区')
    return
  }
  const ok = await addressFormRef.value.validate().catch(() => false)
  if (!ok) return
  savingAddress.value = true
  try {
    const payload = {
      label: addressForm.label,
      province: addressForm.province,
      city: addressForm.city,
      district: addressForm.district,
      detailAddress: addressForm.detailAddress,
      lng: addressForm.lng,
      lat: addressForm.lat,
      defaultAddress: addressForm.defaultAddress
    }
    const saved = addressForm.id
      ? await updateSitterAddress(addressForm.id, payload)
      : await createSitterAddress(payload)
    await loadAddresses()
    const current = addresses.value.find((item) => item.id === saved.id) || saved
    ElMessage.success(addressForm.id ? '地址已更新' : '地址已保存')
    await useAddress(current)
  } catch {
    // 拦截器已提示
  } finally {
    savingAddress.value = false
  }
}

async function useAddress(address) {
  activeAddressId.value = address.id
  applyCenter(address.lng, address.lat, 'address')
  query.page = 1
  addressBookVisible.value = false
  addressEditorVisible.value = false
  await load()
}

async function makeDefaultAddress(address) {
  try {
    await setDefaultAddress(address.id)
    await loadAddresses()
    const current = addresses.value.find((item) => item.id === address.id) || address
    activeAddressId.value = current.id
    applyCenter(current.lng, current.lat, 'address')
    query.page = 1
    await load()
    ElMessage.success(`已将「${address.label}」设为默认地址`)
  } catch {
    // 拦截器已提示
  }
}

async function removeAddress(address) {
  const confirmed = await ElMessageBox.confirm(`确定删除「${address.label}」地址吗？`, '删除地址', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  }).catch(() => false)
  if (!confirmed) return
  try {
    await deleteSitterAddress(address.id)
    if (activeAddressId.value === address.id) activeAddressId.value = null
    await loadAddresses()
    const fallback = addresses.value.find((item) => item.defaultAddress)
    if (fallback) {
      activeAddressId.value = fallback.id
      applyCenter(fallback.lng, fallback.lat, 'address')
      await load()
    }
    ElMessage.success('地址已删除')
  } catch {
    // 拦截器已提示
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

  // 先用默认地址簿或旧版备用坐标出一屏，再异步去要浏览器定位：
  // 定位可能弹权限框卡住、也可能直接被拒，不该让整个大厅干等着。
  await loadAddresses()
  const defaultAddress = addresses.value.find((item) => item.defaultAddress)
  if (defaultAddress) {
    activeAddressId.value = defaultAddress.id
    applyCenter(defaultAddress.lng, defaultAddress.lat, 'address')
  } else if (profile.value?.currentLng && profile.value?.currentLat) {
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
  font-size: 13px;
  font-weight: 600;
  color: var(--pp-ink);
}

.address-book-head,
.editor-head,
.address-card,
.address-title,
.address-actions {
  display: flex;
  align-items: center;
}

.address-book-head {
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.address-book-head p {
  margin: 0;
  color: var(--pp-muted);
  font-size: 13px;
}

.address-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 80px;
}

.address-card {
  justify-content: space-between;
  gap: 16px;
  padding: 14px;
  border: 1px solid var(--pp-tint-2);
  border-radius: 10px;
}

.address-main {
  min-width: 0;
}

.address-title,
.address-actions {
  gap: 8px;
}

.address-main p {
  margin: 8px 0 0;
  color: var(--pp-ink);
  font-size: 13px;
  line-height: 1.6;
}

.address-actions {
  flex: 0 0 auto;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.editor-head {
  gap: 16px;
  margin-bottom: 18px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--pp-tint-2);
}

.region-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  width: 100%;
}

.region-row .el-select {
  width: 100%;
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

.search-error {
  color: var(--el-color-danger);
}

@media (max-width: 640px) {
  .address-card {
    align-items: flex-start;
    flex-direction: column;
  }

  .region-row {
    grid-template-columns: 1fr;
  }
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
