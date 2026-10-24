package com.nestvest.kakeibo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nestvest.expense.domain.Expense
import com.nestvest.kakeibo.domain.EpochRange
import com.nestvest.kakeibo.domain.KakeiboCategory
import com.nestvest.kakeibo.domain.KakeiboPeriod
import com.nestvest.kakeibo.domain.KakeiboRead
import com.nestvest.kakeibo.domain.KakeiboRepository
import com.nestvest.kakeibo.domain.KakeiboWrite
import com.nestvest.kakeibo.domain.LearningPromptState
import com.nestvest.kakeibo.domain.MonthlyPlan
import com.nestvest.kakeibo.domain.PeriodReflection
import com.nestvest.kakeibo.domain.WeeklyAllocation
import com.nestvest.kakeibo.domain.calculateMonthlyProgress
import com.nestvest.kakeibo.domain.parseMinorUnits
import com.nestvest.kakeibo.domain.canonicalKakeiboCategories
import com.nestvest.profile.domain.PrimaryProfile
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class KakeiboPlanDraft(
    val availableAmount: String = "",
    val savingsAmount: String = "0",
    val categoryAmounts: Map<KakeiboCategory, String> =
        canonicalKakeiboCategories.associateWith { "0" },
    val weeklyPacingEnabled: Boolean = false,
    val weeklyAmounts: Map<String, String> = emptyMap(),
)

data class KakeiboUiState(
    val navigation: KakeiboNavigationState = KakeiboNavigationState(),
    val month: YearMonth = YearMonth.now(),
    val plan: KakeiboRead<MonthlyPlan>? = null,
    val expenses: KakeiboRead<List<Expense>>? = null,
    val learningState: KakeiboRead<LearningPromptState>? = null,
    val showLearningPrompt: Boolean = false,
    val learningError: Boolean = false,
    val draft: KakeiboPlanDraft = KakeiboPlanDraft(),
    val planError: String? = null,
    val planSaving: Boolean = false,
    val planSaved: Boolean = false,
    val showPlanConfirmation: Boolean = false,
    val reflection: KakeiboRead<PeriodReflection>? = null,
    val whatWentWell: String = "",
    val whatToChange: String = "",
    val nextMonthIntention: String = "",
    val reflectionSaving: Boolean = false,
    val reflectionError: String? = null,
    val assignmentError: String? = null,
    val assignmentSaving: Boolean = false,
    val assignmentSaved: Boolean = false,
    val assignmentExpenseIds: Set<UUID> = emptySet(),
    val categoryForAssignment: KakeiboCategory? = null,
)

