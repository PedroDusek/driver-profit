package com.driverpro.feature.maintenance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.driverpro.R
import com.driverpro.core.di.DriverProViewModelFactory
import com.driverpro.core.ui.component.AlertCard
import com.driverpro.core.ui.format.BrazilianFormatter
import com.driverpro.core.ui.format.MaintenanceLabels
import com.driverpro.domain.model.MaintenanceAlert
import com.driverpro.domain.model.MaintenanceStatus
import com.driverpro.domain.model.VehicleMaintenance

/**
 * Manutenção preventiva por quilometragem (ROADMAP v0.9.0).
 *
 * A tela mostra cada item com o que o app **sabe**, e não com o que ele
 * supõe. Um item sem histórico aparece como pendência de dado, nunca como "em
 * dia": afirmar que o óleo está em dia sem ter de onde contar é o erro que
 * custa motor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MaintenanceViewModel = viewModel(factory = DriverProViewModelFactory.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val edit by viewModel.intervalEdit.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.maintenance_title)) },
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
    ) { innerPadding ->
        when (val state = uiState) {
            is MaintenanceUiState.Loading -> Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator()
            }

            is MaintenanceUiState.Empty -> EmptyState(Modifier.padding(innerPadding))

            is MaintenanceUiState.Content -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.vehicles.forEach { vehicleMaintenance ->
                    item(key = vehicleMaintenance.vehicle.id) {
                        VehicleCard(
                            maintenance = vehicleMaintenance,
                            onEdit = { alert ->
                                viewModel.onEditRequested(vehicleMaintenance.vehicle.id, alert)
                            },
                            onToggle = { alert, monitored ->
                                viewModel.onMonitoredChange(
                                    vehicleMaintenance.vehicle.id,
                                    alert,
                                    monitored,
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    edit?.let { current ->
        IntervalDialog(
            edit = current,
            onValueChange = viewModel::onIntervalChange,
            onConfirm = viewModel::onEditConfirmed,
            onReset = { viewModel.onResetRequested(current.vehicleId, current.item) },
            onDismiss = viewModel::onEditDismissed,
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.maintenance_empty_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.maintenance_empty_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun VehicleCard(
    maintenance: VehicleMaintenance,
    onEdit: (MaintenanceAlert) -> Unit,
    onToggle: (MaintenanceAlert, Boolean) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = maintenance.vehicle.name,
                style = MaterialTheme.typography.titleMedium,
            )
            maintenance.alerts.forEachIndexed { index, alert ->
                if (index > 0) HorizontalDivider()
                AlertRow(
                    alert = alert,
                    onEdit = { onEdit(alert) },
                    onToggle = { onToggle(alert, !alert.monitored) },
                )
            }
        }
    }
}

/**
 * Um item e sua situação.
 *
 * A linha inteira abre a edição do intervalo — o número que o app usa precisa
 * estar a um toque de quem discorda dele.
 */
