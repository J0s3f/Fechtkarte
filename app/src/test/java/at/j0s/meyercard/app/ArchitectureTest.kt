package at.j0s.meyercard.app

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Enforces the hexagonal boundary D1 and docs/PLAN.md §3 require, rather than
 * relying on discipline. See docs/NEXT_STEPS.md T1.11.
 */
class ArchitectureTest {

    @Test
    @DisplayName("no class in domain imports android.* or androidx.*")
    fun `no class in domain imports android or androidx`() {
        Konsist.scopeFromPackage("at.j0s.meyercard.app.domain..")
            .files
            .assertFalse { file ->
                file.hasImport { import ->
                    import.name.startsWith("android.") || import.name.startsWith("androidx.")
                }
            }
    }

    @Test
    @DisplayName("no class in domain imports from adapter")
    fun `no class in domain imports from adapter`() {
        Konsist.scopeFromPackage("at.j0s.meyercard.app.domain..")
            .files
            .assertFalse { file ->
                file.hasImport { import ->
                    import.name.startsWith("at.j0s.meyercard.app.adapter.")
                }
            }
    }

    /**
     * Added once real code landed in `application.port.api`,
     * `application.port.spi` and `application.service` alike (T3.3) — before
     * that, Konsist's `assertArchitecture` throws `KoPreconditionFailedException:
     * Layer ... doesn't contain any files` for any declared layer matching zero
     * files, by design (it is almost always a typo'd package pattern, not a
     * legitimate empty layer). See docs/NEXT_STEPS.md T1.11 and T3.3.
     */
    @Test
    @DisplayName("application.service depends on ports, not on adapters")
    fun `application service depends on ports not adapters`() {
        Konsist.scopeFromPackage("at.j0s.meyercard.app.application.service..")
            .files
            .assertFalse { file ->
                file.hasImport { import ->
                    import.name.startsWith("at.j0s.meyercard.app.adapter.")
                }
            }
    }
}
