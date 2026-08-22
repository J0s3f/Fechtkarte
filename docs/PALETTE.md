# Palette

The card palette, designed from scratch for this project rather than reused from elsewhere —
an original identity, not a borrowed one.

Generated and checked by [`tools/palette/verify_palette.py`](../tools/palette/verify_palette.py),
which exits non-zero if any constraint breaks. The numbers below are its output, not estimates.

## The six palettes

| Palette | Light | Dark | Ink on fill | Light/dark |
|---|---|---|---|---|
| Iris | `#BB8FD4` | `#934CBB` | 3.31:1 | 2.00:1 |
| **Woad** | `#D3E9F9` | `#58ABE7` | 6.96:1 | 2.00:1 |
| Verdigris | `#3EB9AF` | `#2A7F77` | 3.65:1 | 1.99:1 |
| Moss | `#7CC05A` | `#4F8733` | 4.02:1 | 1.97:1 |
| Madder | `#F3D5D0` | `#DD8476` | 6.33:1 | 2.00:1 |
| **Orpiment** | `#EBAB13` | `#A4770D` | 4.33:1 | 1.98:1 |

Fixed colours:

| Role | Value | Notes |
|---|---|---|
| Ink | `#1A1A1A` | Rays, disc outlines, numerals. Soft black, reads as ink rather than pure `#000` |
| Paper | `#FAF7F0` | Disc fill. Warm off-white |

Ink on paper is **16.27:1** — the numerals are the only real text on the card, and they clear
the WCAG AA threshold with room to spare.

Defaults: **Woad** for the right hand, **Orpiment** for the left.

## Naming

The palettes are named for pigments and dyes a 16th-century workshop would have had on the
bench — woad, madder, verdigris, orpiment, iris green, moss. It gives the set an identity of
its own that suits the subject matter, rather than falling back on bare colour words like
`blue`, `orange` or `teal`.

## Construction

Each palette is **one hue at two luminance steps**: a light and a dark shade for the quadrant
checkerboard, at a fixed 2.0:1 contrast between them. Hue and saturation are chosen per
palette; the dark shade's luminance is assigned by search (below).

## Constraints, and why

| Constraint | Threshold | Reason |
|---|---|---|
| Ink vs quadrant fill | ≥ 3:1 | The rays and disc outlines are graphical objects, not text — WCAG 1.4.11's threshold. This is what keeps the diagram readable |
| Ink on paper disc | ≥ 4.5:1 | The numerals are text — WCAG AA |
| Light vs dark shade | ≥ 1.7:1 | The checkerboard must stay visible. WCAG contrast is luminance-only, so this doubles as the greyscale check |
| Default hand pair, greyscale | ≥ 1.6:1 | Cards get printed in black and white; the hand signal has to survive it |
| Any two palettes, any vision type | ≥ 30 RGB distance | A user picking left and right palettes must not be able to choose a pair they cannot tell apart |

### A correction to the earlier plan

An earlier revision of the plan required "≥ 4.5:1 against the disc fill **and** the ray
stroke". That is arithmetically impossible. For a colour of relative luminance *L*, contrast
against white is `1.05/(L+0.05)` and against black is `(L+0.05)/0.05`; both exceed 4.5 only
for *L* in roughly `[0.175, 0.183]` — a band too narrow to hold six distinct hues.

The requirement was also aimed at the wrong thing. **The disc separates from the quadrant fill
by its ink outline, not by its own fill contrast.** Once that is recognised, the real
constraint is ink-vs-fill, and 3:1 is the correct threshold for a graphical object.

## Colour-vision deficiency

All six palettes are checked under normal vision and the three dichromacies, using the
Viénot et al. (1999) linear simulation.

The luminance assigned to each hue is not chosen by taste — it is **searched**, maximising the
worst-case separation between any two palettes across all four vision types. A first attempt
that gave every palette the same luminance and varied only hue produced a worst-case
separation of **11.8** (Verdigris and Ash were indistinguishable under deuteranopia). Staggering
luminance raises that to **35.4**.

