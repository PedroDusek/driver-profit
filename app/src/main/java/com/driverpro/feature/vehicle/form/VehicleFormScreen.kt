package com.driverpro.feature.vehicle.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.driverpro.R
import com.driverpro.core.di.DriverProViewModelFactory
import com.driverpro.core.ui.format.VehicleLabels
import com.driverpro.core.ui.theme.DriverProTheme
import com.driverpro.domain.model.VehicleField
import com.driverpro.domain.model.VehicleFuel

/**
 * Cadastro e edição de veículo.
 *
 * Dois campos apenas: como o motorista chama o carro, e o que ele coloca no
 * tanque. Marca, modelo e ano não entram em nenhuma conta de rentabilidade, e
 * cada campo a mais é uma barreira entre o motorista e o primeiro lançamento.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleFormScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VehicleFormViewModel = viewModel(factory = DriverProViewModelFactory.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.savedVehicleId) {
        if (uiState.savedVehicleId != null) {
            viewModel.onNavigatedBack()
            onSaved()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (uiState.isEditing) R.string.vehicle_form_edit_title
                            else R.string.vehicle_form_add_title,
                        ),
                    )
                },
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
        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // Ver o comentário em EarningsFormScreen: enableEdgeToEdge faz
                // adjustResize deixar de encolher a janela sozinho.
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            NameField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                error = uiState.errorFor(VehicleField.NAME)?.let {
                    stringResource(VehicleLabels.error(it))
                },
            )

            FuelDropdown(
                selected = uiState.fuel,
                onSelect = viewModel::onFuelChange,
                error = uiState.errorFor(VehicleField.FUEL)?.let {
                    stringResource(VehicleLabels.error(it))
                },
            )

            Button(
                onClick = viewModel::onSave,
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}

@Composable
private fun NameField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.vehicle_name)) },
        placeholder = { Text(stringResource(R.string.vehicle_name_placeholder)) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        isError = error != null,
        supportingText = { Text(error ?: stringResource(R.string.vehicle_name_hint)) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FuelDropdown(
    selected: VehicleFuel?,
    onSelect: (VehicleFuel) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selected?.let { stringResource(VehicleLabels.fuel(it)) }.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.vehicle_fuel)) },
            isError = error != null,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            supportingText = { Text(error ?: stringResource(R.string.vehicle_fuel_hint)) },
            modifier = Modifier
                .fillMaxWidth()
                // PrimaryNotEditable: o campo abre o menu ao toque e não
                // aceita digitação — a escolha é sempre uma das opções.
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            VehicleFuel.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(VehicleLabels.fuel(option))) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VehicleFormFieldsPreview() {
    DriverProTheme(dynamicColor = false) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            NameField(value = "Onix branco", onValueChange = {})
            NameField(value = "", onValueChange = {}, error = "Campo obrigatório")
        }
    }
}
