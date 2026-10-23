package com.nestvest.expense.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nestvest.expense.domain.ReminderPreference
import java.time.LocalDate

@Entity(tableName = "reminder_preference")
data class ReminderPreferenceEntity(
    @PrimaryKey val slot: Int = 1,
    val enabled: Boolean = false,
    val localTimeMinutes: Int = 1230,
    val permissionPrompted: Boolean = false,
    val lastDeliveredLocalDate: String? = null,
) {
    fun toPreference(): ReminderPreference {
        require(slot == 1)
        return ReminderPreference(enabled, localTimeMinutes, permissionPrompted,
            lastDeliveredLocalDate?.let(LocalDate::parse))
    }
}
