#!/usr/bin/env sh
# The fast inner loop for red-green-refactor. Mounts the working tree into the
# already-built image instead of copying it, so only Gradle's own incremental
# compilation runs — no image rebuild, no dependency re-download.
#
# Requires scripts/build.sh (or a prior image build) to have produced the
# fechtkarte-builder image at least once; that build is what warms the Gradle cache
# baked into the image. This script does not rebuild the image itself, by design —
# rebuilding on every test run would defeat the point.
#
# Uses podman if available, otherwise docker — see scripts/container-engine.sh.
#
# Usage: scripts/test.sh [gradle task...]   defaults to `test`

set -eu
cd "$(dirname "$0")/.."

. ./scripts/container-engine.sh

if ! "$CONTAINER_ENGINE" image inspect fechtkarte-builder >/dev/null 2>&1; then
    echo "fechtkarte-builder image not found — building it once first." >&2
    echo "(This is the slow path; subsequent runs of this script will be fast.)" >&2
    ./scripts/build.sh
fi

task="${*:-test}"

# MSYS_NO_PATHCONV: see scripts/build.sh.
export MSYS_NO_PATHCONV=1

exec "$CONTAINER_ENGINE" run --rm \
    -v "$(pwd)":/app \
    -w /app \
    fechtkarte-builder \
    ./gradlew --no-daemon $task
