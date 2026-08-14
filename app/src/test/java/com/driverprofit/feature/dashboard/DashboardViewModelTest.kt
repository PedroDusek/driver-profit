package com.driverprofit.feature.dashboard

import com.driverprofit.core.common.Money
import com.driverprofit.core.common.WorkDuration
import com.driverprofit.domain.model.DashboardPeriod
import com.driverprofit.domain.model.DateRange
import com.driverprofit.domain.model.Expense
import com.driverprofit.domain.model.ExpenseCategory
import com.driverprofit.domain.model.Platform
import com.driverprofit.domain.model.WorkSession
import com.driverprofit.domain.usecase.ObserveDashboardUseCase
import com.driverprofit.domain.usecase.ObserveExpensesBetweenUseCase
import com.driverprofit.domain.usecase.ObserveMaintenanceUseCase
import com.driverprofit.domain.usecase.ObservePersonalUsageInPeriodUseCase
import com.driverprofit.domain.usecase.ObserveWorkSessionsBetweenUseCase
import com.driverprofit.testing.FakeExpenseRepository
import com.driverprofit.testing.FakeMaintenanceScheduleRepository
import com.driverprofit.testing.FakePersonalUsageRepository
import com.driverprofit.testing.FakeVehicleRepository
import com.driverprofit.testing.FakeWorkSessionRepository
import com.driverprofit.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 8, 12)
    private val yesterday = LocalDate.of(2026, 8, 11)
    private val lastMonth = LocalDate.of(2026, 7, 20)

    // Relogio fixo: sem ele, "ontem" passaria hoje e falharia amanha.
    private val clock = Clock.fixed(Instant.parse("2026-08-12T09:00:00Z"), ZoneOffset.UTC)

    private fun session(
        id: Long,
        date: LocalDate,
        revenue: Money,
        km: Long = 100,
        minutes: Long = 300,
        rides: Int = 10,
    ) = WorkSession(
        id = id,
        date = date,
        platform = Platform.UBER,
        rides = rides,
        revenue = revenue,
        onlineTime = WorkDuration(minutes),
        distanceKm = km,
        createdAt = Instant.EPOCH,
    )

    private fun expense(
        id: Long,
        date: LocalDate,
        amount: Money,
        category: ExpenseCategory = ExpenseCategory.FUEL,
    ) = Expense(
        id = id,
        date = date,
        category = category,
        amount = amount,
        createdAt = Instant.EPOCH,
    )

    private val sessions = FakeWorkSessionRepository(
        listOf(
            session(1, today, Money.of(300, 0)),
            session(2, yesterday, Money.of(200, 0)),
            session(3, lastMonth, Money.of(500, 0)),
        ),
    )

    private val expenses = FakeExpenseRepository(
        listOf(
            expense(1, today, Money.of(120, 0)),
            expense(2, yesterday, Money.of(80, 0)),
            expense(3, lastMonth, Money.of(150, 0)),
        ),
    )

    private val personalUsage = FakePersonalUsageRepository()

    // Sem veiculo cadastrado nao ha manutencao a acompanhar, que e o estado
    // certo para os testes de rentabilidade: o aviso nao interfere neles.
    private val vehicles = FakeVehicleRepository()

    private val schedules = FakeMaintenanceScheduleRepository()

    private fun viewModel() = DashboardViewModel(
        observeDashboard = ObserveDashboardUseCase(
            observeWorkSessionsBetween = ObserveWorkSessionsBetweenUseCase(sessions),
            observeExpensesBetween = ObserveExpensesBetweenUseCase(expenses),
            observePersonalUsageInPeriod = ObservePersonalUsageInPeriodUseCase(personalUsage),
        ),
        observeMaintenance = ObserveMaintenanceUseCase(vehicles, expenses, schedules),
        clock = clock,
    )

    @Test
    fun `estado inicial e Loading no periodo de hoje`() {
        val state = viewModel().uiState.value

        assertTrue(state is DashboardUiState.Loading)
        assertEquals(DashboardPeriod.Today, state.period)
    }

    @Test
    fun `hoje traz so os lancamentos de hoje`() = runTest {
        val viewModel = viewModel()

        val job = launchCollector(viewModel)
        advanceUntilIdle()

        val state = viewModel.uiState.value as DashboardUiState.Content
        assertEquals(DateRange(today, today), state.range)
        assertEquals(Money.of(300, 0), state.metrics.totalRevenue)
        assertEquals(Money.of(120, 0), state.metrics.totalExpenses)
        assertEquals(Money.of(180, 0), state.metrics.netProfit)
        job.cancel()
    }

    @Test
    fun `trocar para ontem troca os numeros`() = runTest {
        val viewModel = viewModel()

        val job = launchCollector(viewModel)
        viewModel.onPeriodChange(DashboardPeriod.Yesterday)
        advanceUntilIdle()

        val state = viewModel.uiState.value as DashboardUiState.Content
        assertEquals(DateRange(yesterday, yesterday), state.range)
        assertEquals(Money.of(200, 0), state.metrics.totalRevenue)
        assertEquals(Money.of(120, 0), state.metrics.netProfit)
        job.cancel()
    }

    @Test
    fun `este mes agrega os lancamentos do mes e exclui o mes anterior`() = runTest {
        val viewModel = viewModel()

        val job = launchCollector(viewModel)
        viewModel.onPeriodChange(DashboardPeriod.ThisMonth)
        advanceUntilIdle()

        val state = viewModel.uiState.value as DashboardUiState.Content
        assertEquals(DateRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)), state.range)
        assertEquals(Money.of(500, 0), state.metrics.totalRevenue)
        assertEquals(Money.of(200, 0), state.metrics.totalExpenses)
        job.cancel()
    }

    @Test
    fun `mes anterior traz apenas julho`() = runTest {
        val viewModel = viewModel()

        val job = launchCollector(viewModel)
        viewModel.onPeriodChange(DashboardPeriod.LastMonth)
        advanceUntilIdle()

        val state = viewModel.uiState.value as DashboardUiState.Content
        assertEquals(DateRange(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)), state.range)
        assertEquals(Money.of(500, 0), state.metrics.totalRevenue)
        job.cancel()
    }

    @Test
    fun `periodo personalizado consulta o intervalo escolhido`() = runTest {
        val viewModel = viewModel()

        val job = launchCollector(viewModel)
        viewModel.onCustomRangeChange(yesterday, today)
        advanceUntilIdle()

        val state = viewModel.uiState.value as DashboardUiState.Content
        assertEquals(DashboardPeriod.Custom(DateRange(yesterday, today)), state.period)
        assertEquals(Money.of(500, 0), state.metrics.totalRevenue)
        job.cancel()
    }

    @Test
    fun `periodo personalizado invertido e reordenado em vez de recusado`() = runTest {
        val viewModel = viewModel()

        val job = launchCollector(viewModel)
        // Tocar na data final antes da inicial e engano de toque, nao pedido
        // invalido - e DateRange estouraria com o intervalo ao contrario.
        viewModel.onCustomRangeChange(today, yesterday)
        advanceUntilIdle()

        val state = viewModel.uiState.value as DashboardUiState.Content
        assertEquals(DateRange(yesterday, today), state.range)
        job.cancel()
    }

    @Test
    fun `periodo sem lancamento vem vazio em vez de estourar`() = runTest {
        val viewModel = viewModel()

        val job = launchCollector(viewModel)
        val silence = LocalDate.of(2026, 6, 1)
        viewModel.onCustomRangeChange(silence, silence.plusDays(9))
        advanceUntilIdle()

        val state = viewModel.uiState.value as DashboardUiState.Content
        assertTrue(state.metrics.isEmpty)
        job.cancel()
    }

    @Test
    fun `hoje e o dia do relogio injetado`() {
        assertEquals(today, viewModel().today())
    }

    private fun TestScope.launchCollector(viewModel: DashboardViewModel): Job =
        launch { viewModel.uiState.collect {} }
}
