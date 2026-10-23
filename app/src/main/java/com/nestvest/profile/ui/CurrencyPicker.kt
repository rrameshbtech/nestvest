package com.nestvest.profile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.nestvest.R
import com.nestvest.profile.domain.CurrencyCatalog

@Composable
fun CurrencyPicker(code: String, invalid: Boolean, enabled: Boolean, choose: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    Column {
        Text(stringResource(R.string.currency_label), style = MaterialTheme.typography.labelLarge)
        OutlinedButton(onClick = { open = true }, enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(if (code in CurrencyCatalog.codes) stringResource(
                R.string.currency_item, code, CurrencyCatalog.label(code, locale),
            ) else stringResource(R.string.currency_prompt))
        }
        if (invalid) Text(stringResource(R.string.currency_error),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
    }
    if (open) {
        var query by remember { mutableStateOf("") }
        val codes = remember(query, locale) {
            CurrencyCatalog.codes.filter {
                it.contains(query, ignoreCase = true) ||
                    CurrencyCatalog.label(it, locale).contains(query, ignoreCase = true)
            }
        }
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(stringResource(R.string.currency_label)) },
            text = {
                Column {
                    OutlinedTextField(query, { query = it },
                        label = { Text(stringResource(R.string.currency_search)) }, singleLine = true)
                    LazyColumn(Modifier.heightIn(max = 320.dp)) {
                        if (codes.isEmpty()) item { Text(stringResource(R.string.currency_empty)) }
                        items(codes, key = { it }) { item ->
                            TextButton(onClick = { choose(item); open = false },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics {
                                    selected = item == code
                                }) {
                                Text(stringResource(R.string.currency_item, item, CurrencyCatalog.label(item, locale)))
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.close)) } },
        )
    }
}
