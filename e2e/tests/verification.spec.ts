import { expect, test } from '@playwright/test';
import { fileURLToPath } from 'node:url';
import { asSomeoneElse, navigate, signUp } from '../fixtures/app';

const specimen = fileURLToPath(new URL('../fixtures/specimen-id.pdf', import.meta.url));

test('a verified customer gets higher limits', async ({ page, browser }) => {
  const customer = await signUp(page, 'Vera Verified');
  await navigate(page, 'Identity & limits');
  await expect(page.getByRole('region', { name: 'Your limits' })).toContainText('€1,000.00');

  await page.getByLabel('Identity document').setInputFiles(specimen);
  await page.getByRole('button', { name: 'Send for review' }).click();
  await expect(page.getByText('In review')).toBeVisible();

  // The document went to S3 storage; support opens the queue and approves.
  const support = await asSomeoneElse(browser, 'support@ledgerline.dev');
  await navigate(support, 'Verifications');
  await support.getByRole('button', { name: `Approve ${customer.name}` }).click();
  await expect(support.getByText(`${customer.name} is verified.`)).toBeVisible();

  await page.reload();
  await expect(page.getByText('Verified', { exact: true })).toBeVisible();
  await expect(page.getByRole('region', { name: 'Your limits' })).toContainText('€10,000.00');
});

test('notifications arrive through Kafka', async ({ page }) => {
  await signUp(page, 'Nora Notified');

  // The welcome notification is written by a Kafka consumer a moment after sign-up.
  await expect(async () => {
    await page.reload();
    await page.getByRole('button', { name: /Notifications, \d+ unread/ }).first().click();
  }).toPass({ timeout: 20_000 });
  await expect(page.getByRole('dialog', { name: 'Notifications' })).toContainText(
    'Welcome to Ledgerline',
  );
});
