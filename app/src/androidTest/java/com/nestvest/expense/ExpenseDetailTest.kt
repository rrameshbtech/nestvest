package com.nestvest.expense

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.nestvest.expense.data.*
import com.nestvest.expense.domain.*
import com.nestvest.expense.ui.*
import com.nestvest.profile.data.ProfileDatabase
import com.nestvest.profile.domain.PrimaryProfile
import com.nestvest.profile.ui.theme.NestVestTheme
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ExpenseDetailTest {
    @get:Rule val compose = createComposeRule()

    @Test fun vendorDoneCategoryCorrectionAndDeleteAffectOnlySelectedRecord() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "detail-ui-${UUID.randomUUID()}.db"
        val db = ProfileDatabase.open(context, name)
        try {
            val repo = RoomExpenseRepository(db)
            val profile = PrimaryProfile(UUID.randomUUID(), "Mira", "USD")
            val expense = (repo.create(24000, profile) as ExpenseWrite.Saved).expense
            val other = (repo.create(24000, profile) as ExpenseWrite.Saved).expense
            val vm = ExpenseDetailViewModel(repo, expense)
            var left = false
            var deleted = false
            compose.setContent {
                NestVestTheme {
                    ExpenseDetailScreen(vm, { left = true }, {}, { deleted = true })
                }
            }
            compose.onNodeWithContentDescription("Where was it? · optional").performTextInput("Corner Store")
            compose.onNodeWithText("Needs").performScrollTo().performClick()
            compose.waitUntil(5000) { vm.state.value.expense.category == KakeiboCategory.Needs }
            compose.onNodeWithText("Done for now").performClick()
            compose.waitUntil(5000) { left }
            assertEquals("Corner Store", db.expenseDao().read(expense.expenseId.toString())!!.vendor)
            assertEquals(profile.profileId, vm.state.value.expense.createdByProfileId)
            assertNull(db.expenseDao().read(other.expenseId.toString())!!.vendor)
            compose.onNodeWithText("Delete this expense").performScrollTo().performClick()
            compose.onNodeWithText("Keep expense").performClick()
            assertNotNull(db.expenseDao().read(expense.expenseId.toString()))
            compose.onNodeWithText("Delete this expense").performScrollTo().performClick()
            compose.onAllNodesWithText("Delete this expense").onLast().performClick()
            compose.waitUntil(5000) { deleted }
            assertNull(db.expenseDao().read(expense.expenseId.toString()))
            assertNotNull(db.expenseDao().read(other.expenseId.toString()))
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
