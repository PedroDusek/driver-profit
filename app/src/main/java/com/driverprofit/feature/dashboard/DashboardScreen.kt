package com.driverprofit.feature.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.driverprofit.R
import com.driverprofit.core.common.Money
import com.driverprofit.core.common.WorkDuration
import com.driverprofit.core.ui.DriverProfitViewModelFactory
import com.driverprofit.core.ui.format.BrazilianFormatter
import com.driverprofit.core.ui.format.DashboardLabels
import com.driverprofit.core.ui.format.ExpenseLabels
import com.driverprofit.core.ui.theme.DriverProfitTheme
import com.driverprofit.domain.model.DashboardMetrics
import com.driverprofit.domain.model.DashboardPeriod
import com.driverprofit.domain.model.DateRange
import com.driverprofit.domain.model.ExpenseCategory
import com.driverprofit.domain.usecase.VehicleReconciliation
import com.driverprofit.feature.maintenance.MaintenanceWarningCard
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Dashboard de rentabilidade — tela principal do aplicativo (PRD §20).
 *
 * Responde, para o período escolhido, às perguntas que justificam o produto:
 * quanto entrou, quanto saiu, quanto sobrou, e quanto vale cada quilômetro,
 * cada hora e cada corrida.
 *
 * A tela só desenha. Todo indicador chega pronto de `DashboardMetrics`, que é
 * domínio puro (PRD §29) — conta financeira em Composable é proibida (PRD §54).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenVehicles: () -> Unit,
    onOpenEarnings: () -> Unit,
    onOpenExpenses: () -> Unit,
    onOpenPersonalUsage: () -> Unit,
    onOpenMaintenance: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = viewModel(factory = DriverProfitViewModelFactory.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val maintenanceWarnings by viewModel.maintenanceWarnings.collectAsStateWithLifecycle()
    val divergences by viewModel.odometerDivergences.collectAsStateWithLifecycle()
    var showRangePicker by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenEarnings) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = stringResource(R.string.earnings_list_title),
                        )
                    }
                    IconButton(onClick = onOpenExpenses) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = stringResource(R.string.expenses_list_title),
                        )
                    }
                    IconButton(onClick = onOpenPersonalUsage) {
                        Icon(
                            imageVector = Icons.Default.Weekend,
                            contentDescription = stringResource(R.string.personal_usage_title),
                        )
                    }
                    IconButton(onClick = onOpenMaintenance) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = stringResource(R.string.maintenance_title),
                        )
                    }
                    IconButton(onClick = onOpenVehicles) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = stringResource(R.string.vehicle_list_title),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (divergences.isNotEmpty()) {
                item {
                    OdometerGapCard(
                        divergences = divergences,
                        onResolve = onOpenPersonalUsage,
                    )
                }
            }

            // Antes do seletor de período: o aviso não pertence a um período, e
            // um alerta abaixo dos cartões só seria visto por quem rolasse.
            if (maintenanceWarnings.isNotEmpty()) {
                item {
                    MaintenanceWarningCard(
                        warnings = maintenanceWarnings,
                        onOpen = onOpenMaintenance,
                    )
                }
            }

            item {
                PeriodRow(
                    selected = uiState.period,
                    onSelect = viewModel::onPeriodChange,
                    onCustom = { showRangePicker = true },
                )
            }

            when (val state = uiState) {
                is DashboardUiState.Loading -> item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is DashboardUiState.Content -> {
                    item { RangeCaption(state.range) }

                    if (state.metrics.isEmpty) {
                        item { EmptyPeriod() }
                    } else {
                        item { ResultCard(state.metrics) }
                        item { VolumeCard(state.metrics) }
                        item { RevenueRatiosCard(state.metrics) }
                        item { CostRatiosCard(state.metrics) }
                        if (state.metrics.expensesByCategory.isNotEmpty()) {
                            item { ExpensesByCategoryCard(state.metrics) }
                        }
                    }
                }
            }
        }
    }

    if (showRangePicker) {
        RangePickerDialog(
            today = viewModel.today(),
            onDismiss = { showRangePicker = false },
            onConfirm = { start, end ->
                viewModel.onCustomRangeChange(start, end)
                showRangePicker = false
            },
        )
    }
}

@Composable
private fun PeriodRow(
    selected: DashboardPeriod,
    onSelect: (DashboardPeriod) -> Unit,
    onCustom: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DashboardPeriod.PRESETS.forEach { preset ->
            FilterChip(
                selected = preset == selected,
                onClick = { onSelect(preset) },
                label = { Text(stringResource(DashboardLabels.period(preset))) },
            )
        }
        // "Personalizado" não seleciona nada sozinho: ele abre o seletor, e é a
        // escolha das datas que vira o período.
        FilterChip(
            selected = selected is DashboardPeriod.Custom,
            onClick = onCustom,
            label = { Text(stringResource(R.string.dashboard_period_custom)) },
            leadingIcon = {
                Icon(imageVector = Icons.Default.DateRange, contentDescription = null)
            },
        )
    }
}

