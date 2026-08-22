# Fechtkarte

A drill card generator for Historical European Martial Arts, built on the cutting-diagram
notation Joachim Meyer used in his manuscripts.

A card is a rectangle crossed by eight rays. A numbered disc marks where a cut *starts*; a dot
below it means thrust instead. The background colour tells you which hand to use. Follow the
numbers.

Everything else — edge, guard, footwork, tempo — is left to the practitioner. That is the
point: the card sets the pattern, you supply the fencing.

Browse the 109 historical cards, generate and configure drills, and export or share them as
PNG or vector PDF. Works offline, in English, German and French. See
[`FEATURES.md`](FEATURES.md) for what the app does.

## What is here

| Path | Contents |
|---|---|
| [`FEATURES.md`](FEATURES.md) | What the app does, feature by feature |
| [`CONTRIBUTING.md`](CONTRIBUTING.md) | How to build, test and submit changes |
| [`CLAUDE.md`](CLAUDE.md) | Coding conventions and the development workflow this project follows |
| [`docs/PALETTE.md`](docs/PALETTE.md) | The card palette and its verification |
| [`docs/ASSET_PROVENANCE.md`](docs/ASSET_PROVENANCE.md) | Licence register — every shipped asset |
| [`PRIVACY_POLICY.md`](PRIVACY_POLICY.md) | What the app stores, and what it does not |
| [`data/original_cards.json`](data/original_cards.json) | 109 historical drill cards, 542 actions |
| [`tools/`](tools/) | Palette verification and image-generation scripts, Python |

## Building

The host needs **only a container runtime**. No Android SDK, no JDK, no Gradle install.

```bash
podman build -t fechtkarte-builder .
```

That runs `gradle clean build` inside the image, so a successful image build is a successful
verification. Per [`CLAUDE.md`](CLAUDE.md), no commit lands without one.

Verify the palette:

```bash
python tools/palette/verify_palette.py
```

## The historical cards

The 109 cards in `data/original_cards.json` are digitised from card artwork depicting Joachim
Meyer's manuscript diagrams, by measurement, not transcription by hand. Numeral discs were
located by connected-component labelling and identified by template matching; every one of the
542 glyphs matched exactly. Disc centres were then projected onto the eight rays, giving a
direction and a radius per action.

The data reveals structure that is not obvious from the cards in isolation: cards 1–44 and
45–88 are the same 44 drills mirrored for the other hand, and cards 89–109 are technique cards.

## Sourcing and originality

The drill patterns are transcriptions of 16th-century manuscript diagrams — facts about a
historical pattern, not authored content. Name, artwork, palette, typography and copy are all
original — see [`docs/ASSET_PROVENANCE.md`](docs/ASSET_PROVENANCE.md), which records the
licence and origin of every asset the app ships.

## How this app is built

Fechtkarte is developed with AI assistance — an AI coding agent writes and reviews much of the
code, documentation and translations, working from the conventions in [`CLAUDE.md`](CLAUDE.md)
under human direction and review.

Two things follow from that, and both are deliberate. Every change goes through the same gate
regardless of who or what wrote it: tests first, then a full containerised build that runs the
entire suite — nothing lands red. And decisions get written down with their reasoning rather
than just their outcome, which is why this repository carries as much prose as it does.

## Licence

**Apache License 2.0.** See [`LICENSE`](LICENSE) and [`NOTICE`](NOTICE). An earlier draft of
this file asserted MIT with no `LICENSE` file to back it up; that has been corrected.

## Credits

The notation is Joachim Meyer's (1537–1571), from *MS A.4º.2* (Lund) and *MS Var.82*
(Rostock).
