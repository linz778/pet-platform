<template>
  <div class="page-container">
    <el-card class="head-card">
      <div class="head">
        <div class="head-text">
          <h2 class="title">🎒 我的接单</h2>
          <p class="subtitle">抢单成功后的订单都在这里。上门完成服务并由用户验收后，到手金额才会结算进收益钱包。</p>
        </div>
        <el-button @click="load">刷新</el-button>
      </div>
    </el-card>

    <el-card>
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane v-for="(text, code) in TABS" :key="code" :label="text" :name="String(code)" />
      </el-tabs>

      <div v-loading="loading" class="list-wrap">
        <el-empty v-if="!loading && orders.length === 0" description="这个状态下还没有订单">
          <el-button type="primary" @click="router.push('/sitter/hall')">去接单大厅看看</el-button>
        </el-empty>

        <article v-for="o in orders" :key="o.id" class="order-card">
          <header class="order-top">
            <span class="order-no">{{ o.orderNo }}</span>
            <el-tag :type="STATUS_TAG[o.status] ?? 'info'" effect="light" size="small">
              {{ o.statusText || '未知状态' }}
            </el-tag>
          </header>

          <div class="order-body">
            <div class="order-line">
              <span class="line-label">服务</span>
              <span>{{ o.categoryName || '未知服务' }}<em v-if="o.unit" class="unit">/ {{ o.unit }}</em></span>
            </div>
            <div class="order-line">
              <span class="line-label">宠物</span>
              <span>
                {{ o.petName || '未知宠物' }}
                <el-tag v-if="o.petDeleted" size="small" type="info" effect="plain">档案已删除</el-tag>
              </span>
            </div>
            <div class="order-line">
              <span class="line-label">雇主</span>
              <span>{{ o.ownerNickname || '未知用户' }}</span>
            </div>
            <div class="order-line">
              <span class="line-label">时间</span>
              <span>{{ o.serviceStart }}<template v-if="o.serviceEnd"> ~ {{ o.serviceEnd }}</template></span>
            </div>
            <div class="order-line">
              <span class="line-label">地址</span>
              <span>{{ o.serviceAddress }}</span>
            </div>
          </div>

          <footer class="order-foot">
            <div class="income">
              <span class="income-label">到手</span>
              <span class="amount">¥{{ money(o.sitterIncome) }}</span>
            </div>
            <div class="foot-actions">
              <el-button link @click="openDetail(o.id)">详情</el-button>
              <el-button type="primary" size="small" @click="openDetail(o.id)">
                {{ ACTION_TEXT[o.status] ?? '详情' }}
              </el-button>
            </div>
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

    <el-drawer v-model="detailVisible" title="订单详情" size="520px" @close="onDrawerClose">
      <div v-loading="loadingDetail">
        <template v-if="detail">
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="STATUS_TAG[detail.status] ?? 'info'" effect="light" size="small">
                {{ detail.statusText || '未知状态' }}
              </el-tag>
              <span class="pay-state">{{ detail.payStatusText }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="服务">{{ detail.categoryName || '未知服务' }}</el-descriptions-item>
            <el-descriptions-item label="宠物">
              {{ detail.petName || '未知宠物' }}
              <span v-if="detail.petSpecies" class="pay-state">{{ detail.petSpecies }} · {{ detail.petBreed || '未填品种' }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="上门时间">
              {{ detail.serviceStart }}<template v-if="detail.serviceEnd"> ~ {{ detail.serviceEnd }}</template>
            </el-descriptions-item>
            <el-descriptions-item label="服务地址">{{ detail.serviceAddress }}</el-descriptions-item>
            <el-descriptions-item label="坐标">{{ detail.addressLng }}, {{ detail.addressLat }}</el-descriptions-item>
            <!-- 备注里通常是门禁密码、猫砂位置这类履约必需信息，抢到单之后才对接单员开放 -->
            <el-descriptions-item v-if="detail.remark" label="雇主备注">{{ detail.remark }}</el-descriptions-item>
            <el-descriptions-item label="订单金额">¥{{ money(detail.amount) }}</el-descriptions-item>
            <el-descriptions-item label="我的到手">
              <span class="amount">¥{{ money(detail.sitterIncome) }}</span>
              <span class="pay-state">平台抽成 ¥{{ money(detail.commission) }}</span>
            </el-descriptions-item>
          </el-descriptions>

          <!-- 只有「已接单 / 服务中」还有事可做，其余状态这里整块不渲染 -->
          <template v-if="detail.status === 2 || detail.status === 3">
            <h4 class="drawer-sub">履约操作</h4>

            <div class="step">
              <div class="step-head">
                <span class="step-title">① 到达定位打卡</span>
                <el-tag v-if="detail.checkinTime" type="success" size="small" effect="plain">
                  已打卡 {{ detail.checkinTime }}
                </el-tag>
                <el-button
                  v-else
                  type="primary"
                  size="small"
                  :loading="checkingIn"
                  @click="onCheckIn"
                >
                  打卡
                </el-button>
              </div>
              <p class="step-tip">
                后端按 Haversine 校验坐标与服务地址的距离，超出允许范围会返回「不在服务地址允许范围内」。
                浏览器定位不可用时退回资质档案里的坐标，确认框会告诉你用的是哪一个。
              </p>
            </div>

            <div v-if="detail.status === 3" class="step">
              <div class="step-head">
                <span class="step-title">② 按清单逐项拍照存证</span>
                <el-tag size="small" effect="plain" :type="missingItems.length === 0 ? 'success' : 'warning'">
                  {{ checklist.length - missingItems.length }} / {{ checklist.length }}
                </el-tag>
              </div>
              <el-alert
                v-if="checklist.length === 0"
                type="info"
                :closable="false"
                show-icon
                title="该服务类别没有配置作业清单"
              />
              <ul v-else class="check-list">
                <li v-for="item in checklist" :key="item" class="check-item">
                  <span class="check-name">
                    {{ item }}
                    <el-tag v-if="isDone(item)" type="success" size="small" effect="plain">已存证</el-tag>
                    <el-tag v-else-if="savingItem === item" type="info" size="small" effect="plain">保存中</el-tag>
                  </span>
                  <ImageUpload
                    :model-value="photos[item] || ''"
                    biz-type="evidence"
                    :limit="1"
                    @update:model-value="onPhoto(item, $event)"
                  />
                </li>
              </ul>
              <p class="step-tip">照片上传成功后自动存证；同一项重拍会覆盖旧照片，不会多出一条记录。</p>
            </div>

            <div v-if="detail.status === 3 && isWalking" class="step">
              <div class="step-head">
                <span class="step-title">③ 记录散步轨迹</span>
                <el-tag size="small" effect="plain" :type="tracking ? 'warning' : 'info'">
                  {{ trackPoints.length }} 个点
                </el-tag>
              </div>
              <div class="track-actions">
                <el-button v-if="!tracking" size="small" @click="startTrack">开始记录</el-button>
                <template v-else>
                  <el-button size="small" type="warning" @click="stopWatch">暂停</el-button>
                  <el-button size="small" type="primary" :loading="uploadingTrack" @click="uploadTrack">
                    结束并上传
                  </el-button>
                </template>
              </div>
              <p class="step-tip">
                轨迹靠浏览器 watchPosition 持续采样，桌面端没有 GPS 时可能一个点都采不到。
                它是加分项不是必填项——缺轨迹不影响标记服务完成。
              </p>
            </div>

            <div v-if="detail.status === 3" class="step">
              <div class="step-head">
                <span class="step-title">{{ isWalking ? '④' : '③' }} 标记服务完成</span>
                <el-button
                  type="success"
                  size="small"
                  :disabled="missingItems.length > 0"
                  :loading="finishing"
                  @click="onFinish"
                >
                  完成服务
                </el-button>
              </div>
              <p v-if="missingItems.length > 0" class="step-tip warn">
                还差 {{ missingItems.length }} 项没存证：{{ missingItems.join('、') }}。
                后端会拦（2008），补齐照片后这个按钮才可用。
              </p>
              <p v-else class="step-tip">清单已全部存证。确认后订单转「待验收」，用户验收通过才会结算到手金额。</p>
            </div>
          </template>

          <el-alert
            v-else-if="detail.status === 4"
            class="wait-alert"
            type="warning"
            :closable="false"
            show-icon
            title="服务已完成，等待用户验收"
            description="验收通过后到手金额才会进入收益钱包；用户对服务有异议时会由平台介入。"
          />

          <!-- 未发生的流程节点后端压根不返回该键（Jackson non_null），必须逐个 v-if 守卫 -->
          <h4 class="drawer-sub">履约进度</h4>
          <el-timeline>
            <el-timeline-item :timestamp="detail.createTime" type="primary">用户提交订单</el-timeline-item>
            <el-timeline-item v-if="detail.payTime" :timestamp="detail.payTime" type="primary">
              用户支付，资金进入平台担保
            </el-timeline-item>
            <el-timeline-item v-if="detail.takenTime" :timestamp="detail.takenTime" type="primary">
              我已接单
            </el-timeline-item>
            <el-timeline-item v-if="detail.checkinTime" :timestamp="detail.checkinTime" type="primary">
              到达并定位打卡
            </el-timeline-item>
            <el-timeline-item v-if="detail.finishTime" :timestamp="detail.finishTime" type="primary">
              服务完成，等待用户验收
            </el-timeline-item>
            <el-timeline-item v-if="detail.acceptTime" :timestamp="detail.acceptTime" type="success">
              验收通过，已结算到我的钱包
            </el-timeline-item>
            <el-timeline-item v-if="detail.cancelTime" :timestamp="detail.cancelTime" type="danger">
              订单已取消
            </el-timeline-item>
          </el-timeline>

          <h4 class="drawer-sub">存证记录</h4>
          <EvidenceList :evidences="evidences" />
          <OrderReviews v-if="detail.status === 5" :order-id="detail.id" target-label="雇主" />
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrder, getOrderEvidence } from '@/api/order'
import { getCategory } from '@/api/serviceCategory'
import {
  checkInOrder,
  finishOrder,
  getMySitterProfile,
  pageMyTakenOrders,
  saveOrderEvidence,
  saveOrderTrack
} from '@/api/sitter'
import EvidenceList from '@/components/EvidenceList.vue'
import ImageUpload from '@/components/ImageUpload.vue'
import OrderReviews from '@/components/OrderReviews.vue'
import { getCurrentPosition } from '@/utils/amap'
import { formatDateTime, money } from '@/utils/format'

const router = useRouter()

// 抢单之后的状态才可能出现在这里，所以不含「待支付 / 待接单」；已取消的单主人取消时我还没抢到
const TABS = { 2: '已接单', 3: '服务中', 4: '待验收', 5: '已完成' }
const STATUS_TAG = { 2: 'primary', 3: 'primary', 4: 'warning', 5: 'success', 6: 'info' }
// 卡片上的主按钮只是「下一步该干什么」的提示，四种状态都打开同一个抽屉
const ACTION_TEXT = { 2: '到达打卡', 3: '继续履约', 4: '查看存证', 5: '查看存证' }

const activeTab = ref('all')
const orders = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10 })

const detailVisible = ref(false)
const loadingDetail = ref(false)
const detail = ref(null)
const evidences = ref([])
const category = ref(null)
const checklist = ref([])
// 清单项 → 已存证的照片地址，直接绑到每一行的 ImageUpload 上
const photos = reactive({})

const checkingIn = ref(false)
const savingItem = ref('')
const finishing = ref(false)
const tracking = ref(false)
const uploadingTrack = ref(false)
const trackPoints = ref([])
// 定位被拒时才去拉档案坐标，别每次打开抽屉都多一个请求
const profile = ref(null)
let watchId = null

// 订单详情里没有 categoryCode，靠服务类别详情判断是不是户外散步（只有它需要轨迹）
const isWalking = computed(() => category.value?.code === 'WALKING')
const doneItems = computed(() =>
  evidences.value.filter((e) => e.type === 2 && e.checkItem).map((e) => e.checkItem)
)
const missingItems = computed(() => checklist.value.filter((item) => !doneItems.value.includes(item)))

function isDone(item) {
  return doneItems.value.includes(item)
}

async function load() {
  loading.value = true
  try {
    const res = await pageMyTakenOrders({
      page: query.page,
      size: query.size,
      status: activeTab.value === 'all' ? undefined : Number(activeTab.value)
    })
    orders.value = res.records ?? []
    total.value = res.total ?? 0
  } catch {
    // 错误提示已由 request.js 拦截器统一弹过
  } finally {
    loading.value = false
  }
}

