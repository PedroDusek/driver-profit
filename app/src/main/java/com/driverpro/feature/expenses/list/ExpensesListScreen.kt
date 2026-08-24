package com.driverpro.feature.expenses.list

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.driverpro.core.common.Quantity
import com.driverpro.core.navigation.DriverProBottomBar
import com.driverpro.core.navigation.DriverProTab
import com.driverpro.core.ui.DriverProViewModelFactory
import com.driverpro.core.ui.component.DriverProCard
import com.driverpro.core.ui.component.CategoryLegendRow
import com.driverpro.core.ui.component.DonutChart
import com.driverpro.core.ui.component.DonutSlice
import com.driverpro.core.ui.component.IconChip
import com.driverpro.core.ui.component.ListItemCard
import com.driverpro.core.ui.component.visual
import com.driverpro.core.ui.format.BrazilianFormatter
import com.driverpro.core.ui.format.ExpenseLabels
import com.driverpro.core.ui.format.QuantityInput
import com.driverpro.core.ui.theme.DriverProTheme
import com.driverpro.core.ui.theme.TabularFigures
import com.driverpro.core.ui.theme.driverProTopAppBarColors
import com.driverpro.domain.model.ConsumptionEstimate
import com.driverpro.domain.model.Expense
import com.driverpro.domain.model.ExpenseCategory
import com.driverpro.domain.model.ExpenseDetail
import com.driverpro.domain.model.FuelType
import java.time.Instant
import java.time.LocalDate

/** Histórico de despesas com filtro por natureza (PRD §19). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesListScreen(
    onSelectTab: (DriverProTab) -> Unit,
    onAddExpense: () -> Unit,
    onEditExpense: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExpensesListViewModel = viewModel(factory = DriverProViewModelFactory.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pendingDeletion by viewModel.expensePendingDeletion.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                colors = driverProTopAppBarColors(),
                // Sem seta de voltar: isto é uma aba, não uma tela empilhada.
                // A barra inferior é o caminho de saída.
                title = { Text(stringResource(R.string.expenses_list_title)) },
            )
        },
        bottomBar = {
            DriverProBottomBar(
                selected = DriverProTab.EXPENSES,
                onSelect = onSelectTab,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddExpense,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.expenses_add)) },
            )
        },
    ) { innerPadding ->
        when (val state = uiState) {
            ExpensesListUiState.Loading -> Centered(Modifier.padding(innerPadding)) {
                CircularProgressIndicator()
            }

            ExpensesListUiState.Empty -> Centered(Modifier.padding(innerPadding)) {
                EmptyText(
                    title = stringResource(R.string.expenses_empty_title),
                    message = stringResource(R.string.expenses_empty_message),
                )
            }

            is ExpensesListUiState.Content -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 88.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    FilterRow(
                        selected = state.filter,
                        onSelect = viewModel::onFilterChange,
                    )
                }

                if (state.filteredOut) {
                    item {
                        EmptyText(
                            title = stringResource(R.string.expenses_filter_empty_title),
                            message = stringResource(R.string.expenses_filter_empty_message),
                        )
                    }
                } else {
                    item { SummaryCard(state.summary) }

                    items(items = state.expenses, key = { it.id }) { expense ->
                        ExpenseCard(
                            expense = expense,
                            consumption = state.consumption[expense.id],
                            onEdit = { onEditExpense(expense.id) },
                            onDelete = { viewModel.onDeleteRequested(expense) },
                        )
                    }
                }
            }
        }
    }

    pendingDeletion?.let { expense ->
        AlertDialog(
            onDismissRequest = viewModel::onDeleteDismissed,
            title = { Text(stringResource(R.string.expenses_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.expenses_delete_message,
                        BrazilianFormatter.money(expense.amount),
                        BrazilianFormatter.date(expense.date),
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::onDeleteConfirmed) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDeleteDismissed) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun FilterRow(selected: ExpenseFilter, onSelect: (ExpenseFilter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ExpenseFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelect(filter) },
                label = { Text(stringResource(filterLabel(filter))) },
            )
        }
    }
}

private fun filterLabel(filter: ExpenseFilter): Int = when (filter) {
    ExpenseFilter.ALL -> R.string.expenses_filter_all
    ExpenseFilter.FUEL_AND_CHARGING -> R.string.expenses_filter_fuel
    ExpenseFilter.MAINTENANCE -> R.string.expenses_filter_maintenance
    ExpenseFilter.OTHERS -> R.string.expenses_filter_others
}

@Composable
private fun SummaryCard(summary: ExpensesSummary) {
    DriverProCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.expenses_summary_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = BrazilianFormatter.money(summary.total),
                style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = TabularFigures),
            )

            if (summary.byCategory.size > 1) {
                HorizontalDivider()
                val total = summary.byCategory.values.sumOf { it.cents }.coerceAtLeast(1)
                val entries = summary.byCategory.entries.sortedByDescending { it.value.cents }
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
                            .size(120.dp)
                            .padding(vertical = 4.dp),
                    )
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
    }
}

@Composable
private fun ExpenseCard(
    expense: Expense,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    consumption: ConsumptionEstimate? = null,
) {
    val visual = expense.category.visual()
    ListItemCard(
        leading = {
            IconChip(icon = visual.icon, tint = visual.color, contentDescription = null)
        },
        trailing = {
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit))
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        },
    ) {
        Text(
            text = BrazilianFormatter.money(expense.amount),
            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = TabularFigures),
        )
        Text(
            text = "${BrazilianFormatter.date(expense.date)} · " +
                stringResource(ExpenseLabels.category(expense.category)),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        expenseDetails(expense)?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Sempre rotulado como estimado (PRD §23): o numero so seria
        // exato se os dois abastecimentos tivessem enchido o tanque
        // nas mesmas condicoes, e ninguem garante isso.
        consumption?.let { estimate ->
            Text(
                text = stringResource(
                    R.string.consumption_estimated,
                    BrazilianFormatter.consumption(
                        estimate.consumption,
                        stringResource(ExpenseLabels.unit(estimate.unit)),
                    ),
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Linha de detalhe: quantidade, preço por unidade e local.
 *
 * O preço por unidade é o número que justifica separar abastecimento das
 * demais despesas — é ele que o motorista compara entre postos.
 */
@Composable
private fun expenseDetails(expense: Expense): String? {
    val unit = expense.unit ?: return expense.description.ifBlank { null }
    val quantity = expense.quantity ?: return expense.description.ifBlank { null }
    val unitLabel = stringResource(ExpenseLabels.unit(unit))

    return listOfNotNull(
        "${QuantityInput.display(quantity)} $unitLabel",
        BrazilianFormatter.moneyPerUnit(expense.pricePerUnit, unitLabel),
        expense.description.ifBlank { null },
    ).joinToString(" · ")
}

@Composable
private fun Centered(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

@Composable
private fun EmptyText(title: String, message: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExpenseCardPreview() {
    DriverProTheme(dynamicColor = false) {
        ExpenseCard(
            expense = Expense(
                id = 1,
                date = LocalDate.of(2026, 8, 11),
                category = ExpenseCategory.FUEL,
                amount = Money.of(210, 0),
                detail = ExpenseDetail.Refuel(
                    fuelType = FuelType.ETHANOL,
                    quantity = Quantity.of(35, 400),
                    station = "Posto Shell",
                ),
                createdAt = Instant.EPOCH,
            ),
            onEdit = {},
            onDelete = {},
        )
    }
}
