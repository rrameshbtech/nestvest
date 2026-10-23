package com.nestvest.expense

import com.nestvest.expense.domain.ExpenseAmount
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class ExpenseAmountTest {
    @Test fun rejectsInvalidAmountsWithoutRounding() {
        for (text in listOf("", "0", "-1", "1.001", "NaN", "1x", "1.2.3", "1,000", "92233720368547758.08")) {
            assertNull(text, ExpenseAmount.parse(text, "USD", Locale.US))
        }
        assertEquals(Long.MAX_VALUE, ExpenseAmount.parse("92233720368547758.07", "USD", Locale.US))
    }
    @Test fun currencyFractionsAndLocaleAreRespected() {
        assertEquals(123L, ExpenseAmount.parse("123", "JPY", Locale.US))
        assertNull(ExpenseAmount.parse("1.1", "JPY", Locale.US))
        assertEquals(1234L, ExpenseAmount.parse("1.234", "KWD", Locale.US))
        assertEquals(123L, ExpenseAmount.parse("1,23", "EUR", Locale.GERMANY))
        assertEquals(123L, ExpenseAmount.parse("١٫٢٣", "USD", Locale.forLanguageTag("ar")))
    }
}
