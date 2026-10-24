package com.nestvest.dashboard.domain

import androidx.annotation.StringRes
import com.nestvest.R

enum class DashboardDestination { EXPENSE_CAPTURE, EXPENSE_HISTORY, REMINDERS }

data class DashboardOption(
    val id: String,
    @param:StringRes val title: Int,
    @param:StringRes val description: Int,
    val destination: DashboardDestination,
)

object DashboardOptionCatalog {
    val options = listOf(
        DashboardOption(
            "expense_capture", R.string.dashboard_add_expense, R.string.dashboard_add_expense_description,
            DashboardDestination.EXPENSE_CAPTURE,
        ),
        DashboardOption(
            "expense_history", R.string.dashboard_expenses, R.string.dashboard_expenses_description,
            DashboardDestination.EXPENSE_HISTORY,
        ),
        DashboardOption(
            "reminders", R.string.dashboard_reminders, R.string.dashboard_reminders_description,
            DashboardDestination.REMINDERS,
        ),
    )
    val ids = options.mapTo(linkedSetOf()) { it.id }
}
