const assert = require('node:assert/strict');
const { test } = require('node:test');

const { execFileSync } = require('node:child_process');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const SCRIPT = path.join(__dirname, 'cleanup-rc-releases.sh');

function runScript(args, env = {}) {
  return execFileSync('bash', [SCRIPT, ...args], {
    env: { ...process.env, ...env },
    encoding: 'utf8',
  });
}

/** Writes a mock gh that reports the given tags for release listing and
 *  optionally fails deletions of the given tags. Returns its path. */
function writeMockGh(dir, tags, failingTags = []) {
  const mockPath = path.join(dir, 'gh-mock.sh');
  fs.writeFileSync(
    mockPath,
    `#!/usr/bin/env bash
echo "$*" >> ${JSON.stringify(path.join(dir, 'calls.log'))}
if [ "$1" = "api" ]; then
  cat <<'TAGS'
${tags.join('\n')}
TAGS
  exit 0
fi
# release delete <tag> --repo <repo> --yes  -> tag is $3
for failed in ${failingTags.map((t) => JSON.stringify(t)).join(' ')}; do
  if [ "$3" = "$failed" ]; then
    echo "mock delete failure for $3" >&2
    exit 1
  fi
done
exit 0
`);
  fs.chmodSync(mockPath, 0o755);
  return mockPath;
}

function readCalls(dir) {
  const log = path.join(dir, 'calls.log');
  return fs.existsSync(log) ? fs.readFileSync(log, 'utf8').trim().split('\n') : [];
}

test('matches only RC tags of the exact base version', () => {
  const tags = 'v2.10.0\nv2.10.0-RC1\nv2.10.0-RC2\nv2.9.0\nv2.11.0-RC1\nv2.10.0-BETA1\n';
  const out = execFileSync('bash', [SCRIPT, '--emit-matches', '2.10.0'], {
    input: tags,
    encoding: 'utf8',
  });
  assert.equal(out, 'v2.10.0-RC1\nv2.10.0-RC2\n');
});

test('escapes dots so 2.10.0 does not match v2x1x0-style tags', () => {
  const tags = 'v2100-RC1\nv2.10.0-RC1\n';
  const out = execFileSync('bash', [SCRIPT, '--emit-matches', '2.10.0'], {
    input: tags,
    encoding: 'utf8',
  });
  assert.equal(out, 'v2.10.0-RC1\n');
});

test('deletes every matching RC release and keeps git tags', (t, done) => {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'rc-cleanup-'));
  const mock = writeMockGh(dir, ['v2.10.0', 'v2.10.0-RC1', 'v2.10.0-RC2', 'v2.9.0']);
  t.after(() => fs.rmSync(dir, { recursive: true, force: true }));

  const out = runScript(['2.10.0'], { GH_CMD: mock, REPO: 'owner/repo' });
  assert.match(out, /Deleting release v2\.10\.0-RC1/);
  assert.match(out, /Deleting release v2\.10\.0-RC2/);
  assert.match(out, /RC cleanup for v2\.10\.0 complete\./);

  const deletes = readCalls(dir).filter((line) => line.startsWith('release delete'));
  assert.deepEqual(deletes, [
    `release delete v2.10.0-RC1 --repo owner/repo --yes`,
    `release delete v2.10.0-RC2 --repo owner/repo --yes`,
  ]);
  done();
});

test('exits non-zero when an RC deletion fails, after attempting all', (t, done) => {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'rc-cleanup-'));
  const mock = writeMockGh(dir, ['v2.10.0-RC1', 'v2.10.0-RC2'], ['v2.10.0-RC2']);
  t.after(() => fs.rmSync(dir, { recursive: true, force: true }));

  assert.throws(
    () => runScript(['2.10.0'], { GH_CMD: mock, REPO: 'owner/repo' }),
    (err) => err.status === 1 && /failed to delete release v2\.10\.0-RC2/.test(err.stderr)
  );
  const deletes = readCalls(dir).filter((line) => line.startsWith('release delete'));
  // Both RCs were attempted even though the second failed.
  assert.equal(deletes.length, 2);
  done();
});

test('does nothing when no RC release matches the base version', (t, done) => {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'rc-cleanup-'));
  const mock = writeMockGh(dir, ['v2.9.0', 'v2.10.0']);
  t.after(() => fs.rmSync(dir, { recursive: true, force: true }));

  const out = runScript(['2.11.0'], { GH_CMD: mock, REPO: 'owner/repo' });
  assert.match(out, /No RC releases for v2\.11\.0 - nothing to clean up\./);
  assert.equal(readCalls(dir).filter((line) => line.startsWith('release delete')).length, 0);
  done();
});

test('rejects an RC version passed as the base', () => {
  assert.throws(
    () => runScript(['2.10.0-RC1']),
    (err) => err.status === 1 && /not a bare x\.y\.z version/.test(err.stderr)
  );
});
