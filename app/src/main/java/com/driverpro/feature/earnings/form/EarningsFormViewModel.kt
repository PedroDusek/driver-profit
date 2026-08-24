package com.driverpro.feature.earnings.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driverpro.core.domain.Money
import com.driverpro.core.domain.WorkDuration
import com.driverpro.core.navigation.DriverProDestination
import com.driverpro.core.ui.format.MoneyInput
import com.driverpro.domain.model.Platform
import com.driverpro.domain.model.WorkSession
import com.driverpro.domain.model.WorkSessionDraft
import com.driverpro.domain.model.WorkSessionField
import com.driverpro.domain.model.WorkSessionFieldError
import com.driverpro.domain.model.WorkSessionValidationError
import com.driverpro.domain.model.toDraft
import com.driverpro.domain.usecase.GetWorkSessionUseCase
import com.driverpro.vehicle.domain.ObserveVehiclesUseCase
import com.driverpro.domain.usecase.SaveWorkSessionResult
import com.driverpro.domain.usecase.SaveWorkSessionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

/**
 * Estado do formulário de sessão de trabalho.
 *
 * Os campos numéricos são `String` de dígitos, e não números: o campo precisa
 * representar "vazio" e "meio digitado", estados que `Int` não tem.
 *
 * [revenueDigits] guarda **centavos digitados**: teclar `32050` significa
 * R$ 320,50. É o padrão de entrada monetária que evita o usuário brigar com
 * vírgula, ponto e teclado numérico.
 */
data class EarningsFormUiState(
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val date: LocalDate? = null,
    val platform: Platform? = null,
    val revenueDigits: String = "",
    val ridesInput: String = "",
    val hoursInput: String = "",
    val minutesInput: String = "",
    val distanceInput: String = "",
    val note: String = "",
    val errors: Map<WorkSessionField, WorkSessionValidationError> = emptyMap(),
    val savedSessionId: Long? = null,
    /**
     * Veículo atual no momento do lançamento (v0.12.0).
     *
     * Nenhum campo do formulário edita isto — é gravado automaticamente. Numa
     * sessão nova é o veículo atual no momento em que a tela abriu; numa
     * edição é o que já estava gravado, para não reclassificar histórico
     * quando o motorista troca de veículo depois.
     */
    val vehicleId: Long? = null,
) {
    /**
     * `null` enquanto o campo estiver em branco.
     *
     * Distinguir "vazio" de "zero" é o que permite exigir o preenchimento sem
     * impedir que o motorista registre um dia de R$ 0,00 — que é um dia real.
     */
    val revenue: Money? get() = MoneyInput.toMoney(revenueDigits)

    /** Texto já formatado do campo de valor. */
    val revenueText: String get() = MoneyInput.display(revenueDigits)

    /** `null` enquanto horas e minutos estiverem ambos em branco. */
    val onlineTime: WorkDuration?
        get() = if (hoursInput.isEmpty() && minutesInput.isEmpty()) {
            null
        } else {
            WorkDuration(
                (hoursInput.toLongOrNull() ?: 0L) * WorkDuration.MINUTES_PER_HOUR +
                    (minutesInput.toLongOrNull() ?: 0L),
            )
        }

    fun errorFor(field: WorkSessionField): WorkSessionValidationError? = errors[field]
}

/**
 * Registro e edição de sessão de trabalho.
 *
 * A ViewModel monta o [WorkSessionDraft] e entrega ao
 * [SaveWorkSessionUseCase], que é o dono da validação (PRD §25).
 */
