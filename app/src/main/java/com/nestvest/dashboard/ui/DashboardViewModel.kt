package com.nestvest.dashboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nestvest.dashboard.domain.DashboardOption
import com.nestvest.dashboard.domain.DashboardOptionCatalog
import com.nestvest.dashboard.domain.DashboardPreference
import com.nestvest.dashboard.domain.DashboardPreferenceRead
import com.nestvest.dashboard.domain.DashboardPreferenceRepository
import com.nestvest.dashboard.domain.DashboardPreferenceWrite
import com.nestvest.dashboard.domain.DashboardSummary
import com.nestvest.dashboard.domain.summarizeExpenses
import com.nestvest.expense.domain.ExpenseRead
import com.nestvest.expense.domain.ExpenseRepository
import com.nestvest.profile.domain.PrimaryProfile
import java.time.Clock
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val pinnedOptionIds: Set<String> = DashboardPreference.DEFAULT.pinnedOptionIds,
    val draftOptionIds: Set<String> = emptySet(),
    val customizing: Boolean = false,
    val showingMore: Boolean = false,
    val preferenceLoading: Boolean = true,
    val preferenceError: Boolean = false,
    val saving: Boolean = false,
    val saveSucceeded: Boolean = false,
    val saveError: Boolean = false,
    val summary: DashboardSummary = DashboardSummary.Loading,
)

class DashboardViewModel(
    private val preferences: DashboardPreferenceRepository,
    private val expenses: ExpenseRepository,
    val profile: PrimaryProfile,
    private val clock: () -> Clock = { Clock.systemDefaultZone() },
) : ViewModel() {
    private val mutableState = MutableStateFlow(DashboardUiState())
    val state = mutableState.asStateFlow()
    private var expenseObservation: Job? = null
    private var preferenceObservation: Job? = null
    private var allExpenses = emptyList<com.nestvest.expense.domain.Expense>()
    private var expenseReadFailed = false

    init {
        observePreferences()
        observeExpenses()
    }

    fun resume() {
        observeExpenses()
        if (state.value.preferenceError) observePreferences()
    }

    private fun observePreferences() {
        preferenceObservation?.cancel()
        preferenceObservation = viewModelScope.launch {
            preferences.observe().collect { result ->
                when (result) {
                    is DashboardPreferenceRead.Found -> mutableState.value = state.value.copy(
                        pinnedOptionIds = result.preference.availableTo(DashboardOptionCatalog.ids).pinnedOptionIds,
                        preferenceLoading = false,
                        preferenceError = false,
                    )
                    is DashboardPreferenceRead.Failed -> mutableState.value = state.value.copy(
                        preferenceLoading = false,
                        preferenceError = true,
                    )
                }
            }
        }
    }

    fun beginCustomization() {
        mutableState.value = state.value.copy(
            customizing = true,
            showingMore = false,
            draftOptionIds = state.value.pinnedOptionIds,
            saveError = false,
            saveSucceeded = false,
        )
    }

    fun cancelCustomization() {
        mutableState.value = state.value.copy(
            customizing = false,
            draftOptionIds = emptySet(),
            saveError = false,
        )
    }

    fun showMore() {
        mutableState.value = state.value.copy(customizing = false, showingMore = true)
    }

    fun returnToDashboard() {
        mutableState.value = state.value.copy(
            customizing = false,
            showingMore = false,
            draftOptionIds = emptySet(),
        )
    }

    fun toggle(option: DashboardOption) {
        if (state.value.saving || option.id !in DashboardOptionCatalog.ids) return
        val current = state.value.draftOptionIds
        val changed = if (option.id in current) current - option.id else current + option.id
        mutableState.value = state.value.copy(draftOptionIds = changed, saveError = false)
    }

    fun retryPreferences() {
        mutableState.value = state.value.copy(preferenceLoading = true, preferenceError = false)
        observePreferences()
    }

    fun saveCustomization() {
        if (!state.value.customizing || state.value.saving) return
        mutableState.value = state.value.copy(saving = true, saveError = false, saveSucceeded = false)
        viewModelScope.launch {
            when (val result = preferences.save(
                DashboardPreference(state.value.draftOptionIds.intersect(DashboardOptionCatalog.ids)),
            )) {
                DashboardPreferenceWrite.Saved -> mutableState.value = state.value.copy(
                    pinnedOptionIds = state.value.draftOptionIds,
                    draftOptionIds = emptySet(),
                    customizing = false,
                    saving = false,
                    saveSucceeded = true,
                    saveError = false,
                )
                is DashboardPreferenceWrite.Failed -> mutableState.value = state.value.copy(
                    saving = false,
                    saveError = true,
                    saveSucceeded = false,
                )
            }
        }
    }

    private fun observeExpenses() {
        expenseObservation?.cancel()
        mutableState.value = state.value.copy(summary = DashboardSummary.Loading)
        expenseObservation = viewModelScope.launch {
            expenses.observeAll().collect { result ->
                when (result) {
                    is ExpenseRead.Found -> {
                        allExpenses = result.expenses
                        expenseReadFailed = false
                        refreshSummary()
                    }
                    is ExpenseRead.Failed -> {
                        expenseReadFailed = true
                        mutableState.value = state.value.copy(summary = DashboardSummary.Unavailable)
                    }
                }
            }
        }
    }

    private fun refreshSummary() {
        mutableState.value = state.value.copy(
            summary = if (expenseReadFailed) DashboardSummary.Unavailable
            else summarizeExpenses(allExpenses, profile.preferredCurrency, clock()),
        )
    }
}
