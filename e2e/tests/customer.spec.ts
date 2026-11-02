import { expect, test } from '@playwright/test';
import { addMoney, navigate, openWallet, sendMoney, signUp } from '../fixtures/app';

test('a new customer adds money, sends some and sees every balance add up', async ({ page }) => {
  await signUp(page);
  await openWallet(page, 'EUR');

  await addMoney(page, 'EUR', '200');
  await expect(page.getByText('€200.00 added')).toBeVisible();

  await sendMoney(page, 'EUR', 'jana@ledgerline.dev', '50');
  await expect(page.getByText('€50.00 sent to Jana Nováková')).toBeVisible();

  // The ledger shows both lines with the running balance.
  const ledger = page.getByRole('table', { name: 'Activity' });
  await expect(ledger.getByText('Sent to Jana Nováková')).toBeVisible();
  await expect(ledger.getByRole('row').nth(1)).toContainText('€150.00');

  await navigate(page, 'Payments');
  const payments = page.getByRole('table', { name: 'Payments' });
  await expect(payments.getByRole('row', { name: /Sent to Jana Nováková/ })).toContainText('Completed');
});

test('exchanging between currencies prices the quote and moves both wallets', async ({ page }) => {
  await signUp(page);
  await openWallet(page, 'EUR');
  await addMoney(page, 'EUR', '100');
  await navigate(page, 'Overview');
  await openWallet(page, 'CZK');
  await navigate(page, 'Overview');
  await page.getByRole('link', { name: 'View Euro wallet' }).click();

  await page.getByRole('button', { name: 'Exchange' }).click();
  const dialog = page.getByRole('dialog', { name: 'Exchange' });
  await dialog.getByLabel('You sell (EUR)').fill('40');
  await expect(dialog.getByText(/You get/)).toBeVisible();
  await dialog.getByRole('button', { name: 'Exchange' }).click();
  await expect(dialog).toBeHidden();

  const ledger = page.getByRole('table', { name: 'Activity' });
  await expect(ledger.getByText('Exchanged to CZK')).toBeVisible();
});

test('the activity is the same after signing out and in again', async ({ page }) => {
  const customer = await signUp(page);
  await openWallet(page, 'GBP');
  await addMoney(page, 'GBP', '75.50');

  await page.getByRole('button', { name: 'Sign out' }).first().click();
  await page.getByLabel('Email').fill(customer.email);
  await page.getByLabel('Password').fill(customer.password);
  await page.getByRole('button', { name: 'Sign in', exact: true }).click();

  await expect(
    page.getByRole('article', { name: 'British pound (GBP)' }).getByText('£75.50'),
  ).toBeVisible();
});