class EarningsFormViewModel(
    savedStateHandle: SavedStateHandle,
    private val getWorkSession: GetWorkSessionUseCase,
    private val saveWorkSession: SaveWorkSessionUseCase,
    private val observeVehicles: ObserveVehiclesUseCase,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val sessionId: Long =
        savedStateHandle.get<Long>(DriverProDestination.ARG_SESSION_ID)
            ?: WorkSession.UNSAVED_ID

    private val _uiState = MutableStateFlow(
        EarningsFormUiState(
            isEditing = sessionId != WorkSession.UNSAVED_ID,
            isLoading = sessionId != WorkSession.UNSAVED_ID,
            // Um lançamento novo quase sempre é do dia — começar preenchido
            // com hoje poupa um toque no caso mais comum.
            date = if (sessionId == WorkSession.UNSAVED_ID) LocalDate.now(clock) else null,
        ),
    )
    val uiState: StateFlow<EarningsFormUiState> = _uiState.asStateFlow()

    init {
        if (sessionId != WorkSession.UNSAVED_ID) {
            loadSession(sessionId)
        } else {
            viewModelScope.launch {
                _uiState.update { it.copy(vehicleId = currentVehicleId()) }
            }
        }
    }

    /** Veículo atual no momento em que a tela abriu (v0.12.0). */
    private suspend fun currentVehicleId(): Long? =
        observeVehicles().first().firstOrNull { it.isCurrent }?.id

    private fun loadSession(id: Long) {
        viewModelScope.launch {
            val session = getWorkSession(id)
            if (session == null) {
                // Sessão excluída em outra tela enquanto esta abria — trata
                // como lançamento novo, então busca o veículo atual também.
                val vehicleId = currentVehicleId()
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        isEditing = false,
                        date = LocalDate.now(clock),
                        vehicleId = vehicleId,
                    )
                }
            } else {
                val draft = session.toDraft()
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        isEditing = true,
                        date = draft.date,
                        platform = draft.platform,
                        revenueDigits = draft.revenue?.cents?.toString().orEmpty(),
                        ridesInput = draft.rides?.toString().orEmpty(),
                        hoursInput = draft.onlineTime?.wholeHours?.toString().orEmpty(),
                        minutesInput = draft.onlineTime?.remainingMinutes?.toString().orEmpty(),
                        distanceInput = draft.distanceKm?.toString().orEmpty(),
                        note = draft.note,
                        // Preserva o veículo já gravado — não re-deriva do
                        // atual, para o histórico não mudar de dono quando o
                        // motorista troca de veículo depois.
                        vehicleId = draft.vehicleId,
                    )
                }
            }
        }
    }

    fun onDateChange(value: LocalDate) = updateField(WorkSessionField.DATE) { copy(date = value) }

    fun onPlatformChange(value: Platform) =
        updateField(WorkSessionField.PLATFORM) { copy(platform = value) }

    fun onRevenueChange(value: String) = updateField(WorkSessionField.REVENUE) {
        copy(revenueDigits = MoneyInput.onTextChanged(revenueDigits, value, REVENUE_MAX_DIGITS))
    }

    fun onRidesChange(value: String) = updateField(WorkSessionField.RIDES) {
        copy(ridesInput = value.digits(RIDES_MAX_DIGITS))
    }

    fun onHoursChange(value: String) = updateField(WorkSessionField.ONLINE_TIME) {
        copy(hoursInput = value.digits(HOURS_MAX_DIGITS))
    }

    /** Minutos acima de 59 são rejeitados na digitação, não no save. */
    fun onMinutesChange(value: String) = updateField(WorkSessionField.ONLINE_TIME) {
        val digits = value.digits(MINUTES_MAX_DIGITS)
        val capped = digits.takeIf { (it.toLongOrNull() ?: 0L) < 60 } ?: minutesInput
        copy(minutesInput = capped)
    }

    fun onDistanceChange(value: String) = updateField(WorkSessionField.DISTANCE) {
        copy(distanceInput = value.digits(DISTANCE_MAX_DIGITS))
    }

    fun onNoteChange(value: String) = updateField(WorkSessionField.NOTE) { copy(note = value) }

    fun onSave() {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            when (val result = saveWorkSession(_uiState.value.toDraft(sessionId))) {
                is SaveWorkSessionResult.Success ->
                    _uiState.update { it.copy(isSaving = false, savedSessionId = result.id) }

                is SaveWorkSessionResult.Invalid ->
                    _uiState.update { it.copy(isSaving = false, errors = result.errors.toMap()) }
            }
        }
    }

    /** Consumido pela UI após navegar de volta, para não navegar duas vezes. */
    fun onNavigatedBack() {
        _uiState.update { it.copy(savedSessionId = null) }
    }

    private fun updateField(
        field: WorkSessionField,
        transform: EarningsFormUiState.() -> EarningsFormUiState,
    ) {
        _uiState.update { current ->
            val remaining = current.errors - field
            // EMPTY_SESSION é erro da sessão inteira, apenas exibido no campo
            // de faturamento: preencher qualquer número pode resolvê-lo, então
            // ele sai a cada edição. Os demais erros do faturamento são só
            // dele e continuam até o próximo save.
            val cleared =
                if (remaining[WorkSessionField.REVENUE] ==
                    WorkSessionValidationError.EMPTY_SESSION
                ) {
                    remaining - WorkSessionField.REVENUE
                } else {
                    remaining
                }
            current.transform().copy(errors = cleared)
        }
    }

    private companion object {
        const val REVENUE_MAX_DIGITS = 9
        const val RIDES_MAX_DIGITS = 3
        const val HOURS_MAX_DIGITS = 2
        const val MINUTES_MAX_DIGITS = 2
        const val DISTANCE_MAX_DIGITS = 4
    }
}

private fun String.digits(max: Int): String = filter(Char::isDigit).take(max)

private fun List<WorkSessionFieldError>.toMap(): Map<WorkSessionField, WorkSessionValidationError> =
    associate { it.field to it.error }

/**
 * Converte o estado de tela no rascunho que o domínio entende.
 *
 * [sessionId] precisa ser o id da sessão em edição — é ele que faz o
 * [SaveWorkSessionUseCase] atualizar em vez de inserir.
 */
internal fun EarningsFormUiState.toDraft(
    sessionId: Long = WorkSession.UNSAVED_ID,
): WorkSessionDraft = WorkSessionDraft(
    id = sessionId,
    vehicleId = vehicleId,
    date = date,
    platform = platform,
    rides = ridesInput.toIntOrNull(),
    revenue = revenue,
    onlineTime = onlineTime,
    distanceKm = distanceInput.toLongOrNull(),
    note = note,
)
