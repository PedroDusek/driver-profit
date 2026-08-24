package com.driverpro.expenses.presentation.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Checkbox
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.driverpro.R
import com.driverpro.core.di.DriverProViewModelFactory
import com.driverpro.core.ui.component.CaretAtEndTextField
import com.driverpro.core.ui.format.BrazilianFormatter
import com.driverpro.expenses.presentation.ExpenseLabels
import com.driverpro.vehicle.presentation.VehicleLabels
import com.driverpro.expenses.domain.ChargingLocation
import com.driverpro.expenses.domain.ExpenseCategory
import com.driverpro.expenses.domain.ExpenseDetailKind
import com.driverpro.expenses.domain.ExpenseField
import com.driverpro.maintenance.domain.MaintenanceCategory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Registro e edição de despesa.
 *
 * O formulário se remonta conforme a categoria (PRD §7): abastecimento pede
 * combustível e quantidade, recarga pede kWh e local, manutenção pede a
 * categoria da peça, e pedágio pede só o valor. As opções de combustível vêm
 * do veículo escolhido — nunca se oferece algo que o carro não aceita.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseFormScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExpenseFormViewModel = viewModel(factory = DriverProViewModelFactory.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    var showAccrualStartPicker by remember { mutableStateOf(false) }
    var showAccrualEndPicker by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.savedExpenseId) {
        if (uiState.savedExpenseId != null) {
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
                            if (uiState.isEditing) R.string.expenses_form_edit_title
                            else R.string.expenses_form_add_title,
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
                // Consome o inset do teclado. O manifesto declara
                // adjustResize, mas enableEdgeToEdge faz a janela deixar de
                // encaixar nas system windows, e aí quem precisa encolher a
                // área rolável é o Compose. Sem isto o teclado cobre o campo
                // que está sendo digitado.
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = uiState.date?.let(BrazilianFormatter::date).orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.expense_date)) },
                isError = uiState.errorFor(ExpenseField.DATE) != null,
                supportingText = uiState.errorFor(ExpenseField.DATE)?.let {
                    { Text(stringResource(ExpenseLabels.error(it))) }
                },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = stringResource(R.string.expense_date),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Dropdown(
                label = stringResource(R.string.expense_category),
                options = ExpenseCategory.entries,
                selected = uiState.category,
                optionLabel = { stringResource(ExpenseLabels.category(it)) },
                onSelect = viewModel::onCategoryChange,
                error = uiState.errorFor(ExpenseField.CATEGORY)?.let {
                    stringResource(ExpenseLabels.error(it))
                },
            )

            if (uiState.showVehicle) {
                Dropdown(
                    label = stringResource(R.string.expense_vehicle),
                    options = uiState.vehicles,
                    selected = uiState.selectedVehicle,
                    optionLabel = { it.name },
                    onSelect = { viewModel.onVehicleChange(it.id) },
                    supportingText = if (uiState.vehicles.isEmpty()) {
                        stringResource(R.string.expense_vehicle_none)
                    } else {
                        null
                    },
                    error = uiState.errorFor(ExpenseField.VEHICLE)?.let {
                        stringResource(ExpenseLabels.error(it))
                    },
                )
            }

            CaretAtEndTextField(
                value = uiState.amountText,
                onValueChange = viewModel::onAmountChange,
                label = stringResource(R.string.expense_amount),
                isError = uiState.errorFor(ExpenseField.AMOUNT) != null,
                supportingText = uiState.errorFor(ExpenseField.AMOUNT)
                    ?.let { stringResource(ExpenseLabels.error(it)) }
                    ?: stringResource(R.string.expense_amount_hint),
            )

            if (uiState.showOdometer && !uiState.odometerUnknown) {
                CaretAtEndTextField(
                    value = uiState.odometerInput,
                    onValueChange = viewModel::onOdometerChange,
                    label = stringResource(R.string.expense_odometer),
                    isError = uiState.errorFor(ExpenseField.ODOMETER) != null,
                    // A última leitura fica à vista enquanto ele digita: é o
                    // que faz um dígito trocado saltar aos olhos na hora, em
                    // vez de virar consumo absurdo depois.
                    supportingText = uiState.errorFor(ExpenseField.ODOMETER)
                        ?.let { stringResource(ExpenseLabels.error(it)) }
                        ?: uiState.lastOdometerKm?.let {
                            stringResource(
                                R.string.expense_odometer_last,
                                BrazilianFormatter.kilometers(it),
                            )
                        }
                        ?: stringResource(R.string.expense_odometer_hint),
                )
            }

            // Só em lançamento retroativo: nota de posto não traz odômetro, e
            // exigir um número que não existe empurra o motorista a inventar —
            // o que envenena consumo, marco de manutenção e conciliação.
            if (uiState.allowsUnknownOdometer(viewModel.today())) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = uiState.odometerUnknown,
                        onCheckedChange = viewModel::onOdometerUnknownChange,
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.expense_odometer_unknown),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = stringResource(R.string.expense_odometer_unknown_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Só em custo fixo. Combustível queimado na terça não serve à
            // quarta — competência ali seria pergunta sem resposta útil.
            if (uiState.showAccrual) {
                Text(
                    text = stringResource(R.string.expense_accrual_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AccrualField(
                        label = stringResource(R.string.expense_accrual_start),
                        date = uiState.accrualStart,
                        isError = uiState.errorFor(ExpenseField.ACCRUAL) != null,
                        onClick = { showAccrualStartPicker = true },
                        modifier = Modifier.weight(1f),
                    )
                    AccrualField(
                        label = stringResource(R.string.expense_accrual_end),
                        date = uiState.accrualEnd,
                        isError = uiState.errorFor(ExpenseField.ACCRUAL) != null,
                        onClick = { showAccrualEndPicker = true },
                        modifier = Modifier.weight(1f),
                    )
                }
                uiState.errorFor(ExpenseField.ACCRUAL)?.let { error ->
                    Text(
                        text = stringResource(ExpenseLabels.error(error)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (uiState.accrualStart != null || uiState.accrualEnd != null) {
                    TextButton(onClick = viewModel::onAccrualCleared) {
                        Text(stringResource(R.string.expense_accrual_clear))
                    }
                }
            } else if (uiState.category == ExpenseCategory.VEHICLE_TAX) {
                // IPVA saiu da competência na v0.11.0: o valor entra inteiro
                // no mês do lançamento, sem campo de período pra preencher.
                Text(
                    text = stringResource(R.string.expense_vehicle_tax_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            when (uiState.detailKind) {
                ExpenseDetailKind.REFUEL -> {
                    Dropdown(
                        label = stringResource(R.string.expense_fuel_type),
                        options = uiState.availableFuelTypes,
                        selected = uiState.fuelType,
                        optionLabel = { stringResource(VehicleLabels.fuelType(it)) },
                        onSelect = viewModel::onFuelTypeChange,
                        error = uiState.errorFor(ExpenseField.FUEL_TYPE)?.let {
                            stringResource(ExpenseLabels.error(it))
                        },
                    )
                    QuantityField(uiState, viewModel)
                    PlaceField(uiState, viewModel, R.string.expense_station)
                }

                ExpenseDetailKind.CHARGING -> {
                    QuantityField(uiState, viewModel)
                    Dropdown(
                        label = stringResource(R.string.expense_charging_location),
                        options = ChargingLocation.entries,
                        selected = uiState.chargingLocation,
                        optionLabel = { stringResource(ExpenseLabels.chargingLocation(it)) },
                        onSelect = viewModel::onChargingLocationChange,
                        error = uiState.errorFor(ExpenseField.CHARGING_LOCATION)?.let {
                            stringResource(ExpenseLabels.error(it))
                        },
                    )
                    PlaceField(uiState, viewModel, R.string.expense_place)
                }

                ExpenseDetailKind.MAINTENANCE -> {
                    Dropdown(
                        label = stringResource(R.string.expense_maintenance_category),
                        options = MaintenanceCategory.entries,
                        selected = uiState.maintenanceCategory,
                        optionLabel = { stringResource(ExpenseLabels.maintenance(it)) },
                        onSelect = viewModel::onMaintenanceCategoryChange,
                        error = uiState.errorFor(ExpenseField.MAINTENANCE_CATEGORY)?.let {
                            stringResource(ExpenseLabels.error(it))
                        },
                    )
                    PlaceField(uiState, viewModel, R.string.expense_workshop)
                }

                ExpenseDetailKind.NONE -> Unit
            }

            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text(stringResource(R.string.expense_description)) },
                isError = uiState.errorFor(ExpenseField.DESCRIPTION) != null,
                supportingText = {
                    Text(
                        uiState.errorFor(ExpenseField.DESCRIPTION)
                            ?.let { stringResource(ExpenseLabels.error(it)) }
                            ?: stringResource(R.string.session_note_optional),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
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

    if (showAccrualStartPicker) {
        DatePickerModal(
            initialDate = uiState.accrualStart ?: uiState.date,
            onDismiss = { showAccrualStartPicker = false },
            onConfirm = { date ->
                viewModel.onAccrualStartChange(date)
                showAccrualStartPicker = false
            },
        )
    }

    if (showAccrualEndPicker) {
        DatePickerModal(
            initialDate = uiState.accrualEnd ?: uiState.accrualStart ?: uiState.date,
            onDismiss = { showAccrualEndPicker = false },
            onConfirm = { date ->
                viewModel.onAccrualEndChange(date)
                showAccrualEndPicker = false
            },
        )
    }

    if (showDatePicker) {
        DatePickerModal(
            initialDate = uiState.date,
            onDismiss = { showDatePicker = false },
            onConfirm = { date ->
                viewModel.onDateChange(date)
                showDatePicker = false
            },
        )
    }
}

/** Campo de data somente leitura, aberto por toque. */
@Composable
private fun AccrualField(
    label: String,
    date: java.time.LocalDate?,
    isError: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = date?.let(BrazilianFormatter::date).orEmpty(),
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        isError = isError,
        trailingIcon = {
            IconButton(onClick = onClick) {
                Icon(imageVector = Icons.Default.DateRange, contentDescription = label)
            }
        },
        modifier = modifier,
    )
}

/** Campo de quantidade, com a unidade certa no rótulo (litro, m³ ou kWh). */
@Composable
private fun QuantityField(
    uiState: ExpenseFormUiState,
    viewModel: ExpenseFormViewModel,
) {
    val unitLabel = uiState.quantityUnit
        ?.let { stringResource(ExpenseLabels.unit(it)) }
        .orEmpty()

    CaretAtEndTextField(
        value = uiState.quantityInput,
        onValueChange = viewModel::onQuantityChange,
        label = if (unitLabel.isEmpty()) {
            stringResource(R.string.expense_quantity)
        } else {
            stringResource(R.string.expense_quantity_with_unit, unitLabel)
        },
        isError = uiState.errorFor(ExpenseField.QUANTITY) != null,
        // Decimal, e não Number: a quantidade é digitada com vírgula, como o
        // motorista lê na bomba (v0.4.0).
        keyboardType = KeyboardType.Decimal,
        supportingText = uiState.errorFor(ExpenseField.QUANTITY)
            ?.let { stringResource(ExpenseLabels.error(it)) }
            ?: stringResource(R.string.expense_quantity_hint),
    )
}


/** Posto, eletroposto ou oficina — o mesmo campo com rótulo diferente. */
@Composable
private fun PlaceField(
    uiState: ExpenseFormUiState,
    viewModel: ExpenseFormViewModel,
    labelRes: Int,
) {
    OutlinedTextField(
        value = uiState.place,
        onValueChange = viewModel::onPlaceChange,
        label = { Text(stringResource(labelRes)) },
        singleLine = true,
        isError = uiState.errorFor(ExpenseField.PLACE) != null ||
            uiState.errorFor(ExpenseField.STATION) != null ||
            uiState.errorFor(ExpenseField.WORKSHOP) != null,
        supportingText = { Text(stringResource(R.string.session_note_optional)) },
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModal(
    initialDate: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate
            ?.atStartOfDay(ZoneOffset.UTC)
            ?.toInstant()
            ?.toEpochMilli(),
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onConfirm(
                            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate(),
                        )
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
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> Dropdown(
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
        expanded = expanded && options.isNotEmpty(),
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
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded && options.isNotEmpty(),
            onDismissRequest = { expanded = false },
        ) {
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
