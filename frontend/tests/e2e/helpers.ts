import { Page, expect } from '@playwright/test'
import * as path from 'path'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

// Test results storage
export const testResults: Array<{
  id: number
  module: string
  feature: string
  passed: boolean
  screenshotPath?: string
}> = []

let currentTestId = 1

export function recordTestResult(
  module: string,
  feature: string,
  passed: boolean,
  screenshotPath?: string
) {
  testResults.push({
    id: currentTestId++,
    module,
    feature,
    passed,
    screenshotPath
  })
}

/**
 * Take and save a screenshot with a descriptive name.
 */
export async function takeScreenshot(page: Page, name: string) {
  const timestamp = Date.now()
  const screenshotsDir = path.join(__dirname, '../../screenshots')
  const screenshotPath = path.join(screenshotsDir, `${name}-${timestamp}.png`)
  await page.screenshot({ path: screenshotPath, fullPage: true })
  return screenshotPath
}

/**
 * Open the create-event dialog from CalendarHome page.
 * Clicks the "创建日程" / "新建" button.
 */
export async function openCreateDialog(page: Page) {
  // Try both possible button labels
  const createBtn = page.locator('button', { hasText: /创建日程|新建日程|新建/ }).first()
  await createBtn.click()
  // Wait for the dialog to appear
  await page.waitForSelector('.el-dialog', { timeout: 5000 })
}

/**
 * Fill the event title in the create/edit dialog.
 */
export async function fillEventTitle(page: Page, title: string) {
  // EventForm uses a bare el-input for title (with class .title-input)
  const titleInput = page.locator('.title-input input, .el-dialog input[placeholder*="标题"]').first()
  await titleInput.fill(title)
}

/**
 * Fill location field.
 */
export async function fillEventLocation(page: Page, location: string) {
  const locationInput = page.locator('.el-dialog .el-form-item').filter({ hasText: '地点' }).locator('input').first()
  await locationInput.fill(location)
}

/**
 * Fill description field.
 */
export async function fillEventDescription(page: Page, description: string) {
  const descInput = page.locator('.el-dialog .el-form-item').filter({ hasText: '描述' }).locator('textarea').first()
  await descInput.fill(description)
}

/**
 * Toggle all-day event.
 */
export async function toggleAllDay(page: Page, checked: boolean) {
  const allDayCheckbox = page.locator('.el-dialog .el-checkbox', { hasText: '全天' }).first()
  const isChecked = await allDayCheckbox.locator('input').isChecked()
  if (isChecked !== checked) {
    await allDayCheckbox.click()
  }
}

/**
 * Select a calendar.
 */
export async function selectCalendar(page: Page, calendarName: string) {
  const calendarSelect = page.locator('.el-dialog .el-form-item').filter({ hasText: '日历' }).locator('.el-select').first()
  await calendarSelect.click()
  await page.waitForTimeout(300)
  const option = page.locator('.el-select-dropdown__item', { hasText: calendarName }).first()
  await option.click()
  await page.waitForTimeout(300)
}

/**
 * Select a tag.
 */
export async function selectTag(page: Page, tagName: string) {
  const tagSelect = page.locator('.el-dialog .el-form-item').filter({ hasText: '标签' }).locator('.el-select').first()
  await tagSelect.click()
  await page.waitForTimeout(300)
  const option = page.locator('.el-select-dropdown__item', { hasText: tagName }).first()
  await option.click()
  await page.waitForTimeout(300)
}

/**
 * Select a recurrence rule.
 */
export async function selectRecurrence(page: Page, rule: 'daily' | 'workday' | 'weekly' | 'biweekly' | 'monthly' | 'yearly') {
  const ruleMap: Record<string, string> = {
    daily: '每日',
    workday: '工作日',
    weekly: '每周',
    biweekly: '双周',
    monthly: '每月',
    yearly: '每年'
  }
  const recurrenceSelect = page.locator('.el-dialog .el-form-item').filter({ hasText: '重复' }).locator('.el-select').first()
  await recurrenceSelect.click()
  await page.waitForTimeout(300)
  const option = page.locator('.el-select-dropdown__item', { hasText: ruleMap[rule] }).first()
  await option.click()
  await page.waitForTimeout(300)
}

