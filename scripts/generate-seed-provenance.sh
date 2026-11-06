#!/usr/bin/env bash
# Regenerates the committed seed-provenance manifest.
# Boots the real application context on an empty in-memory H2 database (test profile), runs the
# production seed sync pipeline, and hashes every freshly seeded row through the sync classes' own
# fingerprint methods — see
# src/test/java/com/github/javydreamercsw/management/sync/review/GenerateSeedProvenanceManifest.java.
# Output: src/main/resources/seed-provenance.json
# Deterministic except "generatedAt" (wall-clock, truncated to seconds): two consecutive runs
# differ on that one key only.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

OUTPUT="$ROOT/src/main/resources/seed-provenance.json"
STAGED="$ROOT/target/seed-provenance.json"

cd "$ROOT"

echo "Generating seed provenance manifest..."

# exec:java runs the generator inside the Maven JVM; the generator's System.exit(1|2) terminates
# Maven with that code, so failures propagate to this script (set -e stops here).
# -Dsurefire.skip=true: the pom wires surefire's skipTests to ${surefire.skip} (plain -DskipTests
#   is a no-op); harmless here since test-compile never runs tests, and it protects the
#   invocation if the phases ever change.
# -Dspotless.check.skip=true: the generated manifest lands under src/main/resources, where the
#   JSON formatter would see it on every subsequent build; the spotless exclusion makes this
#   permanent, meanwhile this keeps regeneration working regardless.
# -Xmx3g: mirror the surefire heap — the full context boot + seed sync run in the Maven JVM.
MAVEN_OPTS="${MAVEN_OPTS:-} -Xmx3g" \
  mvn -B -q -Pgenerate-seed-provenance test-compile exec:java@generate-seed-provenance \
    -Dsurefire.skip=true -Dspotless.check.skip=true

mv "$STAGED" "$OUTPUT"

if [[ ! -s "$OUTPUT" ]]; then
  echo "ERROR: $OUTPUT was not written" >&2
  exit 1
fi

echo "  Written seed provenance manifest to $OUTPUT"
