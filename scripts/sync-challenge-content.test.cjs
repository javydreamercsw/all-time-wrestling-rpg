const assert = require('node:assert/strict');
const { test } = require('node:test');

const fs = require('fs');
const os = require('os');
const path = require('path');

const { copyChallengeDirectory, toLiveImageUrl } = require('./sync-challenge-content.cjs');

test('rewrites bundled challenge image paths for live content', () => {
  assert.equal(toLiveImageUrl('images/challenges/week5.png'), 'challenge-content/images/week5.png');
  assert.equal(toLiveImageUrl('/images/challenges/week5.png'), 'challenge-content/images/week5.png');
});

test('preserves live and external challenge image paths', () => {
  assert.equal(toLiveImageUrl('challenge-content/images/week4.png'), 'challenge-content/images/week4.png');
  assert.equal(toLiveImageUrl('https://example.com/challenges/week5.png'), 'https://example.com/challenges/week5.png');
  assert.equal(toLiveImageUrl(null), null);
});

test('copies downloaded challenge content with live image URLs', () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'atw-challenge-sync-'));
  const source = path.join(root, 'source');
  const target = path.join(root, 'target');
  fs.mkdirSync(source, { recursive: true });
  fs.writeFileSync(
    path.join(source, 'weekly.json'),
    JSON.stringify([
      { id: 'week_05_new', imageUrl: 'images/challenges/week5.png' },
      { id: 'week_04_existing', imageUrl: 'challenge-content/images/week4.png' },
    ]),
  );

  try {
    copyChallengeDirectory(source, target);
    const copied = JSON.parse(fs.readFileSync(path.join(target, 'weekly.json')));
    assert.equal(copied[0].imageUrl, 'challenge-content/images/week5.png');
    assert.equal(copied[1].imageUrl, 'challenge-content/images/week4.png');
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});
