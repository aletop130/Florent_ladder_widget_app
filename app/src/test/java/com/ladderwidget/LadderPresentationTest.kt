package com.ladderwidget

import org.junit.Assert.assertEquals
import org.junit.Test

class LadderPresentationTest {
    @Test
    fun `rating thresholds map to the league rank tiers`() {
        assertEquals(RatingTier.LEGENDARY_GRANDMASTER, ratingTierFor(3000.0))
        assertEquals(RatingTier.GRANDMASTER, ratingTierFor(2500.0))
        assertEquals(RatingTier.MASTER, ratingTierFor(2300.0))
        assertEquals(RatingTier.CANDIDATE_MASTER, ratingTierFor(2100.0))
        assertEquals(RatingTier.DIAMOND, ratingTierFor(1900.0))
        assertEquals(RatingTier.EMERALD, ratingTierFor(1700.0))
        assertEquals(RatingTier.GOLD, ratingTierFor(1500.0))
        assertEquals(RatingTier.SILVER, ratingTierFor(1300.0))
        assertEquals(RatingTier.BRONZE, ratingTierFor(1299.0))
    }

    @Test
    fun `podium ranks use their respective visual treatments`() {
        assertEquals(RankTreatment.GOLD, rankTreatmentFor(1))
        assertEquals(RankTreatment.SILVER, rankTreatmentFor(2))
        assertEquals(RankTreatment.BRONZE, rankTreatmentFor(3))
    }

    @Test
    fun `all other ranks use the league accent treatment`() {
        assertEquals(RankTreatment.ACCENT, rankTreatmentFor(4))
        assertEquals(RankTreatment.ACCENT, rankTreatmentFor(null))
    }

    @Test
    fun `member metadata produces unique readable affiliations`() {
        val entry = LadderEntry(
            rank = 1,
            teamName = "Orbit",
            rating = 2200.0,
            matchesPlayed = 5,
            members = listOf(
                LadderMember("Ada", affiliation = "Aalto University"),
                LadderMember("Lin", affiliation = "Aalto University"),
                LadderMember("Sam", affiliation = "KTH"),
            ),
        )

        assertEquals("Aalto University · KTH", entry.affiliationLabel)
        assertEquals("Ada · Lin · Sam", entry.membersLabel)
    }
}
