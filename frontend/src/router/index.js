import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import AdminLayout from '@/layouts/AdminLayout.vue'

const routes = [
  { path: '/login', name: 'login', component: () => import('@/views/auth/Login.vue'), meta: { public: true } },
  { path: '/register', name: 'register', component: () => import('@/views/auth/Register.vue'), meta: { public: true } },

  // ===== 前台：用户端 + 接单员端 =====
  {
    path: '/',
    component: DefaultLayout,
    children: [
      { path: '', redirect: '/portal' },
      { path: 'portal', name: 'portal', component: () => import('@/views/Portal.vue'), meta: { public: true } },

      // 用户端
      { path: 'user/home', name: 'user-home', component: () => import('@/views/user/Home.vue'), meta: { roles: ['USER'] } },
      { path: 'user/pets', name: 'user-pets', component: () => import('@/views/user/Pets.vue'), meta: { roles: ['USER'] } },
      { path: 'user/orders', name: 'user-orders', component: () => import('@/views/user/Orders.vue'), meta: { roles: ['USER'] } },

      // 接单员端
      { path: 'sitter/hall', name: 'sitter-hall', component: () => import('@/views/sitter/Hall.vue'), meta: { roles: ['SITTER'] } },
      { path: 'sitter/orders', name: 'sitter-orders', component: () => import('@/views/sitter/Orders.vue'), meta: { roles: ['SITTER'] } },
      { path: 'sitter/wallet', name: 'sitter-wallet', component: () => import('@/views/sitter/Wallet.vue'), meta: { roles: ['SITTER'] } }
    ]
  },

  // ===== 后台：管理端 =====
  {
    path: '/admin',
    component: AdminLayout,
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'admin-dashboard', component: () => import('@/views/admin/Dashboard.vue'), meta: { roles: ['ADMIN'] } },
      { path: 'audit', name: 'admin-audit', component: () => import('@/views/admin/Audit.vue'), meta: { roles: ['ADMIN'] } },
      { path: 'dispatch', name: 'admin-dispatch', component: () => import('@/views/admin/Dispatch.vue'), meta: { roles: ['ADMIN'] } },
      { path: 'arbitration', name: 'admin-arbitration', component: () => import('@/views/admin/Arbitration.vue'), meta: { roles: ['ADMIN'] } },
      { path: 'config', name: 'admin-config', component: () => import('@/views/admin/Config.vue'), meta: { roles: ['ADMIN'] } }
    ]
  },

  { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/views/NotFound.vue'), meta: { public: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 登录后各角色的默认落地页
const HOME_BY_ROLE = {
  USER: '/user/home',
  SITTER: '/sitter/hall',
  ADMIN: '/admin/dashboard'
}

router.beforeEach((to) => {
  const userStore = useUserStore()

  if (to.meta.public) return true

  if (!userStore.isLogin) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (to.meta.roles && !to.meta.roles.includes(userStore.role)) {
    return HOME_BY_ROLE[userStore.role] || '/portal'
  }
  return true
})

export default router
export { HOME_BY_ROLE }
