package com.driverpro.feature.earnings.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.driverpro.R
import com.driverpro.core.di.DriverProViewModelFactory
import com.driverpro.core.ui.component.CaretAtEndTextField
import com.driverpro.core.ui.format.BrazilianFormatter
import com.driverpro.core.ui.format.EarningsLabels
import com.driverpro.core.ui.theme.DriverProTheme
import com.driverpro.domain.model.Platform
import com.driverpro.domain.model.WorkSessionField
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Registro e edição de sessão de trabalho (PRD §15).
 *
 * O valor é digitado em centavos e formatado enquanto o motorista digita —
 * teclar `32050` mostra `R$ 320,50`. É o padrão que evita briga com vírgula,
 * ponto e teclado numérico.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarningsFormScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EarningsFormViewModel = viewModel(factory = DriverProViewModelFactory.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.savedSessionId) {
        if (uiState.savedSessionId != null) {
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
                            if (uiState.isEditing) R.string.earnings_form_edit_title
                            else R.string.earnings_form_add_title,
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
                label = { Text(stringResource(R.string.session_date)) },
                isError = uiState.errorFor(WorkSessionField.DATE) != null,
                supportingText = uiState.errorFor(WorkSessionField.DATE)?.let {
                    { Text(stringResource(EarningsLabels.error(it))) }
                },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = stringResource(R.string.session_date),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            PlatformDropdown(
                selected = uiState.platform,
                onSelect = viewModel::onPlatformChange,
                error = uiState.errorFor(WorkSessionField.PLATFORM)?.let {
                    stringResource(EarningsLabels.error(it))
                },
            )

            CaretAtEndTextField(
                // Campo em branco fica em branco: mostrar "R$ 0,00" faria
                // parecer preenchido, e o valor é obrigatório.
                value = uiState.revenueText,
                onValueChange = viewModel::onRevenueChange,
                label = stringResource(R.string.session_revenue),
                isError = uiState.errorFor(WorkSessionField.REVENUE) != null,
                supportingText = uiState.errorFor(WorkSessionField.REVENUE)?.let {
                    stringResource(EarningsLabels.error(it))
                },
            )

            NumberField(
                value = uiState.ridesInput,
                onValueChange = viewModel::onRidesChange,
                label = stringResource(R.string.session_rides),
                error = uiState.errorFor(WorkSessionField.RIDES)?.let {
                    stringResource(EarningsLabels.error(it))
                },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField(
                    value = uiState.hoursInput,
                    onValueChange = viewModel::onHoursChange,
                    label = stringResource(R.string.session_hours),
                    modifier = Modifier.weight(1f),
                )
                NumberField(
                    value = uiState.minutesInput,
                    onValueChange = viewModel::onMinutesChange,
                    label = stringResource(R.string.session_minutes),
                    modifier = Modifier.weight(1f),
                )
            }
            uiState.errorFor(WorkSessionField.ONLINE_TIME)?.let {
                Text(text = stringResource(EarningsLabels.error(it)))
            }

            NumberField(
                value = uiState.distanceInput,
                onValueChange = viewModel::onDistanceChange,
                label = stringResource(R.string.session_distance),
                error = uiState.errorFor(WorkSessionField.DISTANCE)?.let {
                    stringResource(EarningsLabels.error(it))
                },
            )

            OutlinedTextField(
                value = uiState.note,
                onValueChange = viewModel::onNoteChange,
                label = { Text(stringResource(R.string.session_note)) },
                isError = uiState.errorFor(WorkSessionField.NOTE) != null,
                supportingText = {
                    Text(
                        uiState.errorFor(WorkSessionField.NOTE)
                            ?.let { stringResource(EarningsLabels.error(it)) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModal(
    initialDate: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    // O DatePicker trabalha em epoch millis UTC; a conversão acontece nas duas
    // pontas para que o dia escolhido não escorregue por causa de fuso.
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
private fun PlatformDropdown(
    selected: Platform?,
    onSelect: (Platform) -> Unit,
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
            value = selected?.let { stringResource(EarningsLabels.platform(it)) }.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.session_platform)) },
            isError = error != null,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            supportingText = error?.let { { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Platform.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(EarningsLabels.platform(option))) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    CaretAtEndTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        isError = error != null,
        supportingText = error,
    )
}

@Preview(showBackground = true)
@Composable
private fun EarningsFormFieldsPreview() {
    DriverProTheme(dynamicColor = false) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            NumberField(value = "18", onValueChange = {}, label = "Corridas")
            NumberField(
                value = "",
                onValueChange = {},
                label = "Km rodados",
                error = "O valor não pode ser negativo",
            )
        }
    }
}
