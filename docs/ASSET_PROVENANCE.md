# Asset provenance and licensing

Fechtkarte targets F-Droid, so **everything shipped must be FOSS-clean** (decision D8). This
file is the human-readable register. Nothing enters `app/src/main/` without a row here.

**There is also a machine check.** `gradle check` runs `checkLicense`
([`config/allowed-licenses.json`](../config/allowed-licenses.json)) and fails the build if any
dependency's licence isn't on the allowlist below. The two should never disagree; if they do,
this file is describing an intent the build isn't actually enforcing, and that's a bug in the
gate, not a footnote here.

Status legend: **cleared** — verified and safe to ship · **pending** — needs checking before
use · **excluded** — must never ship.

## Project licence

| Item | Status | Notes |
|---|---|---|
| Fechtkarte source | **cleared** | Apache License 2.0. See [`LICENSE`](../LICENSE) and [`NOTICE`](../NOTICE) at the repository root |

Apache-2.0 is F-Droid-acceptable, permits commercial and closed-source downstream reuse
(unlike GPL-3.0), and includes an explicit patent grant that plain MIT lacks — a reasonable
default for a project with no other licensing constraint pulling it toward MIT or GPL. Apache-2.0
requires the `NOTICE` file's contents to travel with any redistribution.

## Runtime dependencies

All Apache-2.0 unless noted. None require attribution beyond a notices screen, none are
proprietary, none pull in Google Play Services.

| Dependency | Licence | Status |
|---|---|---|
| AndroidX Core, Activity, Lifecycle, Navigation | Apache-2.0 | cleared |
| Jetpack Compose (UI, Foundation, Material 3) | Apache-2.0 | cleared |
| Room | Apache-2.0 | cleared |
| DataStore | Apache-2.0 | cleared |
| kotlinx.serialization | Apache-2.0 | cleared |
| Kotlin stdlib | Apache-2.0 | cleared |

Removed from the scaffold: **Gson** (Apache-2.0, would have been fine — dropped in favour of
`kotlinx.serialization`, which needs no reflection and so works cleanly with R8).

## Build and test dependencies

Not shipped in the APK, but F-Droid builds from source, so they must not block a reproducible
build.

| Dependency | Licence | Status |
|---|---|---|
| JUnit 5 (Jupiter) | EPL-2.0 | cleared |
| `de.mannodermaus.android-junit5` | Apache-2.0 | cleared |
| Robolectric | MIT | cleared |
| Paparazzi | Apache-2.0 | cleared |
| Konsist | Apache-2.0 | cleared |
| Android Gradle Plugin, Android SDK | proprietary (Google) | cleared — F-Droid builds against the official SDK; this is standard and not a blocker |

## Fonts

| Asset | Licence | Status |
|---|---|---|
| UnifrakturMaguntia.ttf (numerals) | SIL OFL 1.1 | cleared and bundled — `app/src/main/res/font/unifraktur_maguntia.ttf`, 257,648 bytes, **version 2017-03-19**. Downloaded 2026-08-22 from the typeface's own project at `sourceforge.net/projects/unifraktur/files/fonts/UnifrakturMaguntia.2017-03-19.zip` — upstream, rather than the Google Fonts mirror this previously used, which is still on the 2010-11-24 release. Copyright (c) 2010-2017 j. 'mach' wust, Gerrit Ansmann, Georg Duffner, with Reserved Font Name UnifrakturMaguntia; (c) 2009 Peter Wiegel. The `OFL.txt` from the same archive ships alongside it at `app/src/main/assets/licenses/unifraktur_maguntia_OFL.txt` and is surfaced on the notices screen. Shipped unmodified: the Reserved Font Name forbids distributing a changed copy under this name, so the font is not subset even though only the digits 1-8 are used — the OpenType features the app needs (`lnum` for numerals, `ss01` for the wordmark) are requested at render time instead |
| UI sans | pending | Prefer the platform default, or an OFL face. Do not use Google Fonts at runtime |

OFL requires the font's own licence text to ship alongside it — done, see above. Surface it in
the notices screen once one exists (T7.7).

## Card data

| Asset | Status | Notes |
|---|---|---|
| `data/original_cards.json` | cleared | See reasoning below |

The dataset records, per card, which openings are cut, in what order, and which actions are
thrusts, in a notation credited to Joachim Meyer's 16th-century fencing teaching. Each entry is
a short numeric fact — "action 3 starts north-east, outer ring, and is a thrust" — not
narrative or pictorial expression.

