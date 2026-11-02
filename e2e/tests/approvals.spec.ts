import { expect, test } from '@playwright/test';
import { addMoney, asSomeoneElse, navigate, openWallet, sendMoney, signUp } from '../fixtures/app';

test('a transfer above the limit waits for two different members of staff', async ({
  page,
  browser,
}) => {
  const customer = await signUp(page, 'Hana Highroller');
  await openWallet(page, 'EUR');
  await addMoney(page, 'EUR', '2000');

  // Above a basic customer's single-payment limit of €1,000: held, not sent.
  await sendMoney(page, 'EUR', 'jana@ledgerline.dev', '1500');
  await expect(page.getByText(/€1,500.00 to Jana Nováková is waiting for approval/)).toBeVisible();
  const ledger = page.getByRole('table', { name: 'Activity' });
  await expect(ledger.getByText('Sent to Jana Nováková, awaiting approval')).toBeVisible();

  // Support gives the first approval.
  const support = await asSomeoneElse(browser, 'support@ledgerline.dev');
  const approve = support.getByRole('button', { name: `Approve €1,500.00 from ${customer.name}` });
  await approve.click();
  await expect(support.getByText(/First approval given/)).toBeVisible();

  // The same person can't approve twice.
  await approve.click();
  await expect(support.getByText(/A second, different person must approve it/)).toBeVisible();

  // An admin gives the second; the money moves.
  const admin = await asSomeoneElse(browser, 'admin@ledgerline.dev');
  await admin
    .getByRole('button', { name: `Approve €1,500.00 from ${customer.name}` })
    .click();
  await expect(admin.getByText(/Second approval given/)).toBeVisible();

  await navigate(page, 'Payments');
  const payments = page.getByRole('table', { name: 'Payments' });
  await expect(payments.getByRole('row', { name: /Sent to Jana Nováková/ })).toContainText('Completed');

  // The audit log has both approvals.
  await navigate(admin, 'Audit log');
  await expect(admin.getByRole('table', { name: 'Audit log' })).toContainText('support@ledgerline.dev');
});

test('a rejected transfer gives the money back with the reason', async ({ page, browser }) => {
  const customer = await signUp(page, 'Rudi Rejected');
  await openWallet(page, 'EUR');
  await addMoney(page, 'EUR', '1800');
  await sendMoney(page, 'EUR', 'jana@ledgerline.dev', '1200');

  const support = await asSomeoneElse(browser, 'support@ledgerline.dev');
  await support.getByRole('button', { name: `Reject €1,200.00 from ${customer.name}` }).click();
  const dialog = support.getByRole('dialog', { name: 'Reject payment' });
  await dialog.getByLabel('Reason').fill('Unusual recipient for this account');
  await dialog.getByRole('button', { name: 'Reject' }).click();
  await expect(support.getByText(/Rejected; €1,200.00 is back/)).toBeVisible();

  await page.reload();
  await expect(page.getByRole('table', { name: 'Activity' }).getByRole('row').nth(1)).toContainText(
    '€1,800.00',
  );
  await navigate(page, 'Payments');
  await expect(page.getByText(/Unusual recipient for this account/)).toBeVisible();
});
