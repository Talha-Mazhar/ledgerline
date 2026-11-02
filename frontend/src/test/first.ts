/** The first of several matching elements, e.g. a link that is both in the sidebar and the page. */
export function first(elements: readonly HTMLElement[]): HTMLElement {
  const [element] = elements;
  if (element === undefined) {
    throw new Error('No element matched');
  }
  return element;
}
