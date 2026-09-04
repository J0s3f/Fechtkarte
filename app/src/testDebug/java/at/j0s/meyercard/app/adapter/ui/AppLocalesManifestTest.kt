package at.j0s.meyercard.app.adapter.ui

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * PrimeTestLab report M-01: choosing "Deutsch" in Configure's `LanguagePicker` neither applied
 * immediately nor survived reopening the app, on a real API 31 device. Root cause: AppCompat's
 * `AppLocalesMetadataHolderService` manifest declaration -- required to opt an app into
 * automatic locale storage/application on API < 33 -- was missing entirely; without it,
 * `AppCompatDelegate.setApplicationLocales()` only updates its own in-memory getter, with no
 * activity recreation and no persistence across restarts. Checked against Robolectric's real,
 * manifest-backed `PackageManager` (which parses the actual `AndroidManifest.xml`), not by
 * re-reading the XML as a string -- a typo in the declaration would still pass a string search
 * but fail this.
 */
@RunWith(RobolectricTestRunner::class)
class AppLocalesManifestTest {

    @Test
    fun `the app opts into AppCompat's per-app language backport for API less than 33`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val component = ComponentName(context, "androidx.appcompat.app.AppLocalesMetadataHolderService")
        val serviceInfo = context.packageManager.getServiceInfo(
            component,
            PackageManager.GET_META_DATA or PackageManager.MATCH_DISABLED_COMPONENTS,
        )

        assertTrue(
            "expected AppLocalesMetadataHolderService's autoStoreLocales meta-data to be true",
            serviceInfo.metaData?.getBoolean("autoStoreLocales") == true,
        )
    }
}