Closest pair, per vision type:

| Vision | Closest pair | Separation |
|---|---|---|
| Normal | Verdigris / Moss | 77.8 |
| Deuteranopia | Moss / Orpiment | 56.6 |
| Protanopia | Verdigris / Madder | 35.4 |
| Tritanopia | Iris / Orpiment | 35.4 |

### The default pair is the one that matters

Woad and Orpiment are separated by **223–255** under every vision type, and by 1.61:1 in
greyscale. Blue against amber is the standard colour-blind-safe axis, and it is what most
users will never change.

## Open point

Protanopia and tritanopia both bottom out at 35.4, which is distinguishable but not
comfortable. Two options, to settle when the Configure screen is built (task T5.3):

1. Warn when the chosen left and right palettes fall below ~60 separation.
2. Drop to five palettes and re-run the search for a larger margin.

Option 1 is preferred: it keeps the choice available for users who can see the difference,
while steering everyone else away from a pair that will not work for them.

## Dark mode (T7.4)

Generated and checked by
[`tools/palette/verify_dark_palette.py`](../tools/palette/verify_dark_palette.py), which
imports its hues, constraint thresholds, and colour-science functions straight from
`verify_palette.py` rather than duplicating them — only ink, paper, and the luminance search
itself differ.

| Palette | Light | Dark | Ink on fill | Light/dark |
|---|---|---|---|---|
| Iris | `#934CBB` | `#582B72` | 4.38:1 | 1.99:1 |
| **Woad** | `#14588A` | `#09283F` | 6.26:1 | 2.01:1 |
| Verdigris | `#2A7F77` | `#1A4D49` | 3.97:1 | 2.00:1 |
| Moss | `#42702B` | `#253F18` | 4.87:1 | 1.99:1 |
| Madder | `#852E21` | `#3B150F` | 7.25:1 | 1.85:1 |
| **Orpiment** | `#AB7B0E` | `#6E5009` | 3.14:1 | 1.98:1 |

Fixed colours:

| Role | Value | Notes |
|---|---|---|
| Dark ink | `#EDEAE2` | Warm off-white — not pure white, matching Material's dark-theme convention of avoiding harsh full-brightness text |
| Dark paper | `#16140F` | Warm near-black — not pure black, same reasoning |

Dark ink on dark paper is **15.31:1**. Worst separation between any two palettes, any vision
type: **30.7** (above the 30.0 threshold, same as light mode's).

### Why this needed its own search, not just an inverted light palette

Ink and paper swap roles — dark mode's ink is light, its paper is dark — so a quadrant fill's
binding contrast constraint moves to the *lighter* shade of each pair (the one closest to a
now-light ink), not the darker one light mode checked. That shade is also the one the search
optimises CVD separation over, since it's what ends up closest to `DARK_PAPER` and so hardest
to tell apart from another palette's.

**Near-black has less room than near-white.** Light mode's darker shades could range up to
luminance 0.37; dark mode's lighter shades are capped near 0.26 by `DARK_INK`'s own luminance
(`contrast(light_shade, DARK_INK) >= 3.0` bounds it directly) — chroma compresses harder
approaching black than approaching white, a real colorimetric effect, not a modelling choice.
The first grid tried (mirroring light mode's 10-point spacing) landed at 19.2 separation,
well under threshold; widening the grid to use the full available headroom up to that ~0.26
ceiling, then adding finer spacing near where good solutions clustered, found 30.7 — a genuine
search result, not a threshold relaxed to fit.

### What doesn't change

The *hue and saturation* per palette are identical to light mode (`HUES` is imported, not
redefined) — a palette should still read as "the same colour" switching themes, just at a
different luminance. Export (PNG/PDF) always renders the light-mode palette regardless of the
device's theme, on the same reasoning a printed page doesn't have a "dark mode": T6.1/T6.2's
whole point was a fixed, print-ready rendering, not one that varies with a settings toggle.
