package com.ladderwidget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class LadderScreenStateTest {
    @Test
    fun `state identifies the user selected team and retains every entry`() {
        val snapshot = LadderSnapshot(
            entries = listOf(entry("Team One", 1), entry("Team Two", 2), entry("Team Three", 3)),
            fetchedAt = Instant.EPOCH,
        )

        val state = LadderScreenState.from(
            snapshot = snapshot,
            selectedTeamName = "Team Two",
            isRefreshing = false,
            error = null,
        )

        assertEquals("Team Two", state.ownEntry?.teamName)
        assertEquals(3, state.entries.size)
        assertEquals("Updated", state.statusText)
    }

    @Test
    fun `state with no snapshot exposes loading copy`() {
        val state = LadderScreenState.from(null, selectedTeamName = null, isRefreshing = true, error = null)

        assertNull(state.ownEntry)
        assertEquals("Loading leaderboard...", state.statusText)
    }

    private fun entry(name: String, rank: Int) = LadderEntry(
        rank = rank,
        teamName = name,
        rating = 1000.0 - rank,
        matchesPlayed = rank,
    )
}