/** Datas exatas do período, para não restar dúvida sobre o que os números cobrem. */
@Composable
private fun RangeCaption(range: DateRange) {
    val text = if (range.isSingleDay) {
        BrazilianFormatter.date(range.start)
    } else {
        stringResource(
            R.string.dashboard_range,
            BrazilianFormatter.date(range.start),
            BrazilianFormatter.date(range.end),
        )
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun EmptyPeriod() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.dashboard_empty_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.dashboard_empty_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Faturamento, despesas e lucro — a conta que o motorista não consegue fazer
 * olhando só o extrato da plataforma.
 *
 * O lucro fica em destaque e muda de cor quando é negativo: prejuízo escrito
 * com a mesma tinta do lucro passa despercebido justamente no dia em que mais
 * importa.
 */
@Composable
private fun ResultCard(metrics: DashboardMetrics) {
    val profit = metrics.netProfit
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.dashboard_profit),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = BrazilianFormatter.money(profit),
                style = MaterialTheme.typography.displaySmall,
                color = if (profit.isNegative) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                },
            )
            HorizontalDivider()
            MetricRow(
                label = stringResource(R.string.dashboard_revenue),
                value = BrazilianFormatter.money(metrics.totalRevenue),
            )
            MetricRow(
                label = stringResource(R.string.dashboard_expenses),
                // O que e cobrado do trabalho, e nao o total gasto: assim
                // faturamento menos despesas fecha com o lucro exibido acima.
                // Sem uso pessoal os dois numeros sao iguais.
                value = BrazilianFormatter.money(metrics.workExpenses),
            )
        }
    }
}

@Composable
private fun VolumeCard(metrics: DashboardMetrics) {
    MetricsCard(title = stringResource(R.string.dashboard_section_volume)) {
        MetricRow(
            label = stringResource(R.string.dashboard_distance),
            value = BrazilianFormatter.kilometers(metrics.workKilometers),
        )
        MetricRow(
            label = stringResource(R.string.dashboard_online_time),
            value = BrazilianFormatter.duration(metrics.totalOnlineTime),
        )
        MetricRow(
            label = stringResource(R.string.dashboard_rides),
            value = metrics.totalRides.toString(),
        )
    }
}

@Composable
private fun RevenueRatiosCard(metrics: DashboardMetrics) {
    MetricsCard(title = stringResource(R.string.dashboard_section_revenue)) {
        MetricRow(
            label = stringResource(R.string.dashboard_revenue_per_km),
            value = BrazilianFormatter.moneyOrUnavailable(metrics.revenuePerKm),
        )
        MetricRow(
            label = stringResource(R.string.dashboard_revenue_per_hour),
            value = BrazilianFormatter.moneyOrUnavailable(metrics.revenuePerHour),
        )
        MetricRow(
            label = stringResource(R.string.dashboard_revenue_per_ride),
            value = BrazilianFormatter.moneyOrUnavailable(metrics.revenuePerRide),
        )
    }
}

/**
 * Custo/km, lucro/km e lucro/hora.
 *
 * Quando existe custo fixo no período, a nota explica por que o custo por km
 * não é simplesmente despesas ÷ km (PRD §22) — sem ela o número pareceria
 * errado para quem conferisse na calculadora.
 */