function onTabChange() {
  query.page = 1
  load()
}

function onPageChange(page) {
  query.page = page
  load()
}

async function openDetail(id) {
  detailVisible.value = true
  loadingDetail.value = true
  resetDrawer()
  try {
    detail.value = await getOrder(id)
    await loadFulfillment(detail.value)
  } catch {
    detailVisible.value = false
  } finally {
    loadingDetail.value = false
  }
}

function resetDrawer() {
  stopWatch()
  detail.value = null
  evidences.value = []
  category.value = null
  checklist.value = []
  trackPoints.value = []
  Object.keys(photos).forEach((k) => delete photos[k])
}

/** 存证与服务类别互不依赖，并行拉；任一失败都不该让抽屉整个打不开，所以各自兜底。 */
async function loadFulfillment(order) {
  const [list, cat] = await Promise.all([
    getOrderEvidence(order.id).catch(() => []),
    getCategory(order.categoryId).catch(() => null)
  ])
  evidences.value = list ?? []
  category.value = cat
  checklist.value = cat?.checklist ?? []
  syncPhotos()
}

function syncPhotos() {
  Object.keys(photos).forEach((k) => delete photos[k])
  evidences.value
    .filter((e) => e.type === 2 && e.checkItem)
    .forEach((e) => {
      photos[e.checkItem] = e.imageUrl
    })
}

