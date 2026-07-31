package com.ladderwidget

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

data class LadderEntry(
    val rank: Int,
    val teamName: String,
    val rating: Double,
    val matchesPlayed: Int,
)

data class LadderSnapshot(val entries: List<LadderEntry>, val fetchedAt: Instant)

/** Public endpoint that does not require authentication. */
class LadderRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun fetchAndCache(): LadderSnapshot {
        val connection = (URL(LADDER_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/json")
        }
        try {
            check(connection.responseCode in 200..299) { "HTTP ${connection.responseCode}" }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val array = JSONArray(body)
            val entries = buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        LadderEntry(
                            rank = index + 1,
                            teamName = item.getString("teamName"),
                            rating = item.getDouble("rating"),
                            matchesPlayed = item.getInt("matchesPlayed"),
                        ),
                    )
                }
            }
            val snapshot = LadderSnapshot(entries, Instant.now())
            prefs.edit().putString(CACHED_JSON, body).putLong(FETCHED_AT, snapshot.fetchedAt.toEpochMilli()).apply()
            return snapshot
        } finally {
            connection.disconnect()
        }
    }

    fun cached(): LadderSnapshot? {
        val body = prefs.getString(CACHED_JSON, null) ?: return null
        return runCatching {
            val array = JSONArray(body)
            val entries = (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                LadderEntry(index + 1, item.getString("teamName"), item.getDouble("rating"), item.getInt("matchesPlayed"))
            }
            LadderSnapshot(entries, Instant.ofEpochMilli(prefs.getLong(FETCHED_AT, 0)))
        }.getOrNull()
    }

    companion object {
        private const val LADDER_URL = "https://game.code.florent.vc/api/ladder"
        private const val PREFERENCES = "ladder_cache"
        private const val CACHED_JSON = "cached_json"
        private const val FETCHED_AT = "fetched_at"
    }
}
