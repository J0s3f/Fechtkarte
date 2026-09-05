package at.j0s.meyercard.app.adapter.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.j0s.meyercard.app.R
import at.j0s.meyercard.app.application.port.spi.PreferencesStore
import at.j0s.meyercard.app.domain.NoRepeatedDirection
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowToast

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

    private fun setApp(
        preferencesStore: PreferencesStore = FakePreferencesStore(),
        exportCard: FakeExportCard = FakeExportCard(),
        shareCard: FakeShareCard = FakeShareCard(),
    ) {
        composeTestRule.setContent {
            FechtkarteApp(
                browseHistoricalCards = FakeBrowseHistoricalCards,
                preferencesStore = preferencesStore,
                exportCard = exportCard,
                shareCard = shareCard,
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

    @Test
    fun `leaving Train and coming back does not silently replace the active card`() {
        // Found on a real device (PrimeTestLab report M-03): visiting Library or Learn and
        // returning to Train replaced the active drill with a freshly generated one, with no
        // tap on "Generate". regenerate() calls preferencesStore.load() exactly once per actual
        // generation, so a load count that doesn't grow across a Train -> Learn -> Train round
        // trip is direct evidence no second generation happened -- independent of the generated
        // card's own (random, unseeded) content. The baseline isn't asserted as exactly 1: Library
        // (the start destination) mounts before Train ever does and makes its own independent
        // preferencesStore.load() call for its line style, so the true baseline is 2, not 1 --
        // found the hard way, by attaching each call's own stack trace rather than guessing.
        // Learn specifically for the round trip itself, not Library again: Learn takes no
        // preferencesStore at all, so it can't add a second confound the same way.
        val store = FakePreferencesStore()
        setApp(store)
        composeTestRule.onNodeWithText(context.getString(R.string.nav_train)).performClick()
        // Waits for the card to actually finish generating and render (as opposed to a bare
        // performClick(), which doesn't guarantee that) -- matching the real repro exactly: the
        // tester was looking at an already-rendered card, not navigating away mid-generation.
        composeTestRule.onNodeWithText(context.getString(R.string.configure)).assertIsDisplayed()
        val countAfterFirstEntry = store.loadCallCount

        composeTestRule.onNodeWithText(context.getString(R.string.nav_learn)).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.nav_train)).performClick()

        assertEquals(countAfterFirstEntry, store.loadCallCount)
    }

    @Test
    fun `Train's Save PNG, Save PDF and Share buttons reach the ports they're wired to`() {
        // Never clicked in any test before this -- a coverage pass turned up TrainRoute's
        // onSavePng/onSavePdf/onShare/onGenerate lambdas (FechtkarteApp.kt) as entirely
        // unexercised. Checks that a click reaches the port, not what MediaStoreCardExporter/
        // FileProviderCardShare actually do with the card -- that's each adapter's own test's
        // job (MediaStoreCardExporterTest, MediaStoreCardExporterInstrumentedTest).
        val exportCard = FakeExportCard()
        val shareCard = FakeShareCard()
        val store = FakePreferencesStore()
        setApp(preferencesStore = store, exportCard = exportCard, shareCard = shareCard)
        composeTestRule.onNodeWithText(context.getString(R.string.nav_train)).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.configure)).assertIsDisplayed()
        val generationsSoFar = store.loadCallCount

        composeTestRule.onNodeWithText(context.getString(R.string.save_png)).performClick()
        assertEquals(1, exportCard.pngCallCount)

        composeTestRule.onNodeWithText(context.getString(R.string.save_pdf)).performClick()
        assertEquals(1, exportCard.pdfCallCount)

        composeTestRule.onNodeWithText(context.getString(R.string.share)).performClick()
        assertEquals(1, shareCard.callCount)

        composeTestRule.onNodeWithText(context.getString(R.string.generate)).performClick()
        assertEquals(generationsSoFar + 1, store.loadCallCount)
    }

    @Test
    fun `toggling a rule in Configure persists the change`() {
        // ConfigureRoute's onStateChange (FechtkarteApp.kt) -- every rule toggle, palette pick,
        // slider drag, and language choice in Configure funnels through it -- had never fired in
        // any test before this. A rule toggle is the simplest single click that reaches it.
        val store = FakePreferencesStore()
        setApp(preferencesStore = store)
        composeTestRule.onNodeWithText(context.getString(R.string.nav_train)).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.configure)).performClick()

        assertEquals(false, store.preferences.enabledRules.any { it == NoRepeatedDirection })
        // By tag, not by the label's text: RuleToggleRow's bare Row has no semantics boundary of
        // its own, so the label Text and its Switch flatten into the whole screen's single
        // sibling list rather than being grouped together -- confirmed against the real
        // semantics tree, not assumed. The switch's own testTag (its row's label, set in
        // ConfigureScreen.kt) is what actually singles it out among the six other rule switches.
        composeTestRule.onNodeWithTag(context.getString(R.string.rule_no_repeated_direction)).performScrollTo().performClick()
        assertEquals(true, store.preferences.enabledRules.any { it == NoRepeatedDirection })
    }

    @Test
    fun `turning off shake to generate in Configure persists the change`() {
        val store = FakePreferencesStore()
        setApp(preferencesStore = store)
        composeTestRule.onNodeWithText(context.getString(R.string.nav_train)).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.configure)).performClick()

        assertEquals(true, store.preferences.shakeToGenerateEnabled)
        composeTestRule.onNodeWithTag(context.getString(R.string.configure_shake_to_generate)).performScrollTo().performClick()
        assertEquals(false, store.preferences.shakeToGenerateEnabled)
    }

    @Test
    fun `a failed PNG save shows the error toast, not a crash or silence`() {
        // TrainRoute.save's own catch block -- exercised by a real export failure before this
        // (the happy path only, in the button-wiring test above).
        setApp(exportCard = FakeExportCard(shouldThrow = true))
        composeTestRule.onNodeWithText(context.getString(R.string.nav_train)).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.configure)).assertIsDisplayed()

        composeTestRule.onNodeWithText(context.getString(R.string.save_png)).performClick()

        assertEquals(context.getString(R.string.error_saving), ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun `a failed share shows the error toast, not a crash or silence`() {
        // TrainRoute.share's own catch block -- distinct from save's: it returns early instead
        // of falling through to the share-sheet Intent.
        setApp(shareCard = FakeShareCard(shouldThrow = true))
        composeTestRule.onNodeWithText(context.getString(R.string.nav_train)).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.configure)).assertIsDisplayed()

        composeTestRule.onNodeWithText(context.getString(R.string.share)).performClick()

        assertEquals(context.getString(R.string.error_sharing), ShadowToast.getTextOfLatestToast())
    }
}