/**
 * Select a reminder option.
 */
export async function selectReminder(page: Page, reminder: 'none' | 'immediate' | '5m' | '15m' | '30m' | '1h' | '1d' | 'custom') {
  const reminderMap: Record<string, string> = {
    none: '不提醒',
    immediate: '即时',
    '5m': '5分钟前',
    '15m': '15分钟前',
    '30m': '30分钟前',
    '1h': '1小时前',
    '1d': '1天前',
    custom: '自定义'
  }
  const reminderSelect = page.locator('.el-dialog .el-form-item').filter({ hasText: '提醒' }).locator('.el-select').first()
  await reminderSelect.click()
  await page.waitForTimeout(300)
  const option = page.locator('.el-select-dropdown__item', { hasText: reminderMap[reminder] }).first()
  await option.click()
  await page.waitForTimeout(300)
}

/**
 * Add a todo item.
 */
export async function addTodo(page: Page, title: string, priority: 'low' | 'medium' | 'high' = 'medium') {
  const priorityMap: Record<string, string> = {
    low: '低',
    medium: '中',
    high: '高'
  }
  const addTodoBtn = page.locator('.el-dialog button', { hasText: '添加待办' }).first()
  await addTodoBtn.click()
  await page.waitForTimeout(300)
  
  const todoItems = page.locator('.todos-section .todo-item')
  const lastTodo = todoItems.last()
  
  const todoInput = lastTodo.locator('input').first()
  await todoInput.fill(title)
  
  const prioritySelect = lastTodo.locator('.el-select').first()
  await prioritySelect.click()
  await page.waitForTimeout(200)
  const priorityOption = page.locator('.el-select-dropdown__item', { hasText: priorityMap[priority] }).first()
  await priorityOption.click()
  await page.waitForTimeout(200)
}

/**
 * Click "保存日程" / "确定" in the dialog.
 */
export async function saveEvent(page: Page) {
  const saveBtn = page.locator('.el-dialog button', { hasText: /保存日程|确定/ }).last()
  await saveBtn.click()
  // Wait for success message or error
  await page.waitForSelector('.el-message', { timeout: 10000 })
}

/**
 * Click cancel button in dialog.
 */
export async function cancelDialog(page: Page) {
  const cancelBtn = page.locator('.el-dialog button', { hasText: '取消' }).first()
  await cancelBtn.click()
  await page.waitForTimeout(500)
}

/**
 * Get the ElMessage text content (success/error).
 */
export async function getNotificationText(page: Page): Promise<string> {
  const msg = page.locator('.el-message__content').first()
  return (await msg.textContent()) || ''
}

/**
 * Wait for the calendar page to fully load (data fetched).
 */
export async function waitForCalendarLoad(page: Page) {
  // 等待实际渲染的 calendar 阶段出现（与 CalendarHome 真实 className 对齐）
  await page.waitForSelector('.calendar-stage', { timeout: 15000 })
  // Give time for API calls to complete
  await page.waitForTimeout(1000)
}

/**
 * Navigate to calendar home and wait for load.
 */
export async function gotoCalendar(page: Page) {
  await page.goto('/calendar')
  await waitForCalendarLoad(page)
}

/**
 * Navigate to search page.
 */
export async function gotoSearch(page: Page) {
  await page.goto('/calendar/search')
  await page.waitForTimeout(1000)
}

/**
 * Navigate to admin page.
 */
export async function gotoAdmin(page: Page, path = '/admin') {
  await page.goto(path)
  await page.waitForTimeout(1000)
}

/**
 * Switch view mode on the calendar.
 */
export async function switchView(page: Page, view: 'day' | 'week' | 'month' | 'list') {
  const labelMap: Record<string, string> = {
    day: '日',
    week: '周',
    month: '月',
    list: '列表'
  }
  const viewButtons = page.locator('.view-switch button')
  const count = await viewButtons.count()
  for (let i = 0; i < count; i++) {
    const btn = viewButtons.nth(i)
    const text = await btn.textContent()
    if (text?.includes(labelMap[view])) {
      await btn.click()
      await page.waitForTimeout(500)
      return
    }
  }
  // Fallback if not found
  const btn = page.locator('button', { hasText: labelMap[view] }).first()
  if (await btn.isVisible()) {
    await btn.click()
    await page.waitForTimeout(500)
  }
}

