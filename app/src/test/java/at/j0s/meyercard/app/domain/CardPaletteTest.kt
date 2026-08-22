package at.j0s.meyercard.app.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Values must match docs/PALETTE.md exactly — that document is generated and
 * verified by tools/palette/verify_palette.py; this is the one place those
 * numbers get typed in as Kotlin, so a transcription slip here is invisible
 * to the Python verifier and needs its own check.
 */
class CardPaletteTest {

    @Test
    @DisplayName("all six palettes match the verified values in PALETTE.md")
    fun `all six palettes match PALETTE md`() {
        assertEquals(0xFFBB8FD4L to 0xFF934CBBL, CardPalette.IRIS.light to CardPalette.IRIS.dark)
        assertEquals(0xFFD3E9F9L to 0xFF58ABE7L, CardPalette.WOAD.light to CardPalette.WOAD.dark)
        assertEquals(
            0xFF3EB9AFL to 0xFF2A7F77L,
            CardPalette.VERDIGRIS.light to CardPalette.VERDIGRIS.dark
        )
        assertEquals(0xFF7CC05AL to 0xFF4F8733L, CardPalette.MOSS.light to CardPalette.MOSS.dark)
        assertEquals(
            0xFFF3D5D0L to 0xFFDD8476L,
            CardPalette.MADDER.light to CardPalette.MADDER.dark
        )
        assertEquals(
            0xFFEBAB13L to 0xFFA4770DL,
            CardPalette.ORPIMENT.light to CardPalette.ORPIMENT.dark
        )
    }

    @Test
    @DisplayName("default hand pair is Woad (right) and Orpiment (left)")
    fun `default hand pair is Woad and Orpiment`() {
        assertEquals(CardPalette.WOAD, CardPalette.DEFAULT_RIGHT)
        assertEquals(CardPalette.ORPIMENT, CardPalette.DEFAULT_LEFT)
    }

    @Test
    @DisplayName("ink and paper match PALETTE.md")
    fun `ink and paper match PALETTE md`() {
        assertEquals(0xFF1A1A1AL, CardPalette.INK)
        assertEquals(0xFFFAF7F0L, CardPalette.PAPER)
    }

    @Test
    @DisplayName("all six dark-theme palettes match the verified values in PALETTE.md")
    fun `all six dark-theme palettes match PALETTE md`() {
        assertEquals(0xFF934CBBL to 0xFF582B72L, CardPalette.IRIS.darkThemeLight to CardPalette.IRIS.darkThemeDark)
        assertEquals(0xFF14588AL to 0xFF09283FL, CardPalette.WOAD.darkThemeLight to CardPalette.WOAD.darkThemeDark)
        assertEquals(
            0xFF2A7F77L to 0xFF1A4D49L,
            CardPalette.VERDIGRIS.darkThemeLight to CardPalette.VERDIGRIS.darkThemeDark,
        )
        assertEquals(0xFF42702BL to 0xFF253F18L, CardPalette.MOSS.darkThemeLight to CardPalette.MOSS.darkThemeDark)
        assertEquals(
            0xFF852E21L to 0xFF3B150FL,
            CardPalette.MADDER.darkThemeLight to CardPalette.MADDER.darkThemeDark,
        )
        assertEquals(
            0xFFAB7B0EL to 0xFF6E5009L,
            CardPalette.ORPIMENT.darkThemeLight to CardPalette.ORPIMENT.darkThemeDark,
        )
    }

    @Test
    @DisplayName("dark-theme ink and paper match PALETTE.md")
    fun `dark-theme ink and paper match PALETTE md`() {
        assertEquals(0xFFEDEAE2L, CardPalette.DARK_THEME_INK)
        assertEquals(0xFF16140FL, CardPalette.DARK_THEME_PAPER)
    }
}
