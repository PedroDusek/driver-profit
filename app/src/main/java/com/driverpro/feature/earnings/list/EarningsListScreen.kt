package com.driverpro.feature.earnings.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalTaxi
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
import androidx.compose.ui.graphics.Color
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
import com.driverpro.core.ui.component.IconChip
import com.driverpro.core.ui.component.ListItemCard
import com.driverpro.core.ui.format.BrazilianFormatter
import com.driverpro.core.ui.format.EarningsLabels
import com.driverpro.core.ui.theme.DriverProTheme
import com.driverpro.core.ui.theme.PlatformAccentColors
import com.driverpro.core.ui.theme.TabularFigures
import com.driverpro.core.ui.theme.driverProTopAppBarColors
import com.driverpro.domain.model.Platform
import com.driverpro.domain.model.WorkSession
import java.time.Instant
import java.time.LocalDate
import kotlin.math.roundToInt

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
                colors = driverProTopAppBarColors(),
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
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = BrazilianFormatter.money(summary.totalRevenue),
                style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = TabularFigures),
                color = MaterialTheme.colorScheme.primary,
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

            // Só quando há mais de uma plataforma: com uma só, "por
            // plataforma" e o total seriam o mesmo número.
            if (summary.byPlatform.size > 1) {
                HorizontalDivider()
                Text(
                    text = stringResource(R.string.earnings_summary_by_platform),
                    style = MaterialTheme.typography.labelMedium,
                )
                val total = summary.byPlatform.values.sumOf { it.cents }.coerceAtLeast(1)
                summary.byPlatform.entries
                    .sortedByDescending { it.value.cents }
                    .forEach { (platform, amount) ->
                        PlatformRow(
                            platform = platform,
                            value = BrazilianFormatter.money(amount),
                            fraction = amount.cents.toFloat() / total.toFloat(),
                        )
                    }
            }
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

/**
 * Linha do breakdown "Por plataforma": crachá quadrado colorido (iniciais),
 * nome, valor e percentual — mesma ideia da lista de categoria de despesa,
 * mas com crachá quadrado em vez de bolinha, para não confundir as duas
 * legendas de relance.
 */
@Composable
private fun PlatformRow(platform: Platform, value: String, fraction: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlatformBadge(platform)
            Text(
                text = stringResource(EarningsLabels.platform(platform)),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "${(fraction * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun PlatformBadge(platform: Platform) {
    val (initials, color) = when (platform) {
        Platform.UBER -> "U" to PlatformAccentColors.uber
        Platform.NINETY_NINE -> "99" to PlatformAccentColors.ninetyNine
        Platform.INDRIVE -> "iD" to PlatformAccentColors.inDrive
        Platform.OTHER -> "?" to PlatformAccentColors.other
    }
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(color = color, shape = RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
        )
    }
}

@Composable
private fun SessionCard(
    session: WorkSession,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    ListItemCard(
        leading = {
            IconChip(
                icon = Icons.Default.LocalTaxi,
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = null,
            )
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
            text = BrazilianFormatter.money(session.revenue),
            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = TabularFigures),
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
