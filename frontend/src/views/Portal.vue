<template>
  <div class="portal">
    <!-- ============ 1. 顶栏导航 Navbar ============ -->
    <header class="nav" :class="{ 'nav--scrolled': scrolled }">
      <div class="nav__inner">
        <div class="brand" @click="toTop">
          <span class="brand__logo">
            <svg viewBox="0 0 64 64" fill="currentColor" aria-hidden="true">
              <ellipse cx="20" cy="24" rx="5.6" ry="7.6" />
              <ellipse cx="32" cy="18.5" rx="5.6" ry="7.6" />
              <ellipse cx="44" cy="24" rx="5.6" ry="7.6" />
              <path d="M32 32c-8.4 0-14.5 6.2-14.5 12.4 0 5.1 4.1 8.1 9.2 8.1 2.9 0 4.1-1.1 5.3-1.1s2.4 1.1 5.3 1.1c5.1 0 9.2-3 9.2-8.1C46.5 38.2 40.4 32 32 32z" />
            </svg>
          </span>
          <span class="brand__text">
            <strong>乐宠乐</strong>
            <small>上门照料服务平台</small>
          </span>
        </div>

        <nav class="nav__links">
          <a v-for="l in navLinks" :key="l.id" class="nav__link" @click="scrollTo(l.id)">{{ l.label }}</a>
        </nav>

        <div class="nav__actions">
          <template v-if="!userStore.isLogin">
            <el-button text class="nav__btn" @click="goLogin">登录</el-button>
            <el-button text class="nav__btn" @click="goRegister">注册</el-button>
          </template>
          <span v-else class="nav__hi">你好，{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
          <el-button type="primary" round class="nav__cta" @click="goWorkbench">进入工作台</el-button>
        </div>
      </div>
    </header>

    <!-- ============ 2. 主视觉区 Hero ============ -->
    <section id="home" class="hero">
      <div class="hero__blob hero__blob--a"></div>
      <div class="hero__blob hero__blob--b"></div>
      <div class="hero__inner">
        <!-- 左：文案与入口 -->
        <div class="hero__left">
          <span class="chip">🌿 专业 · 透明 · 上门无忧</span>
          <h1 class="hero__title">
            乐宠乐 · 让爱宠在熟悉家中，<br />享受专业温情陪伴
          </h1>
          <p class="hero__sub">
            无需奔波寄养，专业持证陪护员准时上门：日常喂养、定时遛狗、温和洗护，全流程图文视频留证，安全托付。
          </p>
          <div class="hero__cta">
            <button class="btn btn--primary" @click="goIdentity('USER')">
              <span class="btn__emoji">🐶</span> 我是宠物主人 · 立即预约
            </button>
            <button class="btn btn--ghost" @click="goIdentity('SITTER')">
              <span class="btn__emoji">🧑‍⚕️</span> 我是陪护员 · 抢单入驻
            </button>
            <button class="btn btn--outline" @click="goIdentity('ADMIN')">
              <span class="btn__emoji">📊</span> 平台运营调度
            </button>
          </div>
          <div class="hero__trust">
            <span class="hero__trust-item"><i class="dot"></i>持证上岗</span>
            <span class="hero__trust-item"><i class="dot"></i>定位打卡</span>
            <span class="hero__trust-item"><i class="dot"></i>平台担保</span>
            <span class="hero__trust-item"><i class="dot"></i>纠纷仲裁</span>
          </div>
        </div>

        <!-- 右：可交互悬浮卡片组 -->
        <div class="hero__right">
          <div class="float-card float-card--stat f1">
            <div class="float-card__badge">实时履约</div>
            <div class="float-card__num">156<span>单</span></div>
            <div class="float-card__label">今日已完成履约订单</div>
          </div>

          <div class="float-card float-card--sitter f2">
            <div class="avatars">
              <span>🧑‍⚕️</span><span>👩‍⚕️</span><span>🧑‍🔧</span><span class="more">+5</span>
            </div>
            <div class="float-card__text">
              附近 <b>3km</b> 内有 <b class="hl">8 位</b> 持证陪护员<br />在线接单中
            </div>
            <span class="pulse"></span>
          </div>

          <div class="float-card float-card--ticker f3">
            <div class="ticker__head">
              <span class="live"><i></i>最新实时动态</span>
            </div>
            <transition name="tick" mode="out-in">
              <div class="ticker__body" :key="actIndex">
                <b>{{ activities[actIndex].area }}</b>
                {{ activities[actIndex].pet }}
                <span class="ticker__tail">{{ activities[actIndex].text }}</span>
              </div>
            </transition>
          </div>
        </div>
      </div>
    </section>

    <!-- ============ 3. 核心服务矩阵 ============ -->
    <section id="services" class="section">
      <div class="section__head">
        <span class="section__eyebrow">核心服务矩阵</span>
        <h2>为每一种陪伴需求，提供标准化上门服务</h2>
        <p>持证陪护员按标准作业清单执行，全程图文留证，价格透明可查。</p>
      </div>
      <div class="grid grid--4">
        <article v-for="s in services" :key="s.title" class="service-card">
          <div class="service-card__icon">{{ s.icon }}</div>
          <h3 class="service-card__title">{{ s.title }}</h3>
          <span class="price-tag">￥{{ s.price }} 起 / {{ s.unit }}</span>
          <ul class="check-list">
            <li v-for="item in s.items" :key="item">
              <svg viewBox="0 0 24 24" class="check-ico" aria-hidden="true">
                <path d="M20 6 9 17l-5-5" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
              {{ item }}
            </li>
          </ul>
          <button class="btn btn--soft service-card__btn" @click="goIdentity('USER')">立即预约</button>
        </article>
      </div>
    </section>

    <!-- ============ 服务流程（对应导航锚点） ============ -->
    <section id="process" class="process">
      <div class="section__head section__head--light">
        <span class="section__eyebrow">服务流程</span>
        <h2>四步闭环，安心托付</h2>
      </div>
      <div class="process__track">
        <div v-for="(st, i) in steps" :key="st.n" class="step">
          <div class="step__no">{{ st.n }}</div>
          <div class="step__body">
            <h4>{{ st.title }}</h4>
            <p>{{ st.desc }}</p>
          </div>
          <span v-if="i < steps.length - 1" class="step__arrow">→</span>
        </div>
      </div>
    </section>

    <!-- ============ 4. 全流程安全保障 ============ -->
    <section id="trust" class="section">
      <div class="section__head">
        <span class="section__eyebrow">全流程安全保障</span>
        <h2>从匹配到验收，每一步都有据可依</h2>
        <p>数字档案 + LBS 匹配 + 图文存证 + 平台担保，构筑供需双方的信任底座。</p>
      </div>
      <div class="grid grid--4">
        <article v-for="t in trust" :key="t.title" class="trust-card">
          <div class="trust-card__icon">{{ t.icon }}</div>
          <h3>{{ t.title }}</h3>
          <p>{{ t.desc }}</p>
        </article>
      </div>
    </section>

    <!-- ============ 5. 数据背书统计条 ============ -->
    <section class="stats">
      <div class="stats__inner">
        <div v-for="st in stats" :key="st.label" class="stat">
          <strong class="stat__num">{{ displayStat(st) }}</strong>
          <span class="stat__label">{{ st.label }}</span>
        </div>
      </div>
    </section>

    <!-- ============ 6. 页面底栏 Footer ============ -->
    <footer class="footer">
      <div class="footer__inner">
        <div class="footer__brand">
          <div class="brand brand--light">
            <span class="brand__logo">
              <svg viewBox="0 0 64 64" fill="currentColor" aria-hidden="true">
                <ellipse cx="20" cy="24" rx="5.6" ry="7.6" />
                <ellipse cx="32" cy="18.5" rx="5.6" ry="7.6" />
                <ellipse cx="44" cy="24" rx="5.6" ry="7.6" />
                <path d="M32 32c-8.4 0-14.5 6.2-14.5 12.4 0 5.1 4.1 8.1 9.2 8.1 2.9 0 4.1-1.1 5.3-1.1s2.4 1.1 5.3 1.1c5.1 0 9.2-3 9.2-8.1C46.5 38.2 40.4 32 32 32z" />
              </svg>
            </span>
            <span class="brand__text"><strong>乐宠乐</strong><small>上门照料服务平台</small></span>
          </div>
          <p class="footer__idea">
            让每一只爱宠，都能在自己熟悉的家里，被温柔而专业地照顾。<br />
            乐宠乐 —— 专业、透明、有温度的宠物上门服务平台。
          </p>
        </div>

        <div class="footer__col">
          <h5>快速链接</h5>
          <a @click="scrollTo('home')">首页</a>
          <a @click="scrollTo('services')">服务项目</a>
          <a @click="scrollTo('trust')">安全保障</a>
          <a @click="scrollTo('process')">服务流程</a>
        </div>

        <div class="footer__col">
          <h5>身份入口</h5>
          <a @click="goIdentity('USER')">宠物主人预约</a>
          <a @click="goIdentity('SITTER')">陪护员入驻</a>
          <a @click="goIdentity('ADMIN')">运营调度后台</a>
        </div>

        <div class="footer__col">
          <h5>技术栈</h5>
          <span class="tech">Spring Boot 3 + Java 21</span>
          <span class="tech">Vue 3 + Vite + Element Plus</span>
          <span class="tech">MyBatis-Plus + MySQL + Redis</span>
          <span class="tech">高德地图 JS API 2.0</span>
        </div>
      </div>
      <div class="footer__bottom">
        © {{ year }} 乐宠乐 LePetLe · 宠物日常上门服务平台 · 保留所有权利
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { HOME_BY_ROLE } from '@/router'

const router = useRouter()
const userStore = useUserStore()
const year = new Date().getFullYear()

/* ---------- 导航 ---------- */
const scrolled = ref(false)
const navLinks = [
  { label: '首页', id: 'home' },
  { label: '服务项目', id: 'services' },
  { label: '安全保障', id: 'trust' },
  { label: '服务流程', id: 'process' }
]
function onScroll() {
  scrolled.value = window.scrollY > 12
}
function scrollTo(id) {
  const el = document.getElementById(id)
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
function toTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

/* ---------- 身份入口 / 跳转 ---------- */
function goLogin() {
  router.push({ name: 'login' })
}
function goRegister() {
  router.push({ name: 'register' })
}
function goWorkbench() {
  if (userStore.isLogin) router.push(HOME_BY_ROLE[userStore.role] || '/portal')
  else router.push({ name: 'login' })
}
function goIdentity(role) {
  if (userStore.isLogin) {
    router.push(HOME_BY_ROLE[role] || HOME_BY_ROLE[userStore.role] || '/portal')
  } else {
    ElMessage.info('请先登录后进入对应服务')
    router.push({ name: 'login', query: { redirect: HOME_BY_ROLE[role] } })
  }
}

/* ---------- 服务矩阵 ---------- */
const services = [
  { icon: '🍚', title: '上门日常喂养', price: 40, unit: '次', items: ['加粮换水', '清洁猫砂 / 狗窝', '食具清洗', '环境拍照留证'] },
  { icon: '🦮', title: '定时户外遛狗', price: 30, unit: '次', items: ['防挣脱牵引佩戴', '定点排便清理', '运动轨迹打卡', '返程状态反馈'] },
  { icon: '🛁', title: '上门温和洗护', price: 80, unit: '次', items: ['基础梳毛', '眼耳清洁', '局部修剪', '舒缓洗护吹干'] },
  { icon: '🎾', title: '陪伴互动陪玩', price: 35, unit: '小时', items: ['玩具互动逗宠', '喂食零食', '心理疏导抚慰', '陪玩时长记录'] }
]

/* ---------- 安全保障 ---------- */
const trust = [
  { icon: '📋', title: '专属数字档案', desc: '记录免疫证明、特殊习性与喂养禁忌，让每次服务都贴合爱宠需求。' },
  { icon: '📍', title: 'LBS 毫秒级匹配', desc: '基于地理围栏测算周边距离，就近极速响应，缩短等待时间。' },
  { icon: '📸', title: '全流程图文存证', desc: '进门经纬度定位打卡，按作业清单逐项拍照留痕，证据链完整。' },
  { icon: '🛡️', title: '平台担保与仲裁', desc: '资金暂存平台，验收合格后打款；如有纠纷，快速仲裁退款。' }
]

/* ---------- 服务流程 ---------- */
const steps = [
  { n: '01', title: '在线预约', desc: '建档选服务，指定时段与地址' },
  { n: '02', title: '智能匹配', desc: 'LBS 就近派单，持证陪护员抢单' },
  { n: '03', title: '上门存证', desc: '定位打卡，逐项拍照全程留痕' },
  { n: '04', title: '验收评价', desc: '确认无误打款，双向星级评价' }
]

/* ---------- 实时动态滚动 ---------- */
const activities = [
  { area: '朝阳区', pet: '柯基 · 日常散步', text: '刚刚由陪护员李师傅完成验收' },
  { area: '海淀区', pet: '英短 · 上门喂养', text: '陪护员王阿姨已进门定位打卡' },
  { area: '西城区', pet: '金毛 · 户外遛狗', text: '全程运动轨迹已存证' },
  { area: '东城区', pet: '布偶猫 · 温和洗护', text: '用户已给出五星好评' }
]
const actIndex = ref(0)
let ticker = null

/* ---------- 数据背书（数字滚动） ---------- */
const stats = [
  { target: 99.8, decimals: 1, suffix: '%', label: '服务好评率' },
  { target: 100, decimals: 0, suffix: '%', label: '真实定位与履约存证' },
  { target: 15, decimals: 0, suffix: '分钟', label: '极速接单响应' },
  { target: 8000, decimals: 0, suffix: '+', label: '累计安心托养订单' }
]
const progress = ref(0)
let rafId = null
function displayStat(s) {
  const v = s.target * progress.value
  const num = s.decimals ? v.toFixed(s.decimals) : Math.round(v).toLocaleString('en-US')
  return num + s.suffix
}
function animateStats() {
  const start = performance.now()
  const dur = 1500
  const tick = (now) => {
    const p = Math.min((now - start) / dur, 1)
    progress.value = 1 - Math.pow(1 - p, 3) // easeOutCubic
    if (p < 1) rafId = requestAnimationFrame(tick)
  }
  rafId = requestAnimationFrame(tick)
}

/* ---------- 生命周期 ---------- */
onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })
  onScroll()
  ticker = setInterval(() => {
    actIndex.value = (actIndex.value + 1) % activities.length
  }, 3200)
  animateStats()
})
onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
  if (ticker) clearInterval(ticker)
  if (rafId) cancelAnimationFrame(rafId)
})
</script>

