#!/usr/bin/env sh
# Shared by scripts/build.sh and scripts/test.sh. Prefers podman (what the
# project's docs and CI reference); falls back to docker, then to wslc, since
# all three are drop-in compatible for the build/run/image subcommands those
# scripts use.
#
# wslc is Microsoft's own WSL container CLI (ships at
# "C:\Program Files\WSL\wslc.exe", callable as plain `wslc` once that's on
# PATH) -- the fallback on a Windows dev machine that has WSL but neither
# Docker Desktop nor podman/docker installed inside a distro. Checked last,
# not first: it's Windows-only and this project's CI always has real
# docker/podman, so preferring it there would be backwards.

if command -v podman >/dev/null 2>&1; then
    CONTAINER_ENGINE=podman
elif command -v docker >/dev/null 2>&1; then
    CONTAINER_ENGINE=docker
elif command -v wslc >/dev/null 2>&1; then
    CONTAINER_ENGINE=wslc
else
    echo "No container runtime found — install podman, docker, or (on Windows) use wslc via WSL." >&2
    exit 1
fi
