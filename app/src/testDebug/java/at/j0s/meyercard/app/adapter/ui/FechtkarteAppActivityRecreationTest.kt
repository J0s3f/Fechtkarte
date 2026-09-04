package at.j0s.meyercard.app.adapter.ui

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.j0s.meyercard.app.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Found on a real device: switching the in-app language regenerated Train's active card, since
 * that switch is implemented as an `AppCompatDelegate`-triggered activity recreation, and the
 * card was only `remember`ed — which survives recomposition and navigation within the same
 * activity instance (see [FechtkarteAppNavigationTest]'s own Train round-trip test) but not an
 * actual activity recreation.
 *
 * Doesn't touch `AppCompatDelegate`/locales directly here — a real `Activity#recreate()`
 * exercises the exact same save/restore path *any* activity recreation goes through, which is
 * the actual mechanism [encodeTrainCard]/[decodeTrainCard]/`rememberSaveable` depend on,
 * regardless of what triggers it. `createAndroidComposeRule`, not the plain `createComposeRule`
 * every other UI test in this project uses: only this one hosts content in a real, recreatable
 * `ComponentActivity` rather than a lightweight test root.
 */
@RunWith(AndroidJUnit4::class)
class FechtkarteAppActivityRecreationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    private val store = FakePreferencesStore()

    private fun setApp() {
        composeTestRule.setContent {
            FechtkarteApp(
                browseHistoricalCards = FakeBrowseHistoricalCards,
                preferencesStore = store,
                exportCard = FakeExportCard,
                shareCard = FakeShareCard,
            )
        }
    }

    @Test
    fun `Train's active card survives an activity recreation`() {
        setApp()
        composeTestRule.onNodeWithText(context.getString(R.string.nav_train)).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.configure)).assertIsDisplayed()
        val countAfterFirstEntry = store.loadCallCount

        composeTestRule.activityRule.scenario.recreate()
        // The test activity has no onCreate() of its own to call setContent() again the way the
        // real MainActivity does after every recreation (including the real, AppCompatDelegate-
        // triggered one this is standing in for) -- this reattaches the same content the same
        // way, onto the freshly recreated activity instance.
        setApp()

        // "Configure" being displayed again alone wouldn't prove much -- a freshly *regenerated*
        // card would show it too. The load-call count staying flat is what actually proves the
        // same card survived: regenerate() (and so preferencesStore.load()) only runs when
        // Train's card is null, and it would be again here if the recreation had wiped it.
        composeTestRule.onNodeWithText(context.getString(R.string.configure)).assertIsDisplayed()
        assertEquals(countAfterFirstEntry, store.loadCallCount)
    }
}