/** 动作成功后刷新抽屉与列表：状态、打卡时间、清单进度都是服务端算的，本地猜会不一致 */
async function refresh() {
  if (detail.value) {
    const id = detail.value.id
    detail.value = await getOrder(id).catch(() => detail.value)
    await loadFulfillment(detail.value)
  }
  await load()
}

async function resolveCoords() {
  try {
    const pos = await getCurrentPosition()
    return { lng: pos.lng, lat: pos.lat, source: '浏览器定位' }
  } catch {
    if (!profile.value) {
      profile.value = await getMySitterProfile().catch(() => null)
    }
    const lng = Number(profile.value?.currentLng)
    const lat = Number(profile.value?.currentLat)
    if (!Number.isFinite(lng) || !Number.isFinite(lat) || (lng === 0 && lat === 0)) {
      ElMessage.warning('浏览器定位不可用，档案里也没有备用坐标，无法打卡')
      return null
    }
    return { lng, lat, source: '档案坐标（浏览器定位不可用）' }
  }
}

async function onCheckIn() {
  checkingIn.value = true
  const coords = await resolveCoords()
  checkingIn.value = false
  if (!coords) return

  try {
    await ElMessageBox.confirm(
      `将以 ${coords.lng.toFixed(5)}, ${coords.lat.toFixed(5)} 打卡，来源：${coords.source}。`,
      '到达打卡',
      { type: 'warning', confirmButtonText: '确认打卡', cancelButtonText: '取消' }
    )
  } catch {
    return
  }

  checkingIn.value = true
  try {
    await checkInOrder(detail.value.id, { lat: coords.lat, lng: coords.lng })
    ElMessage.success('打卡成功，订单进入服务中')
    await refresh()
  } catch {
    // 距离超限（2004）的提示语里带着实际距离，已由拦截器弹出
  } finally {
    checkingIn.value = false
  }
}

async function onPhoto(item, url) {
  // 清空图片时组件也会 emit 一个空串，那不是存证动作
  if (!url || !detail.value) return
  savingItem.value = item
  try {
    await saveOrderEvidence(detail.value.id, { checkItem: item, imageUrl: url })
    ElMessage.success(`「${item}」已存证`)
    await loadFulfillment(detail.value)
  } catch {
    // 存证没落库就得把照片退回去，否则界面显示「已存证」而完成服务时被后端拦下
    photos[item] = ''
  } finally {
    savingItem.value = ''
  }
}

