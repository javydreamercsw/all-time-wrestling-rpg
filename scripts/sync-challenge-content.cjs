const fs = require('fs');
const path = require('path');

const rootDir = path.join(__dirname, '..');
const sourceChallengesDir = path.join(rootDir, 'src', 'main', 'resources', 'challenges');
const sourceImagesDir = path.join(rootDir, 'src', 'main', 'resources', 'META-INF', 'resources', 'images', 'challenges');
const docsChallengesDir = path.join(rootDir, 'docs', 'challenges');
const docsImagesDir = path.join(docsChallengesDir, 'images');
const vitepressChallengesDir = path.join(rootDir, 'docs', 'site', 'public', 'challenges');
const manifestPath = path.join(docsChallengesDir, 'manifest.json');

function copyDirectoryContents(sourceDir, targetDir) {
  fs.mkdirSync(targetDir, { recursive: true });
  fs.readdirSync(sourceDir, { withFileTypes: true }).forEach((entry) => {
    const source = path.join(sourceDir, entry.name);
    const target = path.join(targetDir, entry.name);
    if (entry.isDirectory()) {
      fs.rmSync(target, { recursive: true, force: true });
      fs.cpSync(source, target, { recursive: true });
    } else {
      fs.copyFileSync(source, target);
    }
  });
}

function toLiveImageUrl(imageUrl) {
  return imageUrl?.replace(/^\/?images\/challenges\//, 'challenge-content/images/') ?? imageUrl;
}

function copyChallengeDirectory(sourceDir, targetDir) {
  fs.mkdirSync(targetDir, { recursive: true });
  fs.readdirSync(sourceDir, { withFileTypes: true }).forEach((entry) => {
    const source = path.join(sourceDir, entry.name);
    const target = path.join(targetDir, entry.name);
    if (entry.isDirectory()) {
      copyChallengeDirectory(source, target);
    } else if (entry.name.endsWith('.json')) {
      const content = JSON.parse(fs.readFileSync(source, 'utf8'));
      const liveContent = Array.isArray(content)
        ? content.map((challenge) =>
            challenge.imageUrl ? { ...challenge, imageUrl: toLiveImageUrl(challenge.imageUrl) } : challenge,
          )
        : content;
      fs.writeFileSync(target, JSON.stringify(liveContent, null, 2) + '\n');
    } else {
      fs.copyFileSync(source, target);
    }
  });
}

function challengeJsonPath(jsonUrl) {
  const marker = '/challenges/';
  const markerIndex = jsonUrl.indexOf(marker);
  if (markerIndex >= 0) {
    return path.join(docsChallengesDir, decodeURIComponent(jsonUrl.slice(markerIndex + marker.length)));
  }
  return path.join(docsChallengesDir, jsonUrl.replace(/^\/?challenges\/?/, ''));
}

function packageImages(pkg, baseUrl) {
  if (!pkg.jsonUrl) {
    return pkg.images || [];
  }

  const jsonPath = challengeJsonPath(pkg.jsonUrl);
  if (!fs.existsSync(jsonPath)) {
    return pkg.images || [];
  }

  const challenges = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));
  const names = new Set(pkg.images?.map((image) => image.name) || []);
  challenges.forEach((challenge) => {
    if (challenge.imageUrl) {
      const name = path.posix.basename(challenge.imageUrl.split('?')[0]);
      if (name && fs.existsSync(path.join(docsImagesDir, name))) {
        names.add(name);
      }
    }
  });

  return [...names].sort().map((name) => ({
    name,
    url: `${baseUrl}/challenges/images/${name}`,
  }));
}

function syncManifestImages() {
  if (!fs.existsSync(manifestPath)) {
    return;
  }

  const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf8'));
  const baseUrl = (process.env.GITHUB_PAGES_BASE || 'https://javydreamercsw.github.io/all-time-wrestling-rpg').replace(
    /\/$/,
    '',
  );
  let changed = false;
  (manifest.packages || []).forEach((pkg) => {
    const images = packageImages(pkg, baseUrl);
    if (JSON.stringify(pkg.images || []) !== JSON.stringify(images)) {
      changed = true;
    }
    pkg.images = images;
  });

  if (changed) {
    manifest.lastUpdated = new Date().toISOString().slice(0, 10);
  }
  fs.writeFileSync(manifestPath, JSON.stringify(manifest, null, 2) + '\n');
}

function syncChallengeContent() {
  if (!fs.existsSync(sourceChallengesDir)) {
    throw new Error(`Challenge source directory not found: ${sourceChallengesDir}`);
  }
  if (!fs.existsSync(sourceImagesDir)) {
    throw new Error(`Challenge image source directory not found: ${sourceImagesDir}`);
  }

  fs.mkdirSync(docsChallengesDir, { recursive: true });
  copyChallengeDirectory(sourceChallengesDir, docsChallengesDir);

  // Keep images belonging to live-update packages while refreshing bundled images.
  copyDirectoryContents(sourceImagesDir, docsImagesDir);
  syncManifestImages();

  // VitePress must receive the synchronized content before its build starts.
  fs.rmSync(vitepressChallengesDir, { recursive: true, force: true });
  fs.mkdirSync(path.dirname(vitepressChallengesDir), { recursive: true });
  fs.cpSync(docsChallengesDir, vitepressChallengesDir, { recursive: true });
}

if (require.main === module) {
  try {
    syncChallengeContent();
  } catch (error) {
    process.stderr.write(`Challenge content synchronization failed: ${error}\n`);
    process.exit(1);
  }
}

module.exports = { copyChallengeDirectory, syncChallengeContent, toLiveImageUrl };
