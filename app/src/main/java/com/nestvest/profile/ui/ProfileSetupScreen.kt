package com.nestvest.profile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.nestvest.R

@Composable
fun ProfileSetupScreen(state: ProfileUiState, accept: (ProfileIntent) -> Unit) {
    val saving = state.stage == ProfileStage.SAVING
    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.semantics { heading() })
        Text(stringResource(R.string.setup_intro))
        OutlinedTextField(
            value = state.draft.name,
            onValueChange = { accept(ProfileIntent.NameChanged(it)) },
            label = { Text(stringResource(R.string.name_label)) },
            singleLine = true,
            enabled = !saving,
            isError = state.nameError,
            supportingText = if (state.nameError) ({ Text(stringResource(R.string.name_error)) }) else null,
            modifier = Modifier.fillMaxWidth().testTag("name"),
        )
        CurrencyPicker(state.draft.currencyCode, state.currencyError, !saving) {
            accept(ProfileIntent.CurrencyChanged(it))
        }
        AppearancePreview(state.draft.name, state.draft.avatarId, state.draft.colorId)
        TextButton(onClick = { accept(ProfileIntent.OpenAppearance) }, enabled = !saving,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.appearance_open))
        }
        if (state.appearanceWarning) Text(stringResource(R.string.appearance_unavailable),
            Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        if (state.saveError != null) {
            Surface(color = MaterialTheme.colorScheme.errorContainer) {
                Text(
                    stringResource(if (state.saveError == com.nestvest.profile.domain.StorageError.FULL) R.string.save_full else R.string.save_error),
                    Modifier.padding(16.dp).semantics { liveRegion = LiveRegionMode.Assertive },
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
        if (saving) {
            Text(stringResource(R.string.saving_profile),
                Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        Button(
            onClick = { accept(ProfileIntent.Submit) },
            enabled = !saving,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) { Text(stringResource(if (state.saveError == null) R.string.create_profile else R.string.retry)) }
        Text(stringResource(R.string.privacy_note), style = MaterialTheme.typography.bodySmall)
    }
}
