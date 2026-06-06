import { expect, test } from '@playwright/test'
import * as fs from 'fs'
import * as path from 'path'

const uploadFixture = path.join(process.cwd(), 'tests/e2e/acceptance-attachment.txt')

test.beforeAll(() => {
  fs.writeFileSync(uploadFixture, 'calendar acceptance attachment')
})

test.afterAll(() => {
  fs.rmSync(uploadFixture, { force: true })
})

async function login(page, username = 'admin') {
  await page.goto('/login', { waitUntil: 'networkidle' })
  await page.getByPlaceholder('用户名').fill(username)
  await page.getByPlaceholder('密码').fill('admin123')
  await page.getByRole('button', { name: '登录' }).click()
  await page.waitForURL(/\/calendar$/, { timeout: 10000 })
  await page.waitForSelector('.calendar-stage', { timeout: 10000 })
  await page.locator('.el-message').waitFor({ state: 'hidden', timeout: 5000 }).catch(() => {})
}

async function openCreateDialog(page) {
  await page.getByRole('button', { name: /新建日程/ }).first().click()
  await page.waitForSelector('.event-dialog', { timeout: 10000 })
}

async function fillTitle(page, title: string) {
  await page.locator('.title-input input').fill(title)
}

async function selectOptionByFormLabel(page, label: string, optionText: string) {
  const item = page.locator('.event-dialog .el-form-item').filter({ hasText: label }).first()
  await item.locator('.el-select').first().click()
  await page.waitForTimeout(200)
  await page.locator('.el-select-dropdown__item:visible', { hasText: optionText }).first().click()
}

async function saveEvent(page) {
  await page.getByRole('button', { name: '保存日程' }).click()
  await expect(page.locator('.el-message__content', { hasText: '已保存' })).toBeVisible({ timeout: 15000 })
  await page.waitForSelector('.event-dialog', { state: 'hidden', timeout: 10000 })
}

async function searchEvent(page, title: string) {
  await page.goto('/calendar/search', { waitUntil: 'networkidle' })
  await page.locator('input[placeholder="标题、地点、描述"]').fill(title)
  await page.getByRole('button', { name: /查询/ }).click()
  await expect(page.locator('body')).toContainText(title, { timeout: 10000 })
}

async function apiGet(page, path: string) {
  const token = await page.evaluate(() => localStorage.getItem('token'))
  const response = await page.request.get(path, {
    headers: { Authorization: `Bearer ${token}` }
  })
  expect(response.ok()).toBeTruthy()
  return response.json()
}

async function openEventFromCalendar(page, title: string) {
  await page.goto('/calendar', { waitUntil: 'networkidle' })
  await expect(page.locator('body')).toContainText(title, { timeout: 10000 })
  await page.locator('.event-pill, .timed-event, .mobile-event-row', { hasText: title }).first().click()
  await page.waitForSelector('.event-detail-drawer', { timeout: 10000 })
}

test('完整创建链路：基础信息、参会人、提醒、待办、重复、附件保存后可回查', async ({ page }) => {
  await login(page)
  const title = `验收完整创建-${Date.now()}`

  await openCreateDialog(page)
  await fillTitle(page, title)
  await page.locator('.event-dialog .el-form-item').filter({ hasText: '地点' }).locator('input').first()
    .fill('验收会议室A https://meeting.example.com/acceptance')
  await page.locator('.event-dialog .el-form-item').filter({ hasText: '描述' }).locator('textarea').first()
    .fill('通过严格验收 E2E 创建的日程')
  await selectOptionByFormLabel(page, '参会人', '张三')
  await selectOptionByFormLabel(page, '提醒', '30分钟前')
  await selectOptionByFormLabel(page, '重复', '每周')
  await page.getByRole('button', { name: /添加待办/ }).click()
  await page.locator('.todos-section .todo-item').last().locator('input').first().fill('验收待办')
  await page.locator('input[type=file]').setInputFiles(uploadFixture)
  await expect(page.locator('.attachment-item.pending', { hasText: 'acceptance-attachment.txt' })).toBeVisible()
  await saveEvent(page)

  await page.reload({ waitUntil: 'networkidle' })
  await expect(page.locator('body')).toContainText(title, { timeout: 10000 })
  await searchEvent(page, title)

  const events = await apiGet(page, `/api/events?keyword=${encodeURIComponent(title)}&allUsers=true`)
  const event = events.data.find((item) => item.title === title)
  expect(event).toBeTruthy()
  expect(event.location).toContain('meeting.example.com')
  expect(event.total_participants).toBeGreaterThanOrEqual(1)

  const [attachments, reminders, todos, participants] = await Promise.all([
    apiGet(page, `/api/events/${event.id}/attachments`),
    apiGet(page, `/api/events/${event.id}/reminders`),
    apiGet(page, `/api/events/${event.id}/todos`),
    apiGet(page, `/api/events/${event.id}/participants`)
  ])
  expect(attachments.data.some((item) => item.file_name === 'acceptance-attachment.txt')).toBeTruthy()
  expect(reminders.data.some((item) => item.minutes_before === 30)).toBeTruthy()
  expect(todos.data.some((item) => item.title === '验收待办')).toBeTruthy()
  expect(participants.data.some((item) => item.name === '张三')).toBeTruthy()
})

