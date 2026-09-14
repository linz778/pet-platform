import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import router from './index'
import { useUserStore } from '@/stores/user'

const userSession = {
  token: 'test-token',
  userId: 7,
  username: 'user',
  nickname: '测试用户',
  role: 'USER'
}

describe('路由权限', () => {
  beforeEach(async () => {
    localStorage.clear()
    setActivePinia(createPinia())
    await router.push('/portal')
  })

  it('未登录访问工作台时跳转登录页并保留原地址', async () => {
    await router.push('/user/home')

    expect(router.currentRoute.value.fullPath).toBe('/login?redirect=/user/home')
  })

  it('不同角色不能进入管理端', async () => {
    useUserStore().setSession(userSession)

    await router.push('/admin/dashboard')

    expect(router.currentRoute.value.fullPath).toBe('/user/home')
  })

  it('角色匹配时允许进入对应页面', async () => {
    useUserStore().setSession(userSession)

    await router.push('/user/orders')

    expect(router.currentRoute.value.fullPath).toBe('/user/orders')
  })
})
