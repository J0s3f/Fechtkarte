package at.j0s.meyercard.app.adapter.ui.sources

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Google Play's automated pre-launch feedback on 1.0.15 flagged [readSourcesScanAsset]'s
 * `BitmapFactory.decodeStream` call for decoding without any [android.graphics.BitmapFactory
 * .Options] -- full-resolution decoding that gets worse if the bundled scan is ever replaced
 * with a higher-resolution one, per Google's own wording. The bundled asset is 1200x1664; this
 * proves it's actually downsampled to the device's own display width, not decoded at full size
 * and then simply scaled down by Compose's `Image` layout (which wouldn't reduce the decoded
 * bitmap's real memory footprint at all).
 */
@RunWith(AndroidJUnit4::class)
class SourcesScreenAssetTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `the scan asset is downsampled, not decoded at its full source resolution`() {
        val bitmap = context.readSourcesScanAsset()

        val targetWidth = context.resources.displayMetrics.widthPixels
        assertTrue(
            "decoded width ${bitmap.width} should be well under the source asset's own 1200px width",
            bitmap.width < 1200,
        )
        // inSampleSize is a power of two, so the decoded size can overshoot the device's actual
        // width by up to 2x -- this isn't asserting an exact pixel match, just that the result
        // tracks the device's own size rather than some other fixed constant.
        assertTrue(
            "decoded width ${bitmap.width} should be within 2x the device's own display width $targetWidth",
            bitmap.width < targetWidth * 2,
        )
    }
}
