package com.nestvest.expense

import androidx.lifecycle.SavedStateHandle
import com.nestvest.expense.domain.*
import com.nestvest.expense.ui.ExpenseViewModel
import com.nestvest.expense.ui.ExpenseScreen
import com.nestvest.profile.MainDispatcherRule
import com.nestvest.profile.domain.*
import java.util.UUID
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val profile = PrimaryProfile(UUID.randomUUID(), "Mira", "USD")
    private class Fake : ExpenseRepository {
        val rows = MutableStateFlow<ExpenseRead>(ExpenseRead.Found(emptyList()))
        var writes = 0
        var gate: CompletableDeferred<Unit>? = null
        var fail = false
        override fun observe(startInclusive: Long, endExclusive: Long) = rows
        override fun observeAll() = rows
        override suspend fun create(amount: Long, profile: PrimaryProfile): ExpenseWrite {
            writes++
            gate?.await()
            return if (fail) ExpenseWrite.Failed(StorageError.FULL)
            else ExpenseWrite.Saved(Expense(UUID.randomUUID(), amount, profile.preferredCurrency, 0, profile.profileId))
        }
    }
    @Test fun saveWaitsForCommitAndRetainsFailureDraft() = runTest {
        val repo = Fake().apply { gate = CompletableDeferred(); fail = true }
        val vm = ExpenseViewModel(repo, profile, SavedStateHandle())
        vm.amountChanged("12.34")
        vm.save(false); vm.save(false)
        runCurrent()
        assertTrue(vm.state.value.saving)
        assertEquals(1, repo.writes)
        repo.gate!!.complete(Unit)
        runCurrent()
        assertEquals("12.34", vm.state.value.amount)
        assertNotNull(vm.state.value.error)
        repo.fail = false
        vm.save(false)
        runCurrent()
        assertEquals("", vm.state.value.amount)
        assertTrue(vm.state.value.saved)
    }
    @Test fun invalidDraftNeverWritesAndRestorationNeverReplays() = runTest {
        val repo = Fake()
        val vm = ExpenseViewModel(repo, profile, SavedStateHandle(mapOf("expenseAmount" to "0")))
        runCurrent()
        vm.save(false)
        runCurrent()
        assertEquals(0, repo.writes)
        assertNotNull(vm.state.value.error)
        vm.amountChanged("1.00")
        vm.save(true)
        runCurrent()
        assertNotNull(vm.state.value.selectedId)
        assertEquals(profile.profileId, vm.state.value.selected?.createdByProfileId)
    }
    @Test fun restoresPeriodAndSelectionFromCommittedDataWithoutReplayingSave() = runTest {
        val expense = Expense(UUID.randomUUID(), 100, "USD", 0, profile.profileId)
        val repo = Fake().apply { rows.value = ExpenseRead.Found(listOf(expense)) }
        val saved = SavedStateHandle(mapOf(
            "expenseAmount" to "2.50",
            "expensePeriod" to ExpensePeriod.MONTH.name,
            "selectedExpenseId" to expense.expenseId.toString(),
            "expenseScreen" to ExpenseScreen.DETAIL.name,
        ))
        val vm = ExpenseViewModel(repo, profile, saved)
        runCurrent()
        assertEquals(ExpensePeriod.MONTH, vm.state.value.period)
        assertEquals(ExpenseScreen.DETAIL, vm.state.value.screen)
        assertEquals(expense, vm.state.value.selected)
        assertEquals("2.50", vm.state.value.amount)
        assertEquals(0, repo.writes)
        repo.rows.value = ExpenseRead.Found(emptyList())
        runCurrent()
        assertNull(vm.state.value.selected)
    }
    @Test fun calendarRefreshCannotHideReadFailure() = runTest {
        val repo = Fake().apply { rows.value = ExpenseRead.Failed(StorageError.FULL) }
        val vm = ExpenseViewModel(repo, profile, SavedStateHandle())
        runCurrent()
        assertTrue(vm.state.value.readError)
        vm.resume()
        vm.periodChanged(ExpensePeriod.WEEK)
        assertTrue(vm.state.value.readError)
    }
    @Test fun historyDefaultsToTodayRetainsDetailPeriodAndRefreshesAcrossMidnight() = runTest {
        var clock = Clock.fixed(Instant.parse("2026-10-23T23:59:00Z"), ZoneOffset.UTC)
        val expense = Expense(UUID.randomUUID(), 100, "USD", clock.millis(), profile.profileId)
        val repo = Fake().apply { rows.value = ExpenseRead.Found(listOf(expense)) }
        val vm = ExpenseViewModel(repo, profile, SavedStateHandle()) { clock }
        runCurrent()
        assertEquals(listOf(expense), vm.state.value.expenses)
        vm.navigate(ExpenseScreen.HISTORY)
        assertEquals(ExpensePeriod.DAY, vm.state.value.period)
        vm.periodChanged(ExpensePeriod.MONTH)
        vm.select(expense)
        vm.navigate(ExpenseScreen.HISTORY)
        assertEquals(ExpensePeriod.MONTH, vm.state.value.period)
        vm.navigate(ExpenseScreen.CAPTURE)
        vm.navigate(ExpenseScreen.HISTORY)
        assertEquals(ExpensePeriod.DAY, vm.state.value.period)
        clock = Clock.fixed(Instant.parse("2026-10-24T00:01:00Z"), ZoneOffset.UTC)
        vm.resume()
        assertTrue(vm.state.value.expenses.isEmpty())
        assertEquals(expense.expenseId, vm.state.value.selectedId)
    }
}
