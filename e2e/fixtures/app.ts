import { expect, type Browser, type Page } from '@playwright/test';
import { randomUUID } from 'node:crypto';

export const DEMO_PASSWORD = 'ledgerline-demo';

export interface Customer {
  readonly name: string;
  readonly email: string;
  readonly password: string;
}

/**
 * Signs up a brand-new customer, so each test starts from an empty account of its own. The name
 * gets a random suffix: the database outlives a test run, and staff pick customers by name.
 */
export async function signUp(page: Page, name = 'Erika Tester'): Promise<Customer> {
  const id = randomUUID();
  const customer = {
    name: `${name} ${id.slice(0, 6)}`,
    email: `e2e-${id}@example.com`,
    password: 'correct-horse-battery',
  };
  await page.goto('/sign-up');
  await page.getByLabel('Your name').fill(customer.name);
  await page.getByLabel('Email').fill(customer.email);
  await page.getByLabel('Password').fill(customer.password);
  await page.getByRole('button', { name: 'Create account' }).click();
  await expect(page.getByRole('heading', { level: 1, name: 'Overview' })).toBeVisible();
  return customer;
}

export async function signIn(page: Page, email: string, password = DEMO_PASSWORD): Promise<void> {
  await page.goto('/sign-in');
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Password').fill(password);
  await page.getByRole('button', { name: 'Sign in', exact: true }).click();
  await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
}

/** A page in a fresh browser context: another person on another computer. */
export async function asSomeoneElse(browser: Browser, email: string): Promise<Page> {
  const context = await browser.newContext();
  const page = await context.newPage();
  await signIn(page, email);
  return page;
}

/** Opens a wallet from the overview and goes to its page. */
export async function openWallet(page: Page, currency: 'EUR' | 'USD' | 'CZK' | 'GBP') {
  await page.getByRole('button', { name: `Open ${currency} wallet` }).click();
  const dialog = page.getByRole('dialog', { name: 'Open a wallet' });
  await dialog.getByRole('button', { name: 'Open wallet' }).click();
  await expect(dialog).toBeHidden();
  const names = { EUR: 'Euro', USD: 'US dollar', CZK: 'Czech koruna', GBP: 'British pound' };
  await page.getByRole('link', { name: `View ${names[currency]} wallet` }).click();
  await expect(page.getByRole('heading', { level: 1, name: `${names[currency]} wallet` })).toBeVisible();
}

export async function addMoney(page: Page, currency: string, amount: string) {
  await page.getByRole('button', { name: 'Add money' }).click();
  const dialog = page.getByRole('dialog', { name: 'Add money' });
  await dialog.getByLabel(`Amount (${currency})`).fill(amount);
  await dialog.getByRole('button', { name: 'Add money' }).click();
  await expect(dialog).toBeHidden();
}

export async function sendMoney(page: Page, currency: string, to: string, amount: string) {
  await page.getByRole('button', { name: 'Send' }).click();
  const dialog = page.getByRole('dialog', { name: 'Send money' });
  await dialog.getByLabel("Recipient's email").fill(to);
  await dialog.getByLabel(`Amount (${currency})`).fill(amount);
  await dialog.getByRole('button', { name: 'Send' }).click();
  await expect(dialog).toBeHidden();
}

/** The sidebar link; on a phone-sized window the same link is in the menu. */
export async function navigate(page: Page, label: string) {
  await page.getByRole('navigation', { name: 'Main navigation' }).getByRole('link', { name: label }).click();
}
