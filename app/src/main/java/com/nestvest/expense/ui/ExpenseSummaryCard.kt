package com.nestvest.expense.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.nestvest.R
import com.nestvest.expense.domain.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

fun expenseDate(expense: Expense, date: Boolean = true): String {
    val formatter = if (date) DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        else DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    return formatter.withLocale(Locale.getDefault()).format(
        Instant.ofEpochMilli(expense.capturedAtEpochMillis).atZone(ZoneId.systemDefault()))
}

@Composable
fun categoryLabel(category: KakeiboCategory): String = stringResource(when (category) {
    KakeiboCategory.Needs -> R.string.expense_needs
    KakeiboCategory.Wants -> R.string.expense_wants
    KakeiboCategory.Culture -> R.string.expense_culture
    KakeiboCategory.Unexpected -> R.string.expense_unexpected
})

@Composable
fun ExpenseSummaryCard(expense: Expense, compact: Boolean = false) {
    OutlinedCard(Modifier.fillMaxWidth().testTag("expense-summary").semantics { isTraversalGroup = true },
        shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, ExpenseStyle.NoteBorder),
        colors = CardDefaults.outlinedCardColors(containerColor = ExpenseStyle.Note,
            contentColor = ExpenseStyle.Ink)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(ExpenseAmount.format(expense.amountMinorUnits, expense.currencyCode),
                fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-1.5).sp, fontWeight = FontWeight.Medium)
            Text(expenseDate(expense), fontSize = 11.sp, color = ExpenseStyle.Muted)
            val pills: @Composable () -> Unit = {
                SummaryPill(expense.vendor?.let { stringResource(R.string.expense_vendor_pill, it) }
                    ?: stringResource(R.string.expense_no_vendor))
                SummaryPill(expense.category?.let { categoryLabel(it) } ?: stringResource(R.string.expense_no_category))
                SummaryPill(stringResource(if (expense.receiptFileName == null) R.string.expense_no_receipt
                    else R.string.expense_receipt_attached))
            }
            if (compact) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) { pills() }
            else FlowRow(Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) { pills() }
        }
    }
}

@Composable
private fun SummaryPill(text: String) {
    Surface(shape = RoundedCornerShape(50), color = ExpenseStyle.Note, contentColor = ExpenseStyle.Muted,
        border = BorderStroke(1.dp, ExpenseStyle.NoteBorder), modifier = Modifier.testTag("expense-detail-pill")) {
        Text(text, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
    }
}

@Composable
fun ExpenseFeedback(error: ExpenseError?) {
    if (error != null) Text(stringResource(when (error) {
        ExpenseError.INVALID_AMOUNT -> R.string.expense_invalid_amount
        ExpenseError.STORAGE -> R.string.expense_storage_error
        ExpenseError.MISSING -> R.string.expense_missing
        ExpenseError.INVALID_VENDOR -> R.string.expense_invalid_vendor
        ExpenseError.CLEANUP_PENDING -> R.string.expense_cleanup_pending
    }), color = MaterialTheme.colorScheme.error,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
}
