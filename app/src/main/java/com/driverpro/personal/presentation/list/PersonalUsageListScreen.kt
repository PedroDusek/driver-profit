package com.driverpro.personal.presentation.list

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
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.driverpro.R
import com.driverpro.core.di.DriverProViewModelFactory
import com.driverpro.core.ui.component.IconChip
import com.driverpro.core.ui.component.ListItemCard
import com.driverpro.core.ui.format.BrazilianFormatter
import com.driverpro.core.ui.theme.TabularFigures
import com.driverpro.personal.domain.PersonalUsage
import com.driverpro.personal.domain.PersonalUsageSource
import com.driverpro.personal.domain.OdometerReconciliation

/** Histórico de quilometragem rodada fora do trabalho (PRD §22). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalUsageListScreen(
    onBack: () -> Unit,
    onAddUsage: () -> Unit,
    onEditUsage: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PersonalUsageListViewModel =
        viewModel(factory = DriverProViewModelFactory.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pendingDeletion by viewModel.usagePendingDeletion.collectAsStateWithLifecycle()
    val reconciliation by viewModel.pendingReconciliation.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.personal_usage_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::onReconcileRequested) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = stringResource(R.string.reconcile_title),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddUsage,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.personal_usage_add)) },
            )
        },
    ) { innerPadding ->
        when (val state = uiState) {
            PersonalUsageListUiState.Loading -> Centered(Modifier.padding(innerPadding)) {
                CircularProgressIndicator()
            }

            PersonalUsageListUiState.Empty -> Centered(Modifier.padding(innerPadding)) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.personal_usage_empty_title),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(R.string.personal_usage_empty_message),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            is PersonalUsageListUiState.Content -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 88.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items = state.usages, key = { it.id }) { usage ->
                    UsageCard(
                        usage = usage,
                        onEdit = { onEditUsage(usage.id) },
                        onDelete = { viewModel.onDeleteRequested(usage) },
                    )
                }
            }
        }
    }

    pendingDeletion?.let { usage ->
        AlertDialog(
            onDismissRequest = viewModel::onDeleteDismissed,
            title = { Text(stringResource(R.string.personal_usage_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.personal_usage_delete_message,
                        BrazilianFormatter.kilometers(usage.distanceKm),
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

    reconciliation?.let {
        ReconcileDialog(
            reconciliation = it,
            onDismiss = viewModel::onReconcileDismissed,
            onConfirmPersonal = viewModel::onReconcileConfirmedAsPersonal,
            onLeaveOut = viewModel::onReconcileLeftOut,
        )
    }
}

/**
 * Conferência do painel contra o que foi lançado.
 *
 * Mostra a conta inteira, e não só o resultado: o motorista precisa ver de
 * onde saiu a sobra para decidir o que ela é. E a pergunta é feita, nunca
 * presumida — uso pessoal e jornada esquecida têm sinais opostos no custo/km.
 */
@Composable
private fun ReconcileDialog(
    reconciliation: OdometerReconciliation,
    onDismiss: () -> Unit,
    onConfirmPersonal: () -> Unit,
    onLeaveOut: () -> Unit,
) {
    val unexplained = reconciliation.unexplainedKilometers

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reconcile_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    reconciliation.odometerKilometers == null ->
                        Text(stringResource(R.string.reconcile_no_readings))

                    !reconciliation.hasUnexplained ->
                        Text(stringResource(R.string.reconcile_all_explained))

                    else -> {
                        // Qual janela está sendo conferida. Com mais de uma
                        // pendente, dois diálogos seguidos seriam idênticos
                        // exceto pelos números.
                        Text(
                            text = stringResource(
                                R.string.reconcile_period,
                                BrazilianFormatter.date(reconciliation.period.start),
                                BrazilianFormatter.date(reconciliation.period.end),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        ReconcileRow(
                            stringResource(R.string.reconcile_odometer),
                            reconciliation.odometerKilometers,
                        )
                        ReconcileRow(
                            stringResource(R.string.reconcile_work),
                            -reconciliation.workKilometers,
                        )
                        ReconcileRow(
                            stringResource(R.string.reconcile_declared),
                            -reconciliation.declaredPersonalKilometers,
                        )
                        HorizontalDivider()
                        ReconcileRow(
                            stringResource(R.string.reconcile_unexplained),
                            unexplained ?: 0L,
                        )
                        Text(
                            text = stringResource(R.string.reconcile_question),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = stringResource(R.string.reconcile_as_work_message),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        HorizontalDivider()
                        // Terceira saída: nem pessoal, nem jornada a lançar.
                        // Fica no corpo porque um diálogo só tem dois lugares
                        // de botão, e as duas primeiras respostas são as que o
                        // motorista escolhe no caso comum.
                        TextButton(onClick = onLeaveOut) {
                            Text(stringResource(R.string.reconcile_leave_out))
                        }
                        Text(
                            text = stringResource(R.string.reconcile_leave_out_message),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (reconciliation.hasUnexplained) {
                TextButton(onClick = onConfirmPersonal) {
                    Text(stringResource(R.string.reconcile_as_personal))
                }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_confirm)) }
            }
        },
        dismissButton = {
            if (reconciliation.hasUnexplained) {
                // "Resolver depois", e não "Cancelar": o aviso vai continuar
                // lá, e dizer isso evita a impressão de que fechar é decidir.
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.reconcile_later)) }
            }
        },
    )
}

@Composable
private fun ReconcileRow(label: String, kilometers: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            // O sinal deixa a conta legível: o painel soma, o resto desconta.
            text = if (kilometers < 0) {
                "− ${BrazilianFormatter.kilometers(-kilometers)}"
            } else {
                BrazilianFormatter.kilometers(kilometers)
            },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun UsageCard(
    usage: PersonalUsage,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    ListItemCard(
        leading = {
            IconChip(
                icon = Icons.Default.Weekend,
                tint = MaterialTheme.colorScheme.secondary,
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
            text = BrazilianFormatter.kilometers(usage.distanceKm),
            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = TabularFigures),
        )
        Text(
            text = if (usage.range.isSingleDay) {
                BrazilianFormatter.date(usage.range.start)
            } else {
                stringResource(
                    R.string.personal_usage_period,
                    BrazilianFormatter.date(usage.range.start),
                    BrazilianFormatter.date(usage.range.end),
                )
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Estimado e declarado não têm a mesma confiança, e o motorista
        // precisa saber qual está olhando antes de decidir corrigir.
        if (usage.source == PersonalUsageSource.RECONCILED) {
            Text(
                text = stringResource(R.string.personal_usage_source_reconciled),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        usage.note.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
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
