package com.nestvest.profile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nestvest.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileRoute(viewModel: ProfileViewModel, savedContent: (@Composable (com.nestvest.profile.domain.PrimaryProfile) -> Unit)? = null) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.stage == ProfileStage.SAVED && savedContent != null) savedContent(checkNotNull(state.profile))
    else ProfileContent(state, viewModel::accept)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileContent(state: ProfileUiState, accept: (ProfileIntent) -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (state.stage) {
                ProfileStage.LOADING -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.loading_profile),
                        Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                ProfileStage.LOAD_ERROR -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.load_error),
                        Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
                    Button(onClick = { accept(ProfileIntent.RetryLoad) },
                        modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.retry)) }
                }
                ProfileStage.SETUP, ProfileStage.SAVING -> ProfileSetupScreen(state, accept)
                ProfileStage.SAVED -> SavedProfileScreen(checkNotNull(state.profile))
            }
        }
        state.sheetDraft?.let { ProfileAppearanceSheet(state.draft.name, it, accept) }
    }
}
