package com.ladderwidget

enum class RankTreatment { GOLD, SILVER, BRONZE, ACCENT }

fun rankTreatmentFor(rank: Int?): RankTreatment = when (rank) {
    1 -> RankTreatment.GOLD
    2 -> RankTreatment.SILVER
    3 -> RankTreatment.BRONZE
    else -> RankTreatment.ACCENT
}
