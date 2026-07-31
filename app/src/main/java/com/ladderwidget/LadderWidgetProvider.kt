package com.ladderwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class LadderWidgetProvider : AppWidgetProvider() {
    override fun onEnabled(context: Context) {
        scheduleRefresh(context)
        refreshNow(context)
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        scheduleRefresh(context)
        updateWidgets(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) refreshNow(context)
    }

    companion object {
        const val ACTION_REFRESH = "com.ladderwidget.REFRESH"
        private const val WORK_NAME = "leaderboard-periodic-refresh"

        fun scheduleRefresh(context: Context) {
            val request = PeriodicWorkRequestBuilder<LadderRefreshWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun refreshNow(context: Context) {
            WorkManager.getInstance(context).enqueue(LadderRefreshWorker.oneTimeRequest())
        }

        fun updateWidgets(context: Context, error: String? = null) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, LadderWidgetProvider::class.java))
            val snapshot = LadderRepository(context).cached()
            ids.forEach { id -> manager.updateAppWidget(id, views(context, snapshot, error)) }
        }

        private fun views(context: Context, snapshot: LadderSnapshot?, error: String?): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.ladder_widget)
            val refreshIntent = Intent(context, LadderWidgetProvider::class.java).setAction(ACTION_REFRESH)
            views.setOnClickPendingIntent(
                R.id.widget_refresh,
                PendingIntent.getBroadcast(context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE),
            )
            val leaderNameIds = intArrayOf(R.id.leader1_name, R.id.leader2_name, R.id.leader3_name)
            val leaderRatingIds = intArrayOf(R.id.leader1_rating, R.id.leader2_rating, R.id.leader3_rating)
            if (snapshot == null) {
                views.setTextViewText(R.id.own_rank, "—")
                views.setTextViewText(R.id.widget_team, "Loading leaderboard...")
                views.setTextViewText(R.id.widget_summary, error ?: "Tap refresh to update")
                repeat(3) { index ->
                    views.setTextViewText(leaderNameIds[index], "—")
                    views.setTextViewText(leaderRatingIds[index], "")
                }
                views.setTextViewText(R.id.widget_updated, "Live data")
                return views
            }
            val selectedTeamName = TeamPreferences(context).selectedTeamName()
            val selectedTeam = snapshot.entries.firstOrNull { it.teamName.equals(selectedTeamName, ignoreCase = true) }
            views.setTextViewText(R.id.own_rank, selectedTeam?.rank?.toString() ?: "—")
            views.setTextViewText(R.id.widget_team, selectedTeam?.teamName ?: "Choose a team in the app")
            views.setTextViewText(
                R.id.widget_summary,
                selectedTeam?.let { "${it.rating.toInt()} rating · ${it.matchesPlayed} matches" } ?: "Open the app to select a team",
            )
            snapshot.entries.take(3).forEachIndexed { index, entry ->
                views.setTextViewText(leaderNameIds[index], entry.teamName)
                views.setTextViewText(leaderRatingIds[index], entry.rating.toInt().toString())
            }
            val time = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(snapshot.fetchedAt)
            views.setTextViewText(R.id.widget_updated, error?.let { "Updated $time · network error" } ?: "Updated $time · live")
            return views
        }
    }
}