@Composable
private fun CostRatiosCard(metrics: DashboardMetrics) {
    MetricsCard(title = stringResource(R.string.dashboard_section_cost)) {
        MetricRow(
            label = stringResource(R.string.dashboard_cost_per_km),
            value = BrazilianFormatter.moneyOrUnavailable(metrics.costPerKm),
        )

        // Sem dado de uso pessoal o app calcula com o que tem — e **diz isso**
        // (PRD §22). Antes da v0.9.1 a tela ficava calada, e um número
        // incompleto exibido como resposta final é indistinguível de um número
        // errado para quem está lendo.
        if (!metrics.hasPersonalUsage && metrics.totalKilometers > 0L) {
            Text(
                text = stringResource(R.string.dashboard_personal_missing_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // A repartição só aparece quando há uso pessoal: sem ele os dois
        // valores seriam "tudo" e "zero", o que não informa nada.
        if (metrics.hasPersonalUsage) {
            SplitRow(
                label = stringResource(R.string.dashboard_split_work),
                kilometers = metrics.workKilometers,
                amount = metrics.workOperationalCost,
            )
            SplitRow(
                label = stringResource(R.string.dashboard_split_personal),
                kilometers = metrics.personalKilometers,
                amount = metrics.personalOperationalCost,
            )
            Text(
                text = stringResource(R.string.dashboard_personal_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider()
        }

        MetricRow(
            label = stringResource(R.string.dashboard_profit_per_km),
            value = BrazilianFormatter.moneyOrUnavailable(metrics.profitPerKm),
        )
        MetricRow(
            label = stringResource(R.string.dashboard_profit_per_hour),
            value = BrazilianFormatter.moneyOrUnavailable(metrics.profitPerHour),
        )

        if (!metrics.fixedExpenses.isZero) {
            HorizontalDivider()
            MetricRow(
                label = stringResource(R.string.dashboard_fixed_expenses),
                value = BrazilianFormatter.money(metrics.fixedExpenses),
            )
            Text(
                text = stringResource(R.string.dashboard_fixed_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ExpensesByCategoryCard(metrics: DashboardMetrics) {
    MetricsCard(title = stringResource(R.string.dashboard_section_expenses)) {
        metrics.expensesByCategory.entries
            .sortedByDescending { it.value.cents }
            .forEach { (category, amount) ->
                MetricRow(
                    label = stringResource(ExpenseLabels.category(category)),
                    value = BrazilianFormatter.money(amount),
                )
            }
    }
}

/**
 * Quilômetros que o painel registra e o lançado não explica.
 *
 * Fica no topo do dashboard porque é aqui que o número afetado aparece:
 * enquanto a sobra não for resolvida, o custo/km divide por menos quilômetros
 * do que o carro rodou. Até a v0.9.0 essa conferência dependia de o motorista
 * abrir a tela de uso pessoal e apertar um botão que ele não tinha como saber
 * que existia.
 */
@Composable
private fun OdometerGapCard(
    divergences: List<VehicleReconciliation>,
    onResolve: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onResolve),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.dashboard_odometer_gap_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            divergences.forEach { item ->
                val gap = item.reconciliation.unexplainedKilometers ?: 0L
                Text(
                    text = stringResource(
                        R.string.dashboard_odometer_gap_line,
                        item.vehicle.name,
                        // Valor absoluto: a divergência negativa é lançamento a
                        // mais, e o diálogo é que explica de que lado ela está.
                        BrazilianFormatter.kilometers(kotlin.math.abs(gap)),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            Text(
                text = stringResource(R.string.dashboard_odometer_gap_action),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

@Composable
private fun MetricsCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.labelMedium)
            content()
        }
    }
}

/**
 * Linha da repartição: quilômetros e reais lado a lado.
 *
 * Os dois números na mesma linha para o motorista ver de onde saiu o valor,
 * em vez de precisar confiar. As duas linhas somam exatamente a despesa
 * operacional do período.
 */
@Composable
private fun SplitRow(label: String, kilometers: Long, amount: Money) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(
                R.string.dashboard_split_line,
                label,
                BrazilianFormatter.kilometers(kilometers),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = BrazilianFormatter.money(amount),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

/**
 * Seleção de período personalizado (PRD §20).
 *
 * O seletor trabalha em epoch millis UTC; a conversão acontece nas duas pontas
 * para que o dia escolhido não escorregue por causa de fuso.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangePickerDialog(
    today: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate) -> Unit,
) {
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = today.toUtcMillis(),
        initialSelectedEndDateMillis = today.toUtcMillis(),
    )

    val start = state.selectedStartDateMillis
    val end = state.selectedEndDateMillis

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = start != null,
                onClick = {
                    start?.let {
                        // Sem data final, o motorista quer um dia só.
                        onConfirm(it.toLocalDate(), (end ?: it).toLocalDate())
                    }
                },
            ) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    ) {
        DateRangePicker(
            state = state,
            title = {
                Text(
                    text = stringResource(R.string.dashboard_period_custom_title),
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                )
            },
        )
    }
}

private fun LocalDate.toUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

@Preview(showBackground = true)
@Composable
private fun DashboardCardsPreview() {
    DriverProfitTheme(dynamicColor = false) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val metrics = DashboardMetrics(
                totalRevenue = Money.of(1_280, 0),
                totalExpenses = Money.of(742, 50),
                expensesByCategory = mapOf(
                    ExpenseCategory.FUEL to Money.of(430, 0),
                    ExpenseCategory.MAINTENANCE to Money.of(132, 50),
                    ExpenseCategory.INSURANCE to Money.of(180, 0),
                ),
                totalRides = 62,
                workKilometers = 980,
                personalKilometers = 220,
                totalOnlineTime = WorkDuration.of(38, 30),
            )
            ResultCard(metrics)
            VolumeCard(metrics)
            RevenueRatiosCard(metrics)
            CostRatiosCard(metrics)
        }
    }
}
