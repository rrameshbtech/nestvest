package com.nestvest.expense.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nestvest.R
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale

@Composable
fun ExpenseCaptureScreen(state: ExpenseUiState, currency: String, change: (String) -> Unit,
    save: (Boolean) -> Unit, history: () -> Unit) {
    ExpenseScaffold(
        actions = {
            ExpenseLocalCue()
            TextButton(onClick = history) { Text(stringResource(R.string.expense_history), fontSize = 12.sp) }
        },
        footer = {
            ExpensePrimaryButton(stringResource(if (state.saving) R.string.expense_saving else R.string.expense_save),
                { save(false) }, !state.saving)
            TextButton(onClick = { save(true) }, enabled = !state.saving) {
                Text(stringResource(R.string.expense_add_details), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(6.dp))
                ExpenseGlyph(ExpenseSymbol.ARROW, Modifier.size(14.dp))
            }
            ExpenseFootnote(stringResource(R.string.expense_capture_footnote))
        },
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ExpenseEyebrow(stringResource(R.string.expense_capture_eyebrow))
            ExpenseHeading(stringResource(R.string.expense_capture_title))
            Text(stringResource(R.string.expense_capture_hint), fontSize = 13.sp, lineHeight = 20.sp,
                color = ExpenseStyle.Muted)
            val unit = Currency.getInstance(currency)
            val amountLabel = stringResource(R.string.expense_amount_label, currency)
            OutlinedCard(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp),
                shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, ExpenseStyle.Line),
                colors = CardDefaults.outlinedCardColors(containerColor = ExpenseStyle.Note)) {
                Column(Modifier.fillMaxWidth().heightIn(min = 112.dp).padding(14.dp),
                    verticalArrangement = Arrangement.Center) {
                    Text(stringResource(R.string.expense_currency_description, currency,
                        unit.getDisplayName(Locale.getDefault())), fontSize = 11.sp, color = ExpenseStyle.Muted)
                    Text("${unit.symbol}${state.amount.ifEmpty { "0" }}", fontSize = 43.sp,
                        lineHeight = 50.sp, fontWeight = FontWeight.Medium, letterSpacing = (-2).sp,
                        modifier = Modifier.semantics { contentDescription = amountLabel; liveRegion = LiveRegionMode.Polite })
                }
            }
            val decimal = DecimalFormatSymbols.getInstance().decimalSeparator.toString()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"),
                    listOf(decimal, "0", "back")).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { key ->
                            val label = when (key) {
                                "back" -> stringResource(R.string.expense_backspace)
                                decimal -> stringResource(R.string.expense_decimal)
                                else -> key
                            }
                            OutlinedButton(onClick = {
                                change(if (key == "back") state.amount.dropLast(1) else (state.amount + key).take(32))
                            }, enabled = !state.saving, shape = RoundedCornerShape(9.dp),
                                border = BorderStroke(1.dp, ExpenseStyle.Line),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseStyle.Ink),
                                modifier = Modifier.weight(1f).heightIn(min = 49.dp)
                                    .semantics { contentDescription = label }) {
                                if (key == "back") ExpenseGlyph(ExpenseSymbol.DELETE)
                                else Text(key, fontSize = 18.sp)
                            }
                        }
                    }
                }
            }
            ExpenseFeedback(state.error)
            if (state.saved) Text(stringResource(R.string.expense_saved), fontSize = 12.sp,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                ExpenseGlyph(ExpenseSymbol.LOCK, Modifier.size(14.dp), ExpenseStyle.Muted)
                Text(stringResource(R.string.expense_local), fontSize = 11.sp, color = ExpenseStyle.Muted)
            }
        }
    }
}
