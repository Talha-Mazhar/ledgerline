import '@fontsource-variable/inter';
import '@fontsource-variable/source-serif-4';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from '@/app/App';
import { sendCsrfToken } from '@/shared/api/csrf';

const rootElement = document.getElementById('root');
if (rootElement === null) {
  throw new Error('Element #root is missing from index.html');
}

sendCsrfToken();

createRoot(rootElement).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
