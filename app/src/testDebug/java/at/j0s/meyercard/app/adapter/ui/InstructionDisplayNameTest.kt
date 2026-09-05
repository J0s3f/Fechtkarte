package at.j0s.meyercard.app.adapter.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import at.j0s.meyercard.app.domain.Instruction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Every [Instruction] maps to its own distinct wording — only [Instruction.MOULINET] was exercised anywhere before this (via a real rendered card fixture elsewhere). */
@RunWith(RobolectricTestRunner::class)
class InstructionDisplayNameTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `every instruction has its own non-blank display name`() {
        val names = Instruction.entries.associateWith { it.displayName(context.resources) }

        names.values.forEach { name -> assertNotEquals("", name.trim()) }
        assertEquals(Instruction.entries.size, names.values.toSet().size)
    }
}
