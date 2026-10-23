package com.nestvest.expense.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nestvest.expense.domain.*
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ExpenseDetailUiState(
    val expense: Expense,
    val vendorDraft: String = expense.vendor.orEmpty(),
    val busy: Boolean = false,
    val error: ExpenseError? = null,
    val saved: Boolean = false,
)

class ExpenseDetailViewModel(private val editor: ExpenseEditor, expense: Expense) : ViewModel() {
    private val mutableState = MutableStateFlow(ExpenseDetailUiState(expense))
    val state = mutableState.asStateFlow()
    private val lock = Mutex()

    fun refresh(expense: Expense) {
        if (state.value.busy || expense.expenseId != state.value.expense.expenseId) return
        val draft = state.value.vendorDraft
        val dirty = draft.trim() != state.value.expense.vendor.orEmpty()
        mutableState.value = state.value.copy(expense = expense,
            vendorDraft = if (dirty) draft else expense.vendor.orEmpty())
    }

    fun vendorChanged(value: String) {
        mutableState.value = state.value.copy(vendorDraft = value, error = null, saved = false)
    }
    fun confirmVendor() {
        viewModelScope.launch { lock.withLock { vendor() } }
    }
    private suspend fun vendor(): Boolean {
        val draft = state.value.vendorDraft.trim()
        if (draft.codePointCount(0, draft.length) > 80) {
            mutableState.value = state.value.copy(error = ExpenseError.INVALID_VENDOR)
            return false
        }
        if (draft == state.value.expense.vendor.orEmpty()) return true
        return perform { editor.edit(state.value.expense.expenseId, ExpenseDetail.Vendor(draft)) }
    }
    fun leave(done: () -> Unit) {
        viewModelScope.launch { lock.withLock { if (vendor()) done() } }
    }
    fun category(category: KakeiboCategory) {
        viewModelScope.launch { lock.withLock {
            if (vendor()) perform {
                editor.edit(state.value.expense.expenseId, ExpenseDetail.Category(
                    category.takeUnless { it == state.value.expense.category }))
            }
        } }
    }
    fun correct(text: String) {
        val amount = ExpenseAmount.parse(text, state.value.expense.currencyCode, Locale.getDefault())
        if (amount == null) {
            mutableState.value = state.value.copy(error = ExpenseError.INVALID_AMOUNT)
        } else viewModelScope.launch { lock.withLock {
            perform { editor.edit(state.value.expense.expenseId, ExpenseDetail.Amount(amount)) }
        } }
    }
    fun removeReceipt() {
        viewModelScope.launch { lock.withLock {
            perform { editor.edit(state.value.expense.expenseId, ExpenseDetail.Receipt(null)) }
        } }
    }
    fun attach(operation: suspend () -> ExpenseWrite) {
        viewModelScope.launch { lock.withLock { if (vendor()) perform(operation) } }
    }
    fun delete(done: (Boolean) -> Unit) {
        viewModelScope.launch { lock.withLock {
            mutableState.value = state.value.copy(busy = true, error = null)
            when (val result = editor.delete(state.value.expense.expenseId)) {
                is ExpenseWrite.Deleted -> {
                    mutableState.value = state.value.copy(busy = false)
                    done(result.cleanupPending)
                }
                else -> mutableState.value = state.value.copy(busy = false, error = ExpenseError.STORAGE)
            }
        } }
    }
    private suspend fun perform(operation: suspend () -> ExpenseWrite): Boolean {
        mutableState.value = state.value.copy(busy = true, error = null, saved = false)
        return when (val result = operation()) {
            is ExpenseWrite.Saved -> {
                mutableState.value = state.value.copy(expense = result.expense,
                    vendorDraft = if (state.value.vendorDraft.trim() == result.expense.vendor.orEmpty())
                        result.expense.vendor.orEmpty() else state.value.vendorDraft,
                    busy = false, saved = true,
                    error = if (result.cleanupPending) ExpenseError.CLEANUP_PENDING else null)
                true
            }
            else -> {
                mutableState.value = state.value.copy(busy = false,
                    error = if (result == ExpenseWrite.Missing) ExpenseError.MISSING else ExpenseError.STORAGE)
                false
            }
        }
    }
}
