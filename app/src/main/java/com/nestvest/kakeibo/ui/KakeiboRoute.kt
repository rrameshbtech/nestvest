package com.nestvest.kakeibo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import com.nestvest.expense.ui.ExpenseEyebrow
import com.nestvest.expense.ui.ExpenseGlyph
import com.nestvest.expense.ui.ExpenseSymbol
import com.nestvest.kakeibo.domain.localDate
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.testTag
import com.nestvest.R
import com.nestvest.expense.domain.Expense
import com.nestvest.expense.ui.ExpenseStyle
import com.nestvest.expense.ui.ExpenseScaffold
import com.nestvest.expense.ui.ExpenseHeading
import com.nestvest.expense.ui.ExpensePrimaryButton
import com.nestvest.expense.ui.ExpenseSecondaryButton
import com.nestvest.dashboard.ui.LocalDashboardHomeAction
import com.nestvest.kakeibo.domain.CategoryHealth
import com.nestvest.kakeibo.domain.KakeiboCategory
import com.nestvest.kakeibo.domain.KakeiboRead
import com.nestvest.kakeibo.domain.MonthlyProgress
import com.nestvest.kakeibo.domain.canonicalKakeiboCategories
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale
import kotlin.math.roundToLong

private fun YearMonth.localizedLabel(): String =
    format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))

@Composable
fun KakeiboRoute(
    viewModel: KakeiboViewModel,
    onExit: () -> Unit,
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val current = state.navigation.current
    BackHandler(enabled = true) {
        if (state.navigation.backStack.size > 1) viewModel.back() else onExit()
    }
    CompositionLocalProvider(LocalDashboardHomeAction provides onExit) {
        when (current) {
            KakeiboDestination.Home -> KakeiboHomeScreen(state, viewModel)
            KakeiboDestination.Learning -> KakeiboLearningScreen(
                onBack = viewModel::back,
                onPlan = viewModel::beginPlan,
            )
            KakeiboDestination.MonthlyPlan -> KakeiboMonthlyPlanScreen(state, viewModel)
            KakeiboDestination.WeeklyPacing -> KakeiboWeeklyPacingScreen(state, viewModel)
            KakeiboDestination.PlanReview -> KakeiboPlanReviewScreen(state, viewModel)
            KakeiboDestination.Progress -> KakeiboProgressScreen(state, viewModel)
            is KakeiboDestination.CategoryDetail -> KakeiboCategoryScreen(
                state,
                KakeiboCategory.valueOf(current.categoryCode),
                viewModel,
            )
            KakeiboDestination.Reflection -> KakeiboReflectionScreen(state, viewModel)
            KakeiboDestination.SavedReflection -> KakeiboSavedReflectionScreen(state, viewModel)
        }
    }
}

@Composable
private fun KakeiboFrame(
    title: String,
    onExit: (() -> Unit)?,
    eyebrow: String? = null,
    subtitle: String? = null,
    content: @Composable () -> Unit,
) {
    val typography = MaterialTheme.typography
    MaterialTheme(typography = typography.copy(
        bodyLarge = typography.bodyLarge.copy(fontSize = 14.sp, lineHeight = 21.sp),
        bodyMedium = typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 20.sp),
        bodySmall = typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp),
        titleMedium = typography.titleMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
        titleSmall = typography.titleSmall.copy(fontSize = 13.sp, lineHeight = 19.sp),
    )) {
        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            ExpenseScaffold(footer = {}) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        .padding(horizontal = 22.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (eyebrow != null) ExpenseEyebrow(eyebrow)
                    ExpenseHeading(title)
                    if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = ExpenseStyle.Muted)
                    content()
                    if (onExit != null) {
                        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text(stringResource(R.string.kakeibo_home))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KakeiboHomeScreen(
    state: KakeiboUiState,
    viewModel: KakeiboViewModel,
) {
    val monthLabel = state.month.localizedLabel()
    KakeiboFrame(
        stringResource(R.string.kakeibo_home_heading), null,
        eyebrow = stringResource(R.string.kakeibo_title),
        subtitle = stringResource(R.string.kakeibo_home_intro),
    ) {
        ExpenseEyebrow(monthLabel)
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = { viewModel.changeMonth(-1) }) {
                Text(stringResource(R.string.kakeibo_previous_month))
            }
            TextButton(onClick = { viewModel.changeMonth(1) }) {
                Text(stringResource(R.string.kakeibo_next_month))
            }
        }
        when (val plan = state.plan) {
            null -> LinearProgressIndicator(Modifier.fillMaxWidth())
            KakeiboRead.Absent -> {
                KakeiboCard(soft = true) {
                    Text(stringResource(R.string.kakeibo_no_plan), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.kakeibo_home_no_plan_hint), color = ExpenseStyle.Muted,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            is KakeiboRead.Found -> {
                val progress = viewModel.progress()
                SummaryCard(plan.value, progress)
            }
            is KakeiboRead.Failed -> ErrorMessage(stringResource(R.string.kakeibo_read_error), viewModel::retry)
        }
        if (state.showLearningPrompt) {
            KakeiboCard(soft = true, modifier = Modifier.testTag("kakeibo-learning-prompt")) {
                Text(stringResource(R.string.kakeibo_learning_prompt_title), fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.kakeibo_learning_prompt_description),
                    color = ExpenseStyle.Muted, style = MaterialTheme.typography.bodySmall)
                FlowRow {
                    TextButton(onClick = viewModel::openLearning) {
                        Text(stringResource(R.string.kakeibo_learning_prompt_explore))
                    }
                    TextButton(onClick = viewModel::planNowFromPrompt) { Text(stringResource(R.string.kakeibo_plan_now)) }
                    TextButton(onClick = viewModel::dismissLearningPrompt) { Text(stringResource(R.string.kakeibo_not_now)) }
                }
            }
        }
        KakeiboEntryTile(
            stringResource(R.string.kakeibo_view_progress),
            stringResource(R.string.kakeibo_home_progress_hint),
            ExpenseSymbol.RECEIPT, viewModel::openProgress,
        )
        KakeiboEntryTile(
            stringResource(R.string.kakeibo_reflection),
            stringResource(R.string.kakeibo_home_reflection_hint),
            ExpenseSymbol.CULTURE, viewModel::openReflection,
        )
        TextButton(onClick = viewModel::openLearning, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.kakeibo_learn))
        }
        if (state.learningError) {
            val errorState = stringResource(R.string.kakeibo_error_state)
            Text(
                stringResource(R.string.kakeibo_learning_storage_error),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { stateDescription = errorState },
            )
        }
        if (state.plan == KakeiboRead.Absent || state.plan is KakeiboRead.Found) {
            ExpensePrimaryButton(
                text = stringResource(if (state.plan is KakeiboRead.Found) R.string.kakeibo_edit_plan
                    else R.string.kakeibo_plan_month),
                onClick = viewModel::beginPlan,
                modifier = Modifier.testTag("kakeibo-plan-month"),
            )
        }
    }
}

