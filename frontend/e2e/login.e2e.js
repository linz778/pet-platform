import { expect, test } from '@playwright/test'

test('访客被引导登录，登录后回到原页面', async ({ page }) => {
  await page.route('**/api/auth/login', (route) => route.fulfill({
    json: { code: 200, data: { token: 'e2e-token', userId: 7, username: 'tester', role: 'USER' } }
  }))
  await page.route('**/api/pet/my', (route) => route.fulfill({ json: { code: 200, data: [] } }))
  await page.route('**/api/notifications?*', (route) => route.fulfill({
    json: { code: 200, data: { items: [], unreadCount: 0 } }
  }))

  await page.goto('/user/pets')
  await expect(page).toHaveURL(/\/login\?redirect=\/user\/pets$/)
  await expect(page.getByRole('link', { name: '返回首页' })).toBeVisible()

  await page.getByPlaceholder('请输入用户名').fill('tester')
  await page.getByPlaceholder('请输入密码').fill('password')
  await page.getByRole('button', { name: '登录', exact: true }).click()

  await expect(page).toHaveURL(/\/user\/pets$/)
  await expect(page.getByText('还没有宠物档案')).toBeVisible()
  expect(await page.evaluate(() => localStorage.getItem('pp_token'))).toBe('e2e-token')
})