## Manuscript imagery — provenance cleared, no specific page scans chosen or bundled yet

Needed for the Sources screen (task T7.3).

**Both source manuscripts' figures are confirmed public domain by their holding institutions
directly** — not just by Wiktenauer's own policy (which separately treats manuscript scans as
public domain per the *Bridgeman Art Library v. Corel Corp.* precedent, see
[Wiktenauer's copyright policy](https://wiktenauer.com/wiki/Wiktenauer:Copyright_Policy) —
itself a reasonable position, but a US legal precedent isn't a guarantee of what a
non-US holding institution's own terms say, so it isn't treated as sufficient on its own here):

| Source | Institution's own rights statement | Verified |
|---|---|---|
| [MS A.4º.2 (Lund)](http://urn.kb.se/resolve?urn=urn:nbn:se:alvin:portal:record-76351), Lunds Universitets Bibliotek, hosted on Alvin (Sweden's national digital collections platform) | "Licensiering av verket: **Public Domain Mark** (Ingen känd upphovsrätt)" — "Licensing of the work: Public Domain Mark (No known copyright)" | 2026-08-21, read directly on the Alvin record page |
| [MS Var.82 (Rostock)](http://purl.uni-rostock.de/rosdok/ppn780606825/phys_0000), Universitätsbibliothek Rostock, hosted on RosDok | "Rechte: **gemeinfrei**. Dieses Werk unterliegt keinen bekannten urheberrechtlichen Beschränkungen." — "Rights: public domain. This work is not subject to any known copyright restrictions." | 2026-08-21, read directly on the RosDok record page |

The Wiktenauer index pages' own per-work licence tables ([Joachim Meyer](https://wiktenauer.com/wiki/Joachim_Meyer),
[MS A.4º.2](https://wiktenauer.com/wiki/Joachim_Meyers_F%C3%A4ktbok_(MS_A.4%C2%BA.2))) independently
mark both manuscripts' *figures* "Public Domain," consistent with the above. Note this is
about the **figures/images** specifically — the *transcriptions* of these same manuscripts
(different assets, prose renderings of the handwritten text) carry more restrictive terms
(CC BY-NC-SA 3.0 for both) and are not needed here regardless, since Fechtkarte only ever
wanted the diagrams, not the manuscript text.

The Wiktenauer URN link for Lund (`urn:nbn:se:lu:ub-MeyerJ_Faktbok-12232688`) is currently dead
(resolves to "no valid destination"); the live Alvin link above was found via web search and
should replace it if this document is updated again.

**What's still open, not a licensing question anymore:** which specific page(s)/diagram(s) to
actually use for the Sources screen, and whether to bundle an institution-hosted scan directly
or fall back to Fechtkarte's own vector redraw of the diagram, which also suits the app's own
visual identity better and sidesteps any residual "faithful reproduction" nuance entirely.
Downloading and bundling an actual scan file is a distinct decision from clearing its licence —
not done as part of this research pass.

### Two sources that must not be used

Both surfaced during research and are **excluded**:

| Source | Reason |
|---|---|
| Redbubble "Meyer Cutting Diagram 1570" art print | A contemporary artist's copyrighted derivative work, offered for sale |
| Talhoffer-blog image of MS Var.82 2v–3v | Carries annotations added by the blog's author, who says so explicitly |

Both are fine to *look at* while researching. Neither may be redistributed.

## Checklist before any public build

- [x] `LICENSE` file exists (Apache-2.0). F-Droid's own metadata repository (not this repo —
  populated when the app is actually submitted) reads licence from the SPDX-detectable
  `LICENSE` file, so there's no separate declaration step on this side to track
- [x] Every asset actually bundled today has a row above marked *cleared* — the one thing not
  yet cleared-and-shipped is manuscript imagery, and nothing manuscript-related is bundled yet
  either (provenance is cleared, no specific page has been chosen; see that section)
- [x] Notices screen lists dependency licences and the font's OFL text — `adapter/ui/notices/NoticesScreen.kt` (T7.7)
- [x] No unreviewed third-party binary asset is tracked by git — confirmed via `git ls-files`,
  2026-08-21
- [x] `python tools/palette/verify_palette.py` exits 0 — confirmed 2026-08-21 ("All constraints satisfied")
