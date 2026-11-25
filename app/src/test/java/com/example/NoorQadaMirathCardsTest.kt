package com.example

import com.example.domain.cards.IslamicCardCategory
import com.example.domain.cards.IslamicCardStudio
import com.example.domain.mirath.DeceasedGender
import com.example.domain.mirath.MirathAdjustmentType
import com.example.domain.mirath.MirathCalculatorEngine
import com.example.domain.mirath.MirathInput
import com.example.domain.qada.QadaPrayerType
import com.example.domain.qada.QadaState
import com.example.domain.qada.QadaTrackerEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoorQadaMirathCardsTest {

    @Test
    fun qadaEngine_addPeriodAndCompleteDay_updatesExactCountsAndEta() {
        val initial = QadaState(dailyPacePrayers = 5)
        // Add 1 month + 5 days = 35 days across 5 fard prayers = 175 total prayers
        val afterPeriod = QadaTrackerEngine.addMissedPeriod(
            current = initial,
            years = 0,
            months = 1,
            days = 5,
            includeWitr = false
        )
        assertEquals(175, afterPeriod.totalRemaining)
        assertEquals(35, afterPeriod.entries[QadaPrayerType.FAJR]?.remaining)
        assertEquals(0, afterPeriod.entries[QadaPrayerType.WITR]?.remaining)
        assertEquals(35, afterPeriod.estimatedDaysToFinish)

        // Complete 1 full day (decrements 5 fard prayers by 1 each)
        val afterDay = QadaTrackerEngine.completeFullDay(afterPeriod, includeWitr = false)
        assertEquals(170, afterDay.totalRemaining)
        assertEquals(5, afterDay.totalCompleted)
        assertEquals(34, afterDay.entries[QadaPrayerType.FAJR]?.remaining)
        assertEquals(1, afterDay.entries[QadaPrayerType.FAJR]?.completed)

        // Mark 1 Fajr prayed
        val afterOne = QadaTrackerEngine.markOnePrayed(afterDay, QadaPrayerType.FAJR)
        assertEquals(33, afterOne.entries[QadaPrayerType.FAJR]?.remaining)
        assertEquals(2, afterOne.entries[QadaPrayerType.FAJR]?.completed)
        assertEquals(169, afterOne.totalRemaining)
    }

    @Test
    fun mirathEngine_wifeMotherSonTwoDaughters_distributesExactShares() {
        // Estate: 240,000
        // Wife: 1/8 = 30,000 (12.5%)
        // Mother: 1/6 = 40,000 (16.6667%)
        // Remainder = 170,000 split 2:1:1 (4 units) -> Son = 85,000, 2 Daughters = 85,000 (42,500 each)
        val result = MirathCalculatorEngine.calculate(
            MirathInput(
                totalEstate = 240_000.0,
                deceasedGender = DeceasedGender.MALE,
                hasSpouse = true,
                wivesCount = 1,
                hasMother = true,
                sonsCount = 1,
                daughtersCount = 2
            )
        )

        assertEquals(MirathAdjustmentType.EXACT, result.adjustmentType)
        val wife = result.shares.first { it.key == "wife" }
        val mother = result.shares.first { it.key == "mother" }
        val son = result.shares.first { it.key == "sons" }
        val daughters = result.shares.first { it.key == "daughters_asabah" }

        assertEquals(30_000.0, wife.totalAmount, 0.01)
        assertEquals(40_000.0, mother.totalAmount, 0.01)
        assertEquals(85_000.0, son.totalAmount, 0.01)
        assertEquals(85_000.0, daughters.totalAmount, 0.01)
        assertEquals(42_500.0, daughters.perPersonAmount, 0.01)
        assertEquals(100.0, result.shares.sumOf { it.sharePercentage }, 0.01)
    }

    @Test
    fun mirathEngine_umariyyataynCase_givesMotherOneThirdOfRemainder() {
        // Deceased Female: Husband (1/2) + Mother (1/3 of remainder = 1/6) + Father (remainder = 1/3)
        val result = MirathCalculatorEngine.calculate(
            MirathInput(
                totalEstate = 60_000.0,
                deceasedGender = DeceasedGender.FEMALE,
                hasSpouse = true,
                hasMother = true,
                hasFather = true
            )
        )

        assertTrue(result.isUmariyyataynCase)
        assertEquals(30_000.0, result.shares.first { it.key == "husband" }.totalAmount, 0.01)
        assertEquals(10_000.0, result.shares.first { it.key == "mother" }.totalAmount, 0.01)
        assertEquals(20_000.0, result.shares.first { it.key == "father_asabah" }.totalAmount, 0.01)
    }

    @Test
    fun mirathEngine_awlCase_husbandAndTwoFullSisters_scalesProportionally() {
        // Husband = 1/2 (3/6), 2 Full Sisters = 2/3 (4/6) -> Sum 7/6 -> Awl to 3/7 & 4/7
        val result = MirathCalculatorEngine.calculate(
            MirathInput(
                totalEstate = 70_000.0,
                deceasedGender = DeceasedGender.FEMALE,
                hasSpouse = true,
                fullSistersCount = 2
            )
        )

        assertEquals(MirathAdjustmentType.AWL, result.adjustmentType)
        assertEquals(30_000.0, result.shares.first { it.key == "husband" }.totalAmount, 0.01)
        assertEquals(40_000.0, result.shares.first { it.key == "full_sisters" }.totalAmount, 0.01)
        assertEquals(20_000.0, result.shares.first { it.key == "full_sisters" }.perPersonAmount, 0.01)
    }

    @Test
    fun mirathEngine_raddCase_wifeAndSingleDaughter_returnsRemainderToDaughter() {
        // Wife = 1/8 (10,000), Daughter = 1/2 + Radd (7/8 = 70,000)
        val result = MirathCalculatorEngine.calculate(
            MirathInput(
                totalEstate = 80_000.0,
                deceasedGender = DeceasedGender.MALE,
                hasSpouse = true,
                wivesCount = 1,
                daughtersCount = 1
            )
        )

        assertEquals(MirathAdjustmentType.RADD, result.adjustmentType)
        assertEquals(10_000.0, result.shares.first { it.key == "wife" }.totalAmount, 0.01)
        assertEquals(70_000.0, result.shares.first { it.key == "daughters" }.totalAmount, 0.01)
    }

    @Test
    fun mirathEngine_wasiyyahExceedingOneThird_isAutomaticallyCappedAndHijabApplied() {
        val result = MirathCalculatorEngine.calculate(
            MirathInput(
                totalEstate = 120_000.0,
                debts = 30_000.0, // Net after debts = 90,000 -> Max Wasiyyah = 30,000
                wasiyyah = 50_000.0, // Exceeds 1/3 -> capped to 30,000 -> Distributable = 60,000
                sonsCount = 1,
                fullBrothersCount = 2,
                hasMother = true,
                hasGrandmother = true
            )
        )

        assertTrue(result.wasiyyahCappedAtOneThird)
        assertEquals(30_000.0, result.allowedWasiyyah, 0.01)
        assertEquals(60_000.0, result.distributableEstate, 0.01)
        assertTrue(result.blockedHeirs.any { it.nameEn == "Full Brothers" })
        assertTrue(result.blockedHeirs.any { it.nameEn == "Grandmother" })
    }

    @Test
    fun islamicCardStudio_hasLuxuryThemesAndAllFourCategories() {
        assertEquals(6, IslamicCardStudio.themes.size)
        IslamicCardCategory.entries.forEach { category ->
            val items = IslamicCardStudio.templates.filter { it.category == category }
            assertTrue("Category $category should have templates", items.isNotEmpty())
            assertTrue(items.all { it.bodyAr.isNotBlank() && it.subtitleEn.isNotBlank() && it.subtitleUz.isNotBlank() })
        }
    }
}
