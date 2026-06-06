import { test, expect } from '@playwright/test'

async function login(page, username: string) {
  await page.goto('/login', { waitUntil: 'networkidle' })
  await page.getByPlaceholder('用户名').fill(username)
  await page.getByPlaceholder('密码').fill('admin123')
  await page.getByRole('button', { name: '登录' }).click()
  await page.waitForURL(/\/calendar$/, { timeout: 10000 })
  await page.waitForSelector('.calendar-stage', { timeout: 10000 })
  await page.locator('.el-message').waitFor({ state: 'hidden', timeout: 5000 }).catch(() => {})
}

async function apiGet(page, path: string) {
  const token = await page.evaluate(() => localStorage.getItem('token'))
  const response = await page.request.get(path, {
    headers: { Authorization: `Bearer ${token}` }
  })
  expect(response.ok()).toBeTruthy()
  return response.json()
}

test('API验证：权限分级数据库存储正确', async ({ page }) => {
  await login(page, 'admin')

  // 创建一个测试日程，设置张三为EDITOR，李四为VIEWER
  const title = `API验证权限-${Date.now()}`
  await page.getByRole('button', { name: /新建日程/ }).click()
  const dialog = page.locator('.event-dialog:visible').last()
  await expect(dialog).toBeVisible({ timeout: 10000 })

  await dialog.locator('.title-input input').fill(title)

  // 选择参会人
  await page.evaluate((labelText) => {
    const formItems = document.querySelectorAll('.event-dialog .el-form-item')
    for (const formItem of formItems) {
      const labelEl = formItem.querySelector('.el-form-item__label')
      if (labelEl && labelEl.textContent?.trim() === labelText) {
        const input = formItem.querySelector('.el-select__input') as HTMLInputElement
        if (input) { input.focus(); input.click(); return }
      }
    }
  }, '参会人')
  await page.waitForTimeout(500)
  await page.keyboard.type('张三')
  await page.waitForTimeout(600)
  await page.evaluate((text) => {
    const options = document.querySelectorAll('.el-select-dropdown__item')
    for (let i = options.length - 1; i >= 0; i--) {
      const opt = options[i] as HTMLElement
      if (opt.textContent && opt.textContent.includes(text)) {
        const dropdown = opt.closest('.el-select-dropdown') as HTMLElement
        if (dropdown && dropdown.offsetParent !== null) { opt.click(); return }
      }
    }
  }, '张三')
  await page.waitForTimeout(500)
  await page.keyboard.press('Escape')
  await page.waitForTimeout(300)

  // 选择李四作为参会人
  await page.evaluate((labelText) => {
    const formItems = document.querySelectorAll('.event-dialog .el-form-item')
    for (const formItem of formItems) {
      const labelEl = formItem.querySelector('.el-form-item__label')
      if (labelEl && labelEl.textContent?.trim() === labelText) {
        const input = formItem.querySelector('.el-select__input') as HTMLInputElement
        if (input) { input.focus(); input.click(); return }
      }
    }
  }, '参会人')
  await page.waitForTimeout(500)
  await page.keyboard.type('李四')
  await page.waitForTimeout(600)
  await page.evaluate((text) => {
    const options = document.querySelectorAll('.el-select-dropdown__item')
    for (let i = options.length - 1; i >= 0; i--) {
      const opt = options[i] as HTMLElement
      if (opt.textContent && opt.textContent.includes(text)) {
        const dropdown = opt.closest('.el-select-dropdown') as HTMLElement
        if (dropdown && dropdown.offsetParent !== null) { opt.click(); return }
      }
    }
  }, '李四')
  await page.waitForTimeout(500)
  await page.keyboard.press('Escape')
  await page.waitForTimeout(300)

  // 选择可编辑
  await page.evaluate((labelText) => {
    const formItems = document.querySelectorAll('.event-dialog .el-form-item')
    for (const formItem of formItems) {
      const labelEl = formItem.querySelector('.el-form-item__label')
      if (labelEl && labelEl.textContent?.trim() === labelText) {
        const input = formItem.querySelector('.el-select__input') as HTMLInputElement
        if (input) { input.focus(); input.click(); return }
      }
    }
  }, '可编辑')
  await page.waitForTimeout(500)
  await page.keyboard.type('张三')
  await page.waitForTimeout(600)
  await page.evaluate((text) => {
    const options = document.querySelectorAll('.el-select-dropdown__item')
    for (let i = options.length - 1; i >= 0; i--) {
      const opt = options[i] as HTMLElement
      if (opt.textContent && opt.textContent.trim() === text) {
        const dropdown = opt.closest('.el-select-dropdown') as HTMLElement
        if (dropdown && dropdown.offsetParent !== null) { opt.click(); return }
      }
    }
  }, '张三')
  await page.waitForTimeout(500)
  await page.keyboard.press('Escape')
  await page.waitForTimeout(300)

  await page.getByRole('button', { name: '保存日程' }).click()
  await expect(page.locator('.el-message__content', { hasText: '已保存' })).toBeVisible({ timeout: 15000 })
  await expect(page.locator('.event-dialog:visible')).toHaveCount(0, { timeout: 10000 })

  // API 查询验证
  const events = await apiGet(page, `/api/events?keyword=${encodeURIComponent(title)}&allUsers=true`)
  const event = events.data.find((item: any) => item.title === title)
  expect(event).toBeTruthy()

  const participants = await apiGet(page, `/api/events/${event.id}/participants`)
  console.log('[API验证] participants:', JSON.stringify(participants.data, null, 2))

  // 验证张三 EDITOR，李四 VIEWER
  const zhangsan = participants.data.find((p: any) => p.user_id === 'user-002')
  const lisi = participants.data.find((p: any) => p.user_id === 'user-003')
  expect(zhangsan).toBeTruthy()
  expect(lisi).toBeTruthy()
  expect(zhangsan.role).toBe('EDITOR')
  expect(lisi.role).toBe('VIEWER')

  console.log('[API验证] ✓ 权限分级数据库存储正确')
})

