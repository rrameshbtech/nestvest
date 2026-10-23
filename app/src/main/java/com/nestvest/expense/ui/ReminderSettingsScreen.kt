package com.nestvest.expense.ui

import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nestvest.R
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSettingsScreen(
    viewModel: ReminderViewModel,
    requestPermission: () -> Unit,
    openSettings: () -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var choosingTime by remember { mutableStateOf(false) }
    LaunchedEffect(state.requestPermission) {
        if (state.requestPermission) {
            viewModel.permissionRequestLaunched()
            requestPermission()
        }
    }
    ExpenseScaffold(footer = {
        ExpenseSecondaryButton(stringResource(R.string.reminder_back), onBack, Modifier.fillMaxWidth())
        ExpenseFootnote(stringResource(R.string.reminder_footnote))
    }) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ExpenseEyebrow(stringResource(R.string.reminder_eyebrow))
            ExpenseHeading(stringResource(R.string.reminder_heading))
            Text(stringResource(R.string.reminder_description), fontSize = 12.sp,
                lineHeight = 20.sp, color = ExpenseStyle.Muted)
            OutlinedCard(Modifier.fillMaxWidth().padding(top = 8.dp).testTag("reminder-settings-card"),
                shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, ExpenseStyle.Line),
                colors = CardDefaults.outlinedCardColors(containerColor = ExpenseStyle.Paper)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .toggleable(value = state.preference.enabled,
                            enabled = !state.loading && !state.saving, role = Role.Switch,
                            onValueChange = viewModel::setEnabled),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.reminder_enable), fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.reminder_default_hint), fontSize = 11.sp,
                                lineHeight = 17.sp, color = ExpenseStyle.Muted)
                        }
                        Switch(checked = state.preference.enabled, onCheckedChange = null,
                            enabled = !state.loading && !state.saving,
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = ExpenseStyle.Green, checkedThumbColor = Color.White,
                                checkedBorderColor = ExpenseStyle.Green, uncheckedTrackColor = ExpenseStyle.Pale,
                                uncheckedThumbColor = ExpenseStyle.Muted, uncheckedBorderColor = ExpenseStyle.ControlBorder))
                    }
                    Text(stringResource(R.string.reminder_time_label), fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold)
                    val time = LocalTime.ofSecondOfDay(state.preference.localTimeMinutes * 60L)
                        .format(DateTimeFormatter.ofPattern(
                            if (DateFormat.is24HourFormat(LocalContext.current)) "HH:mm" else "h:mm a",
                            java.util.Locale.getDefault()))
                    ExpenseSecondaryButton(stringResource(R.string.reminder_time, time),
                        { choosingTime = true }, Modifier.fillMaxWidth().testTag("reminder-time"),
                        enabled = state.preference.enabled && !state.loading && !state.saving)
                    Text(stringResource(if (state.preference.enabled) R.string.reminder_approximate
                        else R.string.reminder_off), fontSize = 11.sp, lineHeight = 18.sp,
                        color = ExpenseStyle.Muted,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    if (state.loading || state.saving) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (state.unavailable) {
                        Text(stringResource(R.string.reminder_unavailable), fontSize = 12.sp,
                            lineHeight = 19.sp, color = ExpenseStyle.Muted,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                        TextButton(onClick = openSettings) {
                            Text(stringResource(R.string.reminder_open_settings), fontSize = 12.sp)
                        }
                    }
                    if (state.error != null || state.schedulingFailed) {
                        Text(stringResource(if (state.schedulingFailed) R.string.reminder_schedule_error
                            else R.string.reminder_storage_error), fontSize = 12.sp,
                            color = ExpenseStyle.Error,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                        TextButton(onClick = viewModel::refreshAvailability) {
                            Text(stringResource(R.string.reminder_retry), fontSize = 12.sp)
                        }
                    }
                    HorizontalDivider(color = ExpenseStyle.Line)
                    Text(stringResource(R.string.reminder_permission_note), fontSize = 11.sp,
                        lineHeight = 18.sp, color = ExpenseStyle.Muted)
                }
            }
        }
        if (choosingTime) {
            val picker = rememberTimePickerState(state.preference.localTimeMinutes / 60,
                state.preference.localTimeMinutes % 60, DateFormat.is24HourFormat(LocalContext.current))
            AlertDialog(onDismissRequest = { choosingTime = false },
                containerColor = ExpenseStyle.Paper,
                title = { Text(stringResource(R.string.reminder_choose_time)) },
                text = {
                    Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                        TimeInput(picker)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.setTime(picker.hour * 60 + picker.minute)
                        choosingTime = false
                    }) { Text(stringResource(R.string.reminder_confirm)) }
                },
                dismissButton = {
                    TextButton(onClick = { choosingTime = false }) { Text(stringResource(R.string.reminder_cancel)) }
                })
        }
    }
}
