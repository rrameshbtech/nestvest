package com.nestvest.expense.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nestvest.expense.domain.Expense
import com.nestvest.expense.domain.KakeiboCategory
import java.util.UUID

@Entity(tableName = "expenses", indices = [Index("capturedAtEpochMillis")])
data class ExpenseEntity(
    @PrimaryKey val expenseId: String,
    val amountMinorUnits: Long,
    val currencyCode: String,
    val capturedAtEpochMillis: Long,
    val createdByProfileId: String,
    val vendor: String? = null,
    val category: String? = null,
    val receiptFileName: String? = null,
) {
    fun toExpense() = Expense(
        UUID.fromString(expenseId), amountMinorUnits, currencyCode, capturedAtEpochMillis,
        UUID.fromString(createdByProfileId), vendor, category?.let(KakeiboCategory::valueOf), receiptFileName,
    )
}
