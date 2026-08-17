package com.driverpro.feature.earnings.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.res.pluralStringResource
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
import com.driverpro.core.ui.format.BrazilianFormatter
import com.driverpro.core.ui.format.EarningsLabels
import com.driverpro.core.ui.theme.DriverProTheme
import com.driverpro.domain.model.Platform
import com.driverpro.domain.model.WorkSession
import java.time.Instant
import java.time.LocalDate

/** Histórico de sessões de trabalho (PRD §19). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarningsListScreen(
    onBack: () -> Unit,
    onAddSession: () -> Unit,
    onEditSession: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EarningsListViewModel = viewModel(factory = DriverProViewModelFactory.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pendingDeletion by viewModel.sessionPendingDeletion.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.earnings_list_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddSession,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.earnings_add)) },
            )
        },
    ) { innerPadding ->
        when (val state = uiState) {
            EarningsListUiState.Loading -> CenteredState(Modifier.padding(innerPadding)) {
                CircularProgressIndicator()
            }

            EarningsListUiState.Empty -> EmptyState(Modifier.padding(innerPadding))

            is EarningsListUiState.Content -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    // Espaço extra para o FAB não cobrir o último item.
                    bottom = innerPadding.calculateBottomPadding() + 88.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { SummaryCard(state.summary) }

                items(items = state.sessions, key = { it.id }) { session ->
                    SessionCard(
                        session = session,
                        onEdit = { onEditSession(session.id) },
                        onDelete = { viewModel.onDeleteRequested(session) },
                    )
                }
            }
        }
    }

    pendingDeletion?.let { session ->
        AlertDialog(
            onDismissRequest = viewModel::onDeleteDismissed,
            title = { Text(stringResource(R.string.earnings_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.earnings_delete_message,
                        BrazilianFormatter.date(session.date),
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
private fun SummaryCard(summary: EarningsSummary) {
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
                text = stringResource(R.string.earnings_summary_title),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = BrazilianFormatter.money(summary.totalRevenue),
                style = MaterialTheme.typography.displaySmall,
            )

            HorizontalDivider()

            SummaryRow(
                stringResource(R.string.earnings_summary_rides),
                summary.totalRides.toString(),
            )
            SummaryRow(
                stringResource(R.string.earnings_summary_time),
                BrazilianFormatter.duration(summary.totalOnlineTime),
            )
            SummaryRow(
                stringResource(R.string.earnings_summary_distance),
                BrazilianFormatter.kilometers(summary.totalDistanceKm),
            )
            SummaryRow(
                stringResource(R.string.earnings_summary_per_hour),
                BrazilianFormatter.moneyPerUnit(summary.revenuePerHour, "h"),
            )
            SummaryRow(
                stringResource(R.string.earnings_summary_per_km),
                BrazilianFormatter.moneyPerUnit(summary.revenuePerKm, "km"),
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SessionCard(
    session: WorkSession,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, end = 4.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = BrazilianFormatter.money(session.revenue),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "${BrazilianFormatter.date(session.date)} · " +
                        stringResource(EarningsLabels.platform(session.platform)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = sessionDetails(session),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
        }
    }
}

/**
 * Linha de detalhe da sessão.
 *
 * Campos não preenchidos são omitidos em vez de exibidos como zero: "0
 * corridas" e "não anotei quantas corridas" não são a mesma informação.
 */
@Composable
private fun sessionDetails(session: WorkSession): String = listOfNotNull(
    session.rides.takeIf { it > 0 }
        ?.let { pluralStringResource(R.plurals.session_rides_count, it, it) },
    session.onlineTime.takeIf { !it.isZero }?.let { BrazilianFormatter.duration(it) },
    session.distanceKm.takeIf { it > 0 }?.let { BrazilianFormatter.kilometers(it) },
    BrazilianFormatter.moneyPerUnit(session.revenuePerHour, "h")
        .takeIf { session.revenuePerHour != null },
).joinToString(" · ")

@Composable
private fun CenteredState(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.earnings_empty_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.earnings_empty_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SessionCardPreview() {
    DriverProTheme(dynamicColor = false) {
        SessionCard(
            session = WorkSession(
                id = 1,
                date = LocalDate.of(2026, 8, 11),
                platform = Platform.UBER,
                rides = 18,
                revenue = Money.of(320, 50),
                onlineTime = WorkDuration.of(8, 20),
                distanceKm = 210,
                createdAt = Instant.EPOCH,
            ),
            onEdit = {},
            onDelete = {},
        )
    }
}
