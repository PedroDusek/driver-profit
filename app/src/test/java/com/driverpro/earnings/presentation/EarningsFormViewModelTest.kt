package com.driverpro.earnings.presentation

import androidx.lifecycle.SavedStateHandle
import com.driverpro.core.domain.Money
import com.driverpro.core.domain.WorkDuration
import com.driverpro.core.navigation.DriverProDestination
import com.driverpro.earnings.domain.Platform
import com.driverpro.vehicle.domain.Vehicle
import com.driverpro.vehicle.domain.VehicleFuel
import com.driverpro.earnings.domain.WorkSession
import com.driverpro.earnings.domain.WorkSessionField
import com.driverpro.earnings.domain.WorkSessionValidationError
import com.driverpro.earnings.domain.GetWorkSessionUseCase
import com.driverpro.vehicle.domain.ObserveVehiclesUseCase
import com.driverpro.earnings.domain.SaveWorkSessionUseCase
import com.driverpro.earnings.domain.WorkSessionValidator
import com.driverpro.earnings.presentation.form.EarningsFormViewModel
import com.driverpro.vehicle.domain.FakeVehicleRepository
import com.driverpro.earnings.domain.FakeWorkSessionRepository
import com.driverpro.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class EarningsFormViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val hoje = LocalDate.of(2026, 8, 11)
    private val clock = Clock.fixed(Instant.parse("2026-08-11T12:00:00Z"), ZoneId.of("UTC"))

    private val existingSession = WorkSession(
        id = 1,
        date = LocalDate.of(2026, 8, 10),
        platform = Platform.NINETY_NINE,
        rides = 18,
        revenue = Money.of(320, 50),
        onlineTime = WorkDuration.of(8, 20),
        distanceKm = 210,
        note = "dia bom",
        createdAt = Instant.parse("2024-01-15T08:00:00Z"),
    )

    private fun viewModel(
        repository: FakeWorkSessionRepository = FakeWorkSessionRepository(),
        sessionId: Long? = null,
        vehicles: FakeVehicleRepository = FakeVehicleRepository(),
    ): EarningsFormViewModel {
        val handle = SavedStateHandle(
            sessionId?.let { mapOf(DriverProDestination.ARG_SESSION_ID to it) } ?: emptyMap(),
        )
        return EarningsFormViewModel(
            savedStateHandle = handle,
            getWorkSession = GetWorkSessionUseCase(repository),
            saveWorkSession = SaveWorkSessionUseCase(repository, WorkSessionValidator(clock)),
            observeVehicles = ObserveVehiclesUseCase(vehicles),
            clock = clock,
        )
    }

    @Test
    fun `lancamento novo comeca com a data de hoje`() = runTest {
        // Um lancamento novo quase sempre e do dia; poupa um toque.
        val state = viewModel().uiState.value

        assertEquals(hoje, state.date)
        assertFalse(state.isEditing)
        assertNull(state.platform)
    }

    @Test
    fun `edicao carrega os dados da sessao`() = runTest {
        val viewModel = viewModel(FakeWorkSessionRepository(listOf(existingSession)), sessionId = 1)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isEditing)
        assertFalse(state.isLoading)
        assertEquals(LocalDate.of(2026, 8, 10), state.date)
        assertEquals(Platform.NINETY_NINE, state.platform)
        assertEquals("32050", state.revenueDigits)
        assertEquals("18", state.ridesInput)
        assertEquals("8", state.hoursInput)
        assertEquals("20", state.minutesInput)
        assertEquals("210", state.distanceInput)
        assertEquals("dia bom", state.note)
    }

    @Test
    fun `lancamento novo pega o veiculo atual`() = runTest {
        val onix = Vehicle(1, "Onix", VehicleFuel.FLEX, Instant.EPOCH, isCurrent = false)
        val civic = Vehicle(2, "Civic", VehicleFuel.FLEX, Instant.EPOCH, isCurrent = true)
        val viewModel = viewModel(vehicles = FakeVehicleRepository(listOf(onix, civic)))
        advanceUntilIdle()

        assertEquals(2L, viewModel.uiState.value.vehicleId)
    }

    @Test
    fun `edicao preserva o veiculo gravado mesmo que o atual tenha mudado`() = runTest {
        val sessaoComVeiculo = existingSession.copy(vehicleId = 1)
        val onix = Vehicle(1, "Onix", VehicleFuel.FLEX, Instant.EPOCH, isCurrent = false)
        val civic = Vehicle(2, "Civic", VehicleFuel.FLEX, Instant.EPOCH, isCurrent = true)
        val viewModel = viewModel(
            FakeWorkSessionRepository(listOf(sessaoComVeiculo)),
            sessionId = 1,
            vehicles = FakeVehicleRepository(listOf(onix, civic)),
        )
        advanceUntilIdle()

        // O veiculo atual agora e o Civic, mas a sessao continua com o Onix,
        // que era o atual quando ela foi lancada.
        assertEquals(1L, viewModel.uiState.value.vehicleId)
    }

    @Test
    fun `valor e digitado em centavos`() = runTest {
        val viewModel = viewModel()

        viewModel.setRevenue("32050")

        // Teclar 32050 significa R$ 320,50 - o padrao que evita briga com
        // virgula e ponto no teclado numerico.
        assertEquals(Money.of(320, 50), viewModel.uiState.value.revenue)
    }

    @Test
    fun `campos numericos descartam caracteres nao numericos`() = runTest {
        val viewModel = viewModel()

        viewModel.onRidesChange("1a8")
        viewModel.onDistanceChange("2.1,0")

        assertEquals("18", viewModel.uiState.value.ridesInput)
        assertEquals("210", viewModel.uiState.value.distanceInput)
    }

    @Test
    fun `minutos acima de 59 sao recusados na digitacao`() = runTest {
        val viewModel = viewModel()

        viewModel.onMinutesChange("45")
        viewModel.onMinutesChange("60")

        // Mantem o ultimo valor valido em vez de aceitar e reclamar depois.
        assertEquals("45", viewModel.uiState.value.minutesInput)
    }

    @Test
    fun `horas e minutos viram uma duracao unica`() = runTest {
        val repository = FakeWorkSessionRepository()
        val viewModel = viewModel(repository)

        preencherValido(viewModel)
        viewModel.onHoursChange("8")
        viewModel.onMinutesChange("20")
        viewModel.onSave()
        advanceUntilIdle()

        assertEquals(WorkDuration(500), repository.current.single().onlineTime)
    }

    @Test
    fun `salvar formulario em branco acusa todos os campos obrigatorios`() = runTest {
        val viewModel = viewModel()

        viewModel.onSave()
        advanceUntilIdle()

        val errors = viewModel.uiState.value.errors
        assertEquals(WorkSessionValidationError.REQUIRED, errors[WorkSessionField.PLATFORM])
        assertEquals(WorkSessionValidationError.REQUIRED, errors[WorkSessionField.REVENUE])
        assertEquals(WorkSessionValidationError.REQUIRED, errors[WorkSessionField.RIDES])
        assertEquals(WorkSessionValidationError.REQUIRED, errors[WorkSessionField.ONLINE_TIME])
        assertEquals(WorkSessionValidationError.REQUIRED, errors[WorkSessionField.DISTANCE])
        assertNull(viewModel.uiState.value.savedSessionId)
    }

    @Test
    fun `campo em branco e diferente de zero`() = runTest {
        val viewModel = viewModel()

        assertNull(viewModel.uiState.value.revenue)
        assertNull(viewModel.uiState.value.onlineTime)

        viewModel.setRevenue("0")
        viewModel.onHoursChange("0")

        // Zero digitado e uma resposta; campo vazio nao e.
        assertEquals(Money.ZERO, viewModel.uiState.value.revenue)
        assertEquals(WorkDuration.ZERO, viewModel.uiState.value.onlineTime)
    }

    @Test
    fun `so os minutos preenchidos ja contam como tempo informado`() = runTest {
        val viewModel = viewModel()

        viewModel.onMinutesChange("40")

        assertEquals(WorkDuration(40), viewModel.uiState.value.onlineTime)
    }

    @Test
    fun `tudo preenchido em zero expoe erro de sessao vazia`() = runTest {
        val viewModel = viewModel()

        viewModel.onPlatformChange(Platform.UBER)
        viewModel.setRevenue("0")
        viewModel.onRidesChange("0")
        viewModel.onHoursChange("0")
        viewModel.onDistanceChange("0")
        viewModel.onSave()
        advanceUntilIdle()

        assertEquals(
            WorkSessionValidationError.EMPTY_SESSION,
            viewModel.uiState.value.errorFor(WorkSessionField.REVENUE),
        )
    }

    @Test
    fun `preencher qualquer numero limpa o erro de sessao vazia`() = runTest {
        val viewModel = viewModel()

        viewModel.onPlatformChange(Platform.UBER)
        viewModel.setRevenue("0")
        viewModel.onRidesChange("0")
        viewModel.onHoursChange("0")
        viewModel.onDistanceChange("0")
        viewModel.onSave()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorFor(WorkSessionField.REVENUE))

        // O erro e da sessao inteira, so exibido no faturamento: mexer nas
        // corridas tambem pode resolve-lo.
        viewModel.onRidesChange("5")

        assertNull(viewModel.uiState.value.errorFor(WorkSessionField.REVENUE))
    }

    @Test
    fun `erro proprio do faturamento nao some ao editar outro campo`() = runTest {
        val viewModel = viewModel()

        viewModel.onPlatformChange(Platform.UBER)
        viewModel.onRidesChange("10")
        viewModel.onSave()
        advanceUntilIdle()
        assertEquals(
            WorkSessionValidationError.REQUIRED,
            viewModel.uiState.value.errorFor(WorkSessionField.REVENUE),
        )

        viewModel.onDistanceChange("100")

        // REQUIRED e erro do proprio campo; so sai quando ele for preenchido.
        assertEquals(
            WorkSessionValidationError.REQUIRED,
            viewModel.uiState.value.errorFor(WorkSessionField.REVENUE),
        )
    }

    @Test
    fun `lancamento valido persiste e sinaliza navegacao`() = runTest {
        val repository = FakeWorkSessionRepository()
        val viewModel = viewModel(repository)

        preencherValido(viewModel)
        viewModel.onSave()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.savedSessionId)
        assertEquals(1, repository.current.size)
        assertEquals(Money.of(320, 50), repository.current.single().revenue)
        assertEquals(hoje, repository.current.single().date)
    }

    @Test
    fun `edicao atualiza a sessao existente em vez de criar outra`() = runTest {
        val repository = FakeWorkSessionRepository(listOf(existingSession))
        val viewModel = viewModel(repository, sessionId = 1)
        advanceUntilIdle()

        viewModel.setRevenue("40000")
        viewModel.onSave()
        advanceUntilIdle()

        assertEquals(1, repository.current.size)
        assertEquals(Money.of(400, 0), repository.current.single().revenue)
        assertEquals(1L, repository.current.single().id)
    }

    @Test
    fun `sessao excluida enquanto a tela abria vira lancamento novo`() = runTest {
        val viewModel = viewModel(FakeWorkSessionRepository(), sessionId = 99)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isEditing)
        assertEquals(hoje, state.date)
    }

    @Test
    fun `onNavigatedBack limpa o sinal para nao navegar duas vezes`() = runTest {
        val viewModel = viewModel()

        preencherValido(viewModel)
        viewModel.onSave()
        advanceUntilIdle()
        viewModel.onNavigatedBack()

        assertNull(viewModel.uiState.value.savedSessionId)
    }

    /**
     * Simula o motorista limpando o campo e digitando os centavos, um toque
     * por vez.
     *
     * O campo monetário raciocina por diferença entre o texto exibido e o
     * devolvido, então mandar o valor inteiro de uma vez não representa o que
     * um `TextField` realmente faz — e foi justamente confiar nisso que
     * escondeu o defeito do campo travado em R$ 0,00.
     */
    private fun EarningsFormViewModel.setRevenue(digits: String) {
        repeat(uiState.value.revenueDigits.length) {
            onRevenueChange(uiState.value.revenueText.dropLast(1))
        }
        digits.forEach { onRevenueChange(uiState.value.revenueText + it) }
    }

    private fun preencherValido(viewModel: EarningsFormViewModel) {
        viewModel.onPlatformChange(Platform.UBER)
        viewModel.setRevenue("32050")
        viewModel.onRidesChange("18")
        viewModel.onHoursChange("8")
        viewModel.onMinutesChange("20")
        viewModel.onDistanceChange("210")
    }
}
