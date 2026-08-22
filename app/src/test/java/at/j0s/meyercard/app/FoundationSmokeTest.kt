package at.j0s.meyercard.app

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Proves the test wiring is actually running on the JUnit Platform / Jupiter engine,
 * not JUnit 4 in vintage mode. `@DisplayName` and `assertThrows` are both
 * Jupiter-only — JUnit 4 has neither, so this test could not even compile against
 * the old wiring.
 */
class FoundationSmokeTest {

    @Test
    @DisplayName("JUnit 5 assertThrows captures the thrown exception")
    fun `assertThrows captures the thrown exception`() {
        val thrown = assertThrows(IllegalStateException::class.java) {
            error("expected failure")
        }
        assert(thrown.message == "expected failure")
    }
}
