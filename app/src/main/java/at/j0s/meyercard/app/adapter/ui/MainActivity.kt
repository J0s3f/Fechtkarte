package at.j0s.meyercard.app.adapter.ui

import android.graphics.Typeface
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import at.j0s.meyercard.app.R
import at.j0s.meyercard.app.adapter.export.FileProviderCardShare
import at.j0s.meyercard.app.adapter.export.MediaStoreCardExporter
import at.j0s.meyercard.app.adapter.persistence.DataStorePreferencesStore
import at.j0s.meyercard.app.adapter.persistence.FechtkarteDatabase
import at.j0s.meyercard.app.adapter.persistence.RoomCardRepository
import at.j0s.meyercard.app.adapter.persistence.preferencesDataStore
import at.j0s.meyercard.app.adapter.persistence.readOriginalCardsAsset
import at.j0s.meyercard.app.adapter.ui.theme.FechtkarteTheme
import at.j0s.meyercard.app.application.port.api.BrowseHistoricalCards
import at.j0s.meyercard.app.application.port.api.ExportCard
import at.j0s.meyercard.app.application.port.api.ShareCard
import at.j0s.meyercard.app.application.port.spi.PreferencesStore
import at.j0s.meyercard.app.application.service.BrowseHistoricalCardsService
import at.j0s.meyercard.app.application.service.ExportCardService
import at.j0s.meyercard.app.application.service.ShareCardService

/**
 * Entry point. Composition root: builds the real adapters, then hands off to [FechtkarteApp].
 *
 * [installSplashScreen] must run before [onCreate]'s own `super` call — it reads the activity's
 * theme (`Theme.Fechtkarte.Starting`, `res/values/themes.xml`) to know what to show, then swaps
 * to `postSplashScreenTheme` once installed. No custom keep-on-screen condition (T7.1): startup
 * work here is local Room/DataStore reads with no network involved, fast enough that the
 * library defaults (dismiss once the first frame draws) are the right amount of splash, not a
 * loading screen in disguise.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val context = applicationContext
        val browseHistoricalCards: BrowseHistoricalCards = BrowseHistoricalCardsService(
            RoomCardRepository(FechtkarteDatabase.create(context).historicalCardDao()) {
                context.readOriginalCardsAsset()
            },
        )
        val preferencesStore: PreferencesStore = DataStorePreferencesStore(context.preferencesDataStore)

        // Loaded once here, not per-export — the same typeface MeyerSquareCard loads via
        // ResourcesCompat.getFont for the live UI (T2.3), just outside a Composable.
        val numeralTypeface = ResourcesCompat.getFont(context, R.font.unifraktur_maguntia) ?: Typeface.DEFAULT
        val exportCard: ExportCard = ExportCardService(MediaStoreCardExporter(context.contentResolver, context.resources, numeralTypeface))
        val shareCard: ShareCard = ShareCardService(FileProviderCardShare(context, numeralTypeface))

        setContent {
            FechtkarteTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    // targetSdk 36 draws edge-to-edge by default (content extends behind the
                    // status/navigation bars); without this the Library screen's top row was
                    // rendered partly behind the status bar.
                    FechtkarteApp(
                        browseHistoricalCards,
                        preferencesStore,
                        exportCard,
                        shareCard,
                        modifier = Modifier.safeDrawingPadding(),
                    )
                }
            }
        }
    }
}
