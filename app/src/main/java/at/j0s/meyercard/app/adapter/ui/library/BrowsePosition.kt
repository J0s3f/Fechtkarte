package at.j0s.meyercard.app.adapter.ui.library

import kotlin.random.Random

/**
 * First/previous/next/last/±10/random navigation over a list's indices —
 * this app's browse controls for stepping through the card library. Holds
 * only [index] — the list size is passed in at each call rather than
 * stored, so it can never drift out of sync with a filtered list that
 * changed size elsewhere.
 */
data class BrowsePosition(val index: Int) {
    fun first() = BrowsePosition(0)
    fun last(size: Int) = BrowsePosition(lastIndex(size))
    fun next(size: Int) = BrowsePosition((index + 1).coerceAtMost(lastIndex(size)))
    fun previous() = BrowsePosition((index - 1).coerceAtLeast(0))
    fun fastForward(size: Int) = BrowsePosition((index + FAST_STEP).coerceAtMost(lastIndex(size)))
    fun fastBackward() = BrowsePosition((index - FAST_STEP).coerceAtLeast(0))

    fun random(size: Int, random: Random = Random): BrowsePosition =
        if (size == 0) this else BrowsePosition(random.nextInt(size))

    private fun lastIndex(size: Int) = (size - 1).coerceAtLeast(0)

    companion object {
        const val FAST_STEP = 10
    }
}
