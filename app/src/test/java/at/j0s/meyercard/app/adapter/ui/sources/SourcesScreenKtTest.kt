package at.j0s.meyercard.app.adapter.ui.sources

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** Pure arithmetic, no Android dependency -- plain JVM test rather than Robolectric. */
class SourcesScreenKtTest {

    @Test
    @DisplayName("a source already at or below the target width needs no downsampling")
    fun `no downsampling needed when source is already small enough`() {
        assertEquals(1, sampleSizeForWidth(sourceWidth = 800, targetWidth = 800))
        assertEquals(1, sampleSizeForWidth(sourceWidth = 400, targetWidth = 800))
    }

    @Test
    @DisplayName("picks the largest power-of-two factor that keeps the result at or above the target")
    fun `picks the largest valid power-of-two factor`() {
        // 1200 / 2 = 600 (< 800, too small) so inSampleSize must stay 1 here...
        assertEquals(1, sampleSizeForWidth(sourceWidth = 1200, targetWidth = 800))
        // ...but 1200 / 2 = 600 >= 320, and 1200 / 4 = 300 < 320, so 2 is the largest valid factor.
        assertEquals(2, sampleSizeForWidth(sourceWidth = 1200, targetWidth = 320))
        // 4800 / 4 = 1200 >= 1080, and 4800 / 8 = 600 < 1080, so 4 is the largest valid factor.
        assertEquals(4, sampleSizeForWidth(sourceWidth = 4800, targetWidth = 1080))
    }
}
