package com.ladderwidget

data class LadderScreenState(
    val ownEntry: LadderEntry?,
    val entries: List<LadderEntry>,
    val statusText: String,
    val isRefreshing: Boolean,
) {
    companion object {
        fun from(snapshot: LadderSnapshot?, selectedTeamName: String?, isRefreshing: Boolean, error: String?): LadderScreenState {
            val entries = snapshot?.entries.orEmpty()
            val statusText = when {
                error != null && entries.isEmpty() -> "Unable to refresh: $error"
                error != null -> "Saved data shown; refresh failed"
                snapshot == null && isRefreshing -> "Loading leaderboard..."
                snapshot == null -> "Refresh to load the leaderboard"
                isRefreshing -> "Refreshing..."
                else -> "Updated"
            }
            return LadderScreenState(
                ownEntry = entries.firstOrNull { it.teamName.equals(selectedTeamName, ignoreCase = true) },
                entries = entries,
                statusText = statusText,
                isRefreshing = isRefreshing,
            )
        }
    }
}
