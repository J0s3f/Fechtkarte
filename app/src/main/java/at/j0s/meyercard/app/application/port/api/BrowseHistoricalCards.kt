package at.j0s.meyercard.app.application.port.api

import at.j0s.meyercard.app.domain.HistoricalDrill
import at.j0s.meyercard.app.domain.MeyerCard

/**
 * The Library screen's two browsable groups (docs/PLAN.md §7): the 44
 * historical drills, and the 21 technique cards (89-109).
 */
interface BrowseHistoricalCards {
    suspend fun drills(): List<HistoricalDrill>
    suspend fun techniqueCards(): List<MeyerCard>
}
