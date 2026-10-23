package com.nestvest.expense.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nestvest.expense.domain.*
import com.nestvest.expense.reminder.ReminderScheduling
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReminderViewModel(
    private val repository: ReminderRepository,
    private val scheduler: ReminderScheduling,
    private val deliveryAvailable: () -> Boolean,
    private val needsRuntimePermission: () -> Boolean,
) : ViewModel() {
    private val mutable = MutableStateFlow(ReminderUiState())
    val state = mutable.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observe().collect { read ->
                when (read) {
                    is ReminderRead.Found -> mutable.update {
                        it.copy(preference = read.preference, loading = false,
                            unavailable = read.preference.enabled && !deliveryAvailable())
                    }
                    is ReminderRead.Failed -> mutable.update { it.copy(loading = false, error = read.error) }
                }
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        if (state.value.saving || state.value.loading) return
        mutable.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                val read = repository.read()
                if (read is ReminderRead.Failed) {
                    mutable.update { it.copy(error = read.error) }
                    return@launch
                }
                val preference = (read as ReminderRead.Found).preference
                val shouldPrompt = enabled && needsRuntimePermission() &&
                    !deliveryAvailable() && !preference.permissionPrompted
                if (shouldPrompt && !accept(repository.markPermissionPrompted())) return@launch
                if (!accept(repository.update(enabled, preference.localTimeMinutes))) return@launch
                mutable.update { it.copy(preference = preference.copy(enabled = enabled,
                    permissionPrompted = preference.permissionPrompted || shouldPrompt),
                    requestPermission = shouldPrompt, unavailable = enabled && !deliveryAvailable()) }
                reconcile(force = true)
            } finally { mutable.update { it.copy(saving = false) } }
        }
    }

    fun setTime(minutes: Int) {
        if (state.value.saving || state.value.loading) return
        mutable.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                val preference = state.value.preference
                if (accept(repository.update(preference.enabled, minutes))) {
                    mutable.update { it.copy(preference = preference.copy(localTimeMinutes = minutes)) }
                    reconcile(force = true)
                }
            } finally { mutable.update { it.copy(saving = false) } }
        }
    }

    fun permissionResult() {
        mutable.update { it.copy(requestPermission = false) }
        refreshAvailability()
    }

    fun permissionRequestLaunched() { mutable.update { it.copy(requestPermission = false) } }

    fun refreshAvailability() {
        viewModelScope.launch {
            val read = repository.read()
            if (read is ReminderRead.Found) {
                mutable.update { it.copy(preference = read.preference,
                    unavailable = read.preference.enabled && !deliveryAvailable(),
                    loading = false, error = null) }
                reconcile()
            } else if (read is ReminderRead.Failed) mutable.update { it.copy(error = read.error) }
        }
    }

    private fun accept(result: ReminderWrite): Boolean {
        if (result is ReminderWrite.Failed) {
            mutable.update { it.copy(error = result.error) }
            return false
        }
        return true
    }

    private suspend fun reconcile(force: Boolean = false) {
        try {
            scheduler.reconcile(state.value.preference, force)
            mutable.update { it.copy(schedulingFailed = false) }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            mutable.update { it.copy(schedulingFailed = true) }
        }
    }
}
