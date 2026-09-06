<template>
  <div class="page-container">
    <el-card class="head-card">
      <div class="head">
        <div class="head-text">
          <h2 class="title">🏠 预约上门服务</h2>
          <p class="subtitle">先挑选需要的服务，再填写本次预约。常用地址保存一次，以后下单直接使用。</p>
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
      v-if="form.categoryId && shortOfBalance"
      class="balance-alert"
      type="warning"
      show-icon
      :closable="false"
      title="余额不足以支付这一单"
    >
      <template #default>
        当前可用 ¥{{ money(wallet.balance) }}，本单需 ¥{{ money(price.amount) }}。可以先下单，再去“我的订单”充值支付。
        <el-button link type="primary" @click="openRecharge">立即充值</el-button>
      </template>
    </el-alert>

    <el-card v-loading="loadingCategories" class="section-card service-card">
      <div class="section-head">
        <div>
          <h3 class="section-title">选择你需要的服务</h3>
          <p>选择后才会展开预约信息，不让页面一进来就堆满表单。</p>
        </div>
        <el-tag v-if="selectedCategory" type="success" effect="plain">已选：{{ selectedCategory.name }}</el-tag>
      </div>
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
          <span class="cat-check">{{ form.categoryId === c.id ? '✓' : '' }}</span>
          <span class="cat-emoji">{{ CATEGORY_EMOJI[c.code] ?? '🐾' }}</span>
          <span class="cat-name">{{ c.name }}</span>
          <span class="cat-price">¥{{ money(c.basePrice) }}<em>/{{ c.unit || '次' }}</em></span>
          <span class="cat-hint">周末 ×{{ c.holidayRate }}</span>
        </button>
      </div>
    </el-card>

    <template v-if="form.categoryId">
      <el-card class="section-card booking-card">
        <div class="section-head">
          <div>
            <h3 class="section-title">填写本次预约</h3>
            <p>{{ selectedCategory?.name }} · 只需确认宠物、时间和服务地址</p>
          </div>
          <span class="step-badge">第 2 步</span>
        </div>

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
            <div v-else-if="selectedPet?.feedingTaboo" class="form-tip">喂养禁忌：{{ selectedPet.feedingTaboo }}</div>
          </el-form-item>

          <div class="time-grid">
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
          </div>

          <el-form-item label="服务地址" prop="serviceAddress">
            <div v-loading="loadingAddresses" class="address-picker">
              <div v-if="selectedAddress" class="selected-address">
                <div class="address-icon">📍</div>
                <div class="selected-address-main">
                  <div class="address-title">
                    <el-tag effect="plain">{{ selectedAddress.label }}</el-tag>
                    <el-tag v-if="selectedAddress.defaultAddress" type="success" effect="plain">默认地址</el-tag>
                  </div>
                  <strong>{{ fullAddress(selectedAddress) }}</strong>
                  <span>本次预约将直接使用此地址，无需重复填写。</span>
                </div>
                <div class="selected-address-actions">
                  <el-button type="primary" plain @click="openAddressBook">更换地址</el-button>
                  <el-button @click="openAddressEditor(selectedAddress, true)">修改</el-button>
                </div>
              </div>
              <div v-else class="first-address">
                <div>
                  <strong>第一次预约，请先保存一个常用地址</strong>
                  <p>首次保存后自动设为默认地址，下次购买服务会直接使用。</p>
                </div>
                <el-button type="primary" @click="openAddressEditor(null, true)">填写第一个地址</el-button>
              </div>
            </div>
          </el-form-item>

          <el-form-item label="备注" prop="remark">
            <el-input
              v-model="form.remark"
              type="textarea"
              :rows="3"
              maxlength="500"
              show-word-limit
              placeholder="门禁方式、宠物用品位置、特殊注意事项等"
            />
          </el-form-item>
        </el-form>
      </el-card>

      <el-card class="section-card summary-card">
        <div class="section-head">
          <div>
            <h3 class="section-title">确认费用</h3>
            <p>提交后订单进入待支付状态，支付成功才会进入接单大厅。</p>
          </div>
          <span class="step-badge">第 3 步</span>
        </div>
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
          <div class="sum-row">
            <span>服务地址</span>
            <span class="sum-val address-value">{{ selectedAddress ? fullAddress(selectedAddress) : '未选择' }}</span>
          </div>
          <div class="sum-row total">
            <span>应付总额</span>
            <span class="sum-val">¥{{ money(price.amount) }}</span>
          </div>
          <p class="sum-note">支付后资金进入平台担保，验收通过才结算给接单员；待支付或待接单阶段取消会原路退款。</p>
        </div>
        <el-empty v-else :image-size="60" description="选择上门时间后显示价格" />

        <el-button type="primary" size="large" class="submit-btn" :loading="submitting" @click="onSubmit">
          立即下单
        </el-button>
      </el-card>
    </template>

    <el-dialog v-model="addressBookVisible" title="服务地址簿" width="720px" top="5vh">
      <template v-if="!addressEditorVisible">
        <div class="address-book-head">
          <p>选择本次服务地址，也可以设置以后自动使用的默认地址。</p>
          <el-button type="primary" @click="openAddressEditor()">新增地址</el-button>
        </div>
        <el-empty v-if="!loadingAddresses && addresses.length === 0" description="还没有保存地址">
          <el-button type="primary" @click="openAddressEditor()">添加第一个地址</el-button>
        </el-empty>
        <div v-else v-loading="loadingAddresses" class="address-list">
          <article
            v-for="address in addresses"
            :key="address.id"
            class="address-card"
            :class="{ active: address.id === selectedAddressId }"
          >
            <div class="address-main">
              <div class="address-title">
                <el-tag effect="plain">{{ address.label }}</el-tag>
                <el-tag v-if="address.defaultAddress" type="success">默认地址</el-tag>
                <el-tag v-if="address.id === selectedAddressId" type="primary" effect="plain">本次使用</el-tag>
              </div>
              <p>{{ fullAddress(address) }}</p>
            </div>
            <div class="address-actions">
              <el-button type="primary" size="small" @click="useAddress(address)">使用此地址</el-button>
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
          <el-button v-if="addresses.length" link type="primary" @click="closeAddressEditor">← 返回地址簿</el-button>
          <strong>{{ addressForm.id ? '修改地址' : '新增地址' }}</strong>
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
              class="full"
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
            <div class="form-tip">请从候选地点中选择，系统会保存准确位置，但不会向用户显示坐标。</div>
            <div v-if="addressSearchError" class="form-tip search-error">{{ addressSearchError }}</div>
          </el-form-item>
          <el-form-item>
            <el-checkbox v-model="addressForm.defaultAddress" :disabled="Boolean(addressForm.id) && addressForm.defaultAddress">
              设为默认服务地址
            </el-checkbox>
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <template v-if="addressEditorVisible">
          <el-button @click="closeAddressEditor">取消</el-button>
          <el-button type="primary" :loading="savingAddress" @click="saveAddress">保存并使用</el-button>
        </template>
        <el-button v-else @click="addressBookVisible = false">关闭</el-button>
      </template>
    </el-dialog>

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
import { ElMessage, ElMessageBox } from 'element-plus'
import { listCategories, previewPrice } from '@/api/serviceCategory'
import { listMyPets } from '@/api/pet'
import { getMyWallet, recharge } from '@/api/wallet'
import { createOrder } from '@/api/order'
import {
  createUserAddress,
  deleteUserAddress,
  listUserAddresses,
  setDefaultUserAddress,
  updateUserAddress
} from '@/api/userAddress'
import { money, petAgeText, formatDateTime } from '@/utils/format'
import { searchAdministrativeChildren, searchPois } from '@/utils/amap'

