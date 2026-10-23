package com.nestvest.expense.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nestvest.R
import com.nestvest.expense.domain.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.math.BigInteger

@Composable
private fun periodLabel(period: ExpensePeriod) = stringResource(when (period) {
    ExpensePeriod.DAY -> R.string.expense_today
    ExpensePeriod.WEEK -> R.string.expense_week
    ExpensePeriod.MONTH -> R.string.expense_month
})

@Composable
fun ExpenseHistoryList(state: ExpenseUiState, period: (ExpensePeriod) -> Unit,
    select: (Expense) -> Unit, retry: () -> Unit, currency: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ExpensePeriod.entries.forEach { value ->
            val selected = state.period == value
            OutlinedButton(onClick = { period(value) }, shape = RoundedCornerShape(7.dp),
                border = BorderStroke(1.dp, if (selected) ExpenseStyle.Green else ExpenseStyle.Line),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (selected) ExpenseStyle.Pale else ExpenseStyle.Paper,
                    contentColor = if (selected) ExpenseStyle.Ink else ExpenseStyle.Muted),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics { this.selected = selected }) {
                Text(periodLabel(value), fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
    when {
        state.readError -> {
            Text(stringResource(R.string.expense_read_error), Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
            TextButton(onClick = retry) { Text(stringResource(R.string.expense_retry)) }
        }
        state.loading -> Text(stringResource(R.string.expense_loading))
        else -> {
            val totals = expenseTotals(state.expenses).ifEmpty { mapOf(currency to BigInteger.ZERO) }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.expense_total_caption, periodLabel(state.period),
                    pluralStringResource(R.plurals.expense_count, state.expenses.size, state.expenses.size)),
                    fontSize = 11.sp, color = ExpenseStyle.Muted, modifier = Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    totals.forEach { (currency, total) ->
                        Text(ExpenseAmount.format(total, currency), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            HorizontalDivider(color = ExpenseStyle.Line)
            if (state.expenses.isEmpty()) OutlinedCard(
                Modifier.fillMaxWidth(), shape = RoundedCornerShape(9.dp),
                border = BorderStroke(1.dp, ExpenseStyle.Line),
                colors = CardDefaults.outlinedCardColors(containerColor = ExpenseStyle.Paper)) {
                Text(stringResource(R.string.expense_empty), fontSize = 13.sp, color = ExpenseStyle.Muted,
                    modifier = Modifier.padding(28.dp))
            }
            state.expenses.forEach { expense ->
                OutlinedCard(onClick = { select(expense) }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(9.dp), border = BorderStroke(1.dp, ExpenseStyle.Line),
                    colors = CardDefaults.outlinedCardColors(containerColor = ExpenseStyle.Paper)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp).background(ExpenseStyle.Pale, CircleShape)
                            .testTag("expense-mark"), contentAlignment = Alignment.Center) {
                            ExpenseGlyph(when (expense.category) {
                                KakeiboCategory.Needs -> ExpenseSymbol.NEEDS
                                KakeiboCategory.Wants -> ExpenseSymbol.WANTS
                                KakeiboCategory.Culture -> ExpenseSymbol.CULTURE
                                KakeiboCategory.Unexpected -> ExpenseSymbol.UNEXPECTED
                                null -> ExpenseSymbol.RECEIPT
                            }, Modifier.size(18.dp))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(expense.vendor ?: stringResource(R.string.expense_generic),
                                fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(stringResource(R.string.expense_row_context,
                                expenseDate(expense, state.period != ExpensePeriod.DAY),
                                expense.category?.let { categoryLabel(it) }
                                    ?: stringResource(R.string.expense_no_category)),
                                fontSize = 10.sp, lineHeight = 15.sp, color = ExpenseStyle.Muted)
                            if (expense.receiptFileName != null) Text(stringResource(R.string.expense_receipt_attached),
                                fontSize = 10.sp, color = ExpenseStyle.Muted)
                        }
                        Text(ExpenseAmount.format(expense.amountMinorUnits, expense.currencyCode),
                            modifier = Modifier.widthIn(max = 160.dp),
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseHistoryScreen(state: ExpenseUiState, period: (ExpensePeriod) -> Unit,
    select: (Expense) -> Unit, retry: () -> Unit, capture: () -> Unit,
    currency: String,
    settings: (@Composable () -> Unit)? = null) {
    ExpenseScaffold(
        actions = { ExpenseLocalCue(); settings?.invoke() },
        footer = {
            OutlinedButton(onClick = capture, shape = ExpenseStyle.ButtonShape,
                border = BorderStroke(1.dp, ExpenseStyle.ControlBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseStyle.Ink),
                modifier = Modifier.fillMaxWidth().heightIn(min = 49.dp)) {
                ExpenseGlyph(ExpenseSymbol.PLUS, Modifier.size(16.dp))
                Spacer(Modifier.width(7.dp))
                Text(stringResource(R.string.expense_add), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            ExpenseFootnote(stringResource(R.string.expense_history_footnote))
        },
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ExpenseEyebrow(stringResource(R.string.expense_history_eyebrow))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) { ExpenseHeading(periodLabel(state.period)) }
                Text(LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                    .withLocale(Locale.getDefault())), fontSize = 10.sp, color = ExpenseStyle.Muted)
            }
            ExpenseFeedback(state.error)
            ExpenseHistoryList(state, period, select, retry, currency)
        }
    }
}
