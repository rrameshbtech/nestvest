package com.nestvest.profile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.nestvest.R
import com.nestvest.profile.domain.CurrencyCatalog
import com.nestvest.profile.domain.PrimaryProfile

@Composable
fun SavedProfileScreen(profile: PrimaryProfile) {
    val locale = LocalConfiguration.current.locales[0]
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(stringResource(R.string.saved_title), style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.semantics { heading() })
        Text(stringResource(R.string.saved_intro))
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AppearancePreview(profile.name, profile.avatarId, profile.colorId)
                Text(profile.name, style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.currency_label), style = MaterialTheme.typography.labelLarge)
                Text(stringResource(R.string.currency_item, profile.preferredCurrency,
                    CurrencyCatalog.label(profile.preferredCurrency, locale)))
                Text(stringResource(R.string.saved_status),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
        }
        Text(stringResource(R.string.privacy_note))
    }
}