const router = useRouter()

const CATEGORY_EMOJI = { FEEDING: '🍚', GROOMING: '🛁', WALKING: '🦮', COMPANION: '🧸' }
const ADDRESS_LABELS = ['家', '学校', '公司', '其他']

const categories = ref([])
const pets = ref([])
const wallet = reactive({ balance: 0, frozen: 0, totalIncome: 0 })
const price = ref(null)
const loadingCategories = ref(false)
const submitting = ref(false)
const rechargeVisible = ref(false)
const recharging = ref(false)
const rechargeAmount = ref(1000)

const addresses = ref([])
const loadingAddresses = ref(false)
const selectedAddressId = ref(null)
const addressBookVisible = ref(false)
const addressEditorVisible = ref(false)
const savingAddress = ref(false)
const addressFormRef = ref(null)
const addressSearchError = ref('')
const provinces = ref([])
const cities = ref([])
const districts = ref([])
const loadingProvinces = ref(false)
const loadingCities = ref(false)
const loadingDistricts = ref(false)
const addressForm = reactive(emptyAddressForm())
let selectedDetailValue = ''
let addressSearchSequence = 0

const formRef = ref(null)
const form = reactive({
  categoryId: null,
  petId: null,
  serviceStart: defaultStart(),
  serviceEnd: null,
  serviceAddress: '',
  addressLat: null,
  addressLng: null,
  remark: ''
})

const rules = {
  petId: [{ required: true, message: '请选择服务宠物', trigger: 'change' }],
  serviceStart: [{ required: true, message: '请选择上门时间', trigger: 'change' }],
  serviceAddress: [{ required: true, message: '请选择或新增一个服务地址', trigger: 'change' }]
}

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

