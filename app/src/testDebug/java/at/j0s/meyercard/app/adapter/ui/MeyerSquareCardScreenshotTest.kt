package at.j0s.meyercard.app.adapter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.j0s.meyercard.app.domain.Action
import at.j0s.meyercard.app.domain.CardId
import at.j0s.meyercard.app.domain.CardLineStyle
import at.j0s.meyercard.app.domain.CardOrigin
import at.j0s.meyercard.app.domain.CardPalette
import at.j0s.meyercard.app.domain.Direction
import at.j0s.meyercard.app.domain.Hand
import at.j0s.meyercard.app.domain.Instruction
import at.j0s.meyercard.app.domain.MeyerCard
import at.j0s.meyercard.app.domain.Radius
import at.j0s.meyercard.app.domain.Slot
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant

/**
 * Golden screenshot tests for [MeyerSquareCard]. Roborazzi renders Compose
 * through Robolectric's native graphics mode, so these run in the container
 * with no emulator — same JVM-only promise Paparazzi made, different engine
 * underneath.
 *
 * JUnit 4, not this project's usual JUnit 5 — Robolectric's own
 * `RobolectricTestRunner` (invoked here via AndroidX's `AndroidJUnit4`
 * wrapper) only runs under `@RunWith`, which JUnit 5 doesn't have.
 * `RoborazziRule` is likewise `org.junit.rules.TestRule`. Bridged into this
 * project's otherwise-JUnit-5 test task via the Vintage engine — see the
 * `roborazzi` version comment in `gradle/libs.versions.toml` for why
 * Paparazzi was replaced with this in the first place.
 *
 * Roborazzi's own API is a bigger shape change than the library swap alone:
 * where Paparazzi took a `snapshot { Composable() }` lambda and rendered it
 * directly, Roborazzi drives a real `ComposeTestRule` — `setContent {}` to
 * render, then `captureRoboImage()` on the node to save the golden.
 *
 * `createComposeRule()` hosts content in a bare `androidx.activity.
 * ComponentActivity`, resolved through Robolectric's own
 * `ActivityScenario.launch` via an implicit MAIN/LAUNCHER intent.
 * `androidx.compose.ui:ui-test-manifest` (`debugImplementation` below)
 * supplies that activity's manifest entry — but even so, resolving it turned
 * out to depend on `android:debuggable="true"` being present in the merged
 * manifest, which only the debug build type carries. Under
 * `testReleaseUnitTest`, the identical test failed every case with
 * `RuntimeException: Unable to resolve activity for Intent`, confirmed by
 * diffing the merged testDebugUnitTest/testReleaseUnitTest manifests
 * (`debuggable="true"` was the only relevant difference). Switching to
 * Roborazzi's own `RoborazziActivity` as the host didn't fix it either — the
 * same resolution failure follows the `debuggable` flag regardless of which
 * activity class is used. So this test lives under `src/testDebug/`, not
 * `src/test/`: AGP's variant-specific unit-test source set, which only
 * `testDebugUnitTest` compiles and runs. Testing the same UI code against
 * both build types would have been redundant here anyway — isMinifyEnabled
 * is false for release, so app bytecode doesn't differ between variants.
 *
 * A screenshot test only proves the output stopped changing, not that it was
 * ever right. Before any of these goldens were accepted, `historicalCard47`
 * was checked against the recovered dataset: cards 45–88 are the
 * orange/left-hand mirrors of the blue/right-hand 1–44, and 47 falls in that
 * range. The first render used `CardPalette.WOAD` — blue, `DEFAULT_RIGHT` —
 * for a `Hand.LEFT` card, visibly contradicting the card's own hand. Caught
 * by this sanity check, not by the test itself (a hand/palette mismatch
 * isn't a type error — `MeyerCard` doesn't couple the two, since players can
 * choose any of the 7 palettes per hand per F4). Fixed to
 * `CardPalette.DEFAULT_LEFT`. See the commit message for the rendered
 * comparison.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MeyerSquareCardScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val roborazziRule = RoborazziRule(
        composeRule = composeTestRule,
        captureRoot = composeTestRule.onRoot(),
    )

    private fun action(seq: Int, direction: Direction, radius: Radius = Radius.OUTER, thrust: Boolean = false) =
        Action(seq, Slot(direction, radius), isThrust = thrust)

    private fun card(
        actions: List<Action>,
        hand: Hand = Hand.RIGHT,
        palette: CardPalette = CardPalette.DEFAULT_RIGHT,
        instruction: Instruction? = null,
    ) = MeyerCard(
        id = CardId(1L),
        actions = actions,
        hand = hand,
        palette = palette,
        instruction = instruction,
        origin = CardOrigin.Generated(Instant.EPOCH),
    )

    @Test
    fun twoActionCard() {
        composeTestRule.setContent {
            MeyerSquareCard(
                card(listOf(action(1, Direction.N), action(2, Direction.S))),
            )
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun eightActionCard() {
        composeTestRule.setContent {
            MeyerSquareCard(
                card(Direction.entries.mapIndexed { index, direction -> action(index + 1, direction) }),
            )
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    /**
     * `CardLineStyle.SEQUENCE`: the compass rose is replaced by a single line tracing 1→2→3→4
     * through the action badges in strike order, rather than the four fixed compass axes every
     * other test in this file exercises (their `lineStyle` all default to `COMPASS`).
     */
    @Test
    fun sequenceLineStyle_fourActionCard() {
        composeTestRule.setContent {
            MeyerSquareCard(
                card(listOf(action(1, Direction.W), action(2, Direction.N), action(3, Direction.E), action(4, Direction.S))),
                lineStyle = CardLineStyle.SEQUENCE,
            )
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun sequenceLineStyle_eightActionCard() {
        composeTestRule.setContent {
            MeyerSquareCard(
                card(Direction.entries.mapIndexed { index, direction -> action(index + 1, direction) }),
                lineStyle = CardLineStyle.SEQUENCE,
            )
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun cardWithThrusts() {
        composeTestRule.setContent {
            MeyerSquareCard(
                card(
                    listOf(
                        action(1, Direction.NW, thrust = true),
                        action(2, Direction.NE),
                        action(3, Direction.SE, thrust = true),
                        action(4, Direction.SW),
                    ),
                ),
            )
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun technique_cardWithInstructionBanner() {
        composeTestRule.setContent {
            MeyerSquareCard(
                card(
                    actions = listOf(action(1, Direction.N), action(2, Direction.S)),
                    hand = Hand.NEUTRAL,
                    palette = CardPalette.VERDIGRIS,
                    instruction = Instruction.MOULINET,
                ),
            )
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun rightHandPalette() {
        composeTestRule.setContent {
            MeyerSquareCard(
                card(
                    actions = listOf(action(1, Direction.NE), action(2, Direction.SW)),
                    hand = Hand.RIGHT,
                    palette = CardPalette.DEFAULT_RIGHT,
                ),
            )
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun leftHandPalette() {
        composeTestRule.setContent {
            MeyerSquareCard(
                card(
                    actions = listOf(action(1, Direction.NW), action(2, Direction.SE)),
                    hand = Hand.LEFT,
                    palette = CardPalette.DEFAULT_LEFT,
                ),
            )
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun cardOnADarkBackground() {
        composeTestRule.setContent {
            Box(modifier = Modifier.background(Color.Black).padding(24.dp)) {
                MeyerSquareCard(
                    card(listOf(action(1, Direction.N), action(2, Direction.E), action(3, Direction.S))),
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    /**
     * Not to be confused with [cardOnADarkBackground] above, which places a light-theme card
     * on a dark surrounding `Box` — this is the card's *own* colours actually switching (T7.4):
     * `@Config(qualifiers = "+night")` makes Robolectric report night mode, which is what
     * `MeyerSquareCard`'s `isSystemInDarkTheme()` call reads, so this exercises the real
     * device-dark-mode code path rather than simulating its visual effect by hand.
     */
    @Test
    @Config(qualifiers = "+night")
    fun cardInDarkTheme() {
        composeTestRule.setContent {
            MeyerSquareCard(
                card(listOf(action(1, Direction.N), action(2, Direction.E), action(3, Direction.S))),
            )
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    /**
     * Historical card 47, reproduced from its exact recovered data
     * (data/original_cards.json, id 47) rather than the idealised OUTER/INNER
     * constants — this is the sanity check against real measurements, so it
     * uses the real measured radii (0.762, 0.711, 0.75, 0.274, 0.542, 0.748),
     * not an approximation of them. Left hand, 6 actions, 1 thrust, no
     * instruction.
     */
    @Test
    fun historicalCard47() {
        composeTestRule.setContent {
            MeyerSquareCard(
                card(
                    actions = listOf(
                        action(1, Direction.NE, Radius(0.762f)),
                        action(2, Direction.W, Radius(0.711f)),
                        action(3, Direction.SE, Radius(0.750f)),
                        action(4, Direction.N, Radius(0.274f), thrust = true),
                        action(5, Direction.NE, Radius(0.542f)),
                        action(6, Direction.NW, Radius(0.748f)),
                    ),
                    hand = Hand.LEFT,
                    palette = CardPalette.DEFAULT_LEFT,
                ),
            )
        }
        composeTestRule.onRoot().captureRoboImage()
    }
}
