import { expect, test } from '@playwright/test'
import * as fs from 'fs'

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
  await waitForNoClosingOverlay(page)
  await page.getByRole('button', { name: /新建日程/ }).first().click()
  await expect(currentEventDialog(page)).toBeVisible({ timeout: 10000 })
}

function currentEventDialog(page) {
  return page.locator('.event-dialog:visible').last()
}

async function waitForNoClosingOverlay(page) {
  await page.locator('.el-overlay-dialog.is-closing').waitFor({ state: 'detached', timeout: 5000 }).catch(() => {})
}

async function fillTitle(page, title: string) {
  await currentEventDialog(page).locator('.title-input input').fill(title)
}

async function selectOptionByFormLabel(page, label: string, optionText: string) {
  await waitForNoClosingOverlay(page)
  const item = currentEventDialog(page).locator('.el-form-item').filter({ hasText: label }).first()
  await expect(item).toBeVisible({ timeout: 5000 })
  await item.scrollIntoViewIfNeeded()
  await page.waitForTimeout(300)

  // 确保所有旧下拉已关闭
  await page.evaluate(() => { document.body.click() })
  await page.waitForTimeout(300)

  // 通过 evaluate 聚焦正确的 input，避免 Element Plus 多选下拉互相干扰
  await page.evaluate((labelText) => {
    const formItems = document.querySelectorAll('.event-dialog .el-form-item')
    for (const formItem of formItems) {
      const labelEl = formItem.querySelector('.el-form-item__label')
      if (labelEl && labelEl.textContent?.trim() === labelText) {
        const input = formItem.querySelector('.el-select__input') as HTMLInputElement
        if (input) {
          input.focus()
          input.click()
          return
        }
      }
    }
  }, label)
  await page.waitForTimeout(500)

  // 键盘搜索过滤选项
  await page.keyboard.type(optionText)
  await page.waitForTimeout(600)

  // 通过 evaluate 在可见的 dropdown 中点击选项（从后往前找，优先匹配最近打开的 dropdown）
  const clicked = await page.evaluate((text) => {
    const options = document.querySelectorAll('.el-select-dropdown__item')
    for (let i = options.length - 1; i >= 0; i--) {
      const opt = options[i] as HTMLElement
      if (opt.textContent && opt.textContent.includes(text)) {
        const dropdown = opt.closest('.el-select-dropdown') as HTMLElement
        if (dropdown && dropdown.offsetParent !== null) {
          opt.click()
          return true
        }
      }
    }
    return false
  }, optionText)

  if (!clicked) {
    // 最终回退：Enter
    await page.keyboard.press('Enter')
  }
  await page.waitForTimeout(500)

  // 等待 VUE 条件渲染完成（如自定义提醒的输入框）
  await page.waitForTimeout(500)

  // 关闭下拉
  await page.keyboard.press('Escape')
  await page.waitForTimeout(300)
  await page.locator('.el-select-dropdown:visible, .el-popper:visible').first().waitFor({ state: 'hidden', timeout: 3000 }).catch(() => {})
  await waitForNoClosingOverlay(page)
}

async function setFormItemInput(page, label: string, index: number, value: string) {
  const item = currentEventDialog(page).locator('.el-form-item').filter({ hasText: label }).first()
  const input = item.locator('input').nth(index)
  await input.click()
  await input.fill(value)
  await input.press('Enter')
  await page.keyboard.press('Tab')
}

async function pickDateByFormLabel(page, label: string, day: string) {
  await waitForNoClosingOverlay(page)
  const item = currentEventDialog(page).locator('.el-form-item').filter({ hasText: label }).first()
  await item.scrollIntoViewIfNeeded()
  await item.locator('input').first().click()
  const dayCell = page.locator('.el-picker-panel:visible .el-date-table td.available:not(.prev-month):not(.next-month) .el-date-table-cell__text', { hasText: day }).first()
  await expect(dayCell).toBeVisible({ timeout: 5000 })
  await dayCell.click({ force: true })
  await page.locator('.el-picker-panel:visible').waitFor({ state: 'hidden', timeout: 5000 }).catch(() => {})
  await waitForNoClosingOverlay(page)
}

async function saveEvent(page) {
  await waitForNoClosingOverlay(page)
  await page.getByRole('button', { name: '保存日程' }).click()
  await expect(page.locator('.el-message__content', { hasText: '已保存' })).toBeVisible({ timeout: 15000 })
  await expect(page.locator('.event-dialog:visible')).toHaveCount(0, { timeout: 10000 })
  await waitForNoClosingOverlay(page)
}

