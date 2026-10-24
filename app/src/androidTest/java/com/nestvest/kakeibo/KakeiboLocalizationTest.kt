package com.nestvest.kakeibo

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.nestvest.kakeibo.ui.DailySpendingChart
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class KakeiboLocalizationTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun chartAccessibilityUsesLocaleAwareDatesAndCurrency() {
        val previousLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            compose.setContent {
                DailySpendingChart(
                    values = listOf(250L),
                    firstDate = LocalDate.of(2026, 6, 1),
                    category = "Needs",
                    currencyCode = "EUR",
                )
            }

            val description = compose.onNodeWithTag("kakeibo-daily-chart")
                .fetchSemanticsNode().config[SemanticsProperties.ContentDescription]
                .joinToString(" ")
            assertTrue(description.contains("01.06.2026"))
            assertTrue(description.contains("2,50"))
        } finally {
            Locale.setDefault(previousLocale)
        }
    }
}