@Composable
private fun SummaryCard(plan: com.nestvest.kakeibo.domain.MonthlyPlan, progress: MonthlyProgress?) {
    KakeiboCard(soft = true) {
        Text(stringResource(R.string.kakeibo_plan_summary), style = MaterialTheme.typography.titleMedium)
        SummaryRow(stringResource(R.string.kakeibo_categories_label), amountText(plan.categoryTotalMinorUnits, plan.currencyCode))
        val spent = progress?.let { checkedTotal(it.categoryProgress.map { row -> row.spentMinorUnits }) }
        if (progress == null || spent == null) {
            Text(stringResource(R.string.kakeibo_progress_unavailable))
        } else {
            val health = progress.categoryProgress.maxByOrNull { it.health.rankForDisplay() }?.health
            Text(
                stringResource(
                    R.string.kakeibo_spent_vs_planned,
                    amountText(spent, plan.currencyCode),
                    amountText(plan.categoryTotalMinorUnits, plan.currencyCode),
                ),
            )
            if (health != null) {
                Text(stringResource(R.string.kakeibo_health_summary, healthLabel(health)))
            }
        }
    }
}

@Composable
private fun KakeiboEntryTile(title: String, description: String, symbol: ExpenseSymbol, onClick: () -> Unit) {
    KakeiboCard(soft = true, modifier = Modifier.clickable(role = Role.Button, onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = CircleShape, color = ExpenseStyle.Pale, modifier = Modifier.size(38.dp)) {
                Box(contentAlignment = Alignment.Center) { ExpenseGlyph(symbol) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(description, style = MaterialTheme.typography.bodySmall, color = ExpenseStyle.Muted)
            }
            ExpenseGlyph(ExpenseSymbol.ARROW, Modifier.size(18.dp))
        }
    }
}

@Composable
internal fun KakeiboLearningScreen(onBack: () -> Unit, onPlan: () -> Unit) {
    var slide by rememberSaveable { mutableIntStateOf(0) }
    val slideTitles = listOf(
        R.string.kakeibo_slide_one_title,
        R.string.kakeibo_slide_two_title,
        R.string.kakeibo_slide_three_title,
    )
    KakeiboFrame(
        stringResource(slideTitles[slide]), onBack,
        eyebrow = stringResource(R.string.kakeibo_learning_eyebrow),
    ) {
        when (slide) {
            0 -> {
                Text(stringResource(R.string.kakeibo_slide_one_body))
                KakeiboCard(soft = true) {
                    ExpenseGlyph(ExpenseSymbol.WANTS, Modifier.size(48.dp))
                    Text(stringResource(R.string.kakeibo_slide_one_prompt), fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.kakeibo_slide_one_benefit))
                }
            }
            1 -> {
                Text(stringResource(R.string.kakeibo_slide_two_body))
                canonicalKakeiboCategories.forEach { category ->
                    KakeiboCard {
                        ExpenseGlyph(categorySymbol(category), Modifier.size(28.dp))
                        Text(categoryLabel(category), fontWeight = FontWeight.SemiBold)
                        Text(stringResource(categoryExample(category)))
                    }
                }
            }
            else -> {
                Text(stringResource(R.string.kakeibo_slide_three_body))
                KakeiboCard(soft = true) {
                    listOf(
                        R.string.kakeibo_slide_three_step_one,
                        R.string.kakeibo_slide_three_step_two,
                        R.string.kakeibo_slide_three_step_three,
                    ).forEach { Text(stringResource(it)) }
                }
            }
        }
        SummaryRow(stringResource(R.string.kakeibo_slide_position, slide + 1),
            stringResource(R.string.kakeibo_learn_at_pace))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExpenseSecondaryButton(
                text = stringResource(R.string.kakeibo_previous),
                onClick = { slide = (slide - 1).coerceAtLeast(0) },
                enabled = slide > 0,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            )
            ExpenseSecondaryButton(
                text = stringResource(if (slide < 2) R.string.kakeibo_next else R.string.kakeibo_finish),
                onClick = {
                    if (slide < 2) slide++ else onPlan()
                },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            )
        }
        ExpensePrimaryButton(text = stringResource(R.string.kakeibo_plan_month), onClick = onPlan)
        TextButton(onClick = onPlan, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.kakeibo_skip_to_plan))
        }
    }
}

