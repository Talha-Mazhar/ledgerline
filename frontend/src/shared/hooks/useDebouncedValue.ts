import { useEffect, useState } from 'react';

/**
 * The value, but only after it has stopped changing for `delay` milliseconds. Used to ask the
 * server for a quote once the user pauses typing, not on every keystroke.
 */
export function useDebouncedValue<T>(value: T, delay = 300): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(timer);
  }, [value, delay]);
  return debounced;
}
