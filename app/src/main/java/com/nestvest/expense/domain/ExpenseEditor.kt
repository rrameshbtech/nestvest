package com.nestvest.expense.domain

import java.util.UUID

sealed interface ExpenseDetail {
    data class Vendor(val value: String) : ExpenseDetail
    data class Category(val value: KakeiboCategory?) : ExpenseDetail
    data class Amount(val value: Long) : ExpenseDetail
    data class Receipt(val value: String?) : ExpenseDetail
}

interface ExpenseEditor {
    suspend fun edit(id: UUID, detail: ExpenseDetail): ExpenseWrite
    suspend fun delete(id: UUID): ExpenseWrite
}
