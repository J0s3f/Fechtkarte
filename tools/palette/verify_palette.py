"""Design and verify the Fechtkarte card palette.

Every palette is one hue rendered at two luminance steps: a light and a dark shade for
the quadrant checkerboard. The luminance assignment across palettes is chosen by search,
maximising the worst-case separation between any two palettes under normal vision and
under the three dichromacies -- so a user cannot pick a left/right pair they are unable
to tell apart.

Run:  python tools/palette/verify_palette.py
Exits non-zero if any constraint is violated, so it can be wired into CI.
"""

import colorsys
import functools
import itertools
import math
import sys

INK = (0x1A, 0x1A, 0x1A)     # rays, disc outlines, numerals
PAPER = (0xFA, 0xF7, 0xF0)   # disc fill

# Hue and saturation per palette; luminance is assigned by the search below.
HUES = [
    ("Iris", 278, 0.45),
    ("Woad", 205, 0.75),
    ("Verdigris", 175, 0.50),
    ("Moss", 100, 0.45),
    ("Madder", 8, 0.60),
    ("Orpiment", 42, 0.85),
]

DEFAULT_RIGHT = "Woad"
DEFAULT_LEFT = "Orpiment"

LUMINANCE_GRID = [0.13, 0.15, 0.17, 0.19, 0.21, 0.24, 0.27, 0.30, 0.33, 0.37]

PAIR_CONTRAST = 2.0          # light shade vs dark shade, within one palette
LIGHT_SHADE_CAP = 0.80       # keep the disc from vanishing into a near-white quadrant

MIN_INK_ON_FILL = 3.0        # WCAG 1.4.11, graphical objects
MIN_PAIR_CONTRAST = 1.7      # checkerboard stays visible, including in greyscale
MIN_HAND_GREYSCALE = 1.6     # the hand signal must survive a greyscale print
MIN_SEPARATION = 30.0        # RGB distance between any two palettes, any vision type

VISION = ["normal", "deuteranopia", "protanopia", "tritanopia"]


def _linear(channel):
    c = channel / 255.0
    return c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4


def luminance(rgb):
    r, g, b = (_linear(c) for c in rgb)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def contrast(a, b):
    high, low = max(luminance(a), luminance(b)), min(luminance(a), luminance(b))
    return (high + 0.05) / (low + 0.05)


def _from_hsl(hue, saturation, lightness):
    r, g, b = colorsys.hls_to_rgb(hue / 360.0, lightness, saturation)
    return tuple(round(c * 255) for c in (r, g, b))


@functools.lru_cache(maxsize=None)
def shade(hue, saturation, target_luminance):
    """The colour at this hue and saturation whose relative luminance is the target."""
    low, high = 0.0, 1.0
    for _ in range(60):
        mid = (low + high) / 2
        if luminance(_from_hsl(hue, saturation, mid)) < target_luminance:
            low = mid
        else:
            high = mid
    return _from_hsl(hue, saturation, (low + high) / 2)


def _to_srgb(value):
    value = max(0.0, min(1.0, value))
    return round(255 * (12.92 * value if value <= 0.0031308
                        else 1.055 * value ** (1 / 2.4) - 0.055))


@functools.lru_cache(maxsize=None)
def simulate(rgb, deficiency):
    """Dichromacy simulation, Vienot et al. (1999) linear approximation."""
    r, g, b = (_linear(c) for c in rgb)
    long_ = 17.8824 * r + 43.5161 * g + 4.11935 * b
    medium = 3.45565 * r + 27.1554 * g + 3.86714 * b
    short = 0.0299566 * r + 0.184309 * g + 1.46709 * b
    if deficiency == "deuteranopia":
        medium = 0.494207 * long_ + 1.24827 * short
    elif deficiency == "protanopia":
        long_ = 2.02344 * medium - 2.52581 * short
    elif deficiency == "tritanopia":
        short = -0.395913 * long_ + 0.801109 * medium
    return tuple(_to_srgb(v) for v in (
        0.080944 * long_ - 0.130504 * medium + 0.116721 * short,
        -0.0102485 * long_ + 0.0540194 * medium - 0.113615 * short,
        -0.000365294 * long_ - 0.00412163 * medium + 0.693513 * short,
    ))


def separation(a, b, deficiency):
    if deficiency != "normal":
        a, b = simulate(a, deficiency), simulate(b, deficiency)
    return math.sqrt(sum((x - y) ** 2 for x, y in zip(a, b)))


def worst_separation(a, b):
    return min(separation(a, b, deficiency) for deficiency in VISION)


def as_hex(rgb):
    return "#%02X%02X%02X" % rgb


def build(hue, saturation, dark_luminance):
    light_luminance = min(PAIR_CONTRAST * (dark_luminance + 0.05) - 0.05, LIGHT_SHADE_CAP)
    return (shade(hue, saturation, light_luminance), shade(hue, saturation, dark_luminance))


def is_usable(light, dark):
    return (contrast(dark, INK) >= MIN_INK_ON_FILL
            and contrast(light, dark) >= MIN_PAIR_CONTRAST)


def search():
    """Assign a distinct dark luminance to each hue, maximising the worst separation."""
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
        distances[(first, second)] = worst_separation(cells[first][1], cells[second][1])

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

    worst, assignment = best
    return worst, [(HUES[i][0],) + cells[(i, target)][:2]
                   for i, target in enumerate(assignment)]


def main():
    worst, palettes = search()
    failures = []

    print("Fechtkarte palette\n")
    print("%-10s %-9s %-9s | ink on fill | light/dark" % ("palette", "light", "dark"))
    print("-" * 60)
    for name, light, dark in palettes:
        ink_on_fill = contrast(dark, INK)
        pair = contrast(light, dark)
        print("%-10s %-9s %-9s |    %5.2f:1  |   %4.2f:1"
              % (name, as_hex(light), as_hex(dark), ink_on_fill, pair))
        if ink_on_fill < MIN_INK_ON_FILL:
            failures.append("%s: ink on fill %.2f:1" % (name, ink_on_fill))
        if pair < MIN_PAIR_CONTRAST:
            failures.append("%s: light/dark %.2f:1" % (name, pair))

    by_name = {name: (light, dark) for name, light, dark in palettes}
    right, left = by_name[DEFAULT_RIGHT][1], by_name[DEFAULT_LEFT][1]

    print("\nDefault hand pair, %s (right) / %s (left)" % (DEFAULT_RIGHT, DEFAULT_LEFT))
    print("  greyscale contrast %.2f:1" % contrast(right, left))
    for deficiency in VISION:
        print("  %-13s separation %5.1f" % (deficiency, separation(right, left, deficiency)))
    if contrast(right, left) < MIN_HAND_GREYSCALE:
        failures.append("default hand pair greyscale %.2f:1" % contrast(right, left))

    print("\nClosest palette pair, per vision type")
    for deficiency in VISION:
        closest = min(itertools.combinations(palettes, 2),
                      key=lambda p: separation(p[0][2], p[1][2], deficiency))
        value = separation(closest[0][2], closest[1][2], deficiency)
        print("  %-13s %-24s %5.1f"
              % (deficiency, "%s/%s" % (closest[0][0], closest[1][0]), value))

    print("\nWorst separation between any two palettes, any vision type: %.1f" % worst)
    print("Ink on paper (numeral on disc): %.2f:1" % contrast(INK, PAPER))
    if worst < MIN_SEPARATION:
        failures.append("worst separation %.1f below %.1f" % (worst, MIN_SEPARATION))

    if failures:
        print("\nFAILED:")
        for failure in failures:
            print("  - " + failure)
        return 1
    print("\nAll constraints satisfied.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
