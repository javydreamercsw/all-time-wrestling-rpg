/******************************************************************************
 * Custom app entry point (copied from the generated one — keep in sync when Vaadin updates it).
 * If you want to customize the entry point, you can copy this file or create
 * your own `index.tsx` in your frontend directory.
 * By default, the `index.tsx` file should be in `./frontend/` folder.
 *
 * NOTE:
 *     - You need to restart the dev-server after adding the new `index.tsx` file.
 *       After that, all modifications to `index.tsx` are recompiled automatically.
 *     - `index.js` is also supported if you don't want to use TypeScript.
 ******************************************************************************/

// Eagerly load the confirm-dialog web component. Production builds keep it out of every
// route chunk, and the lazy DYNAMIC_IMPORT fallback can silently no-op (server/client
// import-hash mismatch), leaving ConfirmDialog-based actions — e.g. the tournament list's
// Delete — permanently inert while the connection indicator reports "Online".
import '@vaadin/confirm-dialog/src/vaadin-confirm-dialog.js';

import { createElement } from 'react';
import { createRoot } from 'react-dom/client';
import { RouterProvider } from 'react-router';
import { router } from 'Frontend/generated/routes.js';

function App() {
  return <RouterProvider router={router} />;
}

const outlet = document.getElementById('outlet')!;
let root = (outlet as any)._root ?? createRoot(outlet);
(outlet as any)._root = root;
root.render(createElement(App));
