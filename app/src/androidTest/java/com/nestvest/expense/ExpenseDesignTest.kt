package com.nestvest.expense

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.nestvest.expense.domain.Expense
import com.nestvest.expense.domain.ExpenseEditor
import com.nestvest.expense.domain.ExpenseDetail
import com.nestvest.expense.domain.ExpenseWrite
import com.nestvest.expense.domain.KakeiboCategory
import com.nestvest.expense.ui.*
import com.nestvest.profile.ui.theme.NestVestTheme
import java.util.UUID
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ExpenseDesignTest {
    @get:Rule val compose = createComposeRule()
    private val expense = Expense(UUID.randomUUID(), 24000, "USD", 0, UUID.randomUUID(),
        vendor = "Corner tea shop", category = KakeiboCategory.Culture)

    @Test fun captureHasNoHistoryAndKeepsPrimaryAndSubtleActionsAtBottom() {
        compose.setContent {
            NestVestTheme {
                ExpenseCaptureScreen(ExpenseUiState(loading = false, expenses = listOf(expense)),
                    "USD", {}, {}, {})
            }
        }
        compose.onNodeWithText("Corner tea shop").assertDoesNotExist()
        compose.onNodeWithText("Today").assertDoesNotExist()
        compose.onNodeWithText("Save expense").assertIsDisplayed()
        compose.onNodeWithText("Add details now").assertIsDisplayed()
        val footer = compose.onNodeWithTag("expense-footer").fetchSemanticsNode().boundsInRoot
        val screen = compose.onNodeWithTag("expense-screen").fetchSemanticsNode().boundsInRoot
        assertEquals(screen.bottom, footer.bottom, 1f)
        val keypad = compose.onNodeWithContentDescription("0").fetchSemanticsNode().boundsInRoot
        assertTrue(keypad.bottom <= footer.top)
        val button = compose.onNodeWithText("Save expense").captureToImage().toPixelMap()
        assertEquals(Color(0xFF202521), button[button.width / 2, 3])
        val paper = compose.onNodeWithTag("expense-screen").captureToImage().toPixelMap()
        assertEquals(Color(0xFFFAFBF7), paper[1, paper.height / 2])
    }

    @Test fun historyUsesIconLedRowsAndBottomActionOutsideScrollingList() {
        compose.setContent {
            NestVestTheme {
                ExpenseHistoryScreen(ExpenseUiState(loading = false,
                    expenses = (1..20).map { expense.copy(expenseId = UUID.randomUUID()) }),
                    {}, {}, {}, {}, "USD")
            }
        }
        compose.onNodeWithText("Add an expense").assertIsDisplayed()
        val footer = compose.onNodeWithTag("expense-footer").fetchSemanticsNode().boundsInRoot
        val screen = compose.onNodeWithTag("expense-screen").fetchSemanticsNode().boundsInRoot
        assertEquals(screen.bottom, footer.bottom, 1f)
        val mark = compose.onAllNodesWithTag("expense-mark", useUnmergedTree = true)[0]
            .fetchSemanticsNode().boundsInRoot
        val vendor = compose.onAllNodesWithText("Corner tea shop", useUnmergedTree = true)[0]
            .fetchSemanticsNode().boundsInRoot
        val amount = compose.onAllNodesWithText("$240.00", useUnmergedTree = true)[0]
            .fetchSemanticsNode().boundsInRoot
        assertTrue(mark.right <= vendor.left)
        assertTrue(vendor.right <= amount.left)
    }

    @Test fun summaryUsesPaleNoteCardAndSavedDetailPills() {
        compose.setContent { NestVestTheme { ExpenseSummaryCard(expense) } }
        compose.onNodeWithTag("expense-summary").assertExists()
        compose.onAllNodesWithTag("expense-detail-pill", useUnmergedTree = true).assertCountEquals(3)
        val image = compose.onNodeWithTag("expense-summary").captureToImage().toPixelMap()
        assertEquals(Color(0xFFF4F7F1), image[image.width / 2, 3])
    }

    @Test fun shortDetailViewportKeepsSummaryAndDoneVisibleWhileFormScrolls() {
        val editor = object : ExpenseEditor {
            override suspend fun edit(id: UUID, detail: ExpenseDetail) = ExpenseWrite.Saved(expense)
            override suspend fun delete(id: UUID) = ExpenseWrite.Deleted()
        }
        lateinit var vm: ExpenseDetailViewModel
        compose.runOnUiThread { vm = ExpenseDetailViewModel(editor, expense) }
        compose.setContent {
            NestVestTheme {
                Box(Modifier.fillMaxWidth().height(430.dp)) { ExpenseDetailScreen(vm, {}, {}, {}) }
            }
        }
        compose.onNodeWithText("Make it yours,\nif you want.").assertDoesNotExist()
        compose.onNodeWithText("Delete this expense").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("$240.00").assertIsDisplayed()
        compose.onNodeWithText("Done for now").assertIsDisplayed()
    }
}
