#!/usr/bin/env node
/**
 * Decide whether worktree package.json churn is a Flow-driven vite downgrade
 * that should be discarded (beads ATW-qzaj).
 *
 * flow-server bundles a frontend/dependencies/vite/package.json pinning its own
 * vite version. When the pre-push hook runs mvn rewrite:run, Flow's
 * TaskUpdatePackages "repairs" package.json toward that bundled default,
 * reverting any newer deliberate bump (e.g. a dependabot vite upgrade). The
 * hook's auto-commit step then commits the downgrade as a style fix.
 *
 * Reads { head, worktree } JSON on stdin (parsed package.json contents) and
 * prints a verdict: { discard: boolean, files: string[] }. discard=true means
 * the only difference between the two trees is vite (dependency version or its
 * overrides echo) NOT moving forward vs HEAD — safe to `git checkout --` both
 * npm files. Any other difference (a deliberate dependency bump, a script
 * edit, a new override) means real work rode along: keep.
 */

const head = JSON.parse(require('node:fs').readFileSync(0, 'utf8'));

const viteVersionOf = (pkg) =>
  pkg && (pkg.devDependencies?.vite ?? pkg.dependencies?.vite ?? null);

const versionLessOrEqual = (a, b) => {
  // a <= b (semver-ish numeric compare)
  const pa = String(a).split('.').map((n) => parseInt(n, 10));
  const pb = String(b).split('.').map((n) => parseInt(n, 10));
  for (let i = 0; i < Math.max(pa.length, pb.length); i++) {
    const d = (pa[i] || 0) - (pb[i] || 0);
    if (d !== 0) return d < 0;
  }
  return true;
};

const headVite = viteVersionOf(head.head);
const worktreeVite = viteVersionOf(head.worktree);

const stripVite = (pkg) => {
  const clone = JSON.parse(JSON.stringify(pkg ?? {}));
  delete clone.devDependencies?.vite;
  delete clone.dependencies?.vite;
  // The overrides entry mirrors the dependency version (or "$vite"); the
  // dependency version is authoritative, so ignore the overrides echo.
  delete clone.overrides?.vite;
  return clone;
};

const noOtherDifference =
  JSON.stringify(stripVite(head.head)) === JSON.stringify(stripVite(head.worktree));

// Identical trees mean there is no churn to discard at all (the hook only
// consults this on a real diff, but keep the pure semantics honest).
const rawIdentical = JSON.stringify(head.head) === JSON.stringify(head.worktree);

const notAnUpgrade = headVite !== null && worktreeVite !== null
    ? versionLessOrEqual(worktreeVite, headVite)
    : headVite === null && worktreeVite === null;

const verdict = {
  discard: !rawIdentical && noOtherDifference && notAnUpgrade,
  files: ['package.json', 'package-lock.json'],
};

process.stdout.write(JSON.stringify(verdict));
