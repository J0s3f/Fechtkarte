# Fechtkarte

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height="80">](https://f-droid.org/packages/at.j0s.meyercard.app/)

A drill card generator for Historical European Martial Arts, built on a cutting-diagram
notation credited to Joachim Meyer's 16th-century fencing teaching.

A card is a rectangle crossed by eight rays. A numbered disc marks where a cut *starts*; a dot
below it means thrust instead. The background colour tells you which hand to use. Follow the
numbers.

Everything else — edge, guard, footwork, tempo — is left to the practitioner. That is the
point: the card sets the pattern, you supply the fencing.

Browse more than 100 classic cards, generate unlimited random drills, and export or share them
as PNG or vector PDF. Works offline, in English, German and French. See
[`FEATURES.md`](FEATURES.md) for what the app does.

## What is here

| Path | Contents |
|---|---|
| [`FEATURES.md`](FEATURES.md) | What the app does, feature by feature |
| [`CONTRIBUTING.md`](CONTRIBUTING.md) | How to build, test and submit changes |
| [`CLAUDE.md`](CLAUDE.md) | Coding conventions and the development workflow this project follows |
| [`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md) | Community standards for contributors |
| [`SECURITY.md`](SECURITY.md) | How to report a vulnerability |
| [`docs/PALETTE.md`](docs/PALETTE.md) | The card palette and its verification |
| [`docs/ASSET_PROVENANCE.md`](docs/ASSET_PROVENANCE.md) | Licence register — every shipped asset |
| [`PRIVACY_POLICY.md`](PRIVACY_POLICY.md) | What the app stores, and what it does not |
| [`PLAY_DATA_SAFETY.md`](PLAY_DATA_SAFETY.md) | Google Play's Data Safety disclosure, and why every category is "not collected" |
| [`data/original_cards.json`](data/original_cards.json) | The classic drill cards |
| [`tools/`](tools/) | Palette verification and image-generation scripts, Python |

## Building

The host needs **only a container runtime** (Podman or Docker). No Android SDK, no JDK, no
Gradle install.

```bash
./scripts/build.sh
```

That runs `gradle clean build` inside the image, so a successful image build is a successful
verification. Per [`CLAUDE.md`](CLAUDE.md), no commit lands without one. For a faster inner loop
while writing a test, see `scripts/test.sh`.

Verify the palette (light and dark theme):

```bash
python tools/palette/verify_palette.py
python tools/palette/verify_dark_palette.py
```

## The classic cards

`data/original_cards.json` ships more than 100 classic drill cards in a notation credited to
Joachim Meyer's 16th-century fencing teaching. The Library screen presents them as distinct
drills with a hand toggle, plus a set of technique cards, rather than as one long flat list.

## Sourcing and originality

Each drill action is a short numeric record — a direction, a distance from centre, a sequence
position, a thrust flag — not narrative or pictorial expression. Name, artwork, palette,
typography and copy are all original — see
[`docs/ASSET_PROVENANCE.md`](docs/ASSET_PROVENANCE.md), which records the licence and origin of
every asset the app ships.

## How this app is built

Fechtkarte is developed with AI assistance — an AI coding agent writes and reviews much of the
code, documentation and translations, working from the conventions in [`CLAUDE.md`](CLAUDE.md)
under human direction and review.

Two things follow from that, and both are deliberate. Every change goes through the same gate
regardless of who or what wrote it: tests first, then a full containerised build that runs the
entire suite — nothing lands red. And decisions get written down with their reasoning rather
than just their outcome, which is why this repository carries as much prose as it does.

## Licence

**Apache License 2.0.** See [`LICENSE`](LICENSE) and [`NOTICE`](NOTICE).

## Credits

The cut-and-thrust notation is credited to Joachim Meyer (1537–1571).
