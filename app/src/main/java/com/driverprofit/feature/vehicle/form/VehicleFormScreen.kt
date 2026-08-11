package com.driverprofit.feature.vehicle.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.driverprofit.R
import com.driverprofit.core.ui.DriverProfitViewModelFactory
import com.driverprofit.core.ui.format.VehicleLabels
import com.driverprofit.core.ui.theme.DriverProfitTheme
import com.driverprofit.domain.model.ChargingCapability
import com.driverprofit.domain.model.CombustionFuel
import com.driverprofit.domain.model.VehicleField
import com.driverprofit.domain.model.VehiclePowertrain

/**
 * Cadastro e edição de veículo.
 *
 * O formulário é dinâmico (PRD §7): combustível só aparece para quem tem motor
 * a combustão, recarga só para quem tem motor elétrico. O motorista nunca vê
 * um campo que não se aplica ao próprio carro.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleFormScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VehicleFormViewModel = viewModel(factory = DriverProfitViewModelFactory.Factory),
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FormTextField(
                value = uiState.brand,
                onValueChange = viewModel::onBrandChange,
                label = stringResource(R.string.vehicle_brand),
                error = uiState.errorFor(VehicleField.BRAND)?.let {
                    stringResource(VehicleLabels.error(it))
                },
            )

            FormTextField(
                value = uiState.model,
                onValueChange = viewModel::onModelChange,
                label = stringResource(R.string.vehicle_model),
                error = uiState.errorFor(VehicleField.MODEL)?.let {
                    stringResource(VehicleLabels.error(it))
                },
            )

            FormTextField(
                value = uiState.yearInput,
                onValueChange = viewModel::onYearChange,
                label = stringResource(R.string.vehicle_year),
                keyboardType = KeyboardType.Number,
                error = uiState.errorFor(VehicleField.YEAR)?.let {
                    stringResource(VehicleLabels.error(it))
                },
            )

            FormTextField(
                value = uiState.odometerInput,
                onValueChange = viewModel::onOdometerChange,
                label = stringResource(R.string.vehicle_initial_odometer),
                keyboardType = KeyboardType.Number,
                supportingText = stringResource(R.string.vehicle_initial_odometer_hint),
                error = uiState.errorFor(VehicleField.INITIAL_ODOMETER)?.let {
                    stringResource(VehicleLabels.error(it))
                },
            )

            EnumDropdown(
                label = stringResource(R.string.vehicle_powertrain),
                options = VehiclePowertrain.entries,
                selected = uiState.powertrain,
                optionLabel = { stringResource(VehicleLabels.powertrain(it)) },
                onSelect = viewModel::onPowertrainChange,
                error = uiState.errorFor(VehicleField.POWERTRAIN)?.let {
                    stringResource(VehicleLabels.error(it))
                },
            )

            if (uiState.showCombustionFuel) {
                EnumDropdown(
                    label = stringResource(R.string.vehicle_combustion_fuel),
                    options = CombustionFuel.entries,
                    selected = uiState.combustionFuel,
                    optionLabel = { stringResource(VehicleLabels.combustionFuel(it)) },
                    onSelect = viewModel::onCombustionFuelChange,
                    error = uiState.errorFor(VehicleField.COMBUSTION_FUEL)?.let {
                        stringResource(VehicleLabels.error(it))
                    },
                )
            }

            if (uiState.showChargingCapability) {
                EnumDropdown(
                    label = stringResource(R.string.vehicle_charging_capability),
                    options = ChargingCapability.entries,
                    selected = uiState.chargingCapability,
                    optionLabel = { stringResource(VehicleLabels.chargingCapability(it)) },
                    onSelect = viewModel::onChargingCapabilityChange,
                    supportingText = stringResource(R.string.vehicle_charging_capability_hint),
                    error = uiState.errorFor(VehicleField.CHARGING_CAPABILITY)?.let {
                        stringResource(VehicleLabels.error(it))
                    },
                )
            }

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
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    supportingText: String? = null,
    error: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        isError = error != null,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        supportingText = (error ?: supportingText)?.let { { Text(it) } },
    )
}

/**
 * Seletor de enum. Genérico porque os três campos de configuração do veículo
 * têm exatamente o mesmo comportamento — duplicar três vezes seria ruído.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> EnumDropdown(
    label: String,
    options: List<T>,
    selected: T?,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    error: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selected?.let { optionLabel(it) }.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            isError = error != null,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            supportingText = (error ?: supportingText)?.let { { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                // PrimaryNotEditable: o campo abre o menu ao toque e não
                // aceita digitação — a escolha é sempre uma das opções.
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
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
private fun VehicleFormFieldPreview() {
    DriverProfitTheme(dynamicColor = false) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FormTextField(value = "Chevrolet", onValueChange = {}, label = "Marca")
            FormTextField(
                value = "",
                onValueChange = {},
                label = "Modelo",
                error = "Campo obrigatório",
            )
        }
    }
}
