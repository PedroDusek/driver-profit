package com.driverprofit.feature.expenses.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driverprofit.core.common.Money
import com.driverprofit.core.common.Quantity
import com.driverprofit.core.navigation.DriverProfitDestination
import com.driverprofit.core.ui.format.MoneyInput
import com.driverprofit.core.ui.format.QuantityInput
import com.driverprofit.domain.model.ChargingLocation
import com.driverprofit.domain.model.Expense
import com.driverprofit.domain.model.ExpenseCategory
import com.driverprofit.domain.model.ExpenseDetailKind
import com.driverprofit.domain.model.ExpenseDraft
import com.driverprofit.domain.model.ExpenseField
import com.driverprofit.domain.model.ExpenseFieldError
import com.driverprofit.domain.model.ExpenseValidationError
import com.driverprofit.domain.model.FuelType
import com.driverprofit.domain.model.MaintenanceCategory
import com.driverprofit.domain.model.MeasurementUnit
import com.driverprofit.domain.model.Vehicle
import com.driverprofit.domain.model.toDraft
import com.driverprofit.domain.usecase.GetExpenseUseCase
import com.driverprofit.domain.usecase.ObserveVehicleOdometersUseCase
import com.driverprofit.domain.usecase.ObserveVehiclesUseCase
import com.driverprofit.domain.usecase.SaveExpenseResult
import com.driverprofit.domain.usecase.SaveExpenseUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

/**
 * Estado do formulário de despesa.
 *
 * O formulário é dinâmico (PRD §7): os campos extras mudam conforme a
 * categoria escolhida, e as opções de combustível vêm do veículo selecionado —
 * o motorista nunca vê etanol se o carro dele só aceita GNV.
 */
data class ExpenseFormUiState(
    val isEditing: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val vehicles: List<Vehicle> = emptyList(),
    val vehicleId: Long? = null,
    val date: LocalDate? = null,
    val category: ExpenseCategory? = null,
    val amountDigits: String = "",
    val odometerInput: String = "",
    /** O motorista declarou que não sabe a leitura deste lançamento antigo. */
    val odometerUnknown: Boolean = false,
    /** Última leitura conhecida de cada veículo, para conferência na tela. */
    val odometers: Map<Long, Long> = emptyMap(),
    val description: String = "",
    val fuelType: FuelType? = null,
    val quantityInput: String = "",
    val chargingLocation: ChargingLocation? = null,
    val place: String = "",
    val maintenanceCategory: MaintenanceCategory? = null,
    /** Início da competência, quando o motorista declarou (PRD §22). */
    val accrualStart: LocalDate? = null,
    /** Fim da competência, inclusive. */
    val accrualEnd: LocalDate? = null,
    val errors: Map<ExpenseField, ExpenseValidationError> = emptyMap(),
    val savedExpenseId: Long? = null,
) {
    val amount: Money? get() = MoneyInput.toMoney(amountDigits)

    /** Texto já formatado do campo de valor. */
    val amountText: String get() = MoneyInput.display(amountDigits)

    val quantity: Quantity? get() = QuantityInput.parse(quantityInput)

    val selectedVehicle: Vehicle? get() = vehicles.firstOrNull { it.id == vehicleId }

    val detailKind: ExpenseDetailKind
        get() = category?.detailKind ?: ExpenseDetailKind.NONE

    /** Só oferece combustíveis que o veículo escolhido aceita (PRD §7). */
    val availableFuelTypes: List<FuelType>
        get() = selectedVehicle?.refuelOptions.orEmpty()
            .filter { it != FuelType.ELECTRICITY }

    /** Unidade do campo de quantidade, conforme o que está sendo lançado. */
    val quantityUnit: MeasurementUnit?
        get() = when (detailKind) {
            ExpenseDetailKind.REFUEL -> fuelType?.unit
            ExpenseDetailKind.CHARGING -> MeasurementUnit.KILOWATT_HOUR
            else -> null
        }

    val odometerKm: Long? get() = odometerInput.toLongOrNull()

    /**
     * Maior leitura já registrada para o veículo escolhido.
     *
     * Exibida como referência no formulário: ver "última leitura: 45.200 km"
     * ao digitar é o que faz o motorista perceber um dígito trocado na hora,
     * em vez de descobrir depois num consumo estimado absurdo.
     */
    val lastOdometerKm: Long? get() = vehicleId?.let { odometers[it] }

    val showVehicle: Boolean get() = category?.requiresVehicle == true

    /**
     * A competência só é oferecida em custo fixo — seguro, IPVA e
     * financiamento (PRD §22).
     *
     * Custo variável se esgota no dia em que foi pago: combustível queimado na
     * terça não serve à quarta. Oferecer o campo ali só criaria uma pergunta
     * sem resposta útil, e um lugar a mais para errar.
     */
    val showAccrual: Boolean get() = category?.isOperationalCost == false

    /** O odômetro só é pedido onde há veículo em jogo (PRD §23). */
    val showOdometer: Boolean get() = showVehicle

    /**
     * A saída "não sei a leitura" só é oferecida em lançamento com data
     * anterior a hoje.
     *
     * No lançamento do dia o painel está à mão. Oferecer a saída ali a
     * transformaria em rotina, e a leitura por lançamento — fundação do
     * consumo, do quilômetro pessoal e do alerta de manutenção — deixaria de
     * existir na prática.
     */
    fun allowsUnknownOdometer(today: LocalDate): Boolean =
        showOdometer && date?.isBefore(today) == true

    fun errorFor(field: ExpenseField): ExpenseValidationError? = errors[field]
}

