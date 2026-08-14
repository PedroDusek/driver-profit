package com.driverprofit.feature.personal.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.driverprofit.R
import com.driverprofit.core.ui.DriverProfitViewModelFactory
import com.driverprofit.core.ui.component.CaretAtEndTextField
import com.driverprofit.core.ui.format.BrazilianFormatter
import com.driverprofit.core.ui.format.PersonalUsageLabels
import com.driverprofit.domain.model.PersonalUsageField
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Registro e edição de viagem pessoal (PRD §22). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalUsageFormScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PersonalUsageFormViewModel =
        viewModel(factory = DriverProfitViewModelFactory.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.savedId) {
        if (uiState.savedId != null) {
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
                            if (uiState.isEditing) {
                                R.string.personal_usage_form_edit_title
                            } else {
                                R.string.personal_usage_form_add_title
                            },
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            VehicleDropdown(
                vehicles = uiState.vehicles.map { it.id to it.name },
                selectedId = uiState.vehicleId,
                onSelect = viewModel::onVehicleChange,
                error = uiState.errorFor(PersonalUsageField.VEHICLE)?.let {
                    stringResource(PersonalUsageLabels.error(it))
                },
            )

            DateField(
                label = stringResource(R.string.personal_usage_start),
                date = uiState.start,
                onPick = { pickingStart = true },
                error = uiState.errorFor(PersonalUsageField.START)?.let {
                    stringResource(PersonalUsageLabels.error(it))
                },
            )

            DateField(
                label = stringResource(R.string.personal_usage_end),
                date = uiState.end,
                onPick = { pickingEnd = true },
                onClear = { viewModel.onEndChange(null) },
                supporting = stringResource(R.string.personal_usage_end_hint),
                error = uiState.errorFor(PersonalUsageField.END)?.let {
                    stringResource(PersonalUsageLabels.error(it))
                },
            )

            CaretAtEndTextField(
                value = uiState.distanceInput,
                onValueChange = viewModel::onDistanceChange,
                label = stringResource(R.string.personal_usage_distance),
                isError = uiState.errorFor(PersonalUsageField.DISTANCE) != null,
                supportingText = uiState.errorFor(PersonalUsageField.DISTANCE)?.let {
                    stringResource(PersonalUsageLabels.error(it))
                },
            )

            OutlinedTextField(
                value = uiState.note,
                onValueChange = viewModel::onNoteChange,
                label = { Text(stringResource(R.string.personal_usage_note)) },
                isError = uiState.errorFor(PersonalUsageField.NOTE) != null,
                supportingText = uiState.errorFor(PersonalUsageField.NOTE)?.let {
                    { Text(stringResource(PersonalUsageLabels.error(it))) }
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

    if (pickingStart) {
        DatePickerModal(
            initialDate = uiState.start,
            onDismiss = { pickingStart = false },
            onConfirm = {
                viewModel.onStartChange(it)
                pickingStart = false
            },
        )
    }

    if (pickingEnd) {
        DatePickerModal(
            initialDate = uiState.end ?: uiState.start,
            onDismiss = { pickingEnd = false },
            onConfirm = {
                viewModel.onEndChange(it)
                pickingEnd = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    label: String,
    date: LocalDate?,
    onPick: () -> Unit,
    error: String?,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
    supporting: String? = null,
) {
    OutlinedTextField(
        value = date?.let(BrazilianFormatter::date).orEmpty(),
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        isError = error != null,
        supportingText = (error ?: supporting)?.let { { Text(it) } },
        trailingIcon = {
            Row {
                if (onClear != null && date != null) {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.action_cancel),
                        )
                    }
                }
                IconButton(onClick = onPick) {
                    Icon(imageVector = Icons.Default.DateRange, contentDescription = label)
                }
            }
        },
        modifier = modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VehicleDropdown(
    vehicles: List<Pair<Long, String>>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    error: String?,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = vehicles.firstOrNull { it.first == selectedId }?.second.orEmpty()

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.personal_usage_vehicle)) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            vehicles.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelect(id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModal(
    initialDate: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    // Epoch millis UTC nas duas pontas, para o dia não escorregar por fuso.
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
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    ) {
        DatePicker(state = state)
    }
}
