import { expect, test } from '@playwright/test'

test('发布动态只填写正文，仍能提交给现有接口', async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem('pp_token', 'e2e-token')
    localStorage.setItem('pp_user', JSON.stringify({ userId: 7, username: 'tester', role: 'USER' }))
  })
  await page.route('**/api/community/posts?*', (route) => route.fulfill({
    json: { code: 200, data: { records: [], total: 0 } }
  }))
  await page.route('**/api/pet/my', (route) => route.fulfill({ json: { code: 200, data: [] } }))
  await page.route('**/api/notifications?*', (route) => route.fulfill({
    json: { code: 200, data: { items: [], unreadCount: 0 } }
  }))
  let submitted
  await page.route('**/api/community/posts', (route) => {
    submitted = route.request().postDataJSON()
    return route.fulfill({ json: { code: 200, data: 1 } })
  })

  await page.goto('/community')
  await page.getByRole('button', { name: '发布动态' }).click()
  const dialog = page.getByRole('dialog')
  await expect(dialog.getByText('标题', { exact: true })).toHaveCount(0)
  await dialog.locator('textarea').fill('奶糖今天学会握手啦')
  await dialog.getByRole('button', { name: '发布到社区' }).click()

  await expect.poll(() => submitted?.title).toBe('奶糖今天学会握手啦')
  expect(submitted.content).toBe('奶糖今天学会握手啦')
})
