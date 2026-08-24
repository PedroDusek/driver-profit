package com.driverpro.feature.vehicle.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import com.driverpro.core.navigation.DriverProBottomBar
import com.driverpro.core.navigation.DriverProTab
import com.driverpro.core.ui.DriverProViewModelFactory
import com.driverpro.core.ui.component.IconChip
import com.driverpro.core.ui.component.ListItemCard
import com.driverpro.core.ui.format.BrazilianFormatter
import com.driverpro.core.ui.format.VehicleLabels
import com.driverpro.core.ui.theme.DriverProTheme
import com.driverpro.core.ui.theme.driverProTopAppBarColors
import com.driverpro.domain.model.Vehicle
import com.driverpro.domain.model.VehicleFuel
import java.time.Instant

/** Lista de veículos cadastrados, com acesso a cadastro, edição e exclusão. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleListScreen(
    onSelectTab: (DriverProTab) -> Unit,
    onAddVehicle: () -> Unit,
    onEditVehicle: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VehicleListViewModel = viewModel(factory = DriverProViewModelFactory.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pendingDeletion by viewModel.vehiclePendingDeletion.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                colors = driverProTopAppBarColors(),
                // Sem seta de voltar: isto é uma aba, não uma tela empilhada.
                // A barra inferior é o caminho de saída.
                title = { Text(stringResource(R.string.vehicle_list_title)) },
            )
        },
        bottomBar = {
            DriverProBottomBar(
                selected = DriverProTab.VEHICLES,
                onSelect = onSelectTab,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddVehicle,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.vehicle_add)) },
            )
        },
    ) { innerPadding ->
        when (val state = uiState) {
            VehicleListUiState.Loading -> LoadingState(Modifier.padding(innerPadding))

            VehicleListUiState.Empty -> EmptyState(Modifier.padding(innerPadding))

            is VehicleListUiState.Content -> LazyColumn(
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
                items(items = state.vehicles, key = { it.id }) { vehicle ->
                    VehicleCard(
                        odometerKm = state.odometers[vehicle.id],
                        vehicle = vehicle,
                        // Com um só veículo ele já nasce atual — o botão só
                        // faz sentido quando há entre quais escolher.
                        showCurrentToggle = state.vehicles.size > 1,
                        onEdit = { onEditVehicle(vehicle.id) },
                        onDelete = { viewModel.onDeleteRequested(vehicle) },
                        onSetCurrent = { viewModel.onSetCurrent(vehicle) },
                    )
                }
            }
        }
    }

    pendingDeletion?.let { vehicle ->
        AlertDialog(
            onDismissRequest = viewModel::onDeleteDismissed,
            title = { Text(stringResource(R.string.vehicle_delete_title)) },
            text = { Text(stringResource(R.string.vehicle_delete_message, vehicle.name)) },
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
private fun VehicleCard(
    vehicle: Vehicle,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetCurrent: () -> Unit,
    showCurrentToggle: Boolean,
    odometerKm: Long? = null,
) {
    ListItemCard(
        leading = {
            IconChip(
                icon = Icons.Default.DirectionsCar,
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = null,
            )
        },
        trailing = {
            if (showCurrentToggle) {
                IconButton(onClick = onSetCurrent) {
                    if (vehicle.isCurrent) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = stringResource(R.string.vehicle_current_description),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.StarBorder,
                            contentDescription = stringResource(R.string.vehicle_action_set_current),
                        )
                    }
                }
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
        },
    ) {
        Text(
            text = vehicle.name,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(VehicleLabels.fuel(vehicle.fuel)),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Quilometragem segundo o app: a maior leitura já lançada.
        // Ausente enquanto nenhum abastecimento registrou odômetro.
        Text(
            text = odometerKm
                ?.let {
                    stringResource(
                        R.string.vehicle_odometer,
                        BrazilianFormatter.kilometers(it),
                    )
                }
                ?: stringResource(R.string.vehicle_odometer_unknown),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
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
            text = stringResource(R.string.vehicle_empty_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.vehicle_empty_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VehicleCardPreview() {
    DriverProTheme(dynamicColor = false) {
        VehicleCard(
            vehicle = Vehicle(
                id = 1,
                name = "Onix branco",
                fuel = VehicleFuel.FLEX,
                createdAt = Instant.EPOCH,
                isCurrent = true,
            ),
            showCurrentToggle = true,
            onEdit = {},
            onDelete = {},
            onSetCurrent = {},
        )
    }
}
