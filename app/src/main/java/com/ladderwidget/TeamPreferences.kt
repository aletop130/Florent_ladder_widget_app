package com.ladderwidget

import android.content.Context

class TeamPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun selectedTeamName(): String? = preferences.getString(SELECTED_TEAM_NAME, null)

    fun selectTeam(teamName: String) {
        preferences.edit().putString(SELECTED_TEAM_NAME, teamName).apply()
    }

    companion object {
        private const val PREFERENCES = "team_preferences"
        private const val SELECTED_TEAM_NAME = "selected_team_name"
    }
}