test('参会回执链路：受邀人接受后发起人统计可见', async ({ page }) => {
  await login(page)
  const title = `验收回执-${Date.now()}`

  await openCreateDialog(page)
  await fillTitle(page, title)
  await selectOptionByFormLabel(page, '参会人', '张三')
  await saveEvent(page)

  await page.getByRole('button', { name: '退出' }).click()
  await login(page, 'zhangsan')
  await searchEvent(page, title)
  await openEventFromCalendar(page, title)
  await page.locator('.respond-buttons button', { hasText: '接受' }).click()
  await expect(page.locator('.el-message__content', { hasText: '回执已更新' })).toBeVisible({ timeout: 10000 })
  await expect(page.locator('.participants-list')).toContainText('已接受')

  const events = await apiGet(page, `/api/events?keyword=${encodeURIComponent(title)}&userId=user-002`)
  const event = events.data.find((item) => item.title === title)
  const participants = await apiGet(page, `/api/events/${event.id}/participants`)
  expect(participants.data.some((item) => item.user_id === 'user-002' && item.response_status === 'ACCEPTED')).toBeTruthy()
})

test('重复日程链路：仅删除本次不取消全系列', async ({ page }) => {
  await login(page)
  const title = `验收重复-${Date.now()}`

  await openCreateDialog(page)
  await fillTitle(page, title)
  await selectOptionByFormLabel(page, '重复', '每日')
  await saveEvent(page)

  await page.reload({ waitUntil: 'networkidle' })
  await openEventFromCalendar(page, title)
  await page.getByRole('button', { name: /删除日程/ }).click()
  await page.getByRole('button', { name: '仅删除本次' }).click()
  await expect(page.locator('.el-message__content', { hasText: '已删除' })).toBeVisible({ timeout: 10000 })

  const events = await apiGet(page, `/api/events?keyword=${encodeURIComponent(title)}&allUsers=true`)
  const activeInstances = events.data.filter((item) => item.title === title)
  expect(activeInstances.length).toBeGreaterThan(1)
})

test('导出链路：页面按钮触发真实 xlsx 下载', async ({ page }) => {
  await login(page)
  const responsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/export/events') && response.request().method() === 'POST'
  )
  const downloadPromise = page.waitForEvent('download')
  await page.locator('.view-actions .el-button').last().click()
  const response = await responsePromise
  const download = await downloadPromise
  expect(response.ok()).toBeTruthy()
  expect(response.headers()['content-disposition']).toMatch(/events-export-.*\.xlsx/)
  expect(download.suggestedFilename()).toMatch(/events-export-.*\.xlsx/)
  const downloadPath = await download.path()
  expect(downloadPath).not.toBeNull()
  const body = fs.readFileSync(downloadPath!)
  expect(body.subarray(0, 2).toString()).toBe('PK')
  await expect(page.locator('.el-message__content', { hasText: '导出成功' })).toBeVisible({ timeout: 10000 })
})
