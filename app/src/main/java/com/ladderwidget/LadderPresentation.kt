package com.ladderwidget

enum class RankTreatment { GOLD, SILVER, BRONZE, ACCENT }

enum class RatingTier(val colorHex: String) {
    LEGENDARY_GRANDMASTER("#FF5C7A"),
    GRANDMASTER("#DC2626"),
    MASTER("#EF4444"),
    CANDIDATE_MASTER("#2563EB"),
    DIAMOND("#0EA5E9"),
    EMERALD("#10B981"),
    GOLD("#CA8A04"),
    SILVER("#94A3B8"),
    BRONZE("#6B3410"),
}

/** Temporary on-device palette preview; keep false in the published app. */
const val SHOWCASE_ALL_RATING_COLORS = false

fun ratingTierFor(rating: Double): RatingTier = when {
    rating >= 3000 -> RatingTier.LEGENDARY_GRANDMASTER
    rating >= 2500 -> RatingTier.GRANDMASTER
    rating >= 2300 -> RatingTier.MASTER
    rating >= 2100 -> RatingTier.CANDIDATE_MASTER
    rating >= 1900 -> RatingTier.DIAMOND
    rating >= 1700 -> RatingTier.EMERALD
    rating >= 1500 -> RatingTier.GOLD
    rating >= 1300 -> RatingTier.SILVER
    else -> RatingTier.BRONZE
}

fun displayedRatingTierFor(rating: Double, rank: Int): RatingTier =
    if (SHOWCASE_ALL_RATING_COLORS) RatingTier.entries[(rank - 1) % RatingTier.entries.size] else ratingTierFor(rating)

fun rankTreatmentFor(rank: Int?): RankTreatment = when (rank) {
    1 -> RankTreatment.GOLD
    2 -> RankTreatment.SILVER
    3 -> RankTreatment.BRONZE
    else -> RankTreatment.ACCENT
}
