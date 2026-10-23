package com.nestvest.expense

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.nestvest.R
import com.nestvest.expense.domain.ExpenseAmount
import com.nestvest.expense.domain.Expense
import com.nestvest.expense.ui.expenseDate
import java.time.Instant
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test

class ExpenseLocaleTest {
    @Test fun unsupportedLanguageUsesEnglishAndCurrencyUsesDeviceLocale() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val other = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag("fr"))
        })
        assertEquals("Save expense", other.getString(R.string.expense_save))
        assertTrue(ExpenseAmount.format(12345, "EUR", Locale.GERMANY).contains("123,45"))
        assertTrue(ExpenseAmount.format(123, "JPY", Locale.JAPAN).contains("123"))
        assertEquals(1234L, ExpenseAmount.parse("1,234", "KWD", Locale.GERMANY))
    }
    @Test fun summaryDatesUseLocalDayAndLocaleRatherThanUtcDay() {
        val locale = Locale.getDefault()
        val zone = TimeZone.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
            val expense = Expense(UUID.randomUUID(), 100, "EUR",
                Instant.parse("2026-10-23T01:00:00Z").toEpochMilli(), UUID.randomUUID())
            assertTrue(expenseDate(expense).startsWith("22.10.2026"))
            assertTrue(expenseDate(expense).endsWith("21:00"))
            assertEquals("21:00", expenseDate(expense, false))
        } finally {
            Locale.setDefault(locale)
            TimeZone.setDefault(zone)
        }
    }
}
