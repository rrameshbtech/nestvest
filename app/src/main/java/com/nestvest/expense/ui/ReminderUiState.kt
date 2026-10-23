package com.nestvest.expense.ui

import com.nestvest.expense.domain.ReminderPreference
import com.nestvest.profile.domain.StorageError

data class ReminderUiState(
    val preference: ReminderPreference = ReminderPreference(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val unavailable: Boolean = false,
    val error: StorageError? = null,
    val schedulingFailed: Boolean = false,
    val requestPermission: Boolean = false,
)
