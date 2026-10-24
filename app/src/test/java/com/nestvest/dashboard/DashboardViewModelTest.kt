package com.nestvest.dashboard

import com.nestvest.dashboard.domain.DashboardOptionCatalog
import com.nestvest.dashboard.domain.DashboardPreference
import com.nestvest.dashboard.domain.DashboardPreferenceRead
import com.nestvest.dashboard.domain.DashboardPreferenceRepository
import com.nestvest.dashboard.domain.DashboardPreferenceWrite
import com.nestvest.dashboard.domain.DashboardSummary
import com.nestvest.dashboard.ui.DashboardViewModel
import com.nestvest.expense.domain.ExpenseRead
import com.nestvest.expense.domain.ExpenseRepository
import com.nestvest.expense.domain.ExpenseWrite
import com.nestvest.profile.MainDispatcherRule
import com.nestvest.profile.domain.PrimaryProfile
import com.nestvest.profile.domain.StorageError
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val profile = PrimaryProfile(UUID.randomUUID(), "Mira", "USD")

    private class FakePreferences(initial: DashboardPreferenceRead) : DashboardPreferenceRepository {
        val state = MutableStateFlow(initial)
        var result: DashboardPreferenceWrite = DashboardPreferenceWrite.Saved
        var lastSaved: DashboardPreference? = null
        override fun observe(): Flow<DashboardPreferenceRead> = state
        override suspend fun save(preference: DashboardPreference): DashboardPreferenceWrite {
            lastSaved = preference
            if (result == DashboardPreferenceWrite.Saved) state.value = DashboardPreferenceRead.Found(preference)
            return result
        }
    }

    private class FakeExpenses : ExpenseRepository {
        val state = MutableStateFlow<ExpenseRead>(ExpenseRead.Found(emptyList()))
        override fun observe(startInclusive: Long, endExclusive: Long) = state
        override fun observeAll() = state
        override suspend fun create(amount: Long, profile: PrimaryProfile): ExpenseWrite =
            error("Dashboard does not create expenses")
    }

    @Test fun loadsDefaultAndPersistsPinChanges() = runTest {
        val preferences = FakePreferences(DashboardPreferenceRead.Found(DashboardPreference.DEFAULT))
        val viewModel = DashboardViewModel(preferences, FakeExpenses(), profile)
        runCurrent()

        assertEquals(DashboardPreference.DEFAULT.pinnedOptionIds, viewModel.state.value.pinnedOptionIds)
        viewModel.beginCustomization()
        viewModel.toggle(DashboardOptionCatalog.options.single { it.id == "reminders" })
        viewModel.toggle(DashboardOptionCatalog.options.single { it.id == "expense_history" })
        viewModel.saveCustomization()
        runCurrent()

        val expected = setOf("expense_capture", "reminders")
        assertEquals(expected, preferences.lastSaved?.pinnedOptionIds)
        assertEquals(expected, viewModel.state.value.pinnedOptionIds)
        assertFalse(viewModel.state.value.customizing)
    }

    @Test fun failedSaveRetainsSavedPinsAndKeepsDraftEditable() = runTest {
        val preferences = FakePreferences(DashboardPreferenceRead.Found(DashboardPreference.DEFAULT))
        val viewModel = DashboardViewModel(preferences, FakeExpenses(), profile)
        runCurrent()
        viewModel.beginCustomization()
        viewModel.toggle(DashboardOptionCatalog.options.single { it.id == "reminders" })
        preferences.result = DashboardPreferenceWrite.Failed(StorageError.FULL)
        viewModel.saveCustomization()
        runCurrent()

        assertEquals(DashboardPreference.DEFAULT.pinnedOptionIds, viewModel.state.value.pinnedOptionIds)
        assertTrue(viewModel.state.value.customizing)
        assertTrue(viewModel.state.value.saveError)
    }

    @Test fun unknownStoredIdsAreNotPresentedAsPinned() = runTest {
        val preferences = FakePreferences(
            DashboardPreferenceRead.Found(DashboardPreference(setOf("future_option", "reminders"))),
        )
        val viewModel = DashboardViewModel(preferences, FakeExpenses(), profile)
        runCurrent()

        assertEquals(setOf("reminders"), viewModel.state.value.pinnedOptionIds)
    }

    @Test fun expenseReadFailureNeverRetainsOrDisplaysAnOldSummary() = runTest {
        val expenses = FakeExpenses()
        val viewModel = DashboardViewModel(
            FakePreferences(DashboardPreferenceRead.Found(DashboardPreference.DEFAULT)),
            expenses,
            profile,
        )
        runCurrent()
        expenses.state.value = ExpenseRead.Failed(StorageError.UNAVAILABLE)
        runCurrent()

        assertEquals(DashboardSummary.Unavailable, viewModel.state.value.summary)
    }
}
