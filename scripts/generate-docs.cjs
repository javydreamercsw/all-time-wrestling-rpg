const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const rootDir = path.join(__dirname, '..');
const manifestPath = path.join(rootDir, 'docs', 'manifest.json');
const videoManifestPath = path.join(rootDir, 'docs', 'video-manifest.json');
const outputDir = path.join(rootDir, 'docs', 'site', 'guide');
const screenshotsDir = path.join(rootDir, 'docs', 'screenshots');
const videosDir = path.join(rootDir, 'docs', 'videos');
const vitepressPublicDir = path.join(rootDir, 'docs', 'site', 'public', 'screenshots');
const vitepressVideosDir = path.join(rootDir, 'docs', 'site', 'public', 'videos');
const appStaticDocsDir = path.join(rootDir, 'src', 'main', 'resources', 'META-INF', 'resources', 'docs');

if (!fs.existsSync(manifestPath)) {
  console.error('Manifest not found at:', manifestPath);
  process.exit(1);
}

// Load video manifest (optional — only present after generate-videos profile runs)
let videoFeatures = [];
if (fs.existsSync(videoManifestPath)) {
  const videoManifest = JSON.parse(fs.readFileSync(videoManifestPath, 'utf8'));
  videoFeatures = videoManifest.videos || [];
  console.log(`Loaded ${videoFeatures.length} video(s) from video-manifest.json`);
} else {
  console.log('No video-manifest.json found — skipping video content');
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

// 2b. Sync videos to VitePress public folder for building
if (videoFeatures.length > 0 && fs.existsSync(videosDir)) {
  console.log('Syncing videos to VitePress public folder...');
  if (!fs.existsSync(vitepressVideosDir)) {
    fs.mkdirSync(vitepressVideosDir, { recursive: true });
  }
  const mp4Files = fs.readdirSync(videosDir).filter(f => f.endsWith('.mp4'));
  mp4Files.forEach(file => {
    fs.copyFileSync(path.join(videosDir, file), path.join(vitepressVideosDir, file));
  });
  console.log(`Copied ${mp4Files.length} video(s) to VitePress public folder`);
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

// Group videos by category
const videosByCategory = {};
videoFeatures.forEach(v => {
  if (!videosByCategory[v.category]) {
    videosByCategory[v.category] = [];
  }
  videosByCategory[v.category].push(v);
});
Object.keys(videosByCategory).forEach(cat => {
  videosByCategory[cat].sort((a, b) => (a.order || 0) - (b.order || 0));
});

// 3. Generate Markdown files
// Merge screenshot categories and video-only categories into one set
const allCategories = new Set([...Object.keys(categories), ...Object.keys(videosByCategory)]);

console.log('Generating Markdown files...');
Array.from(allCategories).sort().forEach(category => {
  const catFeatures = categories[category] || [];
  const catVideos = videosByCategory[category] || [];
  const fileName = category.toLowerCase().replace(/ /g, '-') + '.md';
  const filePath = path.join(outputDir, fileName);

  let content = `# ${category}\n\n`;
  content += `Welcome to the ${category} guide. This documentation is automatically generated from the latest game features.\n\n`;

  catFeatures.forEach(feature => {
    content += `## ${feature.title}\n\n`;
    if (feature.description) {
      content += `${feature.description}\n\n`;
    }
    if (feature.imagePath) {
      // In built site, /public/screenshots/ maps to /screenshots/
      const imagePublicPath = `/screenshots/${path.basename(feature.imagePath)}`;
      content += `![${feature.title}](${imagePublicPath})\n\n`;
    }
    content += `---\n\n`;
  });

  // Append video walkthroughs for this category
  if (catVideos.length > 0) {
    content += `## Video Walkthroughs\n\n`;
    catVideos.forEach(video => {
      const videoPublicPath = `/videos/${path.basename(video.videoPath)}`;
      content += `### ${video.title}\n\n`;
      if (video.description) {
        content += `${video.description}\n\n`;
      }
      content += `<video controls width="100%" style="border-radius:8px;margin-bottom:1rem">\n`;
      content += `  <source src="${videoPublicPath}" type="video/mp4">\n`;
      content += `</video>\n\n`;
      content += `---\n\n`;
    });
  }

  fs.writeFileSync(filePath, content);
  console.log(`Generated: ${filePath}`);
});

// 4. Generate Sidebar for VitePress
console.log('Generating dynamic sidebar...');
const sidebar = [];
Array.from(allCategories).sort().forEach(category => {
  const categoryLink = '/guide/' + category.toLowerCase().replace(/ /g, '-');
  sidebar.push({
    text: category,
    collapsed: false,
    items: [
      { text: 'Overview', link: categoryLink }
    ]
  });
});

fs.writeFileSync(
  path.join(rootDir, 'docs', 'site', '.vitepress', 'sidebar.json'),
  JSON.stringify(sidebar, null, 2)
);

// 5. Build VitePress Site
console.log('Building VitePress site...');
try {
  // Use BASE_URL from environment if provided, otherwise calculate from application.properties
  let baseUrl = process.env.BASE_URL;
  
  if (!baseUrl) {
    // Read context path from application.properties
    let contextPath = '/atw-rpg'; // default fallback
    const propsPath = path.join(rootDir, 'src', 'main', 'resources', 'application.properties');
    if (fs.existsSync(propsPath)) {
      const props = fs.readFileSync(propsPath, 'utf8');
      const match = props.match(/server\.servlet\.context-path\s*=\s*(.+)/);
      if (match && match[1]) {
        contextPath = match[1].trim();
      }
    }
    // Set BASE_URL for local build to include context path and docs sub-path
    baseUrl = contextPath.endsWith('/') ? `${contextPath}docs/` : `${contextPath}/docs/`;
  }
  
  console.log(`Using base URL: ${baseUrl}`);
  
  execSync('npm run docs:build', { 
    cwd: path.join(rootDir, 'docs', 'site'), 
    stdio: 'inherit',
    env: { ...process.env, BASE_URL: baseUrl }
  });
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
if (fs.existsSync(appStaticDocsDir)) {
  console.log(`Clearing existing docs in: ${appStaticDocsDir}`);
  fs.rmSync(appStaticDocsDir, { recursive: true, force: true });
}
fs.mkdirSync(appStaticDocsDir, { recursive: true });

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