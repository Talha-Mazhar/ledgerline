import { defineConfig, devices } from '@playwright/test';

/**
 * End-to-end tests against the whole app running in Docker Compose (`docker compose up --wait`):
 * the real frontend, backend, PostgreSQL, Kafka, Redis and MinIO. Nothing is mocked.
 *
 * Each test signs up its own customer, so tests don't depend on each other's data and can run in
 * parallel against one database.
 */
export default defineConfig({
  testDir: './tests',
  timeout: 60_000,
  expect: { timeout: 10_000 },
  fullyParallel: true,
  forbidOnly: Boolean(process.env['CI']),
  retries: process.env['CI'] ? 1 : 0,
  reporter: process.env['CI'] ? [['github'], ['html', { open: 'never' }]] : [['list'], ['html']],
  use: {
    baseURL: process.env['BASE_URL'] ?? 'http://localhost:8000',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});
