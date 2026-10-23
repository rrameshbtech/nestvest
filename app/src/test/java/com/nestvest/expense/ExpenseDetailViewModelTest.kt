package com.nestvest.expense

import com.nestvest.expense.domain.*
import com.nestvest.expense.ui.*
import com.nestvest.profile.MainDispatcherRule
import com.nestvest.profile.domain.StorageError
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseDetailViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val expense = Expense(UUID.randomUUID(), 120, "USD", 0, UUID.randomUUID())
    private class Editor(var expense: Expense) : ExpenseEditor {
        var fail = false
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun edit(id: UUID, detail: ExpenseDetail): ExpenseWrite {
            gate?.await()
            if (fail) return ExpenseWrite.Failed(StorageError.FULL)
            expense = when (detail) {
                is ExpenseDetail.Vendor -> expense.copy(vendor = detail.value.trim().ifEmpty { null })
                is ExpenseDetail.Category -> expense.copy(category = detail.value)
                is ExpenseDetail.Amount -> expense.copy(amountMinorUnits = detail.value)
                is ExpenseDetail.Receipt -> expense.copy(receiptFileName = detail.value)
            }
            return ExpenseWrite.Saved(expense)
        }
        override suspend fun delete(id: UUID): ExpenseWrite =
            if (fail) ExpenseWrite.Failed(StorageError.FULL) else ExpenseWrite.Deleted()
    }
    @Test fun leavingConfirmsVendorOnlyAfterSuccessfulWrite() = runTest {
        val editor = Editor(expense).apply { fail = true; gate = CompletableDeferred() }
        val vm = ExpenseDetailViewModel(editor, expense)
        var left = false
        vm.vendorChanged(" Mira Store ")
        vm.leave { left = true }
        runCurrent()
        assertFalse(left)
        editor.gate!!.complete(Unit)
        runCurrent()
        assertFalse(left)
        assertEquals(" Mira Store ", vm.state.value.vendorDraft)
        editor.fail = false
        vm.leave { left = true }
        runCurrent()
        assertTrue(left)
        assertEquals("Mira Store", vm.state.value.expense.vendor)
        assertEquals(expense.createdByProfileId, vm.state.value.expense.createdByProfileId)
    }
    @Test fun overlongVendorDoesNotCommitOrNavigate() = runTest {
        val vm = ExpenseDetailViewModel(Editor(expense), expense)
        vm.vendorChanged("a".repeat(81))
        vm.leave { fail("Must retain invalid draft") }
        runCurrent()
        assertEquals(ExpenseError.INVALID_VENDOR, vm.state.value.error)
        assertNull(vm.state.value.expense.vendor)
    }
    @Test fun independentEditsAndRefreshPreserveUnconfirmedVendorDraft() = runTest {
        val vm = ExpenseDetailViewModel(Editor(expense), expense)
        vm.vendorChanged("Unfinished vendor")
        vm.correct("2.50")
        runCurrent()
        assertEquals(250L, vm.state.value.expense.amountMinorUnits)
        assertEquals("Unfinished vendor", vm.state.value.vendorDraft)
        assertNull(vm.state.value.expense.vendor)
        vm.refresh(expense.copy(amountMinorUnits = 300))
        assertEquals(300L, vm.state.value.expense.amountMinorUnits)
        assertEquals("Unfinished vendor", vm.state.value.vendorDraft)
        vm.confirmVendor()
        runCurrent()
        assertEquals("Unfinished vendor", vm.state.value.expense.vendor)
    }
    @Test fun categoryTogglesAndRecreationDoesNotConfirmAnAbandonedDraft() = runTest {
        val editor = Editor(expense)
        val vm = ExpenseDetailViewModel(editor, expense)
        vm.category(KakeiboCategory.Culture)
        runCurrent()
        assertEquals(KakeiboCategory.Culture, vm.state.value.expense.category)
        vm.category(KakeiboCategory.Culture)
        runCurrent()
        assertNull(vm.state.value.expense.category)
        vm.vendorChanged("Abandoned")
        val recreated = ExpenseDetailViewModel(editor, editor.expense)
        assertEquals("", recreated.state.value.vendorDraft)
        assertNull(editor.expense.vendor)
    }
    @Test fun invalidCorrectionAndFailedDeleteNeverReportSuccessOrLeave() = runTest {
        val editor = Editor(expense).apply { fail = true }
        val vm = ExpenseDetailViewModel(editor, expense)
        vm.correct("0")
        assertEquals(ExpenseError.INVALID_AMOUNT, vm.state.value.error)
        vm.delete { fail("Failed deletion cannot leave") }
        runCurrent()
        assertEquals(ExpenseError.STORAGE, vm.state.value.error)
        assertEquals(expense, vm.state.value.expense)
    }
}
