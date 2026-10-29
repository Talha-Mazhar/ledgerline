/**
 * Every route path in one place. Links and routes use these constants, so a typo is a type error
 * and renaming a path is a one-line change.
 */
export const paths = {
  overview: '/',
} as const;

export type AppPath = (typeof paths)[keyof typeof paths];