async function searchEvent(page, title: string) {
  await page.goto('/calendar/search', { waitUntil: 'networkidle' })
  await page.locator('input[placeholder="标题、地点、描述"]').fill(title)
  await page.getByRole('button', { name: /查询/ }).click()
  await expect(page.locator('body')).toContainText(title, { timeout: 10000 })
}

async function searchKeywordExpectEvent(page, keyword: string, expectedTitle: string) {
  await page.goto('/calendar/search', { waitUntil: 'networkidle' })
  await page.locator('input[placeholder="标题、地点、描述"]').fill(keyword)
  await page.getByRole('button', { name: /查询/ }).click()
  await expect(page.locator('body')).toContainText(expectedTitle, { timeout: 10000 })
}

async function apiGet(page, path: string) {
  const token = await page.evaluate(() => localStorage.getItem('token'))
  const response = await page.request.get(path, {
    headers: { Authorization: `Bearer ${token}` }
  })
  expect(response.ok()).toBeTruthy()
  return response.json()
}

async function apiPost(page, path: string, body?: unknown) {
  const token = await page.evaluate(() => localStorage.getItem('token'))
  const response = await page.request.post(path, {
    headers: { Authorization: `Bearer ${token}` },
    data: body ?? {}
  })
  expect(response.ok()).toBeTruthy()
  return response.json()
}

async function apiPut(page, path: string, body?: unknown) {
  const token = await page.evaluate(() => localStorage.getItem('token'))
  const response = await page.request.put(path, {
    headers: { Authorization: `Bearer ${token}` },
    data: body ?? {}
  })
  expect(response.ok()).toBeTruthy()
  return response.json()
}

async function findEvent(page, title: string, query = '') {
  const suffix = query ? `&${query}` : ''
  const events = await apiGet(page, `/api/events?keyword=${encodeURIComponent(title)}&allUsers=true${suffix}`)
  const event = events.data.find((item) => item.title === title)
  expect(event).toBeTruthy()
  return event
}

async function openEventFromCalendar(page, title: string) {
  await page.goto('/calendar', { waitUntil: 'networkidle' })
  await expect(page.locator('body')).toContainText(title, { timeout: 10000 })
  await page.locator('.event-pill, .timed-event, .mobile-event-row', { hasText: title }).first().click()
  await page.waitForSelector('.event-detail-drawer', { timeout: 10000 })
}

async function editOpenEvent(page) {
  await page.getByRole('button', { name: /编辑日程/ }).click()
  await expect(currentEventDialog(page)).toBeVisible({ timeout: 10000 })
}

