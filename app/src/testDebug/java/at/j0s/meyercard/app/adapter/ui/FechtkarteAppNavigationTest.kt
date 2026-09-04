package at.j0s.meyercard.app.adapter.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.j0s.meyercard.app.R
import at.j0s.meyercard.app.application.port.api.BrowseHistoricalCards
import at.j0s.meyercard.app.application.port.api.ExportCard
import at.j0s.meyercard.app.application.port.api.ShareCard
import at.j0s.meyercard.app.application.port.spi.ExportResult
import at.j0s.meyercard.app.application.port.spi.PreferencesStore
import at.j0s.meyercard.app.application.port.spi.ShareableCard
import at.j0s.meyercard.app.domain.CardLineStyle
import at.j0s.meyercard.app.domain.GenerationPreferences
import at.j0s.meyercard.app.domain.HistoricalDrill
import at.j0s.meyercard.app.domain.MeyerCard
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** No drills/technique cards needed — the routes under test here never render an individual card's content. */
private object FakeBrowseHistoricalCards : BrowseHistoricalCards {
    override suspend fun drills(): List<HistoricalDrill> = emptyList()
    override suspend fun techniqueCards(): List<MeyerCard> = emptyList()
}

private class FakePreferencesStore(initial: GenerationPreferences = GenerationPreferences()) : PreferencesStore {
    private var preferences = initial
    override suspend fun load(): GenerationPreferences = preferences
    override suspend fun save(preferences: GenerationPreferences) {
        this.preferences = preferences
    }
}

/** Never actually invoked by these tests — no test here clicks Save/Share — so failing loudly beats a silent stub. */
private object FakeExportCard : ExportCard {
    override suspend fun asPng(card: MeyerCard, lineStyle: CardLineStyle): ExportResult = error("not exercised by navigation tests")
    override suspend fun asPdf(card: MeyerCard, lineStyle: CardLineStyle): ExportResult = error("not exercised by navigation tests")
}

private object FakeShareCard : ShareCard {
    override suspend fun prepare(card: MeyerCard, lineStyle: CardLineStyle): ShareableCard = error("not exercised by navigation tests")
}

/**
 * [FechtkarteApp] wires together six routes behind a three-item bottom nav bar; nothing until now
 * exercised that wiring itself — every other UI test in this project renders one screen at a time
 * in isolation (see e.g. [at.j0s.meyercard.app.adapter.ui.train.TrainScreenScreenshotTest]), which
 * proves each screen renders correctly but never that tapping a nav item, or a button that
 * pushes a second destination, actually gets you there. Semantic-tree assertions rather than
 * Roborazzi screenshots, deliberately: this is behaviour (which screen is showing, which nav item
 * reads as selected), not layout, so a pixel golden would be the wrong tool and would need
 * re-recording on every unrelated visual change to any of the six screens involved.
 *
 * `performScrollTo()` before clicking/asserting on the Learn screen's Sources/Notices buttons and
 * on Configure's "Generation rules" section (the same pattern [at.j0s.meyercard.app.adapter.ui
 * .sources.SourcesScreenScreenshotTest] already uses): Robolectric's default window is only
 * 320x470px, well short of these screens' full scrollable content, and a node scrolled out of
 * that viewport can't actually receive a synthetic click — confirmed with `onRoot().printToLog()`
 * against the real Robolectric semantics tree before reaching for this fix, not guessed at.
 */
@RunWith(AndroidJUnit4::class)
class FechtkarteAppNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setApp() {
        composeTestRule.setContent {
            FechtkarteApp(
                browseHistoricalCards = FakeBrowseHistoricalCards,
                preferencesStore = FakePreferencesStore(),
                exportCard = FakeExportCard,
                shareCard = FakeShareCard,
            )
        }
    }

    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `library is the start destination`() {
        setApp()
        composeTestRule.onNodeWithText(context.getString(R.string.nav_library)).assertIsSelected()
        composeTestRule.onNodeWithText(context.getString(R.string.library_tab_drills)).assertIsDisplayed()
    }

    @Test
    fun `tapping Train in the bottom nav shows the Train screen`() {
        setApp()
        composeTestRule.onNodeWithText(context.getString(R.string.nav_train)).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.configure)).assertIsDisplayed()
    }

    @Test
    fun `tapping Learn in the bottom nav shows the Learn screen`() {
        setApp()
        composeTestRule.onNodeWithText(context.getString(R.string.nav_learn)).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.learn_about_title)).assertIsDisplayed()
    }

    @Test
    fun `Configure is reached from Train, not from the bottom nav directly`() {
        setApp()
        composeTestRule.onNodeWithText(context.getString(R.string.nav_train)).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.configure)).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.configure_generation_rules)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `the Train nav item stays selected while Configure is showing on top of it`() {
        // The whole point of Configure being pushed onto Train rather than being a fourth peer
        // destination (see FechtkarteApp.kt's own doc comment): the bottom nav shouldn't look
        // like it lost track of where the user is just because a sub-screen is showing.
        setApp()
        composeTestRule.onNodeWithText(context.getString(R.string.nav_train)).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.configure)).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.nav_train)).assertIsSelected()
    }

    @Test
    fun `Sources is reached from Learn`() {
        setApp()
        composeTestRule.onNodeWithText(context.getString(R.string.nav_learn)).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.learn_sources)).performScrollTo().performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.sources_title)).assertIsDisplayed()
    }

    @Test
    fun `Notices is reached from Learn`() {
        setApp()
        composeTestRule.onNodeWithText(context.getString(R.string.nav_learn)).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.learn_open_source_notices)).performScrollTo().performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.notices_title)).assertIsDisplayed()
    }
}