@Composable
internal fun KakeiboMonthlyPlanScreen(state: KakeiboUiState, viewModel: KakeiboViewModel) {
    val currencyCode = viewModel.profile.preferredCurrency
    val month = state.month.localizedLabel()
    var editingOverall by rememberSaveable(state.month.toString()) {
        mutableStateOf(state.draft.availableAmount.isNotBlank())
    }
    val allocationTotal = viewModel.allocationTotalMinorUnits()
    val invalidAmount = stringResource(R.string.kakeibo_invalid_amount)
    KakeiboFrame(
        stringResource(R.string.kakeibo_planner_heading), viewModel::back,
        eyebrow = stringResource(R.string.kakeibo_monthly_plan_title, month),
        subtitle = stringResource(R.string.kakeibo_plan_intro),
    ) {
        Text(currencyCode, style = MaterialTheme.typography.labelMedium, color = ExpenseStyle.Muted)
        KakeiboCard(soft = true) {
            if (editingOverall) {
                AmountInput(
                    label = stringResource(R.string.kakeibo_available_optional),
                    value = state.draft.availableAmount,
                    currencyCode = currencyCode,
                    onValueChange = viewModel::updateAvailableAmount,
                    optional = true,
                )
                TextButton(onClick = {
                    viewModel.updateAvailableAmount("")
                    editingOverall = false
                }) { Text(stringResource(R.string.kakeibo_use_calculated)) }
            } else {
                AmountInput(
                    label = stringResource(R.string.kakeibo_calculated_overall),
                    value = allocationTotal?.let { amountInput(it, currencyCode) } ?: invalidAmount,
                    currencyCode = currencyCode,
                    onValueChange = {},
                    readOnly = true,
                )
                Text(stringResource(R.string.kakeibo_calculated_hint), color = ExpenseStyle.Muted)
                TextButton(onClick = { editingOverall = true }) {
                    Text(stringResource(R.string.kakeibo_edit_overall))
                }
            }
        }
        AmountInput(
            label = stringResource(R.string.kakeibo_savings_intention),
            value = state.draft.savingsAmount,
            currencyCode = currencyCode,
            onValueChange = viewModel::updateSavingsAmount,
        )
        Text(stringResource(R.string.kakeibo_category_limits), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(
                R.string.kakeibo_slider_maximum,
                viewModel.sliderMaximumMinorUnits()?.let { amountText(it, currencyCode) } ?: invalidAmount,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = ExpenseStyle.Muted,
        )
        if (state.draft.availableAmount.isBlank()) {
            Text(
                stringResource(R.string.kakeibo_slider_calculated_hint),
                style = MaterialTheme.typography.bodySmall,
                color = ExpenseStyle.Muted,
            )
        }
        KakeiboCard {
            canonicalKakeiboCategories.forEach { category ->
                KakeiboCategoryAmount(
                    category = category,
                    currencyCode = currencyCode,
                    value = state.draft.categoryAmounts[category].orEmpty(),
                    amountMinorUnits = viewModel.categoryAmountMinorUnits(category) ?: 0L,
                    maxMinorUnits = viewModel.sliderMaximumMinorUnits(),
                    onTextChange = { viewModel.updateCategoryAmount(category, it) },
                    onSliderChange = { viewModel.updateCategoryAmount(category, amountInput(it, currencyCode)) },
                )
                if (category != canonicalKakeiboCategories.last()) {
                    androidx.compose.material3.HorizontalDivider(color = ExpenseStyle.Line)
                }
            }
        }
        KakeiboCard(soft = true) {
            Text(stringResource(R.string.kakeibo_allocation_summary), style = MaterialTheme.typography.titleMedium)
            SummaryRow(
                stringResource(R.string.kakeibo_categories_label),
                viewModel.categoryTotalMinorUnits()?.let { amountText(it, currencyCode) } ?: invalidAmount,
            )
            SummaryRow(
                stringResource(R.string.kakeibo_savings_label),
                viewModel.savingsAmountMinorUnits()?.let { amountText(it, currencyCode) } ?: invalidAmount,
            )
            SummaryRow(
                stringResource(R.string.kakeibo_allocated_label),
                allocationTotal?.let { amountText(it, currencyCode) } ?: invalidAmount,
            )
            val available = viewModel.availableMinorUnits()
            if (state.draft.availableAmount.isBlank()) {
                Text(stringResource(R.string.kakeibo_no_total_required))
            } else if (available == null || allocationTotal == null) {
                Text(stringResource(R.string.kakeibo_invalid_amount), color = MaterialTheme.colorScheme.error)
            } else {
                val remaining = available - allocationTotal
                SummaryRow(
                    stringResource(R.string.kakeibo_unassigned_label),
                    amountText(remaining, currencyCode),
                )
                if (remaining < 0) {
                    Text(
                        stringResource(R.string.kakeibo_over_allocation),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().heightIn(min = 52.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = state.draft.weeklyPacingEnabled,
                onCheckedChange = viewModel::toggleWeeklyPacing,
            )
            Text(stringResource(R.string.kakeibo_weekly_pacing))
        }
        if (state.draft.weeklyPacingEnabled) {
            ExpenseSecondaryButton(
                text = stringResource(R.string.kakeibo_set_weekly_amounts),
                onClick = { viewModel.navigate(KakeiboDestination.WeeklyPacing) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            )
        }
        PlanError(state.planError)
        ExpensePrimaryButton(
            text = stringResource(R.string.kakeibo_review_plan),
            onClick = viewModel::reviewPlan,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("kakeibo-review-plan"),
        )
    }
}

@Composable
private fun KakeiboCategoryAmount(
    category: KakeiboCategory,
    currencyCode: String,
    value: String,
    amountMinorUnits: Long,
    maxMinorUnits: Long?,
    onTextChange: (String) -> Unit,
    onSliderChange: (Long) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            categoryLabel(category),
            modifier = Modifier.weight(0.35f),
            style = MaterialTheme.typography.titleSmall,
        )
        Column(Modifier.weight(0.65f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val sliderDescription = stringResource(R.string.kakeibo_category_slider, categoryLabel(category))
            val maximum = maxMinorUnits ?: 0L
            Slider(
                value = (amountMinorUnits.toDouble() / maximum.coerceAtLeast(1L).toDouble()).toFloat().coerceIn(0f, 1f),
                enabled = maximum > 0L,
                onValueChange = { ratio ->
                    onSliderChange((ratio.toDouble() * maximum).roundToLong())
                },
                colors = SliderDefaults.colors(
                    thumbColor = ExpenseStyle.Green,
                    activeTrackColor = ExpenseStyle.Green,
                    inactiveTrackColor = ExpenseStyle.Pale,
                ),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .semantics {
                        contentDescription = sliderDescription
                        stateDescription = value
                    },
            )
            AmountInput(
                label = stringResource(R.string.kakeibo_category_amount, categoryLabel(category)),
                value = value,
                currencyCode = currencyCode,
                onValueChange = onTextChange,
            )
        }
    }
}

@Composable
internal fun KakeiboWeeklyPacingScreen(state: KakeiboUiState, viewModel: KakeiboViewModel) {
    val categories = canonicalKakeiboCategories
    val weeks = remember(state.month, Locale.getDefault()) {
        com.nestvest.kakeibo.domain.KakeiboPeriod(state.month).weeks(Locale.getDefault())
    }
    val currencyCode = viewModel.profile.preferredCurrency
    val dateFormat = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
    KakeiboFrame(
        stringResource(R.string.kakeibo_weekly_heading), viewModel::back,
        eyebrow = stringResource(R.string.kakeibo_weekly_title),
        subtitle = "${state.month.localizedLabel()} · $currencyCode",
    ) {
        KakeiboCard(soft = true) { Text(stringResource(R.string.kakeibo_weekly_description),
            color = ExpenseStyle.Muted, style = MaterialTheme.typography.bodySmall) }
        categories.forEach { category ->
            KakeiboCard {
                Text(categoryLabel(category), style = MaterialTheme.typography.titleSmall)
                val columns = if (androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f) 1 else 2
                weeks.chunked(columns).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { week ->
                            val key = "${category.name}:${week.weekStart}"
                            OutlinedTextField(
                                value = state.draft.weeklyAmounts[key].orEmpty(),
                                onValueChange = { viewModel.updateWeeklyAmount(category, week.weekStart, it) },
                                label = { Text("${week.weekStart.format(dateFormat)} – ${week.weekEndInclusive.format(dateFormat)}") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f).testTag("kakeibo-weekly-$key"),
                                singleLine = true,
                                shape = ExpenseStyle.ButtonShape,
                                prefix = { Text(Currency.getInstance(currencyCode).symbol) },
                            )
                        }
                    }
                }
                SummaryRow(stringResource(R.string.kakeibo_weekly_total),
                    viewModel.weeklyTotalMinorUnits(category)?.let { amountText(it, currencyCode) }
                        ?: stringResource(R.string.kakeibo_invalid_amount))
                SummaryRow(stringResource(R.string.kakeibo_monthly_limit),
                    viewModel.categoryAmountMinorUnits(category)?.let { amountText(it, currencyCode) }
                        ?: stringResource(R.string.kakeibo_invalid_amount))
            }
        }
        PlanError(state.planError)
        ExpensePrimaryButton(text = stringResource(R.string.kakeibo_save_weekly), onClick = viewModel::finishWeeklyPacing)
        TextButton(onClick = viewModel::skipWeeklyPacing, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.kakeibo_skip_weekly))
        }
    }
}

