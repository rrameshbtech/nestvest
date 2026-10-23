package com.nestvest.expense.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nestvest.R
import com.nestvest.expense.domain.KakeiboCategory

@Composable
fun ExpenseDetailScreen(vm: ExpenseDetailViewModel, leave: () -> Unit,
    attach: () -> Unit, deleted: (Boolean) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    var correcting by rememberSaveable { mutableStateOf(false) }
    var correction by rememberSaveable { mutableStateOf("") }
    var deleting by remember { mutableStateOf(false) }
    val done = { focus.clearFocus(); vm.leave(leave) }
    BackHandler { done() }
    ExpenseScaffold(
        actions = {
            ExpenseSecondaryButton(stringResource(R.string.expense_detail_history), done, enabled = !state.busy)
        },
        footer = {
            ExpensePrimaryButton(stringResource(if (state.busy) R.string.expense_saving else R.string.expense_done),
                done, !state.busy)
            ExpenseFootnote(stringResource(R.string.expense_detail_footnote))
        },
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val compact = maxHeight < 550.dp || LocalDensity.current.fontScale > 1.3f
            Column(Modifier.fillMaxSize().padding(horizontal = 22.dp)) {
                if (!compact) Column(Modifier.padding(top = 20.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExpenseEyebrow(stringResource(R.string.expense_detail_eyebrow))
                    ExpenseHeading(stringResource(R.string.expense_detail_title))
                } else Spacer(Modifier.height(12.dp))
                ExpenseSummaryCard(state.expense, compact)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 14.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.expense_detail_hint), fontSize = 12.sp,
                        lineHeight = 19.sp, color = ExpenseStyle.Muted)
                    ExpenseFeedback(state.error)
                    if (state.saved) Text(stringResource(R.string.expense_saved), fontSize = 12.sp,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    TextButton(onClick = { focus.clearFocus(); correcting = !correcting },
                        enabled = !state.busy, contentPadding = PaddingValues(0.dp)) {
                        Text(stringResource(R.string.expense_correct), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (correcting) {
                        OutlinedTextField(correction, { correction = it },
                            enabled = !state.busy, singleLine = true, shape = ExpenseStyle.ButtonShape,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            label = { Text(stringResource(R.string.expense_amount_label, state.expense.currencyCode)) },
                            modifier = Modifier.fillMaxWidth())
                        ExpenseSecondaryButton(stringResource(R.string.expense_correct_save),
                            { focus.clearFocus(); vm.correct(correction) }, enabled = !state.busy)
                        Text(stringResource(R.string.expense_correct_hint), fontSize = 11.sp, color = ExpenseStyle.Muted)
                    }
                    HorizontalDivider(color = ExpenseStyle.Line)
                    val vendorLabel = stringResource(R.string.expense_vendor)
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(vendorLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(state.vendorDraft, vm::vendorChanged, enabled = !state.busy,
                            placeholder = { Text(stringResource(R.string.expense_vendor_placeholder), fontSize = 12.sp) },
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            singleLine = true, shape = ExpenseStyle.ButtonShape,
                            modifier = Modifier.fillMaxWidth().semantics { contentDescription = vendorLabel }
                                .onFocusChanged {
                                    if (focused && !it.isFocused) vm.confirmVendor()
                                    focused = it.isFocused
                                })
                        Text(stringResource(R.string.expense_vendor_hint), fontSize = 11.sp, color = ExpenseStyle.Muted)
                    }
                    Text(stringResource(R.string.expense_category), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    KakeiboCategory.entries.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            row.forEach { category ->
                                val selected = state.expense.category == category
                                OutlinedButton(onClick = { focus.clearFocus(); vm.category(category) },
                                    enabled = !state.busy, shape = ExpenseStyle.ButtonShape,
                                    border = BorderStroke(1.dp, if (selected) ExpenseStyle.Green else ExpenseStyle.Line),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (selected) ExpenseStyle.Pale else ExpenseStyle.Paper,
                                        contentColor = ExpenseStyle.Ink),
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                        .semantics { this.selected = selected }) {
                                    Text(categoryLabel(category), fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = ExpenseStyle.Line)
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        itemVerticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(if (state.expense.receiptFileName == null) R.string.expense_receipt_optional
                            else R.string.expense_receipt_attached), fontSize = 11.sp, color = ExpenseStyle.Muted)
                        ExpenseSecondaryButton(stringResource(if (state.expense.receiptFileName == null)
                            R.string.expense_attach else R.string.expense_replace),
                            { focus.clearFocus(); attach() }, enabled = !state.busy)
                        if (state.expense.receiptFileName != null) TextButton(onClick = {
                            focus.clearFocus(); vm.removeReceipt()
                        }, enabled = !state.busy) {
                            Text(stringResource(R.string.expense_remove_receipt), fontSize = 12.sp)
                        }
                    }
                    HorizontalDivider(color = ExpenseStyle.Line)
                    Text(stringResource(R.string.expense_detail_confirmation), fontSize = 11.sp, color = ExpenseStyle.Muted)
                    TextButton(onClick = { focus.clearFocus(); deleting = true }, enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.expense_delete), fontSize = 12.sp, color = ExpenseStyle.Error)
                    }
                }
            }
        }
    }
    if (deleting) AlertDialog(onDismissRequest = { deleting = false },
        containerColor = ExpenseStyle.Paper,
        title = { Text(stringResource(R.string.expense_delete_title)) },
        text = { Text(stringResource(R.string.expense_delete_body)) },
        confirmButton = { TextButton(onClick = { deleting = false; vm.delete(deleted) }) {
            Text(stringResource(R.string.expense_delete), color = ExpenseStyle.Error)
        } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text(stringResource(R.string.expense_keep)) } })
}
