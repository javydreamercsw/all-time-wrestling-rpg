const assert = require('node:assert/strict');
const { test } = require('node:test');

const { execFileSync } = require('node:child_process');
const path = require('node:path');

const SCRIPT = path.join(__dirname, 'discard-npm-downgrade.cjs');

function decide(headJson, worktreeJson) {
  return execFileSync('node', [SCRIPT], {
    input: JSON.stringify({ head: headJson, worktree: worktreeJson }),
    encoding: 'utf8',
  }).trim();
}

function pkgWith(vite) {
  return {
    name: 'all-time-wrestling-rpg',
    dependencies: { react: '19.2.0' },
    devDependencies: { vite },
  };
}

test('discards a pure vite downgrade vs HEAD', () => {
  // Flow's TaskUpdatePackages "repairs" package.json toward the bundled default
  // (e.g. 8.3.1 in flow-server 25.3.1), reverting a deliberate dependabot bump.
  const verdict = JSON.parse(decide(pkgWith('8.3.5'), pkgWith('8.3.1')));
  assert.equal(verdict.discard, true);
  assert.deepEqual(verdict.files, ['package.json', 'package-lock.json']);
});

test('keeps deliberate non-vite edits even when vite also changed', () => {
  // A real dependency addition rides along — discarding would lose work.
  const head = pkgWith('8.3.5');
  const worktree = pkgWith('8.3.1');
  worktree.dependencies.chalk = '5.4.1';
  const verdict = JSON.parse(decide(head, worktree));
  assert.equal(verdict.discard, false);
});

test('keeps a vite upgrade vs HEAD', () => {
  const verdict = JSON.parse(decide(pkgWith('8.3.4'), pkgWith('8.3.5')));
  assert.equal(verdict.discard, false);
});

test('keeps the tree when vite is unchanged', () => {
  const verdict = JSON.parse(decide(pkgWith('8.3.4'), pkgWith('8.3.4')));
  assert.equal(verdict.discard, false);
});

test('ignores overrides-section-only vite churn', () => {
  // The overrides entry is a "$vite" reference in this repo; if Flow writes a
  // literal version there while the dependency version is untouched, treat the
  // dependency version as authoritative and discard the churn.
  const head = pkgWith('8.3.4');
  head.overrides = { vite: '$vite' };
  const worktree = pkgWith('8.3.4');
  worktree.overrides = { vite: '8.3.1' };
  const verdict = JSON.parse(decide(head, worktree));
  assert.equal(verdict.discard, true);
});
