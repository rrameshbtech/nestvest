package com.nestvest.expense.ui

import com.nestvest.expense.domain.*
import java.util.UUID

enum class ExpenseScreen { CAPTURE, HISTORY, DETAIL, REMINDERS }

data class ExpenseUiState(
    val amount: String = "",
    val saving: Boolean = false,
    val loading: Boolean = true,
    val readError: Boolean = false,
    val error: ExpenseError? = null,
    val saved: Boolean = false,
    val expenses: List<Expense> = emptyList(),
    val period: ExpensePeriod = ExpensePeriod.DAY,
    val screen: ExpenseScreen = ExpenseScreen.CAPTURE,
    val selectedId: UUID? = null,
    val selected: Expense? = null,
)

enum class ExpenseError { INVALID_AMOUNT, STORAGE, MISSING, INVALID_VENDOR, CLEANUP_PENDING }
