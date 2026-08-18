package com.driverpro.feature.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import com.driverpro.R
import com.driverpro.core.common.Money
import com.driverpro.core.common.WorkDuration
import com.driverpro.core.ui.DriverProViewModelFactory
import com.driverpro.core.ui.component.CategoryLegendRow
import com.driverpro.core.ui.component.DonutChart
import com.driverpro.core.ui.component.DonutSlice
import com.driverpro.core.ui.component.StatTile
import com.driverpro.core.ui.component.visual
import com.driverpro.core.ui.format.BrazilianFormatter
import com.driverpro.core.ui.format.DashboardLabels
import com.driverpro.core.ui.format.ExpenseLabels
import com.driverpro.core.ui.theme.DriverProTheme
import com.driverpro.core.ui.theme.ProfitColors
import com.driverpro.core.ui.theme.TabularFigures
import com.driverpro.core.ui.theme.container
import com.driverpro.core.ui.theme.onContainer
import com.driverpro.domain.model.DashboardMetrics
import com.driverpro.domain.model.DashboardPeriod
import com.driverpro.domain.model.DateRange
import com.driverpro.domain.model.ExpenseCategory
import com.driverpro.domain.usecase.VehicleReconciliation
import com.driverpro.feature.maintenance.MaintenanceWarningCard
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
 *
 * Navegação secundária (v0.14.0): a barra inferior leva às três seções mais
 * usadas direto; o resto (uso pessoal, manutenção, backup) mora em "Mais" —
 * ver [com.driverpro.feature.more.MoreScreen]. Antes disso eram seis ícones
 * na TopAppBar, sem rótulo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenVehicles: () -> Unit,
    onOpenEarnings: () -> Unit,
    onOpenExpenses: () -> Unit,
    onOpenMore: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = viewModel(factory = DriverProViewModelFactory.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val maintenanceWarnings by viewModel.maintenanceWarnings.collectAsStateWithLifecycle()
    val divergences by viewModel.odometerDivergences.collectAsStateWithLifecycle()
    var showRangePicker by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(text = stringResource(R.string.app_name)) })
        },
        bottomBar = {
            DashboardNavigationBar(
                onOpenEarnings = onOpenEarnings,
                onOpenExpenses = onOpenExpenses,
                onOpenVehicles = onOpenVehicles,
                onOpenMore = onOpenMore,
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
                        onResolve = onOpenMore,
                    )
                }
            }

            // Antes do seletor de período: o aviso não pertence a um período, e
            // um alerta abaixo dos cartões só seria visto por quem rolasse.
            if (maintenanceWarnings.isNotEmpty()) {
                item {
                    MaintenanceWarningCard(
                        warnings = maintenanceWarnings,
                        onOpen = onOpenMore,
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

/**
 * Barra inferior com as três seções mais usadas + "Mais" (v0.14.0).
 *
 * Substitui a fileira de seis ícones sem rótulo que ocupava a TopAppBar.
 * "Dashboard" fica sempre selecionado e sem ação: é a própria tela.
 */
@Composable
private fun DashboardNavigationBar(
    onOpenEarnings: () -> Unit,
    onOpenExpenses: () -> Unit,
    onOpenVehicles: () -> Unit,
    onOpenMore: () -> Unit,
) {
    NavigationBar {
        NavigationBarItem(
            selected = true,
            onClick = {},
            icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_dashboard)) },
        )
        NavigationBarItem(
            selected = false,
            onClick = onOpenEarnings,
            icon = { Icon(Icons.Default.Payments, contentDescription = null) },
            label = { Text(stringResource(R.string.earnings_list_title)) },
        )
        NavigationBarItem(
            selected = false,
            onClick = onOpenExpenses,
            icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null) },
            label = { Text(stringResource(R.string.expenses_list_title)) },
        )
        NavigationBarItem(
            selected = false,
            onClick = onOpenVehicles,
            icon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) },
            label = { Text(stringResource(R.string.vehicle_list_title)) },
        )
        NavigationBarItem(
            selected = false,
            onClick = onOpenMore,
            icon = { Icon(Icons.Default.MoreHoriz, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_more)) },
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
 * Hero card da tela (v0.14.0): o fundo em si carrega o sinal — verde quando dá
 * lucro, vermelho quando dá prejuízo ([ProfitColors]) — e não mais a cor de
 * marca genérica, que passou a ser só índigo (`Theme.kt`). Algarismo tabular
 * no valor grande para os dígitos não "pularem" de largura ao trocar de
 * período.
 */
@Composable
private fun ResultCard(metrics: DashboardMetrics) {
    val profit = metrics.netProfit
    val positive = !profit.isNegative
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ProfitColors.container(positive),
            contentColor = ProfitColors.onContainer(positive),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.dashboard_profit),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = BrazilianFormatter.money(profit),
                style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = TabularFigures),
            )
            HorizontalDivider(color = LocalContentColor.current.copy(alpha = 0.24f))
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
        StatGrid(
            listOf(
                stringResource(R.string.dashboard_distance) to
                    BrazilianFormatter.kilometers(metrics.workKilometers),
                stringResource(R.string.dashboard_online_time) to
                    BrazilianFormatter.duration(metrics.totalOnlineTime),
                stringResource(R.string.dashboard_rides) to metrics.totalRides.toString(),
            ),
        )
    }
}

