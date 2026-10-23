package com.nestvest.expense.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nestvest.expense.domain.*
import com.nestvest.profile.domain.PrimaryProfile
import java.time.Clock
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExpenseViewModel(
    private val repository: ExpenseRepository,
    val profile: PrimaryProfile,
    private val savedState: SavedStateHandle,
    private val clock: () -> Clock = { Clock.systemDefaultZone() },
) : ViewModel() {
    private val mutableState = MutableStateFlow(ExpenseUiState(
        amount = savedState["expenseAmount"] ?: "",
        period = savedState.get<String>("expensePeriod")?.let(ExpensePeriod::valueOf) ?: ExpensePeriod.DAY,
        selectedId = savedState.get<String>("selectedExpenseId")?.let(UUID::fromString),
        screen = savedState.get<String>("expenseScreen")?.let(ExpenseScreen::valueOf) ?: ExpenseScreen.CAPTURE,
    ))
    val state = mutableState.asStateFlow()
    private var observation: Job? = null
    private var all = emptyList<Expense>()

    init { refresh() }

    fun refresh() {
        observation?.cancel()
        mutableState.value = state.value.copy(loading = true, readError = false)
        observation = viewModelScope.launch {
            repository.observeAll().collect { result ->
                when (result) {
                    is ExpenseRead.Found -> {
                        all = result.expenses
                        mutableState.value = state.value.copy(loading = false, readError = false)
                        filter()
                    }
                    is ExpenseRead.Failed -> mutableState.value = state.value.copy(loading = false, readError = true)
                }
            }
        }
    }

    private fun filter() {
        val range = state.value.period.range(clock())
        mutableState.value = state.value.copy(
            expenses = all.filter { range.contains(it.capturedAtEpochMillis) },
            selected = all.find { it.expenseId == state.value.selectedId })
    }

    fun resume() { filter() }
    fun cleanupPending() { mutableState.value = state.value.copy(error = ExpenseError.CLEANUP_PENDING) }
    fun amountChanged(value: String) {
        if (state.value.saving) return
        savedState["expenseAmount"] = value
        mutableState.value = state.value.copy(amount = value, error = null, saved = false)
    }
    fun periodChanged(period: ExpensePeriod) {
        savedState["expensePeriod"] = period.name
        mutableState.value = state.value.copy(period = period)
        filter()
    }
    fun navigate(screen: ExpenseScreen) {
        savedState["expenseScreen"] = screen.name
        mutableState.value = state.value.copy(screen = screen, error = null,
            period = if (screen == ExpenseScreen.HISTORY && state.value.screen == ExpenseScreen.CAPTURE)
                ExpensePeriod.DAY else state.value.period)
        filter()
        savedState["expensePeriod"] = state.value.period.name
    }
    fun select(expense: Expense) {
        savedState["selectedExpenseId"] = expense.expenseId.toString()
        savedState["expenseScreen"] = ExpenseScreen.DETAIL.name
        mutableState.value = state.value.copy(selectedId = expense.expenseId, selected = expense,
            screen = ExpenseScreen.DETAIL, error = null)
    }
    fun save(details: Boolean) {
        if (state.value.saving) return
        val amount = ExpenseAmount.parse(state.value.amount, profile.preferredCurrency, Locale.getDefault())
        if (amount == null) {
            mutableState.value = state.value.copy(error = ExpenseError.INVALID_AMOUNT)
            return
        }
        mutableState.value = state.value.copy(saving = true, error = null, saved = false)
        viewModelScope.launch {
            when (val result = repository.create(amount, profile)) {
                is ExpenseWrite.Saved -> {
                    savedState.remove<String>("expenseAmount")
                    mutableState.value = state.value.copy(amount = "", saving = false, saved = true)
                    if (details) select(result.expense)
                }
                else -> mutableState.value = state.value.copy(saving = false, error = ExpenseError.STORAGE)
            }
        }
    }
}
