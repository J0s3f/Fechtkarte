#!/usr/bin/env sh
# Shared by scripts/build.sh and scripts/test.sh. Prefers podman (what the
# project's docs and CI reference); falls back to docker when podman isn't
# installed, since the two are drop-in compatible for the build/run/image
# subcommands those scripts use.

if command -v podman >/dev/null 2>&1; then
    CONTAINER_ENGINE=podman
elif command -v docker >/dev/null 2>&1; then
    CONTAINER_ENGINE=docker
else
    echo "No container runtime found — install podman or docker." >&2
    exit 1
fi
