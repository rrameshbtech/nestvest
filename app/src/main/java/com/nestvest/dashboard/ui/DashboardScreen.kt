package com.nestvest.dashboard.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.nestvest.R
import com.nestvest.expense.ui.ExpenseStyle
import com.nestvest.expense.ui.ExpenseGlyph
import com.nestvest.expense.ui.ExpenseSymbol
import com.nestvest.dashboard.domain.DashboardDestination
import com.nestvest.dashboard.domain.DashboardOption
import com.nestvest.dashboard.domain.DashboardOptionCatalog
import com.nestvest.dashboard.domain.DashboardSummary
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.LocalDate
import java.util.Currency
import java.util.Locale
import kotlinx.coroutines.delay

internal val LocalDashboardHomeAction = staticCompositionLocalOf<(() -> Unit)?> { null }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DashboardRoute(
    viewModel: DashboardViewModel,
    onOpenDestination: (DashboardDestination) -> Unit,
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    BackHandler(enabled = state.customizing || state.showingMore) {
        viewModel.returnToDashboard()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.resume()
    }
    androidx.compose.runtime.LaunchedEffect(viewModel) {
        while (true) {
            delay(60_000)
            viewModel.resume()
        }
    }
    DashboardScreen(
        state = state,
        welcomeName = viewModel.profile.name,
        onOpenDestination = onOpenDestination,
        onCustomize = viewModel::beginCustomization,
        onShowMore = viewModel::showMore,
        onHome = viewModel::returnToDashboard,
        onToggle = viewModel::toggle,
        onSave = viewModel::saveCustomization,
        onCancel = viewModel::cancelCustomization,
        onRetryPreferences = viewModel::retryPreferences,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DashboardScreen(
    state: DashboardUiState,
    welcomeName: String = "",
    onOpenDestination: (DashboardDestination) -> Unit,
    onCustomize: () -> Unit,
    onShowMore: () -> Unit = {},
    onHome: () -> Unit = {},
    onToggle: (DashboardOption) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onRetryPreferences: () -> Unit,
) {
    val pinnedIds = if (state.customizing) state.draftOptionIds else state.pinnedOptionIds
    val pinned = DashboardOptionCatalog.options.filter { it.id in pinnedIds }
    val more = DashboardOptionCatalog.options.filterNot { it.id in pinnedIds }
    val onHomeEnabled = state.customizing || state.showingMore
    val homeActionDescription = stringResource(R.string.dashboard_home_action)
    val snackbarHostState = remember { SnackbarHostState() }
    val saveSuccessMessage = stringResource(R.string.dashboard_save_success)
    LaunchedEffect(state.saveSucceeded) {
        if (state.saveSucceeded) {
            snackbarHostState.showSnackbar(
                message = saveSuccessMessage,
                duration = SnackbarDuration.Short,
            )
        }
    }
    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(
        primary = ExpenseStyle.Green,
        onPrimary = Color.White,
        primaryContainer = ExpenseStyle.Pale,
        onPrimaryContainer = ExpenseStyle.Ink,
        background = ExpenseStyle.Paper,
        onBackground = ExpenseStyle.Ink,
        surface = ExpenseStyle.Paper,
        onSurface = ExpenseStyle.Ink,
        surfaceVariant = ExpenseStyle.Pale,
        onSurfaceVariant = ExpenseStyle.Muted,
        outline = ExpenseStyle.ControlBorder,
        outlineVariant = ExpenseStyle.Line,
        error = ExpenseStyle.Error,
    )) {
      Scaffold(
        containerColor = ExpenseStyle.Paper,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { snackbarData ->
                Snackbar(
                    snackbarData = snackbarData,
                    shape = RoundedCornerShape(12.dp),
                    containerColor = ExpenseStyle.Pale,
                    contentColor = ExpenseStyle.Green,
                )
            }
        },
        topBar = {
        TopAppBar(
            title = {
                Row(
                    Modifier
                        .heightIn(min = 48.dp)
                        .then(
                            if (onHomeEnabled) Modifier.clickable(role = Role.Button, onClick = onHome)
                            else Modifier,
                        )
                        .semantics {
                            if (onHomeEnabled) contentDescription = homeActionDescription
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        painterResource(R.drawable.ic_nestvest),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(28.dp),
                    )
                    Text(stringResource(R.string.app_name))
                }
            },
            actions = {
                Text(stringResource(R.string.dashboard_local_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = ExpenseStyle.Green,
                    modifier = Modifier.padding(end = 16.dp))
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = ExpenseStyle.Paper,
                titleContentColor = ExpenseStyle.Ink,
            ),
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.preferenceLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (state.preferenceError) {
                Text(stringResource(R.string.dashboard_preferences_unavailable),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
                TextButton(onClick = onRetryPreferences) { Text(stringResource(R.string.retry)) }
            }
            if (state.customizing) {
                Text(stringResource(R.string.dashboard_customize_eyebrow),
                    style = MaterialTheme.typography.labelSmall, color = ExpenseStyle.Muted)
                Text(stringResource(R.string.dashboard_customize_title), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.dashboard_customize_description))
                DashboardOptionCatalog.options.forEach { option ->
                    DashboardOptionCard(
                        option = option,
                        summary = null,
                        onClick = { onToggle(option) },
                        actionLabel = stringResource(
                            if (option.id in pinnedIds) R.string.dashboard_unpin else R.string.dashboard_pin,
                        ),
                        actionDescription = stringResource(
                            if (option.id in pinnedIds) R.string.dashboard_unpin_option
                            else R.string.dashboard_pin_option,
                            stringResource(option.title),
                        ),
                        status = stringResource(
                            if (option.id in pinnedIds) R.string.dashboard_pinned_status
                            else R.string.dashboard_more_status,
                        ),
                    )
                }
                if (state.saving) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (state.saveError) {
                    Text(stringResource(R.string.dashboard_save_error),
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onSave, enabled = !state.saving) {
                        Text(stringResource(R.string.dashboard_save_choices))
                    }
                    OutlinedButton(onClick = onCancel, enabled = !state.saving) {
                        Text(stringResource(R.string.dashboard_cancel))
                    }
                }
            } else if (state.showingMore) {
                Text(stringResource(R.string.dashboard_more_eyebrow),
                    style = MaterialTheme.typography.labelSmall, color = ExpenseStyle.Muted)
                Text(stringResource(R.string.dashboard_more_heading), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.dashboard_more_description),
                    style = MaterialTheme.typography.bodySmall, color = ExpenseStyle.Muted)
                if (more.isEmpty()) {
                    Text(
                        stringResource(R.string.dashboard_more_empty),
                        Modifier.fillMaxWidth().padding(vertical = 24.dp)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    more.forEach { option ->
                        DashboardOptionCard(option, null, { onOpenDestination(option.destination) })
                    }
                }
            } else {
                Text(stringResource(R.string.dashboard_home_eyebrow),
                    style = MaterialTheme.typography.labelSmall, color = ExpenseStyle.Muted)
                val welcomeMessage = if (welcomeName.isBlank()) {
                    stringResource(R.string.dashboard_welcome_fallback)
                } else {
                    stringResource(R.string.dashboard_welcome, welcomeName)
                }
                Text(welcomeMessage, style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.dashboard_welcome_subtitle),
                    style = MaterialTheme.typography.bodySmall, color = ExpenseStyle.Muted)
                Row(
                    Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(stringResource(R.string.dashboard_today_label),
                        style = MaterialTheme.typography.labelSmall, color = ExpenseStyle.Muted)
                    Text(
                        LocalDate.now().format(
                            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                                .withLocale(Locale.getDefault()),
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = ExpenseStyle.Muted,
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.dashboard_pinned_heading), style = MaterialTheme.typography.titleMedium)
                    TextButton(
                        onClick = onCustomize,
                        enabled = !state.preferenceLoading,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.dashboard_customize))
                    }
                }
                if (pinned.isEmpty()) {
                    Text(stringResource(R.string.dashboard_no_pinned))
                } else pinned.forEach { option ->
                    DashboardOptionCard(
                        option = option,
                        summary = if (option.id == "expense_history") state.summary else null,
                        onClick = { onOpenDestination(option.destination) },
                    )
                }
                if (more.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.dashboard_more_heading), style = MaterialTheme.typography.titleMedium)
                        TextButton(
                            onClick = onShowMore,
                            modifier = Modifier.heightIn(min = 40.dp),
                        ) {
                            Text(stringResource(R.string.dashboard_see_all, more.size))
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun DashboardOptionCard(
    option: DashboardOption,
    summary: DashboardSummary?,
    onClick: () -> Unit,
    actionLabel: String? = null,
    actionDescription: String? = null,
    status: String? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth().then(
            if (actionLabel == null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier,
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (actionLabel == null && option.id == "expense_capture") ExpenseStyle.NoteBorder
            else ExpenseStyle.Line,
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (actionLabel == null && option.id == "expense_capture")
                ExpenseStyle.Note else ExpenseStyle.Paper,
        ),
    ) {
        Row(
            Modifier.fillMaxWidth()
                .heightIn(min = if (actionLabel == null && option.id == "expense_capture") 78.dp else 66.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (actionLabel == null) {
                Surface(
                    modifier = Modifier.size(38.dp).testTag("dashboard-icon-${option.id}"),
                    shape = CircleShape,
                    color = ExpenseStyle.Pale,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        ExpenseGlyph(
                            symbol = when (option.destination) {
                                DashboardDestination.EXPENSE_CAPTURE -> ExpenseSymbol.PLUS
                                DashboardDestination.EXPENSE_HISTORY -> ExpenseSymbol.RECEIPT
                                DashboardDestination.REMINDERS -> ExpenseSymbol.REMINDER
                            },
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(option.title), style = MaterialTheme.typography.titleMedium)
                val summaryMessage = when (summary) {
                    DashboardSummary.NoExpenseHistory -> stringResource(R.string.dashboard_summary_no_history)
                    DashboardSummary.Unavailable -> stringResource(R.string.dashboard_summary_unavailable_short)
                    DashboardSummary.Loading -> stringResource(R.string.dashboard_summary_loading_short)
                    null -> null
                    else -> null
                }
                Text(summaryMessage ?: stringResource(option.description), style = MaterialTheme.typography.bodySmall)
                if (status != null) Text(status, style = MaterialTheme.typography.labelSmall)
            }
            if (summary is DashboardSummary.Available || summary is DashboardSummary.NoExpensesToday) {
                DashboardSummaryPill(summary)
            }
            if (actionLabel == null) {
                ExpenseGlyph(ExpenseSymbol.ARROW, Modifier.size(18.dp))
            }
            if (actionLabel != null) {
                TextButton(
                    onClick = onClick,
                    modifier = Modifier.heightIn(min = 48.dp).semantics {
                        contentDescription = checkNotNull(actionDescription)
                        if (status != null) stateDescription = status
                    },
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun DashboardSummaryPill(summary: DashboardSummary) {
    val currencyCode = when (summary) {
        is DashboardSummary.Available -> summary.currencyCode
        is DashboardSummary.NoExpensesToday -> summary.currencyCode
        else -> return
    }
    val total = when (summary) {
        is DashboardSummary.Available -> summary.totalMinorUnits
        is DashboardSummary.NoExpensesToday -> java.math.BigInteger.ZERO
        else -> return
    }
    val count = (summary as? DashboardSummary.Available)?.expenseCount ?: 0
    val currency = Currency.getInstance(currencyCode)
    val fractionDigits = currency.defaultFractionDigits.coerceAtLeast(0)
    val amount = BigDecimal(total, fractionDigits)
    val formattedAmount = NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
        this.currency = currency
    }.format(amount)
    val label = stringResource(R.string.dashboard_summary_pill, count, formattedAmount)
    val accessibilityLabel = stringResource(R.string.dashboard_summary_accessibility, label)
    Surface(
        shape = RoundedCornerShape(99.dp),
        border = BorderStroke(1.dp, ExpenseStyle.NoteBorder),
        color = ExpenseStyle.Paper,
        modifier = Modifier.widthIn(max = 128.dp).semantics {
            contentDescription = accessibilityLabel
        },
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = ExpenseStyle.Green,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