const selectedCategory = computed(() => categories.value.find((item) => item.id === form.categoryId))
const selectedPet = computed(() => pets.value.find((item) => item.id === form.petId))
const selectedAddress = computed(() => addresses.value.find((item) => item.id === selectedAddressId.value) || null)
const shortOfBalance = computed(
  () => !!price.value && Number(wallet.balance ?? 0) < Number(price.value.amount ?? 0)
)

function defaultStart() {
  const date = new Date()
  date.setDate(date.getDate() + 1)
  date.setHours(10, 0, 0, 0)
  return formatDateTime(date)
}

function disabledDate(date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date.getTime() < today.getTime()
}

function pickCategory(id) {
  form.categoryId = id
  refreshPrice()
  requestAnimationFrame(() => {
    document.querySelector('.booking-card')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  })
}

watch(() => form.serviceStart, refreshPrice)

async function refreshPrice() {
  if (!form.categoryId || !form.serviceStart) {
    price.value = null
    return
  }
  try {
    price.value = await previewPrice(form.categoryId, form.serviceStart)
  } catch {
    price.value = null
  }
}

function fullAddress(address) {
  return `${address.province || ''}${address.city || ''}${address.district || ''}${address.detailAddress || ''}`
}

function applyAddress(address) {
  selectedAddressId.value = address?.id ?? null
  form.serviceAddress = address ? fullAddress(address) : ''
  form.addressLng = address ? Number(address.lng) : null
  form.addressLat = address ? Number(address.lat) : null
  formRef.value?.clearValidate('serviceAddress')
}

async function loadAddresses(preserveSelection = true) {
  loadingAddresses.value = true
  const previousId = preserveSelection ? selectedAddressId.value : null
  try {
    addresses.value = await listUserAddresses()
    const selected = addresses.value.find((item) => item.id === previousId)
      || addresses.value.find((item) => item.defaultAddress)
      || addresses.value[0]
      || null
    applyAddress(selected)
  } catch {
    addresses.value = []
    applyAddress(null)
  } finally {
    loadingAddresses.value = false
  }
}

async function openAddressBook() {
  addressBookVisible.value = true
  addressEditorVisible.value = false
  await loadAddresses()
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

async function openAddressEditor(address = null, direct = false) {
  if (direct) addressBookVisible.value = true
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
  if (addresses.value.length) {
    addressEditorVisible.value = false
  } else {
    addressBookVisible.value = false
  }
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
  addressForm.province = provinces.value.find((item) => item.adcode === adcode)?.name || ''
  addressForm.cityAdcode = ''
  addressForm.city = ''
  addressForm.districtAdcode = ''
  addressForm.district = ''
  cities.value = []
  districts.value = []
  resetDetailAddress()
  if (adcode) await loadCities(adcode)
}

async function onCityChange(adcode) {
  addressForm.city = cities.value.find((item) => item.adcode === adcode)?.name || ''
  addressForm.districtAdcode = ''
  addressForm.district = ''
  districts.value = []
  resetDetailAddress()
  if (adcode) await loadDistricts(adcode)
}

function onDistrictChange(adcode) {
  addressForm.district = districts.value.find((item) => item.adcode === adcode)?.name || ''
  resetDetailAddress()
}

function resetDetailAddress() {
  addressForm.detailAddress = ''
  addressForm.lng = null
  addressForm.lat = null
  selectedDetailValue = ''
}

async function searchAddressSuggestions(keyword, callback) {
  const query = keyword?.trim()
  const sequence = ++addressSearchSequence
  addressSearchError.value = ''
  if (!query || !addressForm.cityAdcode || !addressForm.district) {
    callback([])
    return
  }
  try {
    const pois = await searchPois(`${addressForm.district}${query}`, addressForm.cityAdcode)
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
      ? await updateUserAddress(addressForm.id, payload)
      : await createUserAddress(payload)
    await loadAddresses(false)
    const current = addresses.value.find((item) => item.id === saved.id) || saved
    applyAddress(current)
    addressEditorVisible.value = false
    addressBookVisible.value = false
    ElMessage.success(addressForm.id ? '地址已修改并用于本次预约' : '地址已保存，下次会自动使用默认地址')
  } catch {
    // 请求拦截器已提示
  } finally {
    savingAddress.value = false
  }
}

function useAddress(address) {
  applyAddress(address)
  addressBookVisible.value = false
}

async function makeDefaultAddress(address) {
  try {
    await setDefaultUserAddress(address.id)
    await loadAddresses()
    ElMessage.success(`已将“${address.label}”设为默认地址`)
  } catch {
    // 请求拦截器已提示
  }
}

async function removeAddress(address) {
  const confirmed = await ElMessageBox.confirm(`确定删除“${address.label}”地址吗？`, '删除地址', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  }).catch(() => false)
  if (!confirmed) return
  try {
    await deleteUserAddress(address.id)
    if (selectedAddressId.value === address.id) selectedAddressId.value = null
    await loadAddresses()
    ElMessage.success('地址已删除')
  } catch {
    // 请求拦截器已提示
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
    // 请求拦截器已提示
  } finally {
    recharging.value = false
  }
}