@Composable
internal fun KakeiboPlanReviewScreen(state: KakeiboUiState, viewModel: KakeiboViewModel) {
    val currencyCode = viewModel.profile.preferredCurrency
    val invalidAmount = stringResource(R.string.kakeibo_invalid_amount)
    KakeiboFrame(
        stringResource(R.string.kakeibo_review_heading), viewModel::cancelPlanReview,
        eyebrow = stringResource(R.string.kakeibo_plan_review_title),
        subtitle = stringResource(R.string.kakeibo_plan_review_description, state.month.localizedLabel()),
    ) {
        Text(currencyCode, style = MaterialTheme.typography.labelMedium, color = ExpenseStyle.Muted)
        KakeiboCard(soft = true, modifier = Modifier.testTag("kakeibo-review-totals")) {
            SummaryRow(
                stringResource(
                    if (state.draft.availableAmount.isBlank()) R.string.kakeibo_calculated_overall
                    else R.string.kakeibo_available_optional,
                ),
                (if (state.draft.availableAmount.isBlank()) viewModel.allocationTotalMinorUnits()
                else viewModel.availableMinorUnits())?.let { amountText(it, currencyCode) } ?: invalidAmount,
            )
            SummaryRow(
                stringResource(R.string.kakeibo_savings_label),
                viewModel.savingsAmountMinorUnits()?.let { amountText(it, currencyCode) } ?: invalidAmount,
            )
            SummaryRow(
                stringResource(R.string.kakeibo_allocated_label),
                viewModel.allocationTotalMinorUnits()?.let { amountText(it, currencyCode) } ?: invalidAmount,
            )
            if (state.draft.availableAmount.isBlank()) {
                Text(stringResource(R.string.kakeibo_calculated_hint), color = ExpenseStyle.Muted)
            }
        }
        KakeiboCard(modifier = Modifier.testTag("kakeibo-review-categories")) {
            canonicalKakeiboCategories.forEach { category ->
                SummaryRow(
                    categoryLabel(category),
                    viewModel.categoryAmountMinorUnits(category)?.let { amountText(it, currencyCode) }
                        ?: invalidAmount,
                )
            }
        }
        KakeiboCard(soft = true) { Text(stringResource(R.string.kakeibo_plan_only_hint),
            color = ExpenseStyle.Muted, style = MaterialTheme.typography.bodySmall) }
        Text(stringResource(if (state.draft.weeklyPacingEnabled) R.string.kakeibo_review_weekly_enabled
            else R.string.kakeibo_review_weekly_omitted), color = ExpenseStyle.Muted,
            style = MaterialTheme.typography.bodySmall)
        if (state.planError != null) PlanError(state.planError)
        if (state.planSaving) LinearProgressIndicator(Modifier.fillMaxWidth())
        ExpensePrimaryButton(
            text = stringResource(R.string.kakeibo_confirm_save),
            onClick = viewModel::savePlan,
            enabled = !state.planSaving,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        )
        ExpenseSecondaryButton(
            text = stringResource(R.string.kakeibo_cancel),
            onClick = viewModel::cancelPlanReview,
            enabled = !state.planSaving,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        )
    }
}

