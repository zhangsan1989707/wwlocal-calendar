import { Page, expect } from '@playwright/test'

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
 * Click "保存日程" / "确定" in the dialog.
 */
export async function saveEvent(page: Page) {
  const saveBtn = page.locator('.el-dialog button', { hasText: /保存日程|确定/ }).last()
  await saveBtn.click()
  // Wait for success message or error
  await page.waitForSelector('.el-message', { timeout: 10000 })
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
  // Wait for the calendar grid to render
  await page.waitForSelector('.calendar-grid, .full-calendar, [class*="calendar"]', { timeout: 10000 })
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
  const btn = page.locator('button', { hasText: labelMap[view] }).first()
  await btn.click()
  await page.waitForTimeout(500)
}

/**
 * Open an event detail by clicking on an event block in the calendar.
 */
export async function openEventDetail(page: Page, title: string) {
  const eventBlock = page.locator('.event-block, [class*="event"]', { hasText: title }).first()
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
  const confirmBtn = page.locator('.el-message-box button', { hasText: /确定/ }).first()
  await confirmBtn.click()
  await page.waitForTimeout(1000)
}

/**
 * Verify an event block exists on the calendar.
 */
export async function expectEventVisible(page: Page, title: string) {
  await expect(page.locator('[class*="event"]', { hasText: title }).first()).toBeVisible({ timeout: 5000 })
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
    await option.click()
    await page.waitForTimeout(200)
  }
  // Close dropdown by clicking elsewhere
  await page.locator('.el-dialog__title').first().click()
  await page.waitForTimeout(300)
}

/**
 * Fill a date picker field.
 */
export async function fillDatePicker(page: Page, label: string, value: string) {
  const datePicker = page.locator('.el-dialog .el-form-item').filter({ hasText: label }).locator('.el-date-picker input').first()
  await datePicker.fill(value)
  await datePicker.press('Enter')
}
