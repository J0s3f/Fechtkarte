"""Design and verify Fechtkarte's dark-mode card palette (T7.4, docs/NEXT_STEPS.md Phase 7).

Same six hues, same colour science, same constraint thresholds as
tools/palette/verify_palette.py -- reused from there rather than duplicated. What changes: ink
and paper swap roles (dark mode's ink is light, its paper is dark), so a quadrant fill's
binding contrast constraint is against the *lighter* shade of each pair, not the darker one --
the search runs over that shade's luminance instead, on a correspondingly darker grid, since a
fill now has to read clearly against a light ink and a near-black background rather than a dark
ink and a near-white one.

Run:  python tools/palette/verify_dark_palette.py
Exits non-zero if any constraint is violated, so it can be wired into CI alongside the light
palette's own check.
"""

import itertools
import sys

from verify_palette import (
    HUES,
    DEFAULT_RIGHT,
    DEFAULT_LEFT,
    PAIR_CONTRAST,
    MIN_INK_ON_FILL,
    MIN_PAIR_CONTRAST,
    MIN_HAND_GREYSCALE,
    MIN_SEPARATION,
    VISION,
    luminance,
    contrast,
    shade,
    simulate,
    separation,
    as_hex,
)

# Material dark-theme convention: not pure black/white -- a near-black warm surface and a
# near-white warm text colour, matching the light mode's own restraint (INK=1A1A1A, not pure
# black; PAPER=FAF7F0, not pure white).
DARK_INK = (0xED, 0xEA, 0xE2)     # light warm off-white -- rays, outlines, numerals
DARK_PAPER = (0x16, 0x14, 0x0F)   # near-black warm -- disc fill

# The lighter shade of each dark-mode pair is the one that sits closest to (light) DARK_INK,
# so it's the binding contrast constraint -- the search runs over it, up to the ~0.26 ceiling
# DARK_INK's own luminance allows (contrast(light_shade, DARK_INK) >= 3.0), using the full
# available headroom -- near-black colours have far less room to spread apart than near-white
# ones do (chroma compresses as luminance approaches either extreme, but black crushes harder),
# so light mode's LUMINANCE_GRID ceiling of 0.37 isn't available to mirror here.
LUMINANCE_GRID = [0.03, 0.05, 0.07, 0.09, 0.11, 0.13, 0.15, 0.17, 0.19, 0.21, 0.23, 0.25]
DARK_SHADE_FLOOR = 0.015   # keeps the darker quadrant from crushing into indistinguishable black


def build(hue, saturation, light_luminance):
    """The mirror of verify_palette.build(): there, the search variable is the darker shade
    (closest to a dark ink) and the lighter one is derived; here it's the other way round."""
    dark_luminance = max((light_luminance + 0.05) / PAIR_CONTRAST - 0.05, DARK_SHADE_FLOOR)
    return (shade(hue, saturation, light_luminance), shade(hue, saturation, dark_luminance))


def is_usable(light, dark):
    return (contrast(light, DARK_INK) >= MIN_INK_ON_FILL
            and contrast(light, dark) >= MIN_PAIR_CONTRAST)


def search():
    """Assign a distinct light-shade luminance to each hue, maximising the worst separation
    between the DARKER shade of any two palettes -- that's the pair a user would actually
    struggle to tell apart against DARK_PAPER, not the lighter, more-visible one."""
    index = {name: i for i, (name, _, _) in enumerate(HUES)}
    cells = {}
    for i, (_, hue, saturation) in enumerate(HUES):
        for target in LUMINANCE_GRID:
            light, dark = build(hue, saturation, target)
            cells[(i, target)] = (light, dark, is_usable(light, dark))

    distances = {}
    for first, second in itertools.combinations(cells, 2):
        if first[0] == second[0]:
            continue
        distances[(first, second)] = min(
            separation(cells[first][1], cells[second][1], deficiency) for deficiency in VISION
        )

    def distance(first, second):
        return distances[(first, second)] if (first, second) in distances \
            else distances[(second, first)]

    best = None
    for assignment in itertools.permutations(LUMINANCE_GRID, len(HUES)):
        if not all(cells[(i, target)][2] for i, target in enumerate(assignment)):
            continue
        right = cells[(index[DEFAULT_RIGHT], assignment[index[DEFAULT_RIGHT]])][1]
        left = cells[(index[DEFAULT_LEFT], assignment[index[DEFAULT_LEFT]])][1]
        if contrast(right, left) < MIN_HAND_GREYSCALE:
            continue
        worst = min(distance((i, assignment[i]), (j, assignment[j]))
                    for i, j in itertools.combinations(range(len(HUES)), 2))
        if best is None or worst > best[0]:
            best = (worst, assignment)

    if best is None:
        return None, None
    worst, assignment = best
    return worst, [(HUES[i][0],) + cells[(i, target)][:2]
                   for i, target in enumerate(assignment)]


def main():
    worst, palettes = search()
    if palettes is None:
        print("No luminance assignment satisfies every constraint -- widen LUMINANCE_GRID,")
        print("adjust DARK_INK/DARK_PAPER, or relax a threshold.")
        return 1

    failures = []
    print("Fechtkarte dark-mode palette\n")
    print("%-10s %-9s %-9s | ink on fill | light/dark" % ("palette", "light", "dark"))
    print("-" * 60)
    for name, light, dark in palettes:
        ink_on_fill = contrast(light, DARK_INK)
        pair = contrast(light, dark)
        print("%-10s %-9s %-9s |    %5.2f:1  |   %4.2f:1"
              % (name, as_hex(light), as_hex(dark), ink_on_fill, pair))
        if ink_on_fill < MIN_INK_ON_FILL:
            failures.append("%s: ink on fill %.2f:1" % (name, ink_on_fill))
        if pair < MIN_PAIR_CONTRAST:
            failures.append("%s: light/dark %.2f:1" % (name, pair))

    by_name = {name: (light, dark) for name, light, dark in palettes}
    right, left = by_name[DEFAULT_RIGHT][1], by_name[DEFAULT_LEFT][1]

    print("\nDefault hand pair, %s (right) / %s (left) -- darker shade" % (DEFAULT_RIGHT, DEFAULT_LEFT))
    print("  greyscale contrast %.2f:1" % contrast(right, left))
    for deficiency in VISION:
        print("  %-13s separation %5.1f" % (deficiency, separation(right, left, deficiency)))
    if contrast(right, left) < MIN_HAND_GREYSCALE:
        failures.append("default hand pair greyscale %.2f:1" % contrast(right, left))

    print("\nClosest palette pair (darker shade), per vision type")
    for deficiency in VISION:
        closest = min(itertools.combinations(palettes, 2),
                      key=lambda p: separation(p[0][2], p[1][2], deficiency))
        value = separation(closest[0][2], closest[1][2], deficiency)
        print("  %-13s %-24s %5.1f"
              % (deficiency, "%s/%s" % (closest[0][0], closest[1][0]), value))

    print("\nWorst separation between any two palettes, any vision type: %.1f" % worst)
    print("Ink on paper (numeral on disc): %.2f:1" % contrast(DARK_INK, DARK_PAPER))
    if worst < MIN_SEPARATION:
        failures.append("worst separation %.1f below %.1f" % (worst, MIN_SEPARATION))

    print("\nDark ink:   %s" % as_hex(DARK_INK))
    print("Dark paper: %s" % as_hex(DARK_PAPER))

    if failures:
        print("\nFAILED:")
        for failure in failures:
            print("  - " + failure)
        return 1
    print("\nAll constraints satisfied.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
