package com.example

import com.example.data.local.catalog.AsmaUlHusnaCatalog
import com.example.data.local.catalog.WorshipGuidesCatalog
import com.example.domain.zakat.NisabStandard
import com.example.domain.zakat.ZakatCalculationInput
import com.example.domain.zakat.ZakatCalculatorEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoorZakatAndToolsTest {

    @Test
    fun wealthZakat_belowGoldNisab_returnsZeroDue() {
        val result = ZakatCalculatorEngine.calculateWealthZakat(
            ZakatCalculationInput(
                cashAndBank = 5000.0,
                gold24kPricePerGram = 100.0, // Nisab = 85 * 100 = 8,500
                nisabStandard = NisabStandard.GOLD_85G
            )
        )

        assertEquals(8500.0, result.nisabThreshold, 0.001)
        assertEquals(5000.0, result.netZakatableWealth, 0.001)
        assertFalse(result.isZakatDue)
        assertEquals(0.0, result.zakatDueAmount, 0.001)
    }

    @Test
    fun wealthZakat_mixedGoldCashInventoryMinusDebts_calculatesExact2Point5Percent() {
        // Pure gold: 20g (24K) + 24g (21K -> 21g pure) + 16g (18K -> 12g pure) = 53g pure
        // Gold value at $100/g = $5,300
        // Silver: 200g at $1/g = $200
        // Cash: $10,000, Trade inventory: $4,500, Receivables: $1,000 -> Total assets = $21,000
        // Immediate debts: $1,000 -> Net wealth = $20,000 (>= Nisab $8,500)
        // Zakat due (2.5%) = $500.00
        val result = ZakatCalculatorEngine.calculateWealthZakat(
            ZakatCalculationInput(
                cashAndBank = 10000.0,
                gold24kGrams = 20.0,
                gold21kGrams = 24.0,
                gold18kGrams = 16.0,
                silverGrams = 200.0,
                tradeInventory = 4500.0,
                receivables = 1000.0,
                immediateDebts = 1000.0,
                gold24kPricePerGram = 100.0,
                silverPricePerGram = 1.0,
                nisabStandard = NisabStandard.GOLD_85G
            )
        )

        assertEquals(53.0, result.equivalentPureGoldGrams, 0.001)
        assertEquals(5300.0, result.goldValue, 0.001)
        assertEquals(200.0, result.silverValue, 0.001)
        assertEquals(21000.0, result.totalAssets, 0.001)
        assertEquals(20000.0, result.netZakatableWealth, 0.001)
        assertEquals(8500.0, result.nisabThreshold, 0.001)
        assertTrue(result.isZakatDue)
        assertEquals(500.0, result.zakatDueAmount, 0.001)
    }

    @Test
    fun wealthZakat_silverNisabStandard_uses595GramsThreshold() {
        val result = ZakatCalculatorEngine.calculateWealthZakat(
            ZakatCalculationInput(
                cashAndBank = 1000.0,
                silverPricePerGram = 1.0, // Silver Nisab = 595 * 1.0 = 595
                nisabStandard = NisabStandard.SILVER_595G
            )
        )

        assertEquals(595.0, result.nisabThreshold, 0.001)
        assertTrue(result.isZakatDue)
        assertEquals(25.0, result.zakatDueAmount, 0.001)
    }

    @Test
    fun zakatAlFitr_calculatesTotalCurrencyAndFoodWeight() {
        val result = ZakatCalculatorEngine.calculateZakatAlFitr(
            familyMembers = 5,
            amountPerPerson = 25.0
        )

        assertEquals(5, result.familyMembers)
        assertEquals(125.0, result.totalAmount, 0.001)
        assertEquals(12.5, result.totalFoodKg, 0.001)
    }

    @Test
    fun asmaUlHusnaCatalog_containsAll99UniqueNamesWithTriLingualMeanings() {
        val names = AsmaUlHusnaCatalog.names
        assertEquals(99, names.size)
        assertEquals((1..99).toList(), names.map { it.number })
        assertTrue(names.all { it.arabic.isNotBlank() && it.transliteration.isNotBlank() })
        assertTrue(names.all { it.meaningAr.isNotBlank() && it.meaningEn.isNotBlank() && it.meaningUz.isNotBlank() })
    }

    @Test
    fun worshipGuidesCatalog_containsWuduUmrahAndRuqyahWithUzbekSupport() {
        assertTrue(WorshipGuidesCatalog.wuduSteps.size >= 8)
        assertTrue(WorshipGuidesCatalog.umrahSteps.size >= 5)
        assertTrue(WorshipGuidesCatalog.ruqyahItems.size >= 5)
        val all = WorshipGuidesCatalog.wuduSteps + WorshipGuidesCatalog.umrahSteps + WorshipGuidesCatalog.ruqyahItems
        assertTrue(all.all { it.titleAr.isNotBlank() && it.titleEn.isNotBlank() && it.titleUz.isNotBlank() })
        assertTrue(all.all { it.detailAr.isNotBlank() && it.detailEn.isNotBlank() && it.detailUz.isNotBlank() })
    }
}
