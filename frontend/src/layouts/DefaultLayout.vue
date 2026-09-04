<template>
  <el-container class="layout">
    <el-header class="header">
      <div class="brand" @click="router.push('/portal')">🐾 宠物上门服务平台</div>
      <el-menu mode="horizontal" :default-active="route.path" router :ellipsis="false" class="menu">
        <template v-if="userStore.role === 'USER'">
          <el-menu-item index="/user/home">首页</el-menu-item>
          <el-menu-item index="/user/pets">我的宠物</el-menu-item>
          <el-menu-item index="/user/orders">我的订单</el-menu-item>
        </template>
        <template v-else-if="userStore.role === 'SITTER'">
          <el-menu-item index="/sitter/hall">接单大厅</el-menu-item>
          <el-menu-item index="/sitter/orders">我的接单</el-menu-item>
          <el-menu-item index="/sitter/wallet">收益钱包</el-menu-item>
        </template>
      </el-menu>
      <div class="right">
        <template v-if="userStore.isLogin">
          <span class="nick">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
          <el-button link type="danger" @click="onLogout">退出</el-button>
        </template>
        <template v-else>
          <el-button link @click="router.push('/login')">登录</el-button>
          <el-button type="primary" round @click="router.push('/register')">注册</el-button>
        </template>
      </div>
    </el-header>
    <el-main>
      <router-view />
    </el-main>
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
.header {
  display: flex;
  align-items: center;
  gap: 24px;
  background: #fff;
  border-bottom: 1px solid #eee;
}
.brand {
  font-weight: 700;
  font-size: 18px;
  color: var(--pp-primary);
  cursor: pointer;
  white-space: nowrap;
}
.menu {
  flex: 1;
  border-bottom: none;
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
