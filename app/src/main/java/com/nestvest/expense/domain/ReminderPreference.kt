package com.nestvest.expense.domain

import java.time.LocalDate

data class ReminderPreference(
    val enabled: Boolean = false,
    val localTimeMinutes: Int = 1230,
    val permissionPrompted: Boolean = false,
    val lastDeliveredLocalDate: LocalDate? = null,
) {
    init { require(localTimeMinutes in 0..1439) }
}