@Composable
private fun RevenueRatiosCard(metrics: DashboardMetrics) {
    MetricsCard(title = stringResource(R.string.dashboard_section_revenue)) {
        StatGrid(
            listOf(
                stringResource(R.string.dashboard_revenue_per_km) to
                    BrazilianFormatter.moneyOrUnavailable(metrics.revenuePerKm),
                stringResource(R.string.dashboard_revenue_per_hour) to
                    BrazilianFormatter.moneyOrUnavailable(metrics.revenuePerHour),
                stringResource(R.string.dashboard_revenue_per_ride) to
                    BrazilianFormatter.moneyOrUnavailable(metrics.revenuePerRide),
            ),
        )
    }
}

/** Grade de 2 colunas para indicadores compactos — ver [StatTile]. */
@Composable
private fun StatGrid(items: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { (label, value) ->
                    StatTile(label = label, value = value, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
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
                share = metrics.workKilometerShare,
                amount = metrics.workOperationalCost,
            )
            SplitRow(
                label = stringResource(R.string.dashboard_split_personal),
                kilometers = metrics.personalKilometers,
                share = metrics.personalKilometerShare,
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

        // Competência, não caixa — e por isso pode aparecer num mês em que
        // nenhum custo fixo foi pago. O IPVA de janeiro compete a agosto
        // também, e é isso que impede janeiro de parecer catastrófico e agosto
        // de parecer isento (PRD §22).
        if (!metrics.accruedFixedCost.isZero) {
            HorizontalDivider()
            MetricRow(
                label = stringResource(R.string.dashboard_fixed_accrued),
                value = BrazilianFormatter.money(metrics.accruedFixedCost),
            )
            MetricRow(
                label = stringResource(R.string.dashboard_fixed_per_km),
                value = BrazilianFormatter.moneyOrUnavailable(metrics.fixedCostPerKm),
            )
            Text(
                text = stringResource(R.string.dashboard_fixed_accrual_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Breakdown de despesas por categoria com gráfico de rosca (v0.14.1) —
 * seguindo a referência visual em `IMAGENS/`. Cor e ícone vêm de
 * [ExpenseCategory.visual], a mesma paleta usada nas linhas de
 * `ExpensesListScreen`.
 */
@Composable
private fun ExpensesByCategoryCard(metrics: DashboardMetrics) {
    val total = metrics.expensesByCategory.values.sumOf { it.cents }.coerceAtLeast(1)
    val entries = metrics.expensesByCategory.entries.sortedByDescending { it.value.cents }
    MetricsCard(title = stringResource(R.string.dashboard_section_expenses)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            DonutChart(
                slices = entries.map { (category, amount) ->
                    DonutSlice(
                        fraction = amount.cents.toFloat() / total.toFloat(),
                        color = category.visual().color,
                    )
                },
                modifier = Modifier
                    .size(140.dp)
                    .padding(vertical = 8.dp),
            ) {
                Text(
                    text = BrazilianFormatter.money(metrics.totalExpenses),
                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = TabularFigures),
                    textAlign = TextAlign.Center,
                )
            }
        }
        entries.forEach { (category, amount) ->
            CategoryLegendRow(
                label = stringResource(ExpenseLabels.category(category)),
                value = BrazilianFormatter.money(amount),
                fraction = amount.cents.toFloat() / total.toFloat(),
                color = category.visual().color,
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
                        // Valor absoluto: a divergência negativa é lançamento a
                        // mais, e o diálogo é que explica de que lado ela está.
                        BrazilianFormatter.kilometers(kotlin.math.abs(gap)),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                // O intervalo é de outra natureza que o período selecionado
                // acima: ele vai de uma leitura de odômetro à seguinte. Sem
                // dizê-lo, o número parece pertencer ao filtro escolhido — e
                // com duas janelas pendentes, as duas linhas ficariam
                // indistinguíveis.
                Text(
                    text = stringResource(
                        R.string.dashboard_odometer_gap_window,
                        item.vehicle.name,
                        BrazilianFormatter.date(item.reconciliation.period.start),
                        BrazilianFormatter.date(item.reconciliation.period.end),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(bottom = 4.dp),
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
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
private fun SplitRow(label: String, kilometers: Long, share: Int?, amount: Money) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            // O percentual responde "quanto do carro o trabalho divide com a
            // vida pessoal", sem sugerir uma multiplicação que daria errado.
            text = if (share == null) {
                stringResource(
                    R.string.dashboard_split_line,
                    label,
                    BrazilianFormatter.kilometers(kilometers),
                )
            } else {
                stringResource(
                    R.string.dashboard_split_share,
                    label,
                    BrazilianFormatter.kilometers(kilometers),
                    share,
                )
            },
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
    DriverProTheme {
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
            ExpensesByCategoryCard(metrics)
        }
    }
}
