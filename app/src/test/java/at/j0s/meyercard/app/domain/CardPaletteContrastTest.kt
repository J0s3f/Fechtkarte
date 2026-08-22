package at.j0s.meyercard.app.domain

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.math.pow

/**
 * Independently verifies the constraints `tools/palette/verify_palette.py` (light theme) and
 * `tools/palette/verify_dark_palette.py` (dark theme, T7.4) designed the palette against — a
 * second check that the *values* are sound, not just that they were transcribed correctly
 * (that's `CardPaletteTest`). If someone hand-edits a hex value later without rerunning the
 * Python tool, this is what catches it. WCAG relative-luminance math ported line-for-line from
 * the Python verifiers; see docs/PALETTE.md for the full design rationale and thresholds.
 */
class CardPaletteContrastTest {

    private fun linear(channel: Int): Double {
        val c = channel / 255.0
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(argb: Long): Double {
        val r = ((argb shr 16) and 0xFF).toInt()
        val g = ((argb shr 8) and 0xFF).toInt()
        val b = (argb and 0xFF).toInt()
        return 0.2126 * linear(r) + 0.7152 * linear(g) + 0.0722 * linear(b)
    }

    private fun contrast(a: Long, b: Long): Double {
        val luminanceA = luminance(a)
        val luminanceB = luminance(b)
        val higher = maxOf(luminanceA, luminanceB)
        val lower = minOf(luminanceA, luminanceB)
        return (higher + 0.05) / (lower + 0.05)
    }

    @ParameterizedTest
    @EnumSource(CardPalette::class)
    @DisplayName("dark shade against ink meets the WCAG 1.4.11 graphical-object threshold (>= 3:1)")
    fun `dark shade against ink meets the graphical-object threshold`(palette: CardPalette) {
        val ratio = contrast(palette.dark, CardPalette.INK)
        assertTrue(ratio >= 3.0, "${palette.name}: ink-on-fill contrast $ratio below 3:1")
    }

    @ParameterizedTest
    @EnumSource(CardPalette::class)
    @DisplayName("light and dark shades stay distinguishable, including in greyscale (>= 1.7:1)")
    fun `light and dark shades stay distinguishable`(palette: CardPalette) {
        val ratio = contrast(palette.light, palette.dark)
        assertTrue(ratio >= 1.7, "${palette.name}: light/dark contrast $ratio below 1.7:1")
    }

    @Test
    @DisplayName("ink on paper clears WCAG AA for text (>= 4.5:1)")
    fun `ink on paper clears WCAG AA for text`() {
        val ratio = contrast(CardPalette.INK, CardPalette.PAPER)
        assertTrue(ratio >= 4.5, "ink-on-paper contrast $ratio below 4.5:1")
    }

    @ParameterizedTest
    @EnumSource(CardPalette::class)
    @DisplayName("dark-theme light shade against dark-theme ink meets the graphical-object threshold (>= 3:1)")
    fun `dark-theme light shade against dark-theme ink meets the graphical-object threshold`(palette: CardPalette) {
        val ratio = contrast(palette.darkThemeLight, CardPalette.DARK_THEME_INK)
        assertTrue(ratio >= 3.0, "${palette.name}: dark-theme ink-on-fill contrast $ratio below 3:1")
    }

    @ParameterizedTest
    @EnumSource(CardPalette::class)
    @DisplayName("dark-theme light and dark shades stay distinguishable (>= 1.7:1)")
    fun `dark-theme light and dark shades stay distinguishable`(palette: CardPalette) {
        val ratio = contrast(palette.darkThemeLight, palette.darkThemeDark)
        assertTrue(ratio >= 1.7, "${palette.name}: dark-theme light/dark contrast $ratio below 1.7:1")
    }

    @Test
    @DisplayName("dark-theme ink on dark-theme paper clears WCAG AA for text (>= 4.5:1)")
    fun `dark-theme ink on dark-theme paper clears WCAG AA for text`() {
        val ratio = contrast(CardPalette.DARK_THEME_INK, CardPalette.DARK_THEME_PAPER)
        assertTrue(ratio >= 4.5, "dark-theme ink-on-paper contrast $ratio below 4.5:1")
    }
}