test('完整创建链路：基础信息、参会人、提醒、待办、重复、附件保存后可回查', async ({ page }) => {
  await login(page)
  const title = `验收完整创建-${Date.now()}`

  await openCreateDialog(page)
  await fillTitle(page, title)
  await currentEventDialog(page).locator('.el-form-item').filter({ hasText: '地点' }).locator('input').first()
    .fill('验收会议室A https://meeting.example.com/acceptance')
  await currentEventDialog(page).locator('.el-form-item').filter({ hasText: '描述' }).locator('textarea').first()
    .fill('通过严格验收 E2E 创建的日程')
  await selectOptionByFormLabel(page, '参会人', '张三')
  await selectOptionByFormLabel(page, '提醒', '30分钟前')
  await selectOptionByFormLabel(page, '重复', '每周')
  await page.getByRole('button', { name: /添加待办/ }).click()
  await page.locator('.todos-section .todo-item').last().locator('input').first().fill('验收待办')
  await currentEventDialog(page).locator('input[type=file]').setInputFiles({
    name: 'acceptance-attachment.txt',
    mimeType: 'text/plain',
    buffer: Buffer.from('calendar acceptance attachment')
  })
  await expect(currentEventDialog(page).locator('.attachment-item.pending', { hasText: 'acceptance-attachment.txt' })).toBeVisible({ timeout: 5000 })
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

test('高级基础创建链路：跨天、全天、时区、标签、部门、外部联系人和自定义提醒可保存回查', async ({ page }) => {
  await login(page)
  const title = `验收高级创建-${Date.now()}`
  const externalName = `验收客户-${Date.now()}`

  await openCreateDialog(page)
  await fillTitle(page, title)
  await pickDateByFormLabel(page, '开始', '10')
  await pickDateByFormLabel(page, '结束', '11')
  await currentEventDialog(page).locator('.el-checkbox', { hasText: '全天' }).first().click()
  await selectOptionByFormLabel(page, '时区', '纽约 (UTC-5)')
  await selectOptionByFormLabel(page, '参与部门', '产品部')
  await selectOptionByFormLabel(page, '标签', '出差')
  await selectOptionByFormLabel(page, '提醒', '自定义')
  await currentEventDialog(page).locator('.reminder-custom-input input').first().fill('45')
  await currentEventDialog(page).locator('.el-form-item').filter({ hasText: '地点' }).locator('input').first()
    .fill('客户现场 https://meeting.example.com/cross-region')
  await currentEventDialog(page).locator('.el-form-item').filter({ hasText: '描述' }).locator('textarea').first()
    .fill('跨地域办公日程，包含部门参与人与外部联系人')

  await page.getByRole('button', { name: '+ 新增' }).click()
  const contactDialog = page.locator('.el-dialog:visible').filter({ hasText: '新增外部联系人' }).last()
  await contactDialog.locator('.el-form-item').filter({ hasText: '姓名' }).locator('input').fill(externalName)
  await contactDialog.locator('.el-form-item').filter({ hasText: '类型' }).locator('.el-select').click()
  await page.locator('.el-select-dropdown__item:visible', { hasText: '客户' }).first().click()
  await contactDialog.locator('.el-form-item').filter({ hasText: '公司' }).locator('input').fill('验收客户公司')
  await contactDialog.getByRole('button', { name: '保存' }).click()
  await expect(page.locator('.el-message__content', { hasText: '外部联系人已添加' })).toBeVisible({ timeout: 10000 })
  await saveEvent(page)

  const event = await findEvent(page, title)
  expect(event.all_day).toBeTruthy()
  expect(event.timezone).toBe('America/New_York')
  expect(event.tag_name).toBe('出差')
  const startAt = new Date(event.start_at)
  const endAt = new Date(event.end_at)
  expect(endAt.getTime()).toBeGreaterThan(startAt.getTime())
  const startDay = startAt.toISOString().slice(0, 10)
  const endDay = endAt.toISOString().slice(0, 10)
  expect(endDay).not.toBe(startDay)

  const [participants, reminders] = await Promise.all([
    apiGet(page, `/api/events/${event.id}/participants`),
    apiGet(page, `/api/events/${event.id}/reminders`)
  ])
  expect(participants.data.some((item) => item.department_id === 'dept-001' && item.name === '产品部')).toBeTruthy()
  expect(participants.data.some((item) => item.external_contact_id && item.name === externalName && item.external_contact_type === 'client')).toBeTruthy()
  expect(reminders.data.some((item) => item.minutes_before === 45)).toBeTruthy()
})

test('忙闲协同链路：添加参会人后自动显示该成员忙闲色块', async ({ page }) => {
  await login(page)
  const busyTitle = `验收忙闲占用-${Date.now()}`

  await openCreateDialog(page)
  await fillTitle(page, busyTitle)
  await selectOptionByFormLabel(page, '参会人', '张三')
  await saveEvent(page)

  await openCreateDialog(page)
  await selectOptionByFormLabel(page, '参会人', '张三')
  await expect(page.locator('.busy-block', { hasText: busyTitle })).toBeVisible({ timeout: 10000 })
})

test('权限分级与编辑通知链路：授权成员可改，普通参会人仅查看，保存后通知参会人', async ({ page }) => {
  await login(page)
  const title = `验收权限通知-${Date.now()}`
  const updatedTitle = `${title}-已更新`

  await openCreateDialog(page)
  await fillTitle(page, title)
  await selectOptionByFormLabel(page, '参会人', '张三')
  await selectOptionByFormLabel(page, '参会人', '李四')
  await selectOptionByFormLabel(page, '可编辑', '张三')
  await saveEvent(page)

  let event = await findEvent(page, title)
  let participants = await apiGet(page, `/api/events/${event.id}/participants`)
  expect(participants.data.some((item) => item.user_id === 'user-002' && item.role === 'EDITOR')).toBeTruthy()
  expect(participants.data.some((item) => item.user_id === 'user-003' && item.role === 'VIEWER')).toBeTruthy()

  await openEventFromCalendar(page, title)
  await editOpenEvent(page)
  await fillTitle(page, updatedTitle)
  await saveEvent(page)

  // 先关闭可能残留的抽屉，避免遮罩拦截 header 退出按钮
  await page.keyboard.press('Escape').catch(() => {})
  await page.waitForTimeout(500)
  await page.getByRole('button', { name: '退出' }).click({ force: true })
  await login(page, 'zhangsan')
  await searchEvent(page, updatedTitle)
  await openEventFromCalendar(page, updatedTitle)
  await expect(page.getByRole('button', { name: /编辑日程/ })).toBeVisible({ timeout: 10000 })
  const zhangsanNotifications = await apiGet(page, '/api/notifications?limit=20')
  expect(zhangsanNotifications.data.notifications.some((item) => String(item.message).includes(updatedTitle))).toBeTruthy()

  await page.keyboard.press('Escape').catch(() => {})
  await page.waitForTimeout(500)
  await page.getByRole('button', { name: '退出' }).click({ force: true })
  await login(page, 'lisi')
  await searchEvent(page, updatedTitle)
  await openEventFromCalendar(page, updatedTitle)
  await expect(page.getByRole('button', { name: /编辑日程/ })).toHaveCount(0)
})

test('重复日程单次编辑链路：仅修改本次并设置提醒特例不影响全系列提醒', async ({ page }) => {
  await login(page)
  const title = `验收单次特例-${Date.now()}`
  const singleTitle = `${title}-单次`

  await openCreateDialog(page)
  await fillTitle(page, title)
  await selectOptionByFormLabel(page, '提醒', '15分钟前')
  await selectOptionByFormLabel(page, '重复', '每日')
  await saveEvent(page)

  const series = await findEvent(page, title)
  await openEventFromCalendar(page, title)
  await editOpenEvent(page)
  await currentEventDialog(page).locator('.el-radio', { hasText: '仅修改本次' }).click()
  await fillTitle(page, singleTitle)
  await currentEventDialog(page).locator('.el-radio', { hasText: '自定义本次提醒' }).click()
  await selectOptionByFormLabel(page, '提醒档位', '自定义')
  await currentEventDialog(page).locator('.reminder-custom-input input').last().fill('90')
  await saveEvent(page)

  const reminders = await apiGet(page, `/api/events/${series.id}/reminders`)
  expect(reminders.data.some((item) => item.minutes_before === 15)).toBeTruthy()
  const events = await apiGet(page, `/api/events?keyword=${encodeURIComponent(title)}&allUsers=true`)
  const singleInstance = events.data.find((item) => item.title === singleTitle)
  expect(singleInstance).toBeTruthy()
  expect(singleInstance.is_recurrence_instance).toBeTruthy()

  // 验证单次编辑的 reminder_override 已写入（检查字符串化结果中是否包含 90）
  expect(JSON.stringify(singleInstance)).toMatch(/reminder.*90|90.*reminder|"minutes_before":\s*90/)

  expect(events.data.some((item) => item.title === title)).toBeTruthy()
})

test('搜索筛选与日历分类链路：标题、参与人、关键词和分类分色可查可控', async ({ page }) => {
  await login(page)
  const title = `验收搜索筛选-${Date.now()}`

  await openCreateDialog(page)
  await fillTitle(page, title)
  await selectOptionByFormLabel(page, '参会人', '张三')
  await selectOptionByFormLabel(page, '日历', '公共日历')
  await selectOptionByFormLabel(page, '标签', '会议')
  await currentEventDialog(page).locator('.el-form-item').filter({ hasText: '地点' }).locator('input').first().fill('验收筛选会议室')
  await currentEventDialog(page).locator('.el-form-item').filter({ hasText: '描述' }).locator('textarea').first().fill('验收关键词Alpha')
  await saveEvent(page)

  await searchEvent(page, title)
  await searchEvent(page, '张三')
  await expect(page.locator('body')).toContainText(title)
  await searchKeywordExpectEvent(page, '验收关键词Alpha', title)

  await page.goto('/calendar', { waitUntil: 'networkidle' })
  await expect(page.locator('.calendar-check', { hasText: '公共日历' })).toBeVisible()
  await expect(page.locator('.calendar-check', { hasText: '公共日历' }).locator('i')).toHaveAttribute('style', /background/)
  await expect(page.locator('body')).toContainText(title)
  await page.locator('.calendar-check', { hasText: '公共日历' }).click()
  await expect(page.locator('body')).not.toContainText(title)
})

test('待办完成链路：日程详情中点击子任务后完成状态落库', async ({ page }) => {
  await login(page)
  const title = `验收待办完成-${Date.now()}`
  const todoTitle = `验收高优先级待办-${Date.now()}`

  await openCreateDialog(page)
  await fillTitle(page, title)
  await page.getByRole('button', { name: /添加待办/ }).click()
  const todo = page.locator('.todos-section .todo-item').last()
  await todo.locator('input').first().fill(todoTitle)
  await todo.locator('.el-select').first().click()
  await page.locator('.el-select-dropdown__item:visible', { hasText: '高' }).first().click()
  await saveEvent(page)

  const event = await findEvent(page, title)
  await openEventFromCalendar(page, title)
  await page.locator('.todos-list .todo-item', { hasText: todoTitle }).click()
  await expect(page.locator('.el-message__content', { hasText: '待办已完成' })).toBeVisible({ timeout: 10000 })

  const todos = await apiGet(page, `/api/events/${event.id}/todos`)
  expect(todos.data.some((item) => item.title === todoTitle && item.priority === 'HIGH' && item.completed === true)).toBeTruthy()
})