@Composable
internal fun KakeiboProgressScreen(state: KakeiboUiState, viewModel: KakeiboViewModel) {
    val month = state.month.localizedLabel()
    KakeiboFrame(
        stringResource(R.string.kakeibo_progress_heading), viewModel::back,
        eyebrow = stringResource(R.string.kakeibo_progress_title, month),
        subtitle = stringResource(R.string.kakeibo_progress_intro),
    ) {
        if (state.planSaved) {
            Text(stringResource(R.string.kakeibo_plan_saved), color = ExpenseStyle.Green,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        when {
            state.expenses == null || state.plan == null -> LinearProgressIndicator(Modifier.fillMaxWidth())
            state.expenses is KakeiboRead.Failed || state.plan is KakeiboRead.Failed ->
                ErrorMessage(stringResource(R.string.kakeibo_progress_unavailable), viewModel::retry)
            else -> {
                val progress = viewModel.progress()
                if (progress == null) {
                    ErrorMessage(stringResource(R.string.kakeibo_progress_unavailable), viewModel::retry)
                } else {
                    ProgressOverview(progress)
                    progress.categoryProgress.forEach { row ->
                        KakeiboCategoryProgressRow(row, progress.currencyCode) {
                            viewModel.openCategory(row.category)
                        }
                    }
                    ProgressExceptions(progress)
                }
            }
        }
        ExpenseSecondaryButton(
            text = stringResource(if (state.plan is KakeiboRead.Found) R.string.kakeibo_edit_plan else R.string.kakeibo_plan_month),
            onClick = viewModel::beginPlan,
            enabled = state.plan is KakeiboRead.Found || state.plan == KakeiboRead.Absent,
            modifier = Modifier.fillMaxWidth(),
        )
        ExpensePrimaryButton(
            text = stringResource(R.string.kakeibo_reflection),
            onClick = viewModel::openReflection,
        )
    }
}

@Composable
private fun ProgressOverview(progress: MonthlyProgress) {
    KakeiboCard(soft = true, modifier = Modifier.testTag("kakeibo-progress-overview")) {
        if (progress.plan == null) Text(stringResource(R.string.kakeibo_progress_without_plan),
            style = MaterialTheme.typography.bodySmall, color = ExpenseStyle.Muted)
        val spent = checkedTotal(progress.categoryProgress.map { it.spentMinorUnits })
        SummaryRow(stringResource(R.string.kakeibo_recorded_label),
            spent?.let { amountText(it, progress.currencyCode) } ?: stringResource(R.string.kakeibo_progress_unavailable))
        SummaryRow(stringResource(R.string.kakeibo_categories_label),
            progress.plan?.let { amountText(it.categoryTotalMinorUnits, progress.currencyCode) }
                ?: stringResource(R.string.kakeibo_no_plan_short))
        progress.plan?.let { plan ->
            plan.availableMinorUnits?.let {
                SummaryRow(stringResource(R.string.kakeibo_unassigned_label),
                    amountText(it - plan.totalAllocationMinorUnits, progress.currencyCode))
            }
        }
    }
}

@Composable
private fun ProgressExceptions(progress: MonthlyProgress) {
    Text(stringResource(R.string.kakeibo_exceptions_heading), style = MaterialTheme.typography.titleSmall)
    KakeiboCard(modifier = Modifier.testTag("kakeibo-progress-exceptions")) {
        SummaryRow(stringResource(R.string.kakeibo_unassigned_heading, progress.unassignedExpenses.size),
            checkedTotal(progress.unassignedExpenses.map { it.amountMinorUnits })
                ?.let { amountText(it, progress.currencyCode) } ?: stringResource(R.string.kakeibo_progress_unavailable))
        progress.otherCurrencyExpenses.groupBy { it.currencyCode }.forEach { (currency, expenses) ->
            SummaryRow(stringResource(R.string.kakeibo_foreign_currency, currency),
                checkedTotal(expenses.map { it.amountMinorUnits })?.let { amountText(it, currency) }
                    ?: stringResource(R.string.kakeibo_progress_unavailable))
        }
        if (progress.otherCurrencyExpenses.isNotEmpty()) {
            Text(stringResource(R.string.kakeibo_not_converted), color = ExpenseStyle.Muted,
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun HealthStatus(health: CategoryHealth) {
    val color = if (health == CategoryHealth.OVER_PLAN) ExpenseStyle.Error else ExpenseStyle.Green
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = ExpenseStyle.Note,
            border = BorderStroke(1.dp, ExpenseStyle.Line), modifier = Modifier.size(24.dp)) {
            Box(contentAlignment = Alignment.Center) { Text(healthIcon(health), color = color, fontSize = 12.sp) }
        }
        Text(healthLabel(health), color = color, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun KakeiboCategoryProgressRow(
    row: com.nestvest.kakeibo.domain.CategoryProgress,
    currencyCode: String,
    onClick: () -> Unit,
) {
    val name = categoryLabel(row.category)
    val spent = amountText(row.spentMinorUnits, currencyCode)
    val health = healthLabel(row.health)
    val allocated = row.plannedMinorUnits?.let { amountText(it, currencyCode) } ?: stringResource(R.string.kakeibo_no_plan_short)
    val remaining = row.remainingMinorUnits?.let { amountText(it, currencyCode) } ?: stringResource(R.string.kakeibo_not_available)
    val accessible = stringResource(R.string.kakeibo_category_progress_accessibility, name, allocated, spent, remaining, health)
    KakeiboCard(
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).heightIn(min = 64.dp)
            .testTag("kakeibo-progress-${row.category.name}")
            .semantics {
                contentDescription = accessible
            },
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.kakeibo_planned_label, allocated),
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                color = ExpenseStyle.Muted, style = MaterialTheme.typography.labelSmall,
            )
        }
        val ratio = row.plannedMinorUnits?.let { cap ->
            if (cap == 0L) { if (row.spentMinorUnits > 0L) 1f else 0f }
            else (row.spentMinorUnits.toDouble() / cap.toDouble()).toFloat().coerceIn(0f, 1f)
        } ?: 0f
        LinearProgressIndicator(
            progress = { ratio },
            modifier = Modifier.fillMaxWidth().height(5.dp),
            color = if (row.health == CategoryHealth.OVER_PLAN) ExpenseStyle.Error else ExpenseStyle.Green,
            trackColor = ExpenseStyle.Pale,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.kakeibo_spent_label, spent), modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall, color = ExpenseStyle.Muted)
            Text(stringResource(R.string.kakeibo_remaining_label, remaining), modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                style = MaterialTheme.typography.bodySmall,
                color = if (row.health == CategoryHealth.OVER_PLAN) ExpenseStyle.Error else ExpenseStyle.Muted)
        }
        HealthStatus(row.health)
        TextButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.kakeibo_view_category, name))
        }
    }
}

