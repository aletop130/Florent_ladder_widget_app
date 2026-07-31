package com.ladderwidget

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val repository by lazy { LadderRepository(this) }
    private val teamPreferences by lazy { TeamPreferences(this) }
    private lateinit var content: LinearLayout
    private lateinit var refreshControl: TextView
    private var snapshot: LadderSnapshot? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(createScreen())
        LadderWidgetProvider.scheduleRefresh(this)
        snapshot = repository.cached()
        render(isRefreshing = true, error = null)
        refreshLeaderboard()
    }

    private fun createScreen(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(20, 22, 42))
            setPadding(dp(20), dp(20), dp(20), 0)
        }
        root.addView(createHeader())
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply {
            isFillViewport = true
            addView(content, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        return root
    }

    private fun createHeader(): View = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        orientation = LinearLayout.HORIZONTAL
        addView(label("LIVE LADDER", 16f, Color.rgb(255, 194, 74), bold = true).apply { letterSpacing = 0.12f },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        refreshControl = label("REFRESH", 12f, Color.WHITE, bold = true).apply {
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.refresh_chip)
            contentDescription = getString(R.string.refresh)
            setOnClickListener { refreshLeaderboard() }
        }
        addView(refreshControl, LinearLayout.LayoutParams(dp(94), dp(38)))
    }

    private fun render(isRefreshing: Boolean, error: String?) {
        refreshControl.isEnabled = !isRefreshing
        refreshControl.alpha = if (isRefreshing) 0.55f else 1f
        content.removeAllViews()
        val selectedTeamName = teamPreferences.selectedTeamName()
        if (selectedTeamName == null) {
            renderTeamPicker(isRefreshing, error)
            return
        }
        val state = LadderScreenState.from(snapshot, selectedTeamName, isRefreshing, error)
        content.addView(label(state.statusText, 12f, Color.rgb(185, 190, 208)).apply { setPadding(0, dp(14), 0, dp(10)) })
        content.addView(createSelectedTeamCard(state.ownEntry, selectedTeamName))
        content.addView(createAction("CHANGE TEAM") { renderTeamPicker(isRefreshing = false, error = null) })
        content.addView(sectionLabel("FULL LEADERBOARD"))
        state.entries.forEach { content.addView(createEntryRow(it, it == state.ownEntry)) }
    }

    private fun renderTeamPicker(isRefreshing: Boolean, error: String?) {
        content.addView(ImageView(this).apply {
            setImageResource(R.drawable.robot_logo)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            contentDescription = "Robot logo"
            setPadding(0, dp(22), 0, dp(12))
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(160)))
        content.addView(label("CHOOSE YOUR TEAM", 22f, Color.rgb(244, 245, 249), bold = true))
        content.addView(label("Select your team to personalize the leaderboard and widget.", 14f, Color.rgb(185, 190, 208)).apply {
            setPadding(0, dp(7), 0, dp(12))
        })
        when {
            snapshot == null && isRefreshing -> content.addView(label("Loading teams...", 14f, Color.rgb(185, 190, 208)))
            snapshot == null -> content.addView(label(error ?: "Unable to load teams. Tap refresh to try again.", 14f, Color.rgb(255, 194, 74)))
            else -> snapshot!!.entries.forEach { entry ->
                content.addView(createAction(entry.teamName) {
                    teamPreferences.selectTeam(entry.teamName)
                    LadderWidgetProvider.updateWidgets(this)
                    render(isRefreshing = false, error = null)
                })
            }
        }
    }

    private fun createSelectedTeamCard(entry: LadderEntry?, selectedTeamName: String): View = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        orientation = LinearLayout.HORIZONTAL
        setBackgroundResource(R.drawable.hero_background)
        setPadding(dp(14), dp(14), dp(14), dp(14))
        addView(label(entry?.rank?.toString() ?: "—", 24f, Color.rgb(20, 22, 42), bold = true).apply {
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.chip_accent)
        }, LinearLayout.LayoutParams(dp(52), dp(52)))
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), 0, 0, 0)
            addView(label(entry?.teamName ?: selectedTeamName, 20f, Color.rgb(244, 245, 249), bold = true))
            addView(label(entry?.let { "${it.rating.toInt()} rating · ${it.matchesPlayed} matches" } ?: "Team not currently on the leaderboard", 13f, Color.rgb(185, 190, 208)).apply {
                setPadding(0, dp(3), 0, 0)
            })
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
    }

    private fun createAction(text: String, action: () -> Unit): View = label(text, 14f, Color.rgb(237, 238, 244), bold = true).apply {
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(14), dp(14), dp(14), dp(14))
        setBackgroundResource(R.drawable.refresh_chip)
        setOnClickListener { action() }
    }.also { view ->
        view.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) }
    }

    private fun sectionLabel(text: String) = label(text, 11f, Color.rgb(126, 130, 150), bold = true).apply {
        letterSpacing = 0.16f
        setPadding(0, dp(22), 0, dp(6))
    }

    private fun createEntryRow(entry: LadderEntry, isSelectedTeam: Boolean): View = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        orientation = LinearLayout.HORIZONTAL
        setPadding(dp(12), dp(10), dp(12), dp(10))
        if (isSelectedTeam) setBackgroundResource(R.drawable.hero_background)
        addView(label(entry.rank.toString(), 14f, if (isSelectedTeam) Color.rgb(255, 194, 74) else Color.rgb(185, 190, 208), bold = true).apply {
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(dp(34), dp(30)))
        addView(label(entry.teamName, 16f, Color.rgb(237, 238, 244), bold = isSelectedTeam).apply {
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        addView(label("${entry.rating.toInt()}\n${entry.matchesPlayed} matches", 12f, Color.rgb(255, 217, 138), bold = true).apply {
            gravity = Gravity.END
        }, LinearLayout.LayoutParams(dp(82), LinearLayout.LayoutParams.WRAP_CONTENT))
    }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(3) }
    }

    private fun refreshLeaderboard() {
        render(isRefreshing = true, error = null)
        Thread {
            val result = runCatching { repository.fetchAndCache() }
            runOnUiThread {
                snapshot = result.getOrNull() ?: repository.cached()
                render(isRefreshing = false, error = result.exceptionOrNull()?.message)
                LadderWidgetProvider.updateWidgets(this, result.exceptionOrNull()?.message)
            }
        }.start()
    }

    private fun label(text: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