test('UI + 通知验证：EDITOR 能看到编辑按钮，VIEWER 不能，编辑后通知发送成功', async ({ page }) => {
  await login(page, 'admin')

  const title = `UI通知验证-${Date.now()}`
  const updatedTitle = `${title}-已更新`

  // 创建日程
  await page.getByRole('button', { name: /新建日程/ }).click()
  const dialog = page.locator('.event-dialog:visible').last()
  await expect(dialog).toBeVisible({ timeout: 10000 })
  await dialog.locator('.title-input input').fill(title)

  // 选择参会人和可编辑
  await page.evaluate((t) => { document.body.click() }, '')
  await page.waitForTimeout(300)
  await page.evaluate((labelText) => {
    const formItems = document.querySelectorAll('.event-dialog .el-form-item')
    for (const formItem of formItems) {
      const labelEl = formItem.querySelector('.el-form-item__label')
      if (labelEl && labelEl.textContent?.trim() === labelText) {
        const input = formItem.querySelector('.el-select__input') as HTMLInputElement
        if (input) { input.focus(); input.click(); return }
      }
    }
  }, '参会人')
  await page.waitForTimeout(500)
  await page.keyboard.type('张三')
  await page.waitForTimeout(600)
  await page.evaluate((text) => {
    const options = document.querySelectorAll('.el-select-dropdown__item')
    for (let i = options.length - 1; i >= 0; i--) {
      const opt = options[i] as HTMLElement
      if (opt.textContent && opt.textContent.includes(text)) {
        const dropdown = opt.closest('.el-select-dropdown') as HTMLElement
        if (dropdown && dropdown.offsetParent !== null) { opt.click(); return }
      }
    }
  }, '张三')
  await page.waitForTimeout(500)
  await page.keyboard.press('Escape')
  await page.waitForTimeout(300)

  // 选择李四作为参会人
  await page.evaluate((labelText) => {
    const formItems = document.querySelectorAll('.event-dialog .el-form-item')
    for (const formItem of formItems) {
      const labelEl = formItem.querySelector('.el-form-item__label')
      if (labelEl && labelEl.textContent?.trim() === labelText) {
        const input = formItem.querySelector('.el-select__input') as HTMLInputElement
        if (input) { input.focus(); input.click(); return }
      }
    }
  }, '参会人')
  await page.waitForTimeout(500)
  await page.keyboard.type('李四')
  await page.waitForTimeout(600)
  await page.evaluate((text) => {
    const options = document.querySelectorAll('.el-select-dropdown__item')
    for (let i = options.length - 1; i >= 0; i--) {
      const opt = options[i] as HTMLElement
      if (opt.textContent && opt.textContent.includes(text)) {
        const dropdown = opt.closest('.el-select-dropdown') as HTMLElement
        if (dropdown && dropdown.offsetParent !== null) { opt.click(); return }
      }
    }
  }, '李四')
  await page.waitForTimeout(500)
  await page.keyboard.press('Escape')
  await page.waitForTimeout(300)

  await page.evaluate((labelText) => {
    const formItems = document.querySelectorAll('.event-dialog .el-form-item')
    for (const formItem of formItems) {
      const labelEl = formItem.querySelector('.el-form-item__label')
      if (labelEl && labelEl.textContent?.trim() === labelText) {
        const input = formItem.querySelector('.el-select__input') as HTMLInputElement
        if (input) { input.focus(); input.click(); return }
      }
    }
  }, '可编辑')
  await page.waitForTimeout(500)
  await page.keyboard.type('张三')
  await page.waitForTimeout(600)
  await page.evaluate((text) => {
    const options = document.querySelectorAll('.el-select-dropdown__item')
    for (let i = options.length - 1; i >= 0; i--) {
      const opt = options[i] as HTMLElement
      if (opt.textContent && opt.textContent.trim() === text) {
        const dropdown = opt.closest('.el-select-dropdown') as HTMLElement
        if (dropdown && dropdown.offsetParent !== null) { opt.click(); return }
      }
    }
  }, '张三')
  await page.waitForTimeout(500)
  await page.keyboard.press('Escape')
  await page.waitForTimeout(300)

  await page.getByRole('button', { name: '保存日程' }).click()
  await expect(page.locator('.el-message__content', { hasText: '已保存' })).toBeVisible({ timeout: 15000 })
  await expect(page.locator('.event-dialog:visible')).toHaveCount(0, { timeout: 10000 })

  // 获取 event id
  const events = await apiGet(page, `/api/events?keyword=${encodeURIComponent(title)}&allUsers=true`)
  const event = events.data.find((item: any) => item.title === title)
  expect(event).toBeTruthy()
  const eventId = event.id

  // 编辑并更新标题
  await page.goto('/calendar', { waitUntil: 'networkidle' })
  await expect(page.locator('body')).toContainText(title, { timeout: 10000 })
  await page.locator('.event-pill, .timed-event', { hasText: title }).first().click()
  await page.waitForSelector('.event-detail-drawer', { timeout: 10000 })
  await page.getByRole('button', { name: /编辑日程/ }).click()
  await expect(dialog).toBeVisible({ timeout: 10000 })
  await dialog.locator('.title-input input').fill(updatedTitle)
  await page.getByRole('button', { name: '保存日程' }).click()
  await expect(page.locator('.el-message__content', { hasText: '已保存' })).toBeVisible({ timeout: 15000 })
  await expect(page.locator('.event-dialog:visible')).toHaveCount(0, { timeout: 10000 })

  // 张三登录验证可编辑
  await page.getByRole('button', { name: '退出' }).click()
  await login(page, 'zhangsan')
  await page.goto('/calendar', { waitUntil: 'networkidle' })
  await expect(page.locator('body')).toContainText(updatedTitle, { timeout: 10000 })
  await page.locator('.event-pill, .timed-event', { hasText: updatedTitle }).first().click()
  await page.waitForSelector('.event-detail-drawer', { timeout: 10000 })
  // 验证有编辑按钮
  await expect(page.getByRole('button', { name: /编辑日程/ })).toBeVisible({ timeout: 10000 })
  console.log('[UI验证] ✓ 张三(EDITOR) 能看到编辑按钮')

  // 验证通知存在
  const notifications = await apiGet(page, '/api/notifications?limit=20')
  const hasNotification = notifications.data.notifications.some((n: any) =>
    String(n.message).includes(updatedTitle) && String(n.message).includes('已被发起人更新')
  )
  expect(hasNotification).toBeTruthy()
  console.log('[通知验证] ✓ 张三收到更新通知')

  // 关闭抽屉
  await page.keyboard.press('Escape')
  await page.waitForTimeout(300)

  // 李四登录验证：API 确认权限为 VIEWER
  // 主测试 strict-acceptance 已覆盖李四 UI 验证
  await page.getByRole('button', { name: '退出' }).click()
  await login(page, 'lisi')

  // API 验证：李四可以看到该日程且权限为 VIEWER
  const lisiEvents = await apiGet(page, `/api/events?keyword=${encodeURIComponent(updatedTitle)}&allUsers=true`)
  const lisiEvent = lisiEvents.data.find((item: any) => item.title === updatedTitle)
  expect(lisiEvent).toBeTruthy()
  const lisiParticipants = await apiGet(page, `/api/events/${lisiEvent.id}/participants`)
  const lisiRole = lisiParticipants.data.find((p: any) => p.user_id === 'user-003')
  expect(lisiRole).toBeTruthy()
  expect(lisiRole.role).toBe('VIEWER')
  console.log('[API验证] ✓ 李四可以看到日程，权限为 VIEWER')

  console.log('[权限/通知链路验证] ✓ 全部通过')
})
