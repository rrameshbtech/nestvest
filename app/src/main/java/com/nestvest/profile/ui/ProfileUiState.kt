package com.nestvest.profile.ui

import com.nestvest.profile.domain.PrimaryProfile
import com.nestvest.profile.domain.ProfileDraft
import com.nestvest.profile.domain.StorageError

enum class ProfileStage { LOADING, LOAD_ERROR, SETUP, SAVING, SAVED }

data class AppearanceSelection(val avatarId: String? = null, val colorId: String? = null)

data class ProfileUiState(
    val stage: ProfileStage = ProfileStage.LOADING,
    val draft: ProfileDraft = ProfileDraft(),
    val profile: PrimaryProfile? = null,
    val nameError: Boolean = false,
    val currencyError: Boolean = false,
    val saveError: StorageError? = null,
    val sheetDraft: AppearanceSelection? = null,
    val appearanceWarning: Boolean = false,
)
