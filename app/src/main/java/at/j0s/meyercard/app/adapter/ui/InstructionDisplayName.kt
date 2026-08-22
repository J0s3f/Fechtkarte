package at.j0s.meyercard.app.adapter.ui

import android.content.res.Resources
import at.j0s.meyercard.app.R
import at.j0s.meyercard.app.domain.Instruction

/**
 * The technique banner's wording. Shared between
 * [at.j0s.meyercard.app.adapter.ui.render.CardRenderer]'s banner and the Library screen's
 * technique filter, rather than each defining its own copy of the mapping — so a filter chip
 * always reads exactly like the banner on the card it selects.
 *
 * Takes [Resources] rather than being a plain `val`, because this text is drawn *onto the card*
 * and therefore appears in exported PNG/PDF: the export carries whatever language the app is
 * set to. That's the user's own explicit choice rather than the device's, which is why the
 * language setting (T9.8) exists — a card can be exported in a deliberately chosen language
 * instead of whatever the phone happens to be configured for.
 *
 * [CardRenderer] itself takes no `Context`/`Resources` and shouldn't (it stays
 * plain-JVM-constructible so screen, PNG and PDF share one implementation), so callers resolve
 * this and pass the resulting string in — the same arrangement as the numeral typeface.
 */
fun Instruction.displayName(resources: Resources): String = resources.getString(
    when (this) {
        Instruction.DOUBLE_FEINT -> R.string.instruction_double_feint
        Instruction.MOULINET -> R.string.instruction_moulinet
        Instruction.PROVOKER_TAKER_HITTER -> R.string.instruction_provoker_taker_hitter
    },
)