/**
 * Registro e edição de despesa.
 *
 * A ViewModel monta o [ExpenseDraft] e entrega ao [SaveExpenseUseCase], que é
 * o dono da validação — inclusive das regras que dependem do veículo (PRD §25).
 */
class ExpenseFormViewModel(
    savedStateHandle: SavedStateHandle,
    observeVehicles: ObserveVehiclesUseCase,
    observeVehicleOdometers: ObserveVehicleOdometersUseCase,
    private val getExpense: GetExpenseUseCase,
    private val saveExpense: SaveExpenseUseCase,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val expenseId: Long =
        savedStateHandle.get<Long>(DriverProfitDestination.ARG_EXPENSE_ID)
            ?: Expense.UNSAVED_ID

    private val _uiState = MutableStateFlow(
        ExpenseFormUiState(isEditing = expenseId != Expense.UNSAVED_ID),
    )
    val uiState: StateFlow<ExpenseFormUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Um snapshot basta: a lista de veículos não muda enquanto este
            // formulário está aberto, e observá-la continuamente só traria o
            // risco de o campo trocar sozinho sob o dedo do motorista.
            val vehicles = observeVehicles().first()
            val odometers = observeVehicleOdometers().first()
            val existing = expenseId.takeIf { it != Expense.UNSAVED_ID }?.let { getExpense(it) }

            _uiState.update { state ->
                val loaded = existing?.toDraft()
                state.copy(
                    isLoading = false,
                    isEditing = loaded != null,
                    vehicles = vehicles,
                    date = loaded?.date ?: LocalDate.now(clock),
                    // Com um veículo só, escolher por ele é o certo — pedir
                    // que selecione o único carro que existe é burocracia.
                    vehicleId = loaded?.vehicleId ?: vehicles.singleOrNull()?.id,
                    category = loaded?.category,
                    amountDigits = loaded?.amount?.cents?.toString().orEmpty(),
                    odometerInput = loaded?.odometerKm?.toString().orEmpty(),
                    // Despesa antiga gravada sem leitura volta com a caixa
                    // marcada, para editar não exigir um número que nunca houve.
                    odometerUnknown = loaded != null &&
                        loaded.category?.requiresVehicle == true &&
                        loaded.odometerKm == null,
                    odometers = odometers,
                    description = loaded?.description.orEmpty(),
                    fuelType = loaded?.fuelType,
                    quantityInput = loaded?.quantity?.let(QuantityInput::display).orEmpty(),
                    chargingLocation = loaded?.chargingLocation,
                    place = loaded?.let { it.station.ifEmpty { it.place.ifEmpty { it.workshop } } }
                        .orEmpty(),
                    maintenanceCategory = loaded?.maintenanceCategory,
                    accrualStart = loaded?.accrualStart,
                    accrualEnd = loaded?.accrualEnd,
                )
            }
        }
    }

    /**
     * Troca a data e, se ela deixar de ser retroativa, desfaz a declaração de
     * leitura desconhecida.
     *
     * Sem isso a declaração ficaria pendurada num estado que não a permite: a
     * caixa some da tela, o campo de odômetro continua escondido por causa dela,
     * e o motorista recebe "campo obrigatório" sem ter onde preencher.
     */
    fun onDateChange(value: LocalDate) = updateField(ExpenseField.DATE) {
        copy(
            date = value,
            odometerUnknown = odometerUnknown && value.isBefore(LocalDate.now(clock)),
        )
    }

    /**
     * Troca a categoria e limpa os campos de detalhe que deixaram de existir.
     *
     * Sem isso, escolher "abastecimento", preencher litros e depois trocar para
     * "pedágio" salvaria um pedágio com combustível pendurado.
     */
    fun onCategoryChange(value: ExpenseCategory) {
        _uiState.update { state ->
            val keepsDetail = state.category?.detailKind == value.detailKind
            state.copy(
                category = value,
                fuelType = state.fuelType.takeIf { keepsDetail },
                quantityInput = if (keepsDetail) state.quantityInput else "",
                chargingLocation = state.chargingLocation.takeIf { keepsDetail },
                place = if (keepsDetail) state.place else "",
                maintenanceCategory = state.maintenanceCategory.takeIf { keepsDetail },
                errors = emptyMap(),
            )
        }
    }

    fun onVehicleChange(value: Long) = updateField(ExpenseField.VEHICLE) {
        // O combustível escolhido pode não existir no carro novo.
        val stillValid = fuelType != null &&
            vehicles.firstOrNull { it.id == value }?.refuelOptions?.contains(fuelType) == true
        copy(vehicleId = value, fuelType = fuelType.takeIf { stillValid })
    }

    fun onAmountChange(value: String) = updateField(ExpenseField.AMOUNT) {
        // Preserva um "0" digitado: recarga gratuita é R$ 0,00 (PRD §11).
        copy(amountDigits = MoneyInput.onTextChanged(amountDigits, value))
    }

    /** Só dígitos: odômetro é quilômetro inteiro, sem separador nem fração. */
    fun onOdometerChange(value: String) = updateField(ExpenseField.ODOMETER) {
        copy(
            odometerInput = value.filter(Char::isDigit).take(MAX_ODOMETER_DIGITS),
            // Digitar a leitura desmarca a declaração de ignorância: os dois
            // juntos seriam contraditórios.
            odometerUnknown = false,
        )
    }

    fun onDescriptionChange(value: String) =
        updateField(ExpenseField.DESCRIPTION) { copy(description = value) }

    fun onFuelTypeChange(value: FuelType) =
        updateField(ExpenseField.FUEL_TYPE) { copy(fuelType = value) }

    fun onQuantityChange(value: String) = updateField(ExpenseField.QUANTITY) {
        copy(quantityInput = QuantityInput.sanitize(value))
    }

    fun onChargingLocationChange(value: ChargingLocation) =
        updateField(ExpenseField.CHARGING_LOCATION) { copy(chargingLocation = value) }

    fun onMaintenanceCategoryChange(value: MaintenanceCategory) =
        updateField(ExpenseField.MAINTENANCE_CATEGORY) { copy(maintenanceCategory = value) }

    fun onPlaceChange(value: String) = updateField(ExpenseField.PLACE) { copy(place = value) }

    fun onAccrualStartChange(value: LocalDate) = updateField(ExpenseField.ACCRUAL) {
        // O fim acompanha o início quando ainda não existe ou ficou para trás:
        // um intervalo invertido não é escolha, é ordem de preenchimento.
        copy(accrualStart = value, accrualEnd = accrualEnd?.takeIf { !it.isBefore(value) })
    }

    fun onAccrualEndChange(value: LocalDate) = updateField(ExpenseField.ACCRUAL) {
        copy(accrualEnd = value)
    }

    /** Limpa a competência — a despesa volta a contar no próprio dia. */
    fun onAccrualCleared() = updateField(ExpenseField.ACCRUAL) {
        copy(accrualStart = null, accrualEnd = null)
    }

    /** Dia de referência, para decidir se o lançamento é retroativo. */
    fun today(): LocalDate = LocalDate.now(clock)

    /** Alterna a declaração de que a leitura é desconhecida. */
    fun onOdometerUnknownChange(value: Boolean) {
        _uiState.update {
            it.copy(
                odometerUnknown = value,
                odometerInput = if (value) "" else it.odometerInput,
                errors = it.errors - ExpenseField.ODOMETER,
            )
        }
    }

    fun onSave() {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            when (val result = saveExpense(_uiState.value.toDraft(expenseId))) {
                is SaveExpenseResult.Success ->
                    _uiState.update { it.copy(isSaving = false, savedExpenseId = result.id) }

                is SaveExpenseResult.Invalid ->
                    _uiState.update { it.copy(isSaving = false, errors = result.errors.toMap()) }
            }
        }
    }

    /** Consumido pela UI após navegar de volta, para não navegar duas vezes. */
    fun onNavigatedBack() {
        _uiState.update { it.copy(savedExpenseId = null) }
    }

    private fun updateField(
        field: ExpenseField,
        transform: ExpenseFormUiState.() -> ExpenseFormUiState,
    ) {
        _uiState.update { it.transform().copy(errors = it.errors - field) }
    }

    private companion object {
        /** Sete dígitos cobrem o teto de `Expense.MAX_ODOMETER_KM`. */
        const val MAX_ODOMETER_DIGITS = 7
    }
}

