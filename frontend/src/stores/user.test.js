import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import * as authApi from '@/api/auth'
import { useUserStore } from './user'

vi.mock('@/api/auth', () => ({
  login: vi.fn(),
  register: vi.fn()
}))

const session = {
  token: 'test-token',
  userId: 7,
  username: 'user',
  nickname: '测试用户',
  avatar: null,
  role: 'USER'
}

describe('用户会话', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('登录后保存角色和 token', async () => {
    vi.mocked(authApi.login).mockResolvedValue(session)
    const store = useUserStore()

    await store.login({ username: 'user', password: '123456' })

    expect(store.isLogin).toBe(true)
    expect(store.role).toBe('USER')
    expect(localStorage.getItem('pp_token')).toBe('test-token')
    expect(JSON.parse(localStorage.getItem('pp_user'))).toMatchObject({ userId: 7, role: 'USER' })
  })

  it('退出后清空本地会话', () => {
    const store = useUserStore()
    store.setSession(session)

    store.logout()

    expect(store.isLogin).toBe(false)
    expect(localStorage.getItem('pp_token')).toBeNull()
    expect(localStorage.getItem('pp_user')).toBeNull()
  })
})
