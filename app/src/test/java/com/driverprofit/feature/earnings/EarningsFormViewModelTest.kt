package com.driverprofit.feature.earnings

import androidx.lifecycle.SavedStateHandle
import com.driverprofit.core.common.Money
import com.driverprofit.core.common.WorkDuration
import com.driverprofit.core.navigation.DriverProfitDestination
import com.driverprofit.domain.model.Platform
import com.driverprofit.domain.model.WorkSession
import com.driverprofit.domain.model.WorkSessionField
import com.driverprofit.domain.model.WorkSessionValidationError
import com.driverprofit.domain.usecase.GetWorkSessionUseCase
import com.driverprofit.domain.usecase.SaveWorkSessionUseCase
import com.driverprofit.domain.usecase.WorkSessionValidator
import com.driverprofit.feature.earnings.form.EarningsFormViewModel
import com.driverprofit.testing.FakeWorkSessionRepository
import com.driverprofit.testing.MainDispatcherRule
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
    ): EarningsFormViewModel {
        val handle = SavedStateHandle(
            sessionId?.let { mapOf(DriverProfitDestination.ARG_SESSION_ID to it) } ?: emptyMap(),
        )
        return EarningsFormViewModel(
            savedStateHandle = handle,
            getWorkSession = GetWorkSessionUseCase(repository),
            saveWorkSession = SaveWorkSessionUseCase(repository, WorkSessionValidator(clock)),
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
    fun `valor e digitado em centavos`() = runTest {
        val viewModel = viewModel()

        viewModel.onRevenueChange("32050")

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
    fun `salvar sem plataforma expoe o erro`() = runTest {
        val viewModel = viewModel()

        viewModel.onRevenueChange("10000")
        viewModel.onSave()
        advanceUntilIdle()

        assertEquals(
            WorkSessionValidationError.REQUIRED,
            viewModel.uiState.value.errorFor(WorkSessionField.PLATFORM),
        )
        assertNull(viewModel.uiState.value.savedSessionId)
    }

    @Test
    fun `salvar sessao sem nenhum numero expoe erro de sessao vazia`() = runTest {
        val viewModel = viewModel()

        viewModel.onPlatformChange(Platform.UBER)
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
        viewModel.onSave()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorFor(WorkSessionField.REVENUE))

        // O erro e reportado no faturamento, mas as corridas tambem o resolvem.
        viewModel.onRidesChange("5")

        assertNull(viewModel.uiState.value.errorFor(WorkSessionField.REVENUE))
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

        viewModel.onRevenueChange("40000")
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

    private fun preencherValido(viewModel: EarningsFormViewModel) {
        viewModel.onPlatformChange(Platform.UBER)
        viewModel.onRevenueChange("32050")
        viewModel.onRidesChange("18")
        viewModel.onDistanceChange("210")
    }
}