/**
 * Go to today.
 */
export async function goToday(page: Page) {
  const todayBtn = page.locator('button', { hasText: '今天' }).first()
  await todayBtn.click()
  await page.waitForTimeout(500)
}

/**
 * Navigate previous/next period.
 */
export async function navigatePeriod(page: Page, direction: 'prev' | 'next') {
  const navBtns = page.locator('.period-title .el-button')
  const index = direction === 'prev' ? 0 : 1
  await navBtns.nth(index).click()
  await page.waitForTimeout(500)
}

/**
 * Open an event detail by clicking on an event block in the calendar.
 */
export async function openEventDetail(page: Page, title: string) {
  const eventBlock = page.locator('.event-pill, .timed-event, [class*="event"]', { hasText: title }).first()
  await eventBlock.click()
  await page.waitForSelector('.el-dialog, .el-drawer, [class*="detail"]', { timeout: 5000 })
}

/**
 * Delete an event via the detail dialog.
 */
export async function deleteEventFromDetail(page: Page) {
  const deleteBtn = page.locator('button', { hasText: /删除|取消日程/ }).first()
  await deleteBtn.click()
  // Confirm dialog
  await page.waitForTimeout(500)
  const confirmBtn = page.locator('.el-message-box button', { hasText: /确定/ }).first()
  if (await confirmBtn.isVisible()) {
    await confirmBtn.click()
  }
  await page.waitForTimeout(1000)
}

/**
 * Verify an event block exists on the calendar.
 */
export async function expectEventVisible(page: Page, title: string) {
  await expect(page.locator('[class*="event"]', { hasText: title }).first()).toBeVisible({ timeout: 5000 })
}

/**
 * Verify an event is NOT visible.
 */
export async function expectEventNotVisible(page: Page, title: string) {
  await expect(page.locator('[class*="event"]', { hasText: title })).toHaveCount(0, { timeout: 5000 })
}

/**
 * Select participants in the EventForm.
 */
export async function selectParticipants(page: Page, names: string[]) {
  const participantSelect = page.locator('.el-dialog .el-form-item').filter({ hasText: '参与人' }).locator('.el-select').first()
  await participantSelect.click()
  await page.waitForTimeout(300)
  for (const name of names) {
    const option = page.locator('.el-select-dropdown__item', { hasText: name }).first()
    if (await option.isVisible()) {
      await option.click()
      await page.waitForTimeout(200)
    }
  }
  // Close dropdown by clicking elsewhere
  await page.locator('.el-dialog__title').first().click()
  await page.waitForTimeout(300)
}

/**
 * Perform a search on the search page.
 */
export async function performSearch(page: Page, keyword: string) {
  const searchInput = page.locator('input[placeholder*="标题"], input[placeholder*="地点"], input[placeholder*="描述"]').first()
  await searchInput.fill(keyword)
  
  const searchBtn = page.locator('button', { hasText: /查询|搜索/ }).first()
  await searchBtn.click()
  await page.waitForTimeout(1000)
}

/**
 * Check that availability panel is visible.
 */
export async function expectAvailabilityPanelVisible(page: Page) {
  const panel = page.locator('.availability-pane')
  await expect(panel).toBeVisible({ timeout: 5000 })
}

/**
 * Click export button.
 */
export async function clickExport(page: Page) {
  const exportBtn = page.locator('button', { hasText: /导出/ }).first()
  if (await exportBtn.isVisible()) {
    await exportBtn.click()
    await page.waitForTimeout(1000)
  }
}

/**
 * Toggle calendar visibility in sidebar.
 */
export async function toggleCalendarVisibility(page: Page, calendarName: string) {
  const calendarLabel = page.locator('.calendar-check', { hasText: calendarName }).first()
  await calendarLabel.click()
  await page.waitForTimeout(500)
}
