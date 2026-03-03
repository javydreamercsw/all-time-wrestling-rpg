#!/usr/bin/env node
/**
 * Sync `package.json` Vaadin frontend dependencies to match the Vaadin version used by Maven.
 *
 * Reality check: Vaadin's npm packages aren't always published for every Flow/Vaadin patch.
 * For this repo, many @vaadin/* npm packages are still at 25.0.3 while Maven is 25.0.4.
 *
 * Strategy:
 * - Keep a small allow-list of packages that should follow the Maven Vaadin version.
 * - Keep an explicit pin-list for packages with independent versioning or known npm lag.
 * - Do NOT blindly force every @vaadin/* dependency to the Maven version.
 */

const fs = require('node:fs');
const path = require('node:path');

const vaadinVersion = process.argv[2];
if (!vaadinVersion) {
  console.error('Missing Vaadin version. Example: node scripts/sync-vaadin-overrides.cjs 25.0.4');
  process.exit(2);
}

// Most @vaadin/* packages are at 25.0.4 or 25.0.3 even when Flow is 25.0.6
const fallbackVaadinVersion = '25.0.4';

const packageJsonPath = path.resolve(process.cwd(), 'package.json');
if (!fs.existsSync(packageJsonPath)) {
  console.error(`package.json not found at ${packageJsonPath}`);
  process.exit(2);
}

/** @type {any} */
const pkg = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));

const ensureObject = (obj, key) => {
  if (!obj[key] || typeof obj[key] !== 'object') obj[key] = {};
  return obj[key];
};

const overrides = ensureObject(pkg, 'overrides');

const pinnedNpmVersions = {
  // Independent versioning / not published for every Vaadin patch
  '@vaadin/common-frontend': '0.0.19',
  '@vaadin/vaadin-development-mode-detector': '2.0.7',
  '@vaadin/vaadin-usage-statistics': '2.1.3',

  // Npm lagging / not published for 25.0.6
  '@vaadin/aura': '25.0.4',
  '@vaadin/react-components': '25.0.4',
  '@vaadin/vaadin-lumo-styles': '25.0.4',
  '@vaadin/vaadin-themable-mixin': '25.0.4',
  '@vaadin/router': '2.0.1',
  '@vaadin/a11y-base': '25.0.4'
};

// Only these packages will be updated to the Maven Vaadin version automatically.
// Everything else is left as-is to avoid requesting unpublished versions.
const syncToMavenVersionAllowList = new Set([
  // Core packages that usually follow patch versions immediately
]);

const resolveVaadinNpmVersion = (pkgName, currentVersion) => {
  if (pinnedNpmVersions[pkgName]) return pinnedNpmVersions[pkgName];
  if (syncToMavenVersionAllowList.has(pkgName)) return vaadinVersion;
  return fallbackVaadinVersion;
};

// Keep direct Vaadin deps aligned too (prevents EOVERRIDE conflicts with overrides)
const syncVaadinVersionsInObject = (obj) => {
  if (!obj || typeof obj !== 'object') return;
  for (const k of Object.keys(obj)) {
    if (!k.startsWith('@vaadin/')) continue;
    obj[k] = resolveVaadinNpmVersion(k, obj[k]);
  }
};

syncVaadinVersionsInObject(pkg.dependencies);
if (pkg.vaadin && typeof pkg.vaadin === 'object') {
  syncVaadinVersionsInObject(pkg.vaadin.dependencies);
}

// Sync/clean @vaadin/* overrides.
for (const k of Object.keys(overrides)) {
  if (!k.startsWith('@vaadin/')) continue;
  overrides[k] = resolveVaadinNpmVersion(k, overrides[k]);
}

// Add missing allowed packages to overrides ONLY if not in dependencies
const allVaadinPackages = new Set();
const scanForVaadin = (obj) => {
  if (!obj || typeof obj !== 'object') return;
  for (const k of Object.keys(obj)) {
    if (k.startsWith('@vaadin/')) allVaadinPackages.add(k);
  }
};
scanForVaadin(pkg.dependencies);
scanForVaadin(pkg.devDependencies);
if (pkg.vaadin && typeof pkg.vaadin === 'object') {
  scanForVaadin(pkg.vaadin.dependencies);
  scanForVaadin(pkg.vaadin.devDependencies);
}

for (const k of Array.from(allVaadinPackages)) {
  if (!pkg.dependencies[k]) {
    overrides[k] = resolveVaadinNpmVersion(k, overrides[k]);
  }
}

// If there were no @vaadin/* overrides, optionally seed from dependencies/devDependencies.
// We only seed pinned overrides, never the full @vaadin/* set.
const hasVaadinOverride = Object.keys(overrides).some((k) => k.startsWith('@vaadin/'));
if (!hasVaadinOverride) {
  const candidates = new Set();
  const addKeys = (obj) => {
    if (!obj || typeof obj !== 'object') return;
    for (const k of Object.keys(obj)) {
      if (pinnedNpmVersions[k]) candidates.add(k);
    }
  };

  addKeys(pkg.dependencies);
  addKeys(pkg.devDependencies);
  if (pkg.vaadin && typeof pkg.vaadin === 'object') {
    addKeys(pkg.vaadin.dependencies);
    addKeys(pkg.vaadin.devDependencies);
  }

  for (const k of Array.from(candidates).sort()) {
    overrides[k] = pinnedNpmVersions[k];
  }
}

pkg.overrides = overrides;

fs.writeFileSync(packageJsonPath, JSON.stringify(pkg, null, 2) + '\n', 'utf8');
console.log(`Synced @vaadin/* overrides to ${vaadinVersion}`);
