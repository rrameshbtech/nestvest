package com.nestvest.expense

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import com.nestvest.expense.domain.*
import com.nestvest.expense.ui.*
import com.nestvest.profile.ui.theme.NestVestTheme
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class ExpenseAccessibilityTest {
    @get:Rule val compose = createComposeRule()
    @Test fun keypadHasAccessibleLabelsAndTouchTargets() {
        compose.setContent {
            NestVestTheme {
                ExpenseCaptureScreen(ExpenseUiState(loading = false), "USD", {}, {}, {})
            }
        }
        compose.onNodeWithContentDescription("1").assertHeightIsAtLeast(48.dp)
        compose.onNodeWithContentDescription("Delete last digit").assertExists()
        compose.onNodeWithContentDescription("Decimal point").assertExists()
        compose.onNodeWithText("Save expense").assertHasClickAction()
        compose.onNodeWithText("Add details now").assertHasClickAction()
    }

    @Test fun largeTextKeepsExpenseSummaryVisibleWhileDetailsScroll() {
        val expense = Expense(UUID.randomUUID(), 24000, "USD", 0, UUID.randomUUID(),
            vendor = "Corner Store", category = KakeiboCategory.Needs)
        val editor = object : ExpenseEditor {
            override suspend fun edit(id: UUID, detail: ExpenseDetail) = ExpenseWrite.Saved(expense)
            override suspend fun delete(id: UUID) = ExpenseWrite.Deleted()
        }
        lateinit var vm: ExpenseDetailViewModel
        compose.runOnUiThread { vm = ExpenseDetailViewModel(editor, expense) }
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                NestVestTheme { ExpenseDetailScreen(vm, {}, {}, {}) }
            }
        }
        val amount = ExpenseAmount.format(24000, "USD")
        compose.onNodeWithText(amount).assertIsDisplayed()
        compose.onNodeWithText("Delete this expense").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(amount).assertIsDisplayed()
        compose.onNodeWithText("Done for now").assertIsDisplayed()
    }

    @Test fun errorsAndSuccessfulSavesAreAnnounced() {
        compose.setContent {
            NestVestTheme {
                ExpenseCaptureScreen(ExpenseUiState(loading = false, saved = true,
                    error = ExpenseError.CLEANUP_PENDING), "USD", {}, {}, {})
            }
        }
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion,
            LiveRegionMode.Assertive)).assertExists()
        compose.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion,
            LiveRegionMode.Polite)).assertCountEquals(2)
    }
}
