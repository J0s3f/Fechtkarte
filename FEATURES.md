# Features

User-facing capabilities, added as they ship. Per `CLAUDE.md`, an entry lands here when the
feature works end to end — not when the code compiles.

## Shipped

**A library of more than 100 classic drill cards.** Opens straight to the Library screen: a
Drills tab (44 classic drills, each viewable for either hand, filterable by action
count and thrust count) and a Techniques tab (21 technique cards, filterable by
technique). Each tab has first/previous/next/last, ±10, and a random pick.

**Random drill generation.** The Train screen generates a new drill card on demand — tap the
Generate button, tap the card itself, or shake the device. Hand (and so background colour) is
picked at random per card.

**Configurable generation.** A Configure screen (reached from Train) controls how cards are
generated: how many actions (1–8) and how many of those are thrusts, which of the 6 palettes
each hand uses, and five opt-in shaping rules — no repeated direction between consecutive
actions, a minimum angular distance between them (adjustable), alternating hands card to card,
restricting to the outer ring only, and weighting directions to match the frequencies observed
across the classic cards. All of it persists across restarts.

**Export as PNG or vector PDF, and share.** From the Train screen: Save PNG writes a
high-resolution image to the gallery; Save PDF writes a true-vector, print-ready PDF sized to
A4 to Downloads; Share hands a copy to any other app (chat, mail, notes) without saving one to
either.

**Landscape layout and a dark theme.** Every screen reflows correctly in landscape, including
the card itself, which sizes to whichever of width or height is more constraining rather than
overflowing. A system-dark-theme device gets a separately verified dark palette, not just an
inverted light one.

**Screen reader support.** A generated or classic card has no text of its own — it's pure
vector drawing — so a screen reader would otherwise announce nothing. Each card now has a
spoken description of its full action sequence ("Right hand, three actions: one, upper left
outer; two, thrust to right outer; three, bottom outer").

**A Learn screen explaining the notation**, with a live worked example: an actual rendered
card alongside a step-by-step breakdown of what each numbered action means, in the same wording
a screen reader would announce.

**An open-source notices screen** (reached from Learn), listing every runtime dependency's
licence and the numeral font's full OFL licence text.

**A real launcher icon and splash screen**, replacing the Android Studio scaffold's generic
placeholder — built from the card's own checkerboard-and-ray-star motif in the default
right-hand palette, so it reads as "this app" to anyone who has already seen a card.

**German and French, with the app language selectable independently of the device.** Every
screen, every button, and the screen-reader description of a card are translated; the language
picker in Configure defaults to following the device but can be set explicitly, and on Android
13+ the same choice appears in Android's own per-app language settings. The language names in
the picker are each written in their own language, so a wrong choice is always recoverable.

**A verifiable release, signed by the same key wherever you get it.** The release APK is
reproducible: F-Droid rebuilds it from the tagged source and publishes it only if their build
matches the APK on GitHub Releases byte for byte, so two independent parties vouch for what
you install. Because F-Droid then ships this project's signature rather than its own, an
install from GitHub updates cleanly from F-Droid and vice versa — no uninstall, no data loss.

**The running version, visible in Configure.** A small "Version X.Y.Z" line at the bottom of the
Configure screen, so it's always possible to confirm which build is actually installed — useful
when troubleshooting whether an update really took effect.

**A choice of card line style.** A "Card lines" dropdown in Configure (under a new "Card
appearance" section), with two options today. **Compass** (the default) keeps every card's
familiar compass rose — two diagonals plus the vertical and horizontal centre lines, edge to
edge, unchanged. **Sequence** replaces it with a single connected line through the action
badges in strike order — 1 to 2, 2 to 3, and so on — the way the Meyer Square's own numbering is
meant to be read. Applies everywhere a card is shown or saved — on screen (Train and the
Library), in a saved PNG or PDF, and when shared to another app — and persists across restarts
like the rest of Configure. A PNG/PDF's filename reflects which style it was saved with, so
exporting the same card under both styles keeps both files instead of one overwriting the
other. The Learn screen's worked example always shows the compass, regardless of the setting
(see `docs/LINE_STYLE_DESIGN.md`).

**Exported and shared files carry their own metadata.** Every PNG or PDF saved from Train, and
every card shared to another app, embeds the same content code used in the filename, which
version of Fechtkarte produced it, and a link to fechtkarte.j0s.at — so the file identifies
itself even after being renamed, copied, or sent somewhere the app that made it never gets
uninstalled from. Readable with any metadata viewer (a PNG's `Software`/`Comment`/`URL` text
chunks, a PDF's Author/Subject/Creator/Producer).

This section stays honest: a feature lands here when it works end to end, not when the code
compiles.

## Planned

- A "Home" screen with its own title treatment, beyond the launcher label — Library is still
  the start destination
- A Sources screen crediting Joachim Meyer's manuscripts
