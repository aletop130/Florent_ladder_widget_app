package com.ladderwidget

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.PopupWindow
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val repository by lazy { LadderRepository(this) }
    private val teamPreferences by lazy { TeamPreferences(this) }
    private lateinit var content: LinearLayout
    private lateinit var refreshControl: TextView
    private lateinit var teamControl: TextView
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
            setBackgroundResource(R.drawable.app_background)
            setPadding(dp(20), dp(48), dp(20), 0)
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
        addView(label("FCODE LADDER", 16f, Color.WHITE, bold = true).apply { letterSpacing = 0.12f },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        teamControl = label("TEAM", 11f, Color.WHITE, bold = true).apply {
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.refresh_chip)
            setOnClickListener { showTeamMenu(this) }
        }
        addView(teamControl, LinearLayout.LayoutParams(dp(66), dp(38)).apply { marginEnd = dp(6) })
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
        lateinit var changeTeamControl: View
        changeTeamControl = createAction("CHANGE TEAM") { showTeamMenu(changeTeamControl, showAboveAnchor = true) }
        content.addView(changeTeamControl)
        content.addView(sectionLabel("FULL LEADERBOARD"))
        content.addView(createLeaderboard(state.entries, state.ownEntry))
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
            else -> {
                lateinit var selector: View
                selector = createAction("SELECT TEAM") { showTeamMenu(selector) }
                content.addView(selector)
            }
        }
    }

    private fun showTeamMenu(anchor: View, showAboveAnchor: Boolean = false) {
        val entries = snapshot?.entries.orEmpty()
        if (entries.isEmpty()) return
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        val popup = PopupWindow(this).apply {
            isFocusable = true
            isOutsideTouchable = true
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            elevation = dp(12).toFloat()
        }
        entries.forEach { entry ->
            list.addView(createAction("#${entry.rank}  ${entry.teamName.uppercase()}") {
                teamPreferences.selectTeam(entry.teamName)
                LadderWidgetProvider.updateWidgets(this)
                popup.dismiss()
                render(isRefreshing = false, error = null)
            })
        }
        popup.contentView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.dropdown_background)
            addView(label("SELECT YOUR TEAM", 11f, Color.WHITE, bold = true).apply {
                gravity = Gravity.CENTER_VERTICAL
                letterSpacing = 0.12f
                setPadding(dp(14), 0, dp(14), 0)
                setBackgroundColor(Color.rgb(24, 24, 24))
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(42)))
            addView(ScrollView(this@MainActivity).apply { addView(list) }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(label("TAP A TEAM TO APPLY", 10f, Color.rgb(185, 190, 208), bold = true).apply {
                gravity = Gravity.CENTER_VERTICAL
                letterSpacing = 0.1f
                setPadding(dp(14), 0, dp(14), 0)
                setBackgroundColor(Color.rgb(24, 24, 24))
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(36)))
        }
        popup.width = dp(320)
        popup.height = dp(480)
        // A separate overlay keeps the selector from moving the surrounding content.
        if (showAboveAnchor) {
            val location = IntArray(2)
            anchor.getLocationOnScreen(location)
            val y = (location[1] - popup.height - dp(8)).coerceAtLeast(dp(88))
            popup.showAtLocation(anchor.rootView, Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, y)
        } else {
            popup.showAsDropDown(anchor, -dp(254), dp(8))
        }
    }

    private fun createSelectedTeamCard(entry: LadderEntry?, selectedTeamName: String): View = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        orientation = LinearLayout.HORIZONTAL
        setBackgroundResource(R.drawable.hero_background)
        setPadding(dp(14), dp(14), dp(14), dp(14))
        addView(label(entry?.rank?.toString() ?: "—", 24f, Color.rgb(20, 22, 42), bold = true).apply {
            gravity = Gravity.CENTER
            setBackgroundResource(rankDrawable(entry?.rank))
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

    private fun createLeaderboard(entries: List<LadderEntry>, ownEntry: LadderEntry?): View = HorizontalScrollView(this).apply {
        isHorizontalScrollBarEnabled = false
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            addView(createTableHeader())
            entries.forEach { addView(createEntryRow(it, it == ownEntry)) }
        }, ViewGroup.LayoutParams(dp(760), ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun createTableHeader(): View = tableRow().apply {
        setPadding(dp(12), dp(8), dp(12), dp(8))
        addTableCell("#", 38, Color.rgb(126, 130, 150), true)
        addTableCell("RATING", 74, Color.rgb(126, 130, 150), true)
        addTableCell("TEAM", 180, Color.rgb(126, 130, 150), true)
        addTableCell("AFFILIATION", 220, Color.rgb(126, 130, 150), true)
        addTableCell("MEMBERS", 220, Color.rgb(126, 130, 150), true)
    }

    private fun createEntryRow(entry: LadderEntry, isSelectedTeam: Boolean): View = tableRow().apply {
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(12), dp(10), dp(12), dp(10))
        if (isSelectedTeam) setBackgroundResource(R.drawable.hero_background)
        addTableCell(entry.rank.toString(), 38, if (isSelectedTeam) Color.rgb(255, 85, 0) else Color.rgb(185, 190, 208), true)
        addTableCell(entry.rating.toInt().toString(), 74, Color.rgb(255, 217, 138), true)
        addTeamCell(entry, isSelectedTeam)
        addTableCell(entry.affiliationLabel, 220, Color.rgb(185, 190, 208), false)
        addTableCell(entry.membersLabel, 220, Color.rgb(237, 238, 244), false)
    }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(2) }
    }

    private fun tableRow(): LinearLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

    private fun LinearLayout.addTeamCell(entry: LadderEntry, isSelectedTeam: Boolean) {
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            addView(label(entry.teamName, 15f, Color.rgb(237, 238, 244), bold = isSelectedTeam).apply {
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            })
            val flags = listOfNotNull(entry.region, entry.studentStatus).joinToString(" · ")
            if (flags.isNotBlank()) addView(label(flags.uppercase(), 10f, Color.rgb(185, 190, 208), bold = true).apply {
                setPadding(0, dp(3), 0, 0)
            })
        }, LinearLayout.LayoutParams(dp(180), LinearLayout.LayoutParams.WRAP_CONTENT))
    }

    private fun LinearLayout.addTableCell(text: String, width: Int, color: Int, bold: Boolean, gravity: Int = Gravity.START) {
        addView(label(text, 12f, color, bold).apply {
            this.gravity = gravity or Gravity.CENTER_VERTICAL
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
        }, LinearLayout.LayoutParams(dp(width), LinearLayout.LayoutParams.WRAP_CONTENT))
    }

    private fun rankDrawable(rank: Int?): Int = when (rankTreatmentFor(rank)) {
        RankTreatment.GOLD -> R.drawable.chip_gold
        RankTreatment.SILVER -> R.drawable.chip_silver
        RankTreatment.BRONZE -> R.drawable.chip_bronze
        RankTreatment.ACCENT -> R.drawable.chip_accent
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
