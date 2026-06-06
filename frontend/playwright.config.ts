import { defineConfig, devices } from '@playwright/test'

const baseURL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:3007'

export default defineConfig({
  testDir: './tests/e2e',
  timeout: 60000,
  expect: { timeout: 15000 },
  retries: 0,
  reporter: [
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
    ['list'],
    ['json', { outputFile: 'playwright-report/test-results.json' }]
  ],
  use: {
    baseURL,
    trace: 'on',
    screenshot: 'on',
    video: 'retain-on-failure'
  },
  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        viewport: { width: 1440, height: 900 }
      }
    }
  ],
  webServer: process.env.PLAYWRIGHT_BASE_URL ? undefined : {
    command: 'npm run dev',
    port: 3007,
    reuseExistingServer: true,
    timeout: 15000
  }
})
