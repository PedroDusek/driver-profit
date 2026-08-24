package com.driverpro.earnings.presentation

import com.driverpro.core.domain.Money
import com.driverpro.core.domain.WorkDuration
import com.driverpro.earnings.domain.Platform
import com.driverpro.earnings.domain.WorkSession
import com.driverpro.earnings.domain.DeleteWorkSessionUseCase
import com.driverpro.earnings.domain.ObserveWorkSessionsUseCase
import com.driverpro.earnings.presentation.list.EarningsListUiState
import com.driverpro.earnings.presentation.list.EarningsListViewModel
import com.driverpro.earnings.domain.FakeWorkSessionRepository
import com.driverpro.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class EarningsListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun session(
        id: Long,
        date: LocalDate,
        revenue: Money = Money.of(100, 0),
        onlineTime: WorkDuration = WorkDuration.of(5, 0),
        distanceKm: Long = 100,
        rides: Int = 10,
    ) = WorkSession(
        id = id,
        date = date,
        platform = Platform.UBER,
        rides = rides,
        revenue = revenue,
        onlineTime = onlineTime,
        distanceKm = distanceKm,
        createdAt = Instant.EPOCH,
    )

    private fun viewModel(repository: FakeWorkSessionRepository) = EarningsListViewModel(
        observeWorkSessions = ObserveWorkSessionsUseCase(repository),
        deleteWorkSession = DeleteWorkSessionUseCase(repository),
    )

    @Test
    fun `estado inicial e Loading`() = runTest {
        assertEquals(
            EarningsListUiState.Loading,
            viewModel(FakeWorkSessionRepository()).uiState.value,
        )
    }

    @Test
    fun `historico vazio produz estado Empty`() = runTest {
        val viewModel = viewModel(FakeWorkSessionRepository())

        val job = launchCollector(viewModel)
        advanceUntilIdle()

        assertEquals(EarningsListUiState.Empty, viewModel.uiState.value)
        job.cancel()
    }

    @Test
    fun `lista vem da data mais recente para a mais antiga`() = runTest {
        val repository = FakeWorkSessionRepository(
            listOf(
                session(1, LocalDate.of(2026, 8, 1)),
                session(2, LocalDate.of(2026, 8, 11)),
                session(3, LocalDate.of(2026, 8, 5)),
            ),
        )
        val viewModel = viewModel(repository)

        val job = launchCollector(viewModel)
        advanceUntilIdle()

        val state = viewModel.uiState.value as EarningsListUiState.Content
        assertEquals(listOf(2L, 3L, 1L), state.sessions.map { it.id })
        job.cancel()
    }

    @Test
    fun `resumo soma faturamento, corridas, tempo e distancia`() = runTest {
        val repository = FakeWorkSessionRepository(
            listOf(
                session(
                    1,
                    LocalDate.of(2026, 8, 10),
                    revenue = Money.of(320, 50),
                    onlineTime = WorkDuration.of(8, 20),
                    distanceKm = 210,
                    rides = 18,
                ),
                session(
                    2,
                    LocalDate.of(2026, 8, 11),
                    revenue = Money.of(279, 50),
                    onlineTime = WorkDuration.of(7, 40),
                    distanceKm = 190,
                    rides = 15,
                ),
            ),
        )
        val viewModel = viewModel(repository)

        val job = launchCollector(viewModel)
        advanceUntilIdle()

        val summary = (viewModel.uiState.value as EarningsListUiState.Content).summary
        assertEquals(Money.of(600, 0), summary.totalRevenue)
        assertEquals(33, summary.totalRides)
        assertEquals(WorkDuration.of(16, 0), summary.totalOnlineTime)
        assertEquals(400L, summary.totalDistanceKm)
        // 60000 centavos / 16h = 3750 centavos
        assertEquals(Money.of(37, 50), summary.revenuePerHour)
        assertEquals(Money.of(1, 50), summary.revenuePerKm)
        job.cancel()
    }

    @Test
    fun `resumo sem tempo nem distancia nao inventa indicadores`() = runTest {
        val repository = FakeWorkSessionRepository(
            listOf(
                session(
                    1,
                    LocalDate.of(2026, 8, 11),
                    revenue = Money.of(100, 0),
                    onlineTime = WorkDuration.ZERO,
                    distanceKm = 0,
                ),
            ),
        )
        val viewModel = viewModel(repository)

        val job = launchCollector(viewModel)
        advanceUntilIdle()

        val summary = (viewModel.uiState.value as EarningsListUiState.Content).summary
        assertNull(summary.revenuePerHour)
        assertNull(summary.revenuePerKm)
        job.cancel()
    }

    @Test
    fun `pedir exclusao apenas marca a sessao, sem excluir`() = runTest {
        val target = session(1, LocalDate.of(2026, 8, 11))
        val repository = FakeWorkSessionRepository(listOf(target))
        val viewModel = viewModel(repository)

        viewModel.onDeleteRequested(target)
        advanceUntilIdle()

        assertEquals(target, viewModel.sessionPendingDeletion.value)
        assertEquals(1, repository.current.size)
    }

    @Test
    fun `cancelar exclusao mantem a sessao`() = runTest {
        val target = session(1, LocalDate.of(2026, 8, 11))
        val repository = FakeWorkSessionRepository(listOf(target))
        val viewModel = viewModel(repository)

        viewModel.onDeleteRequested(target)
        viewModel.onDeleteDismissed()
        advanceUntilIdle()

        assertNull(viewModel.sessionPendingDeletion.value)
        assertEquals(1, repository.current.size)
    }

    @Test
    fun `confirmar exclusao remove a sessao`() = runTest {
        val target = session(1, LocalDate.of(2026, 8, 11))
        val repository = FakeWorkSessionRepository(listOf(target))
        val viewModel = viewModel(repository)

        viewModel.onDeleteRequested(target)
        viewModel.onDeleteConfirmed()
        advanceUntilIdle()

        assertNull(viewModel.sessionPendingDeletion.value)
        assertTrue(repository.current.isEmpty())
    }

    /**
     * `stateIn(WhileSubscribed)` só consulta a fonte enquanto houver coletor.
     * Sem isso, o estado ficaria em `Loading` para sempre no teste.
     */
    private fun TestScope.launchCollector(viewModel: EarningsListViewModel): Job =
        launch { viewModel.uiState.collect {} }
}
