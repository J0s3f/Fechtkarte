package at.j0s.meyercard.app.adapter.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs on a real device/emulator (`.github/workflows/instrumented-tests.yml`), not Robolectric:
 * [AppLocalesManifestTest] (Robolectric) only confirms `AndroidManifest.xml` declares
 * AppCompat's per-app-language backport correctly — the manifest fact behind the PrimeTestLab
 * M-01 fix — not that the actual runtime persistence mechanism that declaration turns on
 * (real `SharedPreferences`/`LocaleManager` plumbing Robolectric doesn't run) genuinely engages.
 * This closes that gap directly, reproducing M-01's own repro steps almost verbatim: choose a
 * language, then "close and reopen the app."
 */
@RunWith(AndroidJUnit4::class)
class LanguagePersistenceInstrumentedTest {

    @After
    fun resetToSystemDefault() {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
    }

    @Test
    fun chosenLanguageSurvivesClosingAndReopeningTheApp() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("de"))
            }
        }

        ActivityScenario.launch(MainActivity::class.java).use {
            val restored = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-')
            assertEquals("de", restored)
        }
    }
}
