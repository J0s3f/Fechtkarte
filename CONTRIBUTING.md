# Contributing to Fechtkarte

Thanks for your interest. This project follows a strict workflow — please read this before
opening a pull request, since PRs that don't match it will need rework before they can merge.

## Development workflow

This project follows Test-Driven Development: red (write a failing test), green (simplest code
that passes), refactor (clean up with the test as a safety net). See `CLAUDE.md` at the
repository root for the full set of coding conventions this project follows — naming,
architecture (hexagonal: `domain` → `application` → `adapter`), comment style, and commit
message format. Read it before writing code; PRs that don't follow it will be asked to.

## Building and testing

Everything runs in a container — the host only needs a container runtime (Docker or Podman):

```bash
./scripts/build.sh
```

That runs the full test suite as part of the image build (`gradle clean build`), so a
successful build **is** the verification. There's also a faster loop for local development —
see `scripts/test.sh`.

## Branch and commit conventions

- One logical change per commit; commit message in the imperative mood ("Add X", not "Added
  X").
- Commit prefixes, one per commit: `feat:` (new user-visible capability), `fix:` (bug fix),
  `refactor:` (behaviour-preserving), `test:` (test-only), `docs:`, `build:` (Gradle, container,
  tooling), `chore:` (everything else).
- Every commit should build and pass its tests. Don't commit red.

## Submitting a pull request

1. Fork the repository and create a branch for your change.
2. Follow the TDD workflow above — tests first, then the implementation.
3. Make sure the full container build passes before opening the PR.
4. Describe what changed and why in the PR description, not just what.

## Reporting issues

Open a GitHub issue with clear reproduction steps. For anything security-related, see
[`SECURITY.md`](SECURITY.md) instead of opening a public issue.

## Code of conduct

This project follows the guidelines in [`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md).
