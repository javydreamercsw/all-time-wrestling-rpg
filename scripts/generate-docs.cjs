const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const rootDir = path.join(__dirname, '..');
const manifestPath = path.join(rootDir, 'docs', 'manifest.json');
const outputDir = path.join(rootDir, 'docs', 'site', 'guide');
const screenshotsDir = path.join(rootDir, 'docs', 'screenshots');
const vitepressPublicDir = path.join(rootDir, 'docs', 'site', 'public', 'screenshots');
const appStaticDocsDir = path.join(rootDir, 'src', 'main', 'resources', 'META-INF', 'resources', 'docs');

if (!fs.existsSync(manifestPath)) {
  console.error('Manifest not found at:', manifestPath);
  process.exit(1);
}

// 1. Prepare Markdown directory
if (!fs.existsSync(outputDir)) {
  fs.mkdirSync(outputDir, { recursive: true });
}

// 2. Sync screenshots to VitePress public folder for building
console.log('Syncing screenshots to VitePress public folder...');
if (!fs.existsSync(vitepressPublicDir)) {
  fs.mkdirSync(vitepressPublicDir, { recursive: true });
}
if (fs.existsSync(screenshotsDir)) {
  const files = fs.readdirSync(screenshotsDir);
  files.forEach(file => {
    fs.copyFileSync(path.join(screenshotsDir, file), path.join(vitepressPublicDir, file));
  });
}

const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf8'));
const features = manifest.features || [];

// Group features by category
const categories = {};
features.forEach(feature => {
  if (!categories[feature.category]) {
    categories[feature.category] = [];
  }
  categories[feature.category].push(feature);
});

// Sort features within each category by order
Object.keys(categories).forEach(cat => {
  categories[cat].sort((a, b) => (a.order || 0) - (b.order || 0));
});

// 3. Generate Markdown files
console.log('Generating Markdown files...');
Object.entries(categories).forEach(([category, catFeatures]) => {
  const fileName = category.toLowerCase().replace(/ /g, '-') + '.md';
  const filePath = path.join(outputDir, fileName);
  
  let content = `# ${category}\n\n`;
  content += `Welcome to the ${category} guide. This documentation is automatically generated from the latest game features.\n\n`;

  catFeatures.forEach(feature => {
    content += `## ${feature.title}\n\n`;
    content += `${feature.description}\n\n`;
    
    // In built site, /public/screenshots/ maps to /screenshots/
    const imagePublicPath = `/screenshots/${path.basename(feature.imagePath)}`;
    content += `![${feature.title}](${imagePublicPath})\n\n`;
    content += `---\n\n`;
  });

  fs.writeFileSync(filePath, content);
  console.log(`Generated: ${filePath}`);
});

// 4. Build VitePress Site
console.log('Building VitePress site...');
try {
  execSync('npm run docs:build', { cwd: path.join(rootDir, 'docs', 'site'), stdio: 'inherit' });
} catch (error) {
  console.error('VitePress build failed:', error);
  process.exit(1);
}

// 5. Create .nojekyll for GitHub Pages
const buildDistDir = path.join(rootDir, 'docs', 'site', '.vitepress', 'dist');
fs.writeFileSync(path.join(buildDistDir, '.nojekyll'), '');
console.log('Created .nojekyll file');

// 6. Copy to App Static Resources
console.log('Embedding docs in Spring Boot application...');
if (!fs.existsSync(appStaticDocsDir)) {
  fs.mkdirSync(appStaticDocsDir, { recursive: true });
}

// Simple recursive copy
function copyRecursiveSync(src, dest) {
  const exists = fs.existsSync(src);
  const stats = exists && fs.statSync(src);
  const isDirectory = exists && stats.isDirectory();
  if (isDirectory) {
    if (!fs.existsSync(dest)) {
      fs.mkdirSync(dest);
    }
    fs.readdirSync(src).forEach((childItemName) => {
      copyRecursiveSync(path.join(src, childItemName), path.join(dest, childItemName));
    });
  } else {
    fs.copyFileSync(src, dest);
  }
}

if (fs.existsSync(buildDistDir)) {
  copyRecursiveSync(buildDistDir, appStaticDocsDir);
  console.log(`Docs embedded in: ${appStaticDocsDir}`);
} else {
  console.error('Build output not found at:', buildDistDir);
  process.exit(1);
}

console.log('Documentation integration complete.');