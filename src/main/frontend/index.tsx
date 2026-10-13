/******************************************************************************
 * Custom app entry point (copied from the Flow-generated default — keep in sync
 * when Vaadin updates it).
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

// import Vaadin client-router to handle client-side and server-side navigation
import { Router } from '@vaadin/router';

// import Flow module to enable navigation to Vaadin server-side views
import { Flow } from 'Frontend/generated/jar-resources/Flow.js';

const { serverSideRoutes } = new Flow({
  imports: () => import('Frontend/generated/flow/generated-flow-imports.js'),
});

const routes = [
  // for client-side, place routes below (more info https://hilla.dev/docs/lit/guides/routing#initializing-the-router)

  // for server-side, the next magic line sends all unmatched routes:
  ...serverSideRoutes, // IMPORTANT: this must be the last entry in the array
];

// Vaadin router needs an outlet in the index.html page to display views
const router = new Router(document.querySelector('#outlet'));
router.setRoutes(routes);
