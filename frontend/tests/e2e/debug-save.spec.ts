import { test } from '@playwright/test'

test('debug: API and save flow', async ({ page }) => {
  // 1. Check API health
  await page.goto('/calendar', { waitUntil: 'networkidle' })
  await page.waitForTimeout(2000)
  
  const apiHealth = await page.evaluate(async () => {
    const resp = await fetch('/api/events')
    const data = await resp.json()
    return { status: resp.status, total: (data.data || []).length }
  })
  console.log(`API Health: ${JSON.stringify(apiHealth)}`)

  // 2. Open event form
  const createBtn = page.locator('button').filter({ hasText: /新建日程|新建/ }).first()
  await createBtn.click()
  await page.waitForSelector('.event-dialog', { timeout: 8000 })
  await page.waitForTimeout(500)
  console.log('Event dialog opened')

  // 3. Fill title
  const testTitle = `DEBUG-${Date.now()}`
  const titleInput = page.locator('.title-input input').first()
  await titleInput.fill(testTitle)
  await page.waitForTimeout(300)
  console.log(`Title filled: ${testTitle}`)

  // 4. Save
  const saveBtn = page.locator('.event-dialog button').filter({ hasText: /保存日程/ }).first()
  await saveBtn.click()
  console.log('Save clicked')
  await page.waitForTimeout(3000)
  console.log('Waited 3s after save')

  // 5. Check API
  const apiResult = await page.evaluate(async (title) => {
    const resp = await fetch('/api/events')
    const data = await resp.json()
    const found = (data.data || []).find((e: any) => e.title === title)
    return { total: (data.data || []).length, found: !!found, title: found?.title }
  }, testTitle)
  console.log(`API result: ${JSON.stringify(apiResult)}`)

  // 6. Reload and check store
  await page.goto('/calendar', { waitUntil: 'networkidle' })
  await page.waitForSelector('.calendar-stage', { timeout: 10000 })
  await page.waitForTimeout(2000)

  const storeCheck = await page.evaluate(async (title) => {
    const appEl = document.querySelector('#app')
    if (appEl && (appEl as any).__vue_app__) {
      const pinia = (appEl as any).__vue_app__.config.globalProperties.$pinia
      if (pinia) {
        const events = pinia.state.value.app?.events || []
        const found = events.some((e: any) => e.title === title)
        return { total: events.length, found }
      }
    }
    return { total: 0, found: false }
  }, testTitle)
  console.log(`Store check: ${JSON.stringify(storeCheck)}`)

  await page.screenshot({ path: 'debug-save.png', fullPage: true })
  console.log('Debug complete')
})