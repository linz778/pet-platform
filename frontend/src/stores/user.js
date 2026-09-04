import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as authApi from '@/api/auth'

const TOKEN_KEY = 'pp_token'
const USER_KEY = 'pp_user'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const userInfo = ref(JSON.parse(localStorage.getItem(USER_KEY) || 'null'))

  const isLogin = computed(() => !!token.value)
  const role = computed(() => userInfo.value?.role || '')

  function setSession(data) {
    token.value = data.token
    userInfo.value = {
      userId: data.userId,
      username: data.username,
      nickname: data.nickname,
      avatar: data.avatar,
      role: data.role
    }
    localStorage.setItem(TOKEN_KEY, token.value)
    localStorage.setItem(USER_KEY, JSON.stringify(userInfo.value))
  }

  async function login(form) {
    const data = await authApi.login(form)
    setSession(data)
    return data
  }

  async function register(form) {
    const data = await authApi.register(form)
    setSession(data)
    return data
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return { token, userInfo, isLogin, role, login, register, logout, setSession }
})
