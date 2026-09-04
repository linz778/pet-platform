<template>
  <div class="portal">
    <section class="hero">
      <h1>宠物日常上门服务系统平台</h1>
      <p>熟悉的环境，专业的照护 —— 上门喂养 / 洗护 / 散步，全流程图文存证，安心托付。</p>
      <div class="actions">
        <el-button type="primary" size="large" round @click="go('USER')">我要下单（宠物主人）</el-button>
        <el-button size="large" round @click="go('SITTER')">我要接单（陪护员）</el-button>
        <el-button size="large" round @click="go('ADMIN')">运营管理</el-button>
      </div>
    </section>

    <section class="features">
      <el-card v-for="f in features" :key="f.title" class="feature" shadow="hover">
        <div class="icon">{{ f.icon }}</div>
        <h3>{{ f.title }}</h3>
        <p>{{ f.desc }}</p>
      </el-card>
    </section>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { HOME_BY_ROLE } from '@/router'

const router = useRouter()
const userStore = useUserStore()

const features = [
  { icon: '📋', title: '专属数字档案', desc: '登记品种、疫苗、习性与喂养禁忌，服务更贴合。' },
  { icon: '📍', title: 'LBS 精准匹配', desc: '基于地理位置毫秒检索周边订单，抢单防超卖。' },
  { icon: '📸', title: '全流程存证', desc: '进门定位打卡，逐项拍照存证，证据链完整。' },
  { icon: '⚖️', title: '信用与仲裁', desc: '资质审核、双向评价、纠纷仲裁退款有保障。' }
]

function go(role) {
  if (!userStore.isLogin) {
    router.push({ name: 'login' })
    return
  }
  router.push(HOME_BY_ROLE[role] || '/portal')
}
</script>

<style scoped>
.hero {
  text-align: center;
  padding: 60px 16px 40px;
}
.hero h1 {
  font-size: 32px;
  margin: 0 0 12px;
}
.hero p {
  color: #909399;
  margin: 0 0 24px;
}
.actions {
  display: flex;
  gap: 16px;
  justify-content: center;
  flex-wrap: wrap;
}
.features {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
  max-width: 1100px;
  margin: 0 auto;
  padding: 24px 16px 60px;
}
.feature {
  text-align: center;
}
.icon {
  font-size: 36px;
}
.feature h3 {
  margin: 8px 0;
}
.feature p {
  color: #909399;
  font-size: 14px;
  margin: 0;
}
</style>
