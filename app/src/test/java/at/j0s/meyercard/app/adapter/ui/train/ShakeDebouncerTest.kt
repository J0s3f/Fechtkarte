package at.j0s.meyercard.app.adapter.ui.train

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ShakeDebouncerTest {

    @Test
    @DisplayName("a magnitude below the threshold never fires")
    fun `below threshold never fires`() {
        val debouncer = ShakeDebouncer(magnitudeThreshold = 12f, minIntervalMillis = 1000L)
        assertFalse(debouncer.onAccelerationSample(5f, nowMillis = 0L))
    }

    @Test
    @DisplayName("a magnitude at or above the threshold fires")
    fun `at or above threshold fires`() {
        val debouncer = ShakeDebouncer(magnitudeThreshold = 12f, minIntervalMillis = 1000L)
        assertTrue(debouncer.onAccelerationSample(15f, nowMillis = 0L))
    }

    @Test
    @DisplayName("a second shake within the debounce window is suppressed")
    fun `a second shake within the debounce window is suppressed`() {
        val debouncer = ShakeDebouncer(magnitudeThreshold = 12f, minIntervalMillis = 1000L)
        assertTrue(debouncer.onAccelerationSample(15f, nowMillis = 0L))
        assertFalse(debouncer.onAccelerationSample(15f, nowMillis = 500L))
    }

    @Test
    @DisplayName("a shake once the debounce window has fully elapsed fires again")
    fun `a shake after the debounce window fires again`() {
        val debouncer = ShakeDebouncer(magnitudeThreshold = 12f, minIntervalMillis = 1000L)
        assertTrue(debouncer.onAccelerationSample(15f, nowMillis = 0L))
        assertTrue(debouncer.onAccelerationSample(15f, nowMillis = 1000L))
    }

    @Test
    @DisplayName("many rapid samples from one physical shake fire only once")
    fun `many rapid samples from one shake fire only once`() {
        val debouncer = ShakeDebouncer(magnitudeThreshold = 12f, minIntervalMillis = 1000L)
        val fireCount = (0..50).count { sample -> debouncer.onAccelerationSample(20f, nowMillis = sample * 10L) }
        assertEquals(1, fireCount)
    }
}