private fun List<ExpenseFieldError>.toMap(): Map<ExpenseField, ExpenseValidationError> =
    associate { it.field to it.error }

/**
 * Converte o estado de tela no rascunho que o domínio entende.
 *
 * O campo de local é um só na tela — posto, eletroposto ou oficina, conforme a
 * categoria — e só aqui ele é direcionado para o campo certo do rascunho.
 */
internal fun ExpenseFormUiState.toDraft(expenseId: Long = Expense.UNSAVED_ID): ExpenseDraft =
    ExpenseDraft(
        id = expenseId,
        vehicleId = vehicleId.takeIf { showVehicle },
        date = date,
        category = category,
        amount = amount,
        description = description,
        accrualStart = accrualStart.takeIf { showAccrual },
        accrualEnd = accrualEnd.takeIf { showAccrual },
        odometerKm = odometerKm.takeIf { showOdometer && !odometerUnknown },
        odometerUnknown = odometerUnknown && showOdometer,
        fuelType = fuelType,
        quantity = quantity,
        station = if (detailKind == ExpenseDetailKind.REFUEL) place else "",
        chargingLocation = chargingLocation,
        place = if (detailKind == ExpenseDetailKind.CHARGING) place else "",
        maintenanceCategory = maintenanceCategory,
        workshop = if (detailKind == ExpenseDetailKind.MAINTENANCE) place else "",
    )