@Composable
private fun AlertRow(
    alert: MaintenanceAlert,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(MaintenanceLabels.item(alert.item)),
                style = MaterialTheme.typography.bodyLarge,
            )
            StatusChip(alert)
        }

        Text(
            text = detailText(alert),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // De onde a contagem parte. Sem isso o motorista não tem como conferir
        // o número contra a nota da oficina.
        val serviceKm = alert.lastServiceKm
        val serviceDate = alert.lastServiceDate
        if (serviceKm != null && serviceDate != null) {
            Text(
                text = stringResource(
                    R.string.maintenance_last_service,
                    BrazilianFormatter.date(serviceDate),
                    BrazilianFormatter.kilometers(serviceKm),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // O piso por combustível avisa que o número é mínimo, não medido — e
        // pede a leitura em vez de deixar o motorista confiar nele.
        if (alert.distanceIsImplied && alert.monitored) {
            Text(
                text = stringResource(R.string.maintenance_implied_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        TextButton(onClick = onToggle) {
            Text(
                text = if (alert.monitored) {
                    stringResource(R.string.maintenance_action_stop)
                } else {
                    stringResource(R.string.maintenance_action_resume)
                },
            )
        }
    }
}

@Composable
private fun StatusChip(alert: MaintenanceAlert) {
    val label = if (alert.monitored) {
        stringResource(MaintenanceLabels.status(alert.status))
    } else {
        stringResource(R.string.maintenance_not_monitored)
    }

    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = statusContainerColor(alert),
            disabledLabelColor = statusLabelColor(alert),
        ),
    )
}

@Composable
private fun statusContainerColor(alert: MaintenanceAlert): Color = when {
    !alert.monitored -> MaterialTheme.colorScheme.surfaceVariant
    alert.status == MaintenanceStatus.OVERDUE -> MaterialTheme.colorScheme.errorContainer
    alert.status == MaintenanceStatus.DUE_SOON -> MaterialTheme.colorScheme.tertiaryContainer
    else -> MaterialTheme.colorScheme.surfaceVariant
}

@Composable
private fun statusLabelColor(alert: MaintenanceAlert): Color = when {
    !alert.monitored -> MaterialTheme.colorScheme.onSurfaceVariant
    alert.status == MaintenanceStatus.OVERDUE -> MaterialTheme.colorScheme.onErrorContainer
    alert.status == MaintenanceStatus.DUE_SOON -> MaterialTheme.colorScheme.onTertiaryContainer
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

/**
 * A frase principal do item, ou a que admite não saber.
 *
 * Exibe a **quilometragem de vencimento**, não uma contagem regressiva. O alvo
 * é a soma de dois fatos — a leitura da última troca e o intervalo escolhido —
 * e por isso é exato mesmo com o painel atrasado. Contagem regressiva
 * carregaria para dentro do número toda a incerteza sobre onde o carro está
 * agora, e um motorista que lê "faltam 600" quando faltam 550 para de confiar
 * no aplicativo inteiro.
 */
@Composable
private fun detailText(alert: MaintenanceAlert): String {
    val target = alert.nextServiceKm
        ?: return stringResource(R.string.maintenance_unknown_message)

    return stringResource(
        R.string.maintenance_next_service,
        BrazilianFormatter.kilometers(target),
    )
}

/**
 * Edição do intervalo.
 *
 * Mostra o padrão do app junto do campo: o motorista precisa saber de onde veio
 * o número que está mudando, e ter o caminho de volta sem procurar.
 */
@Composable
private fun IntervalDialog(
    edit: MaintenanceIntervalEdit,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    R.string.maintenance_interval_title,
                    stringResource(MaintenanceLabels.item(edit.item)),
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = edit.input,
                    onValueChange = onValueChange,
                    label = { Text(stringResource(R.string.maintenance_interval_label)) },
                    isError = edit.error != null,
                    supportingText = edit.error?.let { error ->
                        { Text(stringResource(MaintenanceLabels.error(error))) }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                Text(
                    text = stringResource(
                        R.string.maintenance_interval_default,
                        BrazilianFormatter.kilometers(edit.item.defaultIntervalKm),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onReset) {
                    Text(stringResource(R.string.maintenance_action_reset))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

/**
 * Aviso compacto para o dashboard.
 *
 * Só aparece quando existe item vencido ou próximo — o dashboard é a tela de
 * rentabilidade, e enchê-la de "está tudo em dia" gastaria a atenção que o
 * alerta precisa ter no dia em que importa.
 */
@Composable
fun MaintenanceWarningCard(
    warnings: List<VehicleMaintenance>,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertCard(
        icon = Icons.Default.Build,
        accentColor = MaterialTheme.colorScheme.error,
        title = stringResource(R.string.maintenance_dashboard_title),
        onClick = onOpen,
        modifier = modifier,
    ) {
        // Manutenção conta quilometragem acumulada do carro, e não o
        // período do seletor logo abaixo. Sem dizê-lo, o alvo parece uma
        // informação do mês escolhido.
        Text(
            text = stringResource(R.string.maintenance_dashboard_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        warnings.forEach { vehicleMaintenance ->
            vehicleMaintenance.needingAttention.forEach { alert ->
                // Item que pede atenção sempre tem alvo — só se chega a
                // "em atraso" havendo marco. Ainda assim o `let` decide
                // pela omissão em vez de imprimir um zero, porque um aviso
                // dizendo "próxima aos 0 km" queimaria a confiança que o
                // alvo existe para construir.
                alert.nextServiceKm?.let { target ->
                    Text(
                        text = stringResource(
                            R.string.maintenance_dashboard_line,
                            stringResource(MaintenanceLabels.item(alert.item)),
                            stringResource(MaintenanceLabels.status(alert.status)),
                            BrazilianFormatter.kilometers(target),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
