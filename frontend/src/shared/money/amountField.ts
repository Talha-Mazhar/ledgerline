import { z } from 'zod';
import { zAmountRequest } from '@/shared/api/generated/zod.gen';

/**
 * The rule for an amount a user types. The format check is the contract's own (`zAmountRequest`,
 * generated from openapi.yaml); the messages are ours, written for people.
 */
export const amountField = z
  .string()
  .trim()
  .min(1, 'Enter an amount.')
  .refine(
    (value) => zAmountRequest.shape.amount.safeParse(value).success,
    'Enter an amount like 250 or 250.50, with at most two decimals.',
  )
  .refine((value) => Number(value) > 0, 'Enter an amount greater than zero.');

/** True when the text is a valid amount, e.g. to decide whether a live quote is worth asking for. */
export function isValidAmount(value: string): boolean {
  return amountField.safeParse(value).success;
}