class KakeiboViewModel(
    private val repository: KakeiboRepository,
    val profile: PrimaryProfile,
    private val clock: () -> Clock = { Clock.systemDefaultZone() },
    private val locale: () -> Locale = { Locale.getDefault() },
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        KakeiboUiState(month = YearMonth.now(clock())),
    )
    val state = mutableState.asStateFlow()
    private var expenseObservation: Job? = null
    private var planReadJob: Job? = null
    private var reflectionReadJob: Job? = null

    init {
        loadMonth()
        loadLearningState()
    }

    fun navigate(destination: KakeiboDestination) {
        mutableState.value = state.value.copy(
            navigation = state.value.navigation.navigate(destination),
            planError = null,
            planSaved = false,
            assignmentError = null,
        )
    }

    fun back() {
        if (state.value.reflectionSaving || state.value.assignmentSaving) return
        reflectionReadJob?.cancel()
        mutableState.value = state.value.copy(
            navigation = state.value.navigation.back(),
            showPlanConfirmation = false,
            assignmentExpenseIds = emptySet(),
            categoryForAssignment = null,
        )
    }

    fun selectMonth(month: YearMonth) {
        if (state.value.assignmentSaving) return
        reflectionReadJob?.cancel()
        mutableState.value = state.value.copy(
            month = month,
            plan = null,
            expenses = null,
            draft = KakeiboPlanDraft(),
            planError = null,
            assignmentExpenseIds = emptySet(),
            categoryForAssignment = null,
            assignmentError = null,
            assignmentSaved = false,
        )
        loadMonth()
    }

    fun changeMonth(months: Long) {
        selectMonth(state.value.month.plusMonths(months))
    }

    fun retry() {
        loadMonth()
        loadLearningState()
    }

    fun openLearning() {
        viewModelScope.launch {
            val current = (state.value.learningState as? KakeiboRead.Found)?.value
            if (current == null) {
                mutableState.value = state.value.copy(learningError = true)
                return@launch
            }
            if (current == LearningPromptState.UNSEEN) {
                when (repository.saveLearningState(LearningPromptState.VIEWED)) {
                    KakeiboWrite.Saved -> {
                        mutableState.value = state.value.copy(
                            learningState = KakeiboRead.Found(LearningPromptState.VIEWED),
                            showLearningPrompt = false,
                            learningError = false,
                        )
                    }
                    else -> {
                        mutableState.value = state.value.copy(learningError = true)
                        return@launch
                    }
                }
            }
            navigate(KakeiboDestination.Learning)
        }
    }

    fun dismissLearningPrompt() {
        viewModelScope.launch {
            when (repository.saveLearningState(LearningPromptState.DISMISSED)) {
                KakeiboWrite.Saved -> mutableState.value = state.value.copy(
                    learningState = KakeiboRead.Found(LearningPromptState.DISMISSED),
                    showLearningPrompt = false,
                )
                else -> mutableState.value = state.value.copy(learningError = true)
            }
        }
    }

    fun planNowFromPrompt() {
        viewModelScope.launch {
            when (repository.saveLearningState(LearningPromptState.DISMISSED)) {
                KakeiboWrite.Saved -> {
                    mutableState.value = state.value.copy(
                        learningState = KakeiboRead.Found(LearningPromptState.DISMISSED),
                        showLearningPrompt = false,
                    )
                    beginPlan()
                }
                else -> mutableState.value = state.value.copy(learningError = true)
            }
        }
    }

    fun beginPlan() {
        val saved = (state.value.plan as? KakeiboRead.Found)?.value
        mutableState.value = state.value.copy(
            draft = saved?.toDraft() ?: KakeiboPlanDraft(),
            planError = null,
            showPlanConfirmation = false,
            planSaved = false,
        )
        navigate(KakeiboDestination.MonthlyPlan)
    }

    fun updateAvailableAmount(value: String) {
        mutableState.value = state.value.copy(draft = state.value.draft.copy(availableAmount = value), planError = null)
    }

    fun updateSavingsAmount(value: String) {
        mutableState.value = state.value.copy(draft = state.value.draft.copy(savingsAmount = value), planError = null)
    }

    fun updateCategoryAmount(category: KakeiboCategory, value: String) {
        mutableState.value = state.value.copy(
            draft = state.value.draft.copy(categoryAmounts = state.value.draft.categoryAmounts + (category to value)),
            planError = null,
        )
    }

    fun toggleWeeklyPacing(enabled: Boolean) {
        mutableState.value = state.value.copy(
            draft = state.value.draft.copy(weeklyPacingEnabled = enabled),
            planError = null,
        )
    }

    fun updateWeeklyAmount(category: KakeiboCategory, weekStart: LocalDate, value: String) {
        val key = "${category.name}:$weekStart"
        mutableState.value = state.value.copy(
            draft = state.value.draft.copy(weeklyAmounts = state.value.draft.weeklyAmounts + (key to value)),
            planError = null,
        )
    }

    fun weeklyTotalMinorUnits(category: KakeiboCategory): Long? = try {
        KakeiboPeriod(state.value.month).weeks(locale()).fold(0L) { total, week ->
            val amount = parseMinorUnits(
                state.value.draft.weeklyAmounts["${category.name}:${week.weekStart}"].orEmpty(),
                profile.preferredCurrency,
                locale(),
                blankAsZero = true,
            ) ?: return null
            Math.addExact(total, amount)
        }
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: ArithmeticException) {
        null
    }

    fun finishWeeklyPacing() {
        if (buildDraftPlan() != null) back()
    }

    fun skipWeeklyPacing() {
        toggleWeeklyPacing(false)
        back()
    }

    fun reviewPlan() {
        if (buildDraftPlan() == null) return
        mutableState.value = state.value.copy(showPlanConfirmation = true)
        navigate(KakeiboDestination.PlanReview)
    }

    fun cancelPlanReview() {
        mutableState.value = state.value.copy(showPlanConfirmation = false)
        back()
    }

    fun savePlan() {
        val plan = buildDraftPlan() ?: return
        mutableState.value = state.value.copy(planSaving = true, planError = null)
        viewModelScope.launch {
            when (repository.savePlan(plan)) {
                KakeiboWrite.Saved -> {
                    mutableState.value = state.value.copy(
                        planSaving = false,
                        showPlanConfirmation = false,
                        planSaved = true,
                        plan = KakeiboRead.Found(plan),
                        navigation = KakeiboNavigationState(listOf(KakeiboDestination.Home, KakeiboDestination.Progress)),
                    )
                    loadMonth()
                }
                KakeiboWrite.InvalidDraft -> mutableState.value = state.value.copy(
                    planSaving = false,
                    planError = "invalid_plan",
                )
                is KakeiboWrite.Failed -> mutableState.value = state.value.copy(
                    planSaving = false,
                    planError = "storage",
                )
                KakeiboWrite.NotEligible -> mutableState.value = state.value.copy(
                    planSaving = false,
                    planError = "invalid_plan",
                )
            }
        }
    }

    fun openProgress() {
        mutableState.value = state.value.copy(planSaved = false)
        navigate(KakeiboDestination.Progress)
        loadMonth()
    }

    fun openCategory(category: KakeiboCategory) {
        mutableState.value = state.value.copy(assignmentExpenseIds = emptySet(), categoryForAssignment = null,
            assignmentError = null, assignmentSaved = false)
        navigate(KakeiboDestination.CategoryDetail(category.name))
    }

    fun beginAssignment(category: KakeiboCategory, expense: Expense) {
        if (state.value.assignmentSaving) return
        val selected = if (state.value.categoryForAssignment == category) state.value.assignmentExpenseIds else emptySet()
        mutableState.value = state.value.copy(
            assignmentExpenseIds = if (expense.expenseId in selected) selected - expense.expenseId else selected + expense.expenseId,
            categoryForAssignment = category,
            assignmentError = null,
            assignmentSaved = false,
        )
    }

    fun cancelAssignment() {
        if (state.value.assignmentSaving) return
        mutableState.value = state.value.copy(assignmentExpenseIds = emptySet(), categoryForAssignment = null,
            assignmentError = null, assignmentSaved = false)
    }

    fun confirmAssignment() {
        if (state.value.assignmentSaving) return
        val expenseIds = state.value.assignmentExpenseIds
        if (expenseIds.isEmpty()) return
        val category = state.value.categoryForAssignment ?: return
        val month = state.value.month
        mutableState.value = state.value.copy(assignmentSaving = true, assignmentError = null)
        viewModelScope.launch {
            when (repository.assignExpenses(expenseIds, category, month, profile.preferredCurrency)) {
                KakeiboWrite.Saved -> {
                    mutableState.value = state.value.copy(
                        assignmentExpenseIds = emptySet(),
                        categoryForAssignment = null,
                        assignmentError = null,
                        assignmentSaving = false,
                        assignmentSaved = true,
                    )
                    loadMonth()
                }
                KakeiboWrite.NotEligible -> {
                    mutableState.value = state.value.copy(
                        assignmentExpenseIds = emptySet(),
                        categoryForAssignment = null,
                        assignmentError = "assignment_changed",
                        assignmentSaving = false,
                    )
                    loadMonth()
                }
                else -> mutableState.value = state.value.copy(assignmentError = "assignment_failed", assignmentSaving = false)
            }
        }
    }

    fun openReflection() {
        reflectionReadJob?.cancel()
        val month = state.value.month
        if (state.value.navigation.current == KakeiboDestination.Reflection ||
            state.value.navigation.current == KakeiboDestination.SavedReflection) {
            mutableState.value = state.value.copy(navigation = state.value.navigation.replace(KakeiboDestination.Reflection))
        } else navigate(KakeiboDestination.Reflection)
        loadMonth()
        mutableState.value = state.value.copy(
            reflection = null,
            reflectionError = null,
            whatWentWell = "",
            whatToChange = "",
            nextMonthIntention = "",
        )
        reflectionReadJob = viewModelScope.launch {
            val result = repository.readReflection(month)
            if (state.value.month != month || state.value.navigation.current != KakeiboDestination.Reflection) {
                return@launch
            }
            when (result) {
                KakeiboRead.Absent -> mutableState.value = state.value.copy(
                    reflection = KakeiboRead.Absent,
                    whatWentWell = "",
                    whatToChange = "",
                    nextMonthIntention = "",
                )
                is KakeiboRead.Found -> mutableState.value = state.value.copy(
                    reflection = result,
                    navigation = state.value.navigation.replace(KakeiboDestination.SavedReflection),
                    whatWentWell = result.value.whatWentWell,
                    whatToChange = result.value.whatToChange,
                    nextMonthIntention = result.value.nextMonthIntention,
                )
                is KakeiboRead.Failed -> mutableState.value = state.value.copy(
                    reflection = result,
                    reflectionError = "read_failed",
                )
            }
        }
    }

    fun editReflection() {
        mutableState.value = state.value.copy(
            navigation = state.value.navigation.replace(KakeiboDestination.Reflection),
            reflectionError = null,
        )
    }

    fun returnToProgress() {
        val stack = state.value.navigation.backStack
        val index = stack.indexOfLast { it == KakeiboDestination.Progress }
        mutableState.value = state.value.copy(
            navigation = KakeiboNavigationState(
                if (index >= 0) stack.take(index + 1)
                else listOf(KakeiboDestination.Home, KakeiboDestination.Progress),
            ),
        )
        loadMonth()
    }

    fun updateReflectionField(field: ReflectionField, value: String) {
        mutableState.value = when (field) {
            ReflectionField.WENT_WELL -> state.value.copy(whatWentWell = value)
            ReflectionField.CHANGE -> state.value.copy(whatToChange = value)
            ReflectionField.NEXT_INTENTION -> state.value.copy(nextMonthIntention = value)
        }
    }

    fun saveReflection() {
        if (state.value.reflectionSaving) return
        if (state.value.reflection == null || state.value.reflection is KakeiboRead.Failed) {
            mutableState.value = state.value.copy(reflectionError = "read_failed")
            return
        }
        val existing = (state.value.reflection as? KakeiboRead.Found)?.value
        val now = clock().millis()
        val reflection = PeriodReflection(
            reflectionId = existing?.reflectionId ?: UUID.randomUUID(),
            month = state.value.month,
            whatWentWell = state.value.whatWentWell,
            whatToChange = state.value.whatToChange,
            nextMonthIntention = state.value.nextMonthIntention,
            createdAtEpochMillis = existing?.createdAtEpochMillis ?: now,
            updatedAtEpochMillis = now,
        )
        mutableState.value = state.value.copy(reflectionSaving = true, reflectionError = null)
        viewModelScope.launch {
            when (repository.saveReflection(reflection)) {
                KakeiboWrite.Saved -> {
                    mutableState.value = state.value.copy(
                        reflectionSaving = false,
                        reflection = KakeiboRead.Found(reflection),
                        navigation = state.value.navigation.replace(KakeiboDestination.SavedReflection),
                    )
                }
                else -> mutableState.value = state.value.copy(
                    reflectionSaving = false,
                    reflectionError = "storage",
                )
            }
        }
    }

    fun progress() : com.nestvest.kakeibo.domain.MonthlyProgress? {
        val expenseRead = state.value.expenses
        if (expenseRead !is KakeiboRead.Found) return null
        val planRead = state.value.plan ?: return null
        if (planRead is KakeiboRead.Failed) return null
        val plan = (planRead as? KakeiboRead.Found)?.value
        return try {
            calculateMonthlyProgress(
                plan = plan,
                expenses = expenseRead.value,
                zoneId = clock().zone,
                selectedMonth = state.value.month,
                currencyCode = plan?.currencyCode ?: profile.preferredCurrency,
            )
        } catch (_: ArithmeticException) {
            null
        }
    }

    fun zoneId(): ZoneId = clock().zone

    fun availableMinorUnits(): Long? = try {
        parseMinorUnits(state.value.draft.availableAmount, profile.preferredCurrency, locale())
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: ArithmeticException) {
        null
    }

    fun categoryTotalMinorUnits(): Long? = try {
        canonicalKakeiboCategories.fold(0L) { total, category ->
            Math.addExact(total, categoryAmountMinorUnits(category) ?: return null)
        }
    } catch (_: ArithmeticException) {
        null
    }

    fun allocationTotalMinorUnits(): Long? {
        val categories = categoryTotalMinorUnits() ?: return null
        val savings = savingsAmountMinorUnits() ?: return null
        return try {
            Math.addExact(categories, savings)
        } catch (_: ArithmeticException) {
            null
        }
    }

    fun sliderMaximumMinorUnits(): Long? =
        if (state.value.draft.availableAmount.isBlank()) allocationTotalMinorUnits() else availableMinorUnits()

    fun categoryAmountMinorUnits(category: KakeiboCategory): Long? = try {
        parseMinorUnits(
            state.value.draft.categoryAmounts[category].orEmpty(),
            profile.preferredCurrency,
            locale(),
            blankAsZero = true,
        )
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: ArithmeticException) {
        null
    }

    fun savingsAmountMinorUnits(): Long? = try {
        parseMinorUnits(
            state.value.draft.savingsAmount,
            profile.preferredCurrency,
            locale(),
            blankAsZero = true,
        )
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: ArithmeticException) {
        null
    }

    private fun buildDraftPlan(): MonthlyPlan? = try {
        val current = state.value
        val available = parseMinorUnits(current.draft.availableAmount, profile.preferredCurrency, locale())
        val savings = parseMinorUnits(
            current.draft.savingsAmount,
            profile.preferredCurrency,
            locale(),
            blankAsZero = true,
        ) ?: throw IllegalArgumentException("Savings amount is missing")
        val categories = canonicalKakeiboCategories.associateWith { category ->
            parseMinorUnits(
                current.draft.categoryAmounts[category].orEmpty(),
                profile.preferredCurrency,
                locale(),
                blankAsZero = true,
            ) ?: throw IllegalArgumentException("Category amount is missing")
        }
        val weekly = if (current.draft.weeklyPacingEnabled) buildWeeklyAllocations(categories) else emptyList()
        val existing = (current.plan as? KakeiboRead.Found)?.value
        MonthlyPlan(
            planId = existing?.planId ?: UUID.randomUUID(),
            month = current.month,
            currencyCode = profile.preferredCurrency,
            availableMinorUnits = available,
            savingsIntentionMinorUnits = savings,
            weeklyPacingEnabled = current.draft.weeklyPacingEnabled,
            categoryAmounts = categories,
            weeklyAllocations = weekly,
            createdAtEpochMillis = existing?.createdAtEpochMillis ?: clock().millis(),
            updatedAtEpochMillis = clock().millis(),
        )
    } catch (error: IllegalArgumentException) {
        mutableState.value = state.value.copy(planError = "invalid_amount")
        null
    } catch (error: ArithmeticException) {
        mutableState.value = state.value.copy(planError = "invalid_amount")
        null
    }

    private fun buildWeeklyAllocations(categories: Map<KakeiboCategory, Long>): List<WeeklyAllocation> {
        val weeks = KakeiboPeriod(state.value.month).weeks(locale())
        return canonicalKakeiboCategories.flatMap { category ->
            weeks.mapNotNull { week ->
                val text = state.value.draft.weeklyAmounts["${category.name}:${week.weekStart}"].orEmpty()
                val amount = parseMinorUnits(text, profile.preferredCurrency, locale(), blankAsZero = true)
                    ?: throw IllegalArgumentException("Weekly amount is missing")
                if (amount == 0L) null else WeeklyAllocation(
                    category,
                    week.weekStart,
                    week.weekEndInclusive,
                    amount,
                )
            }.also { rows ->
                require(rows.fold(0L) { total, row -> Math.addExact(total, row.plannedMinorUnits) } <=
                    categories.getValue(category))
            }
        }
    }

    private fun loadMonth() {
        val month = state.value.month
        mutableState.value = state.value.copy(plan = null, expenses = null)
        planReadJob?.cancel()
        planReadJob = viewModelScope.launch {
            val planRead = repository.readPlan(month)
            if (state.value.month != month) return@launch
            mutableState.value = state.value.copy(plan = planRead)
            when (planRead) {
                is KakeiboRead.Found -> {
                    if (state.value.navigation.current == KakeiboDestination.Home) {
                        mutableState.value = state.value.copy(draft = planRead.value.toDraft())
                    }
                }
                KakeiboRead.Absent -> {
                    if (state.value.navigation.current == KakeiboDestination.Home) {
                        mutableState.value = state.value.copy(draft = KakeiboPlanDraft())
                    }
                }
                is KakeiboRead.Failed -> Unit
            }
        }
        expenseObservation?.cancel()
        val range: EpochRange = KakeiboPeriod(month).epochRange(clock().zone)
        expenseObservation = viewModelScope.launch {
            repository.observeExpenses(range).collect { expenses ->
                mutableState.value = state.value.copy(expenses = expenses)
            }
        }
    }

    private fun loadLearningState() {
        viewModelScope.launch {
            when (val read = repository.readLearningState()) {
                is KakeiboRead.Found -> mutableState.value = state.value.copy(
                    learningState = read,
                    showLearningPrompt = read.value == LearningPromptState.UNSEEN,
                )
                KakeiboRead.Absent -> mutableState.value = state.value.copy(
                    learningState = read,
                    showLearningPrompt = true,
                )
                is KakeiboRead.Failed -> mutableState.value = state.value.copy(learningState = read)
            }
        }
    }

    private fun MonthlyPlan.toDraft(): KakeiboPlanDraft = KakeiboPlanDraft(
        availableAmount = availableMinorUnits?.asInput(profile.preferredCurrency, locale()).orEmpty(),
        savingsAmount = savingsIntentionMinorUnits.asInput(profile.preferredCurrency, locale()),
        categoryAmounts = categoryAmounts.mapValues { it.value.asInput(profile.preferredCurrency, locale()) },
        weeklyPacingEnabled = weeklyPacingEnabled,
        weeklyAmounts = weeklyAllocations.associate {
            "${it.category.name}:${it.weekStartDate}" to it.plannedMinorUnits.asInput(profile.preferredCurrency, locale())
        },
    )

    private fun Long.asInput(currencyCode: String, locale: Locale): String {
        val digits = java.util.Currency.getInstance(currencyCode).defaultFractionDigits
        val value = BigDecimal.valueOf(this, digits).stripTrailingZeros().toPlainString()
        val decimalSeparator = java.text.DecimalFormatSymbols.getInstance(locale).decimalSeparator
        return if (decimalSeparator == '.') value else value.replace('.', decimalSeparator)
    }

    enum class ReflectionField { WENT_WELL, CHANGE, NEXT_INTENTION }
}