async function onSubmit() {
  if (!form.categoryId) {
    ElMessage.warning('请先选择一项服务')
    return
  }
  const ok = await formRef.value?.validate().catch(() => false)
  if (!ok) return
  if (!selectedAddress.value || !Number.isFinite(form.addressLng) || !Number.isFinite(form.addressLat)) {
    ElMessage.warning('请选择一个有效的服务地址')
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
    // 请求拦截器已提示
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  loadingCategories.value = true
  try {
    const [cats, myPets, myWallet] = await Promise.all([
      listCategories(),
      listMyPets(),
      getMyWallet().catch(() => null),
      loadAddresses()
    ])
    categories.value = cats ?? []
    pets.value = myPets ?? []
    if (myWallet) Object.assign(wallet, myWallet)
    if (pets.value.length === 1) form.petId = pets.value[0].id
  } catch {
    // 请求拦截器已提示
  } finally {
    loadingCategories.value = false
  }
})
</script>

<style scoped>
.head-card,
.balance-alert,
.section-card {
  margin-bottom: 16px;
}

.head,
.section-head,
.address-book-head,
.editor-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.head {
  flex-wrap: wrap;
}

.title,
.section-title {
  margin: 0;
}

.title {
  margin-bottom: 6px;
  font-size: 20px;
}

.subtitle,
.section-head p,
.address-book-head p,
.first-address p,
.selected-address span,
.dialog-note {
  margin: 4px 0 0;
  font-size: 12px;
  line-height: 1.6;
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

.section-title {
  font-size: 17px;
}

.section-head {
  margin-bottom: 18px;
}

.step-badge {
  padding: 5px 10px;
  border-radius: 999px;
  background: var(--pp-tint);
  color: var(--pp-primary);
  font-size: 12px;
}

.cat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
}

.cat-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  padding: 18px;
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
  box-shadow: 0 0 0 2px rgb(79 130 95 / 10%);
}

.cat-check {
  position: absolute;
  top: 12px;
  right: 12px;
  color: var(--pp-primary);
  font-weight: 700;
}

.cat-emoji {
  font-size: 28px;
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

.cat-hint,
.form-tip {
  font-size: 12px;
  line-height: 1.6;
  color: var(--pp-muted);
}

.form-tip.search-error {
  color: var(--el-color-danger);
}

.full {
  width: 100%;
}

.opt-hint {
  float: right;
  color: var(--pp-muted);
  font-size: 12px;
}

.time-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.address-picker {
  width: 100%;
  min-height: 84px;
}

.selected-address,
.first-address {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  padding: 14px 16px;
  border: 1px solid var(--pp-tint-2);
  border-radius: var(--pp-radius);
  background: var(--pp-tint);
}

.address-icon {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: #fff;
  font-size: 20px;
}

.selected-address-main {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  gap: 5px;
}

.selected-address-main strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.selected-address-actions,
.address-actions,
.address-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.first-address {
  justify-content: space-between;
  border-style: dashed;
  background: #fff;
}

.summary {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.sum-row {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: center;
  font-size: 14px;
  color: var(--pp-muted);
}

.sum-val {
  color: var(--pp-ink);
  font-weight: 600;
}

.address-value {
  max-width: 70%;
  text-align: right;
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

.address-book-head {
  margin-bottom: 14px;
}

.address-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.address-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px;
  border: 1px solid var(--pp-tint-2);
  border-radius: var(--pp-radius);
}

.address-card.active {
  border-color: var(--pp-primary);
  background: var(--pp-tint);
}

.address-main {
  min-width: 0;
}

.address-main p {
  margin: 8px 0 0;
  line-height: 1.6;
}

.editor-head {
  justify-content: flex-start;
  margin-bottom: 18px;
}

.region-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  width: 100%;
}

.location-option {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 4px 0;
  line-height: 1.4;
}

.location-option span {
  color: var(--pp-muted);
  font-size: 12px;
}

@media (max-width: 760px) {
  .time-grid,
  .region-row {
    grid-template-columns: 1fr;
  }

  .selected-address,
  .first-address,
  .address-card {
    align-items: flex-start;
    flex-direction: column;
  }

  .selected-address-actions,
  .address-actions {
    flex-wrap: wrap;
  }
}
</style>
