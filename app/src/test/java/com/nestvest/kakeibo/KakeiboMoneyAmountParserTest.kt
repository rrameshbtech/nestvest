package com.nestvest.kakeibo

import com.nestvest.kakeibo.domain.parseMinorUnits
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class KakeiboMoneyAmountParserTest {
    @Test
    fun parsesLocaleDecimalSeparatorsIntoExactMinorUnits() {
        assertEquals(123L, parseMinorUnits("1.23", "USD", Locale.US))
        assertEquals(123L, parseMinorUnits("1,23", "EUR", Locale.GERMANY))
        assertEquals(123L, parseMinorUnits("123", "JPY", Locale.JAPAN))
    }

    @Test
    fun rejectsNegativeMalformedAndExcessPrecisionWithoutRounding() {
        assertThrows(IllegalArgumentException::class.java) { parseMinorUnits("-1.00", "USD", Locale.US) }
        assertThrows(ArithmeticException::class.java) { parseMinorUnits("1.001", "USD", Locale.US) }
        assertThrows(IllegalArgumentException::class.java) { parseMinorUnits("one", "USD", Locale.US) }
    }

    @Test
    fun optionalBlankInputIsDistinctFromZero() {
        assertNull(parseMinorUnits("", "USD", Locale.US))
        assertEquals(0L, parseMinorUnits("", "USD", Locale.US, blankAsZero = true))
    }
}
