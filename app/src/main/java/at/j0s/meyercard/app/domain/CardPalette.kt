package at.j0s.meyercard.app.domain

/**
 * The card palette — six light/dark pairs, designed and verified against
 * WCAG contrast and colour-vision-deficiency separation (see
 * DESIGN_CHOICES.md D4/D6 and docs/PALETTE.md).
 * `Long` ARGB rather than `androidx.compose.ui.graphics.Color`, so the
 * domain stays free of Compose.
 *
 * [light]/[dark] are the light-theme quadrant shades; [darkThemeLight]/[darkThemeDark] are a
 * *separately verified* dark-theme pair (T7.4, docs/PALETTE.md "Dark mode") — not a filter or
 * an inversion applied to the light-theme values, since ink and paper swap which end of the
 * luminance scale they sit at, which shifts where the whole search has to look for
 * distinguishable hues. Which pair a card actually renders with is
 * [at.j0s.meyercard.app.adapter.ui.render.CardRenderer]'s call, not this enum's.
 */
enum class CardPalette(val light: Long, val dark: Long, val darkThemeLight: Long, val darkThemeDark: Long) {
    IRIS(0xFFBB8FD4L, 0xFF934CBBL, 0xFF934CBBL, 0xFF582B72L),
    WOAD(0xFFD3E9F9L, 0xFF58ABE7L, 0xFF14588AL, 0xFF09283FL),
    VERDIGRIS(0xFF3EB9AFL, 0xFF2A7F77L, 0xFF2A7F77L, 0xFF1A4D49L),
    MOSS(0xFF7CC05AL, 0xFF4F8733L, 0xFF42702BL, 0xFF253F18L),
    MADDER(0xFFF3D5D0L, 0xFFDD8476L, 0xFF852E21L, 0xFF3B150FL),
    ORPIMENT(0xFFEBAB13L, 0xFFA4770DL, 0xFFAB7B0EL, 0xFF6E5009L);

    companion object {
        /** The app's default hand pairing: blue/amber, colour-vision-deficiency-safe. */
        val DEFAULT_RIGHT = WOAD
        val DEFAULT_LEFT = ORPIMENT

        /** Technique cards (`Hand.NEUTRAL`) aren't handedness-specific; teal reads as neither. */
        val DEFAULT_NEUTRAL = VERDIGRIS

        /** Rays, disc outlines, numerals. */
        const val INK = 0xFF1A1A1AL

        /** Disc fill. */
        const val PAPER = 0xFFFAF7F0L

        /** Dark-theme rays, disc outlines, numerals — a warm off-white, not pure white. */
        const val DARK_THEME_INK = 0xFFEDEAE2L

        /** Dark-theme disc fill — a warm near-black, not pure black. */
        const val DARK_THEME_PAPER = 0xFF16140FL

        /**
         * The palette a card gets before the player has chosen one for its hand —
         * palette choice is a persisted per-hand preference (F4), not part of the
         * historical dataset itself.
         */
        fun default(hand: Hand): CardPalette = when (hand) {
            Hand.RIGHT -> DEFAULT_RIGHT
            Hand.LEFT -> DEFAULT_LEFT
            Hand.NEUTRAL -> DEFAULT_NEUTRAL
        }
    }
}