<style scoped>
.portal {
  --primary: #4e7c59;
  --primary-strong: #3e6547;
  --primary-soft: #5e8b67;
  --tint: #ebf3ee;
  --tint-2: #ddeade;
  --bg: #f7faf8;
  --ink: #23302a;
  --muted: #6b7c72;
  --line: #e8efe9;
  --radius: 16px;
  --shadow: 0 10px 30px rgba(78, 124, 89, 0.06);
  --shadow-hover: 0 18px 40px rgba(78, 124, 89, 0.14);

  min-height: 100vh;
  background: var(--bg);
  color: var(--ink);
  overflow-x: hidden;
}

/* ============ 通用按钮 ============ */
.btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 13px 24px;
  border-radius: 999px;
  border: 1px solid transparent;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.25s ease, background 0.25s ease, color 0.25s ease, border-color 0.25s ease;
  white-space: nowrap;
}
.btn__emoji { font-size: 17px; }
.btn--primary {
  background: linear-gradient(135deg, var(--primary-soft), var(--primary));
  color: #fff;
  box-shadow: 0 10px 24px rgba(78, 124, 89, 0.28);
}
.btn--primary:hover { transform: translateY(-3px); box-shadow: 0 16px 32px rgba(62, 101, 71, 0.34); }
.btn--ghost { background: #fff; color: var(--primary-strong); border-color: var(--tint-2); box-shadow: var(--shadow); }
.btn--ghost:hover { transform: translateY(-3px); border-color: var(--primary-soft); box-shadow: var(--shadow-hover); }
.btn--outline { background: transparent; color: var(--primary); border-color: var(--primary); }
.btn--outline:hover { background: var(--tint); transform: translateY(-3px); }
.btn--soft { background: var(--tint); color: var(--primary-strong); padding: 10px 20px; font-size: 14px; }
.btn--soft:hover { background: var(--primary); color: #fff; transform: translateY(-2px); }

/* ============ 1. Navbar ============ */
.nav {
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  background: rgba(255, 255, 255, 0.85);
  border-bottom: 1px solid transparent;
  transition: box-shadow 0.3s ease, border-color 0.3s ease;
}
.nav--scrolled { box-shadow: 0 6px 24px rgba(78, 124, 89, 0.08); border-bottom-color: var(--line); }
.nav__inner {
  max-width: 1200px;
  margin: 0 auto;
  height: 72px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  gap: 28px;
}
.brand { display: flex; align-items: center; gap: 12px; cursor: pointer; user-select: none; }
.brand__logo {
  width: 42px; height: 42px;
  display: grid; place-items: center;
  color: #fff;
  background: linear-gradient(135deg, var(--primary-soft), var(--primary));
  border-radius: 13px;
  box-shadow: 0 6px 16px rgba(78, 124, 89, 0.3);
}
.brand__logo svg { width: 26px; height: 26px; }
.brand__text { display: flex; flex-direction: column; line-height: 1.15; }
.brand__text strong { font-size: 20px; letter-spacing: 1px; color: var(--primary-strong); }
.brand__text small { font-size: 12px; color: var(--muted); letter-spacing: 0.5px; }
.brand--light .brand__text strong { color: #fff; }
.brand--light .brand__text small { color: rgba(255, 255, 255, 0.7); }

.nav__links { display: flex; gap: 6px; margin-left: 8px; flex: 1; }
.nav__link {
  padding: 8px 14px;
  font-size: 15px;
  color: #40524a;
  border-radius: 999px;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease;
}
.nav__link:hover { background: var(--tint); color: var(--primary-strong); }

.nav__actions { display: flex; align-items: center; gap: 6px; }
.nav__hi { font-size: 14px; color: var(--muted); margin-right: 6px; }
.nav__btn { color: #40524a; font-weight: 600; }
.nav__cta { font-weight: 600; padding: 10px 22px; }

/* ============ 2. Hero ============ */
.hero { position: relative; padding: 60px 24px 80px; scroll-margin-top: 84px; }
.hero__blob { position: absolute; border-radius: 50%; filter: blur(60px); opacity: 0.5; z-index: 0; pointer-events: none; }
.hero__blob--a { width: 420px; height: 420px; background: #cfe3d3; top: -80px; right: -60px; }
.hero__blob--b { width: 340px; height: 340px; background: #e2eef0; bottom: -60px; left: -80px; }
.hero__inner {
  position: relative;
  z-index: 1;
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  gap: 48px;
  align-items: center;
}
.chip {
  display: inline-flex; align-items: center;
  padding: 7px 16px;
  background: var(--tint);
  color: var(--primary-strong);
  border: 1px solid var(--tint-2);
  border-radius: 999px;
  font-size: 13.5px; font-weight: 600;
}
.hero__title { font-size: 42px; line-height: 1.28; margin: 20px 0 18px; font-weight: 800; letter-spacing: 0.5px; color: #1f2d25; }
.hero__sub { font-size: 16px; line-height: 1.85; color: var(--muted); margin: 0 0 30px; max-width: 540px; }
.hero__cta { display: flex; flex-wrap: wrap; gap: 14px; }
.hero__trust { display: flex; flex-wrap: wrap; gap: 18px; margin-top: 30px; }
.hero__trust-item { display: inline-flex; align-items: center; gap: 7px; font-size: 13.5px; color: #4a5c52; font-weight: 500; }
.hero__trust-item .dot { width: 7px; height: 7px; border-radius: 50%; background: var(--primary-soft); display: inline-block; }

.hero__right { position: relative; display: flex; flex-direction: column; gap: 18px; padding: 10px 0; }
.float-card {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid var(--line);
  border-radius: 18px;
  box-shadow: var(--shadow-hover);
  padding: 20px 22px;
  backdrop-filter: blur(6px);
  animation: floaty 6s ease-in-out infinite;
}
.f1 { align-self: flex-start; width: 78%; }
.f2 { align-self: flex-end; width: 82%; animation-delay: 1.2s; }
.f3 { align-self: flex-start; width: 88%; animation-delay: 2.1s; }
@keyframes floaty { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-10px); } }

.float-card__badge {
  display: inline-block; font-size: 12px; font-weight: 700; color: #fff;
  background: linear-gradient(135deg, var(--primary-soft), var(--primary));
  padding: 4px 12px; border-radius: 999px; letter-spacing: 0.5px;
}
.float-card__num { font-size: 40px; font-weight: 800; color: var(--primary-strong); line-height: 1.1; margin-top: 10px; }
.float-card__num span { font-size: 16px; font-weight: 600; margin-left: 4px; color: var(--muted); }
.float-card__label { font-size: 13.5px; color: var(--muted); margin-top: 4px; }

.float-card--sitter { display: flex; align-items: center; gap: 16px; position: relative; }
.avatars { display: flex; }
.avatars span {
  width: 40px; height: 40px; border-radius: 50%;
  display: grid; place-items: center; font-size: 20px;
  background: var(--tint); border: 2px solid #fff; margin-left: -10px;
}
.avatars span:first-child { margin-left: 0; }
.avatars .more { font-size: 13px; font-weight: 700; color: var(--primary-strong); background: var(--tint-2); }
.float-card__text { font-size: 14px; line-height: 1.6; color: #43534b; }
.float-card__text b { color: var(--primary-strong); }
.float-card__text .hl { font-size: 18px; }
.pulse { position: absolute; top: 18px; right: 18px; width: 10px; height: 10px; border-radius: 50%; background: #52c41a; box-shadow: 0 0 0 0 rgba(82, 196, 26, 0.6); animation: pulse 1.8s infinite; }
@keyframes pulse { 70% { box-shadow: 0 0 0 10px rgba(82, 196, 26, 0); } 100% { box-shadow: 0 0 0 0 rgba(82, 196, 26, 0); } }

.ticker__head { display: flex; align-items: center; margin-bottom: 10px; }
.live { display: inline-flex; align-items: center; gap: 7px; font-size: 12.5px; font-weight: 700; color: var(--primary-strong); }
.live i { width: 8px; height: 8px; border-radius: 50%; background: #ff6b6b; animation: blink 1.4s infinite; }
@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0.3; } }
.ticker__body { font-size: 14px; color: #43534b; line-height: 1.6; min-height: 44px; }
.ticker__body b { color: var(--primary-strong); }
.ticker__tail { display: block; color: var(--muted); font-size: 13px; margin-top: 2px; }
.tick-enter-active, .tick-leave-active { transition: all 0.45s ease; }
.tick-enter-from { opacity: 0; transform: translateY(10px); }
.tick-leave-to { opacity: 0; transform: translateY(-10px); }

/* ============ 通用 Section ============ */
.section { max-width: 1200px; margin: 0 auto; padding: 72px 24px; scroll-margin-top: 84px; }
.section__head { text-align: center; max-width: 680px; margin: 0 auto 46px; }
.section__eyebrow {
  display: inline-block; font-size: 13px; font-weight: 700; letter-spacing: 1px;
  color: var(--primary); background: var(--tint); padding: 6px 16px; border-radius: 999px; margin-bottom: 16px;
}
.section__head h2 { font-size: 32px; font-weight: 800; margin: 0 0 12px; color: #1f2d25; }
.section__head p { font-size: 15.5px; color: var(--muted); line-height: 1.8; margin: 0; }

.grid { display: grid; gap: 22px; }
.grid--4 { grid-template-columns: repeat(4, 1fr); }

/* ============ 3. 服务卡片 ============ */
.service-card {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 18px;
  padding: 26px 22px;
  box-shadow: var(--shadow);
  display: flex;
  flex-direction: column;
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
}
.service-card:hover { transform: translateY(-6px); border-color: var(--primary-soft); box-shadow: var(--shadow-hover); }
.service-card__icon {
  width: 58px; height: 58px; border-radius: 16px; font-size: 30px;
  display: grid; place-items: center;
  background: linear-gradient(135deg, var(--tint), var(--tint-2));
  margin-bottom: 16px;
}
.service-card__title { font-size: 18px; font-weight: 700; margin: 0 0 10px; color: #24332b; }
.price-tag {
  align-self: flex-start; font-size: 13px; font-weight: 700; color: var(--primary-strong);
  background: var(--tint); border: 1px solid var(--tint-2); padding: 4px 12px; border-radius: 999px; margin-bottom: 16px;
}
.check-list { list-style: none; padding: 0; margin: 0 0 20px; flex: 1; }
.check-list li { display: flex; align-items: center; gap: 8px; font-size: 13.5px; color: #55655c; padding: 5px 0; }
.check-ico { width: 16px; height: 16px; color: var(--primary-soft); flex-shrink: 0; }
.service-card__btn { align-self: stretch; justify-content: center; }

/* ============ 服务流程 ============ */
.process { background: linear-gradient(180deg, #fff, var(--tint)); padding: 72px 24px; scroll-margin-top: 84px; }
.process__track { max-width: 1100px; margin: 0 auto; display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.step { position: relative; background: #fff; border: 1px solid var(--line); border-radius: 16px; padding: 24px 20px; box-shadow: var(--shadow); }
.step__no { font-size: 26px; font-weight: 800; color: var(--tint-2); background: linear-gradient(135deg, var(--primary-soft), var(--primary)); -webkit-background-clip: text; background-clip: text; -webkit-text-fill-color: transparent; }
.step__body h4 { font-size: 16px; margin: 8px 0 6px; color: #24332b; }
.step__body p { font-size: 13px; color: var(--muted); line-height: 1.6; margin: 0; }
.step__arrow { position: absolute; right: -14px; top: 50%; transform: translateY(-50%); color: var(--primary-soft); font-size: 20px; z-index: 2; }

/* ============ 4. 安全保障 ============ */
.trust-card {
  background: #fff; border: 1px solid var(--line); border-radius: 18px; padding: 28px 24px;
  box-shadow: var(--shadow); text-align: left;
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
}
.trust-card:hover { transform: translateY(-6px); border-color: var(--primary-soft); box-shadow: var(--shadow-hover); }
.trust-card__icon {
  width: 54px; height: 54px; border-radius: 15px; font-size: 27px; display: grid; place-items: center;
  background: linear-gradient(135deg, var(--primary-soft), var(--primary)); color: #fff; margin-bottom: 16px;
  box-shadow: 0 8px 18px rgba(78, 124, 89, 0.24);
}
.trust-card h3 { font-size: 17px; margin: 0 0 8px; color: #24332b; }
.trust-card p { font-size: 13.5px; color: var(--muted); line-height: 1.75; margin: 0; }

/* ============ 5. 数据背书 ============ */
.stats { background: linear-gradient(135deg, var(--tint), var(--tint-2)); padding: 54px 24px; }
.stats__inner { max-width: 1100px; margin: 0 auto; display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; text-align: center; }
.stat__num { display: block; font-size: 40px; font-weight: 800; color: var(--primary-strong); letter-spacing: 0.5px; }
.stat__label { display: block; margin-top: 6px; font-size: 14px; color: #4f6058; }

/* ============ 6. Footer ============ */
.footer { background: #2b3f31; color: rgba(255, 255, 255, 0.82); padding: 56px 24px 0; }
.footer__inner { max-width: 1200px; margin: 0 auto; display: grid; grid-template-columns: 1.6fr 1fr 1fr 1.2fr; gap: 36px; padding-bottom: 40px; }
.footer__idea { font-size: 13.5px; line-height: 1.9; color: rgba(255, 255, 255, 0.6); margin: 18px 0 0; max-width: 320px; }
.footer__col { display: flex; flex-direction: column; gap: 10px; }
.footer__col h5 { font-size: 15px; color: #fff; margin: 0 0 6px; font-weight: 700; }
.footer__col a { font-size: 13.5px; color: rgba(255, 255, 255, 0.66); cursor: pointer; transition: color 0.2s ease; }
.footer__col a:hover { color: #a9d4b4; }
.tech { font-size: 12.5px; color: rgba(255, 255, 255, 0.55); background: rgba(255, 255, 255, 0.06); border: 1px solid rgba(255, 255, 255, 0.1); padding: 6px 12px; border-radius: 8px; }
.footer__bottom { border-top: 1px solid rgba(255, 255, 255, 0.1); text-align: center; padding: 20px; font-size: 13px; color: rgba(255, 255, 255, 0.5); }

/* ============ 响应式 ============ */
@media (max-width: 1024px) {
  .grid--4 { grid-template-columns: repeat(2, 1fr); }
  .process__track { grid-template-columns: repeat(2, 1fr); }
  .step__arrow { display: none; }
  .stats__inner { grid-template-columns: repeat(2, 1fr); gap: 28px; }
  .footer__inner { grid-template-columns: 1fr 1fr; }
  .hero__inner { grid-template-columns: 1fr; gap: 40px; }
  .hero__title { font-size: 34px; }
  .f1, .f2, .f3 { width: 100%; align-self: stretch; }
  .nav__links { display: none; }
}
@media (max-width: 640px) {
  .grid--4, .process__track { grid-template-columns: 1fr; }
  .stats__inner { grid-template-columns: 1fr 1fr; }
  .footer__inner { grid-template-columns: 1fr; }
  .hero { padding: 40px 18px 56px; }
  .hero__title { font-size: 28px; }
  .hero__cta .btn { width: 100%; justify-content: center; }
  .nav__inner { gap: 12px; padding: 0 16px; }
  .nav__btn { display: none; }
  .section { padding: 52px 18px; }
  .section__head h2 { font-size: 25px; }
}
</style>
