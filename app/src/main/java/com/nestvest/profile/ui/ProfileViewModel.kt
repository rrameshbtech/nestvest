package com.nestvest.profile.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nestvest.profile.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: ProfileRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ProfileUiState(draft = ProfileDraft(
        name = savedState["name"] ?: "",
        currencyCode = savedState["currency"] ?: "",
        avatarId = savedState["avatar"],
        colorId = savedState["color"],
    )))
    val state = mutableState.asStateFlow()

    init {
        val draft = state.value.draft
        val normalized = draft.copy(
            avatarId = AppearanceCatalog.avatar(draft.avatarId),
            colorId = AppearanceCatalog.color(draft.colorId),
        )
        if (draft != normalized) {
            saveDraft(normalized)
            mutableState.value = state.value.copy(draft = normalized, appearanceWarning = true)
        }
        load()
    }

    fun accept(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.RetryLoad -> if (state.value.stage == ProfileStage.LOAD_ERROR) load()
            ProfileIntent.Submit -> submit()
            is ProfileIntent.NameChanged -> edit(state.value.draft.copy(name = intent.value))
            is ProfileIntent.CurrencyChanged -> edit(state.value.draft.copy(currencyCode = intent.code))
            else -> if (state.value.stage == ProfileStage.SETUP) handleAppearance(intent)
        }
    }

    private fun edit(draft: ProfileDraft) {
        if (state.value.stage != ProfileStage.SETUP) return
        saveDraft(draft)
        mutableState.value = state.value.copy(draft = draft, nameError = false, currencyError = false, saveError = null)
    }

    private fun saveDraft(draft: ProfileDraft) {
        savedState["name"] = draft.name
        savedState["currency"] = draft.currencyCode
        savedState["avatar"] = draft.avatarId
        savedState["color"] = draft.colorId
    }

    private fun handleAppearance(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.OpenAppearance -> mutableState.value = state.value.copy(
                sheetDraft = AppearanceSelection(state.value.draft.avatarId, state.value.draft.colorId),
            )
            ProfileIntent.DismissAppearance -> mutableState.value = state.value.copy(sheetDraft = null)
            is ProfileIntent.AvatarChanged -> state.value.sheetDraft?.let {
                mutableState.value = state.value.copy(sheetDraft = it.copy(avatarId = intent.id))
            }
            is ProfileIntent.ColorChanged -> state.value.sheetDraft?.let {
                mutableState.value = state.value.copy(sheetDraft = it.copy(colorId = intent.id))
            }
            ProfileIntent.ApplyAppearance, ProfileIntent.SkipAppearance -> {
                val selection = state.value.sheetDraft ?: return
                val avatar = if (intent == ProfileIntent.SkipAppearance) null else AppearanceCatalog.avatar(selection.avatarId)
                val color = if (intent == ProfileIntent.SkipAppearance) null else AppearanceCatalog.color(selection.colorId)
                edit(state.value.draft.copy(avatarId = avatar, colorId = color))
                mutableState.value = state.value.copy(
                    sheetDraft = null,
                    appearanceWarning = intent == ProfileIntent.ApplyAppearance &&
                        (avatar != selection.avatarId || color != selection.colorId),
                )
            }
            else -> error("Not an appearance intent")
        }
    }

    private fun load() {
        mutableState.value = state.value.copy(stage = ProfileStage.LOADING)
        viewModelScope.launch {
            when (val result = repository.read()) {
                is ProfileRead.Found -> showSaved(result.profile)
                ProfileRead.Absent -> mutableState.value = state.value.copy(stage = ProfileStage.SETUP)
                is ProfileRead.Failed -> mutableState.value = state.value.copy(stage = ProfileStage.LOAD_ERROR)
            }
        }
    }

    private fun submit() {
        if (state.value.stage != ProfileStage.SETUP) return
        val draft = state.value.draft
        if (!draft.validName || !draft.validCurrency) {
            mutableState.value = state.value.copy(nameError = !draft.validName, currencyError = !draft.validCurrency)
            return
        }
        mutableState.value = state.value.copy(stage = ProfileStage.SAVING, saveError = null)
        viewModelScope.launch {
            when (val result = repository.create(draft.copy(name = draft.name.trim()))) {
                is ProfileWrite.Created -> showSaved(result.profile)
                is ProfileWrite.Existing -> showSaved(result.profile)
                is ProfileWrite.Failed -> mutableState.value = state.value.copy(
                    stage = ProfileStage.SETUP, saveError = result.error,
                )
                ProfileWrite.InvalidDraft -> mutableState.value = state.value.copy(
                    stage = ProfileStage.SETUP, nameError = !draft.validName, currencyError = !draft.validCurrency,
                )
            }
        }
    }

    private fun showSaved(profile: PrimaryProfile) {
        listOf("name", "currency", "avatar", "color").forEach { savedState.remove<String>(it) }
        mutableState.value = ProfileUiState(stage = ProfileStage.SAVED, profile = profile)
    }
}