@Composable
internal fun KakeiboCategoryScreen(
    state: KakeiboUiState,
    category: KakeiboCategory,
    viewModel: KakeiboViewModel,
) {
    val progress = viewModel.progress()
    val row = progress?.categoryProgress?.firstOrNull { it.category == category }
    KakeiboFrame(categoryLabel(category), viewModel::back,
        eyebrow = "${state.month.localizedLabel()} · ${viewModel.profile.preferredCurrency}") {
        if (row == null) {
            ErrorMessage(stringResource(R.string.kakeibo_progress_unavailable), viewModel::retry)
        } else {
            KakeiboCard(soft = true) {
                SummaryRow(stringResource(R.string.kakeibo_allocated_short), row.plannedMinorUnits?.let {
                    amountText(it, progress.currencyCode)
                } ?: stringResource(R.string.kakeibo_no_plan_short))
                SummaryRow(stringResource(R.string.kakeibo_recorded_label), amountText(row.spentMinorUnits, progress.currencyCode))
                SummaryRow(stringResource(R.string.kakeibo_remaining_short), row.remainingMinorUnits?.let {
                    amountText(it, progress.currencyCode)
                } ?: stringResource(R.string.kakeibo_not_available))
                HealthStatus(row.health)
            }
            KakeiboCard {
                DailySpendingChart(
                    row.dailySpendingMinorUnits,
                    state.month.atDay(1),
                    categoryLabel(category),
                    progress.currencyCode,
                )
            }
            val unassigned = progress.unassignedExpenses
            Text(stringResource(R.string.kakeibo_pull_unassigned), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.kakeibo_assignment_hint, progress.currencyCode), color = ExpenseStyle.Muted,
                style = MaterialTheme.typography.bodySmall)
            if (unassigned.isNotEmpty()) {
                KakeiboCard {
                    unassigned.forEach { expense ->
                        UnassignedExpenseRow(
                            expense, expense.expenseId in state.assignmentExpenseIds, viewModel.zoneId(),
                            enabled = !state.assignmentSaving,
                        ) { viewModel.beginAssignment(category, expense) }
                    }
                }
            } else {
                Text(stringResource(R.string.kakeibo_no_unassigned))
            }
            if (state.assignmentSaved) {
                Text(stringResource(R.string.kakeibo_assignment_saved), color = ExpenseStyle.Green,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
            if (state.assignmentError != null) {
                Text(
                    stringResource(
                        if (state.assignmentError == "assignment_changed") R.string.kakeibo_assignment_changed
                        else R.string.kakeibo_assignment_failed,
                    ),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            ExpensePrimaryButton(
                text = stringResource(R.string.kakeibo_assign_confirm, state.assignmentExpenseIds.size),
                enabled = state.assignmentExpenseIds.isNotEmpty() && !state.assignmentSaving,
                onClick = viewModel::confirmAssignment,
            )
            ExpenseSecondaryButton(
                text = stringResource(R.string.kakeibo_cancel), onClick = viewModel::cancelAssignment,
                enabled = state.assignmentExpenseIds.isNotEmpty() && !state.assignmentSaving,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun UnassignedExpenseRow(expense: Expense, selected: Boolean, zoneId: java.time.ZoneId,
    enabled: Boolean, onAssign: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .testTag("kakeibo-select-${expense.expenseId}")
            .toggleable(value = selected, enabled = enabled, role = Role.Checkbox, onValueChange = { onAssign() }),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(checked = selected, onCheckedChange = null, enabled = enabled)
        Column(Modifier.weight(1f)) {
            Text(expense.localDate(zoneId).format(DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)),
                style = MaterialTheme.typography.labelSmall, color = ExpenseStyle.Muted)
            Text(stringResource(
                R.string.kakeibo_expense_description,
                expense.vendor ?: stringResource(R.string.kakeibo_expense_no_vendor),
                amountText(expense.amountMinorUnits, expense.currencyCode),
            ), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun DailySpendingChart(
    values: List<Long>,
    firstDate: LocalDate,
    category: String,
    currencyCode: String,
) {
    val max = values.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    val context = LocalContext.current
    val dailyAmounts = values.mapIndexed { index, amount ->
        context.getString(
            R.string.kakeibo_chart_day,
            firstDate.plusDays(index.toLong()).format(
                DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)
                    .withLocale(Locale.getDefault()),
            ),
            amountText(amount, currencyCode),
        )
    }.joinToString(separator = "; ")
    val summary = stringResource(
        R.string.kakeibo_chart_accessibility,
        category,
        values.size,
        dailyAmounts,
    )
    Row(
        Modifier.fillMaxWidth().height(90.dp).semantics { contentDescription = summary }
            .testTag("kakeibo-daily-chart"),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEach { amount ->
            val barHeight = if (amount == 0L) 2.dp
            else (8 + (66 * (amount.toDouble() / max.toDouble())).roundToLong().toInt()).dp
            Box(
                Modifier.weight(1f).height(barHeight)
                    .background(ExpenseStyle.Green, RoundedCornerShape(2.dp)),
            )
        }
    }
    Text(stringResource(R.string.kakeibo_chart_label), style = MaterialTheme.typography.labelSmall)
    if (values.isNotEmpty()) {
        val format = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
        SummaryRow(firstDate.format(format), firstDate.plusDays(values.lastIndex.toLong()).format(format))
    }
    if (values.all { it == 0L }) {
        Text(stringResource(R.string.kakeibo_chart_empty), style = MaterialTheme.typography.bodySmall, color = ExpenseStyle.Muted)
    }
}

@Composable
internal fun KakeiboReflectionScreen(state: KakeiboUiState, viewModel: KakeiboViewModel) {
    val loaded = state.reflection != null && state.reflection !is KakeiboRead.Failed
    KakeiboFrame(
        stringResource(R.string.kakeibo_reflection_heading), viewModel::back,
        eyebrow = stringResource(R.string.kakeibo_reflection_month, state.month.localizedLabel()),
        subtitle = stringResource(R.string.kakeibo_reflection_intro),
    ) {
        when (state.reflection) {
            null -> LinearProgressIndicator(Modifier.fillMaxWidth())
            is KakeiboRead.Failed -> ErrorMessage(stringResource(R.string.kakeibo_reflection_read_error), viewModel::openReflection)
            else -> Unit
        }
        ReflectionComparison(state, viewModel)
        ReflectionInput(
            stringResource(R.string.kakeibo_reflection_went_well),
            state.whatWentWell, loaded && !state.reflectionSaving,
        ) { viewModel.updateReflectionField(KakeiboViewModel.ReflectionField.WENT_WELL, it) }
        ReflectionInput(
            stringResource(R.string.kakeibo_reflection_change),
            state.whatToChange, loaded && !state.reflectionSaving,
        ) { viewModel.updateReflectionField(KakeiboViewModel.ReflectionField.CHANGE, it) }
        ReflectionInput(
            stringResource(R.string.kakeibo_reflection_next),
            state.nextMonthIntention, loaded && !state.reflectionSaving,
        ) { viewModel.updateReflectionField(KakeiboViewModel.ReflectionField.NEXT_INTENTION, it) }
        if (state.reflectionError == "storage") {
            Text(stringResource(R.string.kakeibo_reflection_save_error), color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        if (state.reflectionSaving) LinearProgressIndicator(Modifier.fillMaxWidth())
        ExpensePrimaryButton(
            text = stringResource(R.string.kakeibo_save_reflection),
            onClick = viewModel::saveReflection,
            enabled = loaded && !state.reflectionSaving,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        )
        TextButton(onClick = viewModel::back, enabled = !state.reflectionSaving,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.kakeibo_not_now))
        }
    }
}

@Composable
private fun ReflectionComparison(state: KakeiboUiState, viewModel: KakeiboViewModel) {
    val progress = viewModel.progress()
    KakeiboCard(soft = true, modifier = Modifier.testTag("kakeibo-reflection-comparison")) {
        Text(stringResource(R.string.kakeibo_recorded_heading), style = MaterialTheme.typography.titleSmall)
        if (state.plan == null || state.expenses == null) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        } else if (progress == null) {
            ErrorMessage(stringResource(R.string.kakeibo_progress_unavailable), viewModel::retry)
        } else {
            progress.categoryProgress.forEach { row ->
                SummaryRow(
                    categoryLabel(row.category),
                    if (row.plannedMinorUnits == null) stringResource(
                        R.string.kakeibo_recorded_without_plan, amountText(row.spentMinorUnits, progress.currencyCode),
                    ) else stringResource(
                        R.string.kakeibo_spent_of_planned,
                        amountText(row.spentMinorUnits, progress.currencyCode),
                        amountText(row.plannedMinorUnits, progress.currencyCode),
                    ),
                )
                if (row.category != canonicalKakeiboCategories.last()) {
                    androidx.compose.material3.HorizontalDivider(color = ExpenseStyle.Line)
                }
            }
            Text(stringResource(R.string.kakeibo_reflection_live_hint), style = MaterialTheme.typography.bodySmall,
                color = ExpenseStyle.Muted)
        }
    }
}

@Composable
internal fun KakeiboSavedReflectionScreen(state: KakeiboUiState, viewModel: KakeiboViewModel) {
    KakeiboFrame(
        stringResource(R.string.kakeibo_saved_reflection_heading), viewModel::back,
        eyebrow = stringResource(R.string.kakeibo_saved_reflection_eyebrow, state.month.localizedLabel()),
        subtitle = stringResource(R.string.kakeibo_saved_reflection_intro),
    ) {
        val saved = (state.reflection as? KakeiboRead.Found)?.value
        if (saved == null) {
            ErrorMessage(stringResource(R.string.kakeibo_reflection_read_error), viewModel::openReflection)
        } else {
            KakeiboCard(soft = true, modifier = Modifier.testTag("kakeibo-saved-reflection")) {
                listOf(
                    R.string.kakeibo_reflection_went_well to saved.whatWentWell,
                    R.string.kakeibo_reflection_change to saved.whatToChange,
                    R.string.kakeibo_reflection_next to saved.nextMonthIntention,
                ).forEach { (label, answer) ->
                    Text(stringResource(label), color = ExpenseStyle.Muted, style = MaterialTheme.typography.labelMedium)
                    Text(answer.ifBlank { stringResource(R.string.kakeibo_reflection_empty_answer) },
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
            ReflectionComparison(state, viewModel)
            ExpenseSecondaryButton(
                text = stringResource(R.string.kakeibo_back_to_progress),
                onClick = viewModel::returnToProgress, modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = viewModel::editReflection, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.kakeibo_edit_reflection))
            }
        }
    }
}

@Composable
private fun ReflectionInput(label: String, value: String, enabled: Boolean, onValueChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = value, onValueChange = onValueChange, enabled = enabled,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
            shape = ExpenseStyle.ButtonShape, minLines = 2,
        )
    }
}

@Composable
private fun AmountInput(
    label: String,
    value: String,
    currencyCode: String,
    onValueChange: (String) -> Unit,
    optional: Boolean = false,
    readOnly: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        readOnly = readOnly,
        label = { Text(label) },
        supportingText = if (optional) ({ Text(stringResource(R.string.kakeibo_optional)) }) else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = ExpenseStyle.ButtonShape,
        prefix = { Text(Currency.getInstance(currencyCode).symbol) },
        isError = !readOnly && value.isNotBlank() && !isValidInput(value, currencyCode),
    )
}

@Composable
private fun KakeiboCard(
    modifier: Modifier = Modifier,
    soft: Boolean = false,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (soft) ExpenseStyle.NoteBorder else ExpenseStyle.Line),
        colors = CardDefaults.cardColors(
            containerColor = if (soft) ExpenseStyle.Note else ExpenseStyle.Paper,
        ),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) { content() }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = ExpenseStyle.Muted,
        )
        Text(
            value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

@Composable
private fun ErrorMessage(message: String, onRetry: () -> Unit) {
    Text(message, color = MaterialTheme.colorScheme.error)
    TextButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) {
        Text(stringResource(R.string.retry))
    }
}

@Composable
private fun PlanError(error: String?) {
    if (error == null) return
    val message = when (error) {
        "invalid_amount" -> R.string.kakeibo_invalid_amount
        "invalid_plan" -> R.string.kakeibo_invalid_plan
        "learning_storage" -> R.string.kakeibo_learning_storage_error
        else -> R.string.kakeibo_save_error
    }
    Text(stringResource(message), color = MaterialTheme.colorScheme.error)
}

@Composable
private fun categoryLabel(category: KakeiboCategory): String = stringResource(
    when (category) {
        KakeiboCategory.Needs -> R.string.kakeibo_category_needs
        KakeiboCategory.Wants -> R.string.kakeibo_category_wants
        KakeiboCategory.Culture -> R.string.kakeibo_category_culture
        KakeiboCategory.Unexpected -> R.string.kakeibo_category_unexpected
    },
)

@Composable
private fun categoryExample(category: KakeiboCategory): Int = when (category) {
    KakeiboCategory.Needs -> R.string.kakeibo_example_needs
    KakeiboCategory.Wants -> R.string.kakeibo_example_wants
    KakeiboCategory.Culture -> R.string.kakeibo_example_culture
    KakeiboCategory.Unexpected -> R.string.kakeibo_example_unexpected
}

private fun categorySymbol(category: KakeiboCategory): ExpenseSymbol = when (category) {
    KakeiboCategory.Needs -> ExpenseSymbol.NEEDS
    KakeiboCategory.Wants -> ExpenseSymbol.WANTS
    KakeiboCategory.Culture -> ExpenseSymbol.CULTURE
    KakeiboCategory.Unexpected -> ExpenseSymbol.UNEXPECTED
}

@Composable
private fun healthLabel(health: CategoryHealth): String = stringResource(
    when (health) {
        CategoryHealth.NO_PLAN -> R.string.kakeibo_health_no_plan
        CategoryHealth.NO_ALLOCATION -> R.string.kakeibo_health_no_allocation
        CategoryHealth.WITHIN_PLAN -> R.string.kakeibo_health_within_plan
        CategoryHealth.AT_LIMIT -> R.string.kakeibo_health_at_limit
        CategoryHealth.OVER_PLAN -> R.string.kakeibo_health_over_plan
    },
)

private fun healthIcon(health: CategoryHealth): String = when (health) {
    CategoryHealth.NO_PLAN -> "—"
    CategoryHealth.NO_ALLOCATION -> "○"
    CategoryHealth.WITHIN_PLAN -> "✓"
    CategoryHealth.AT_LIMIT -> "="
    CategoryHealth.OVER_PLAN -> "!"
}

@Composable
private fun amountText(minorUnits: Long, currencyCode: String): String {
    val currency = Currency.getInstance(currencyCode)
    val value = BigDecimal.valueOf(minorUnits, currency.defaultFractionDigits.coerceAtLeast(0))
    return NumberFormat.getCurrencyInstance(Locale.getDefault()).apply { this.currency = currency }
        .format(value)
}

private fun amountInput(minorUnits: Long, currencyCode: String): String {
    val digits = Currency.getInstance(currencyCode).defaultFractionDigits.coerceAtLeast(0)
    val decimalSeparator = java.text.DecimalFormatSymbols.getInstance(Locale.getDefault()).decimalSeparator
    val raw = BigDecimal.valueOf(minorUnits, digits).stripTrailingZeros().toPlainString()
    return if (decimalSeparator == '.') raw else raw.replace('.', decimalSeparator)
}

private fun isValidInput(text: String, currencyCode: String): Boolean = try {
    com.nestvest.kakeibo.domain.parseMinorUnits(text, currencyCode, Locale.getDefault())
    true
} catch (_: IllegalArgumentException) {
    false
} catch (_: ArithmeticException) {
    false
}

private fun checkedTotal(values: List<Long>): Long? = try {
    values.fold(0L, Math::addExact)
} catch (_: ArithmeticException) {
    null
}

private fun CategoryHealth.rankForDisplay(): Int = when (this) {
    CategoryHealth.NO_PLAN -> 0
    CategoryHealth.NO_ALLOCATION -> 1
    CategoryHealth.WITHIN_PLAN -> 2
    CategoryHealth.AT_LIMIT -> 3
    CategoryHealth.OVER_PLAN -> 4
}