function startTrack() {
  if (!navigator.geolocation) {
    ElMessage.warning('当前浏览器不支持定位，无法记录轨迹')
    return
  }
  trackPoints.value = []
  tracking.value = true
  watchId = navigator.geolocation.watchPosition(
    (pos) => {
      trackPoints.value.push({
        lat: pos.coords.latitude,
        lng: pos.coords.longitude,
        time: formatDateTime(new Date(pos.timestamp || Date.now()))
      })
    },
    (err) => {
      ElMessage.warning(`定位中断：${err.message}`)
      stopWatch()
    },
    { enableHighAccuracy: true, maximumAge: 2000, timeout: 15000 }
  )
}

/** watchPosition 不清掉会一直在后台采样，关抽屉和离开页面都必须停 */
function stopWatch() {
  if (watchId !== null) {
    navigator.geolocation.clearWatch(watchId)
    watchId = null
  }
  tracking.value = false
}

async function uploadTrack() {
  stopWatch()
  if (trackPoints.value.length === 0) {
    ElMessage.warning('一个轨迹点都没采到，先确认浏览器定位权限')
    return
  }
  uploadingTrack.value = true
  try {
    await saveOrderTrack(detail.value.id, { points: trackPoints.value })
    ElMessage.success(`已上传 ${trackPoints.value.length} 个轨迹点`)
    trackPoints.value = []
    await loadFulfillment(detail.value)
  } catch {
    // 提示已由拦截器弹过；点数保留，改好网络还能再传一次
  } finally {
    uploadingTrack.value = false
  }
}

async function onFinish() {
  try {
    await ElMessageBox.confirm(
      '确认服务已完成？订单将转为「待验收」，等用户验收通过后到手金额才结算进收益钱包。',
      '完成服务',
      { type: 'warning', confirmButtonText: '确认完成', cancelButtonText: '再检查一下' }
    )
  } catch {
    return
  }
  finishing.value = true
  try {
    await finishOrder(detail.value.id)
    ElMessage.success('已提交，等待用户验收')
    await refresh()
  } catch {
    // 清单缺项（2008）的提示语里列了缺哪几项
  } finally {
    finishing.value = false
  }
}

function onDrawerClose() {
  stopWatch()
  trackPoints.value = []
}

onBeforeUnmount(stopWatch)
onMounted(load)
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

.list-wrap {
  min-height: 220px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-card {
  padding: 16px;
  border: 1px solid var(--pp-tint-2);
  border-radius: var(--pp-radius);
  background: #fff;
  transition: box-shadow 0.2s ease;
}

.order-card:hover {
  box-shadow: var(--pp-shadow);
}

.order-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 10px;
  border-bottom: 1px dashed var(--pp-tint-2);
}

.order-no {
  font-family: monospace;
  font-size: 13px;
  color: var(--pp-muted);
}

.order-body {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 8px 24px;
  padding: 12px 0;
}

.order-line {
  display: flex;
  gap: 8px;
  font-size: 13px;
  align-items: baseline;
}

.line-label {
  flex: 0 0 34px;
  color: var(--pp-muted);
}

.unit {
  font-style: normal;
  color: var(--pp-muted);
  margin-left: 6px;
  font-size: 12px;
}

.order-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 10px;
  border-top: 1px dashed var(--pp-tint-2);
}

.foot-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.income {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.income-label {
  font-size: 12px;
  color: var(--pp-muted);
}

.amount {
  font-size: 18px;
  font-weight: 700;
  color: var(--pp-primary);
}

.pager {
  margin-top: 16px;
  justify-content: center;
}

.drawer-sub {
  margin: 20px 0 12px;
  font-size: 14px;
}

.pay-state {
  margin-left: 8px;
  font-size: 12px;
  color: var(--pp-muted);
}

.wait-alert {
  margin-top: 16px;
}

.step {
  padding: 12px;
  margin-bottom: 10px;
  border: 1px solid var(--pp-tint-2);
  border-radius: var(--pp-radius);
}

.step-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.step-title {
  font-size: 13px;
  font-weight: 600;
}

.step-tip {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--pp-muted);
}

.step-tip.warn {
  color: var(--el-color-warning);
}

.check-list {
  margin: 10px 0 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.check-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.check-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.track-actions {
  margin-top: 10px;
  display: flex;
  gap: 8px;
}
</style>
