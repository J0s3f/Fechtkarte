#!/usr/bin/env sh
# The commit gate. A successful image build IS the verification — see CLAUDE.md and
# docs/NEXT_STEPS.md. Slow (~2-3 min): a full COPY of the source tree plus a clean
# Gradle build, deliberately not reusing anything from a previous run of the app
# layers. For the fast inner loop while writing a test, use scripts/test.sh instead.
#
# Usage: scripts/build.sh [--no-cache]

set -eu
cd "$(dirname "$0")/.."

# MSYS_NO_PATHCONV stops Git Bash on Windows from mangling the image tag or any
# leading-slash argument into a Windows path. Harmless, and a no-op, on other shells.
export MSYS_NO_PATHCONV=1

exec podman build "$@" -t fechtkarte-builder .
