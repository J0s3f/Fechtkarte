package at.j0s.meyercard.app.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class RadiusTest {

    @ParameterizedTest
    @ValueSource(floats = [-0.001f, -1f, 1.001f, 2f])
    @DisplayName("construction outside 0f..1f throws")
    fun `construction outside range throws`(value: Float) {
        assertThrows(IllegalArgumentException::class.java) { Radius(value) }
    }

    @ParameterizedTest
    @ValueSource(floats = [0f, 1f])
    @DisplayName("the boundary values 0 and 1 are accepted")
    fun `boundary values are accepted`(value: Float) {
        assertEquals(value, Radius(value).value)
    }

    @Test
    @DisplayName("OUTER matches the value measured in the historical dataset")
    fun `OUTER matches its measured value`() {
        // Radii cluster at t=0.75 in the historical dataset (outer, 410 of 542 actions).
        assertEquals(0.75f, Radius.OUTER.value)
    }

    @Test
    @DisplayName("INNER is far enough from OUTER that no two generator slots' discs can overlap")
    fun `INNER leaves no two generator slots overlapping`() {
        // Not the dataset's t=0.5 cluster value (99 of 542 actions) — moved to 0.32 (T8) once
        // that value turned out to let the two shortest rays' (E, W) OUTER and INNER discs
        // overlap outright. See CardRendererGeometryTest for the
        // actual pairwise geometry check across all 16 generator slots; this just pins the
        // constant itself so a future edit here doesn't silently reopen that bug.
        assertEquals(0.32f, Radius.INNER.value)
    }

    @Test
    @DisplayName("CENTRE is zero")
    fun `CENTRE is zero`() {
        assertEquals(0f, Radius.CENTRE.value)
    }
}
