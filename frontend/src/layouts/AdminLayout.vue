<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">🐾 运营管理后台</div>
      <el-menu :default-active="route.path" router class="side-menu">
        <el-menu-item index="/admin/dashboard">数据看板</el-menu-item>
        <el-menu-item index="/admin/audit">资质审核</el-menu-item>
        <el-menu-item index="/admin/dispatch">订单调度</el-menu-item>
        <el-menu-item index="/admin/arbitration">纠纷仲裁</el-menu-item>
        <el-menu-item index="/admin/config">服务规则配置</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="title">{{ route.meta.title || '管理端' }}</span>
        <div class="right">
          <span class="nick">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
          <el-button link type="danger" @click="onLogout">退出</el-button>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

function onLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
}
.aside {
  background: #001529;
}
.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-weight: 700;
}
.side-menu {
  border-right: none;
  background: #001529;
}
.side-menu :deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.75);
}
.side-menu :deep(.el-menu-item.is-active) {
  color: #fff;
  background: var(--pp-primary);
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #eee;
}
.right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.nick {
  color: #606266;
  font-size: 14px;
}
</style>
