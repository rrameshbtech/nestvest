package com.nestvest.dashboard.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dashboard_preference")
data class DashboardPreferenceEntity(
    @PrimaryKey val preferenceId: Int = SINGLETON_ID,
    val pinnedOptionIds: String = DEFAULT_PINNED_OPTION_IDS,
) {
    companion object {
        const val SINGLETON_ID = 1
        const val DEFAULT_PINNED_OPTION_IDS = "expense_capture,expense_history"
    }
}
