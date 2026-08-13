package com.driverprofit.feature.vehicle

import com.driverprofit.domain.model.Vehicle
import com.driverprofit.domain.model.VehicleFuel
import com.driverprofit.domain.usecase.DeleteVehicleUseCase
import com.driverprofit.domain.usecase.ObserveVehicleOdometersUseCase
import com.driverprofit.domain.usecase.ObserveVehiclesUseCase
import com.driverprofit.feature.vehicle.list.VehicleListUiState
import com.driverprofit.feature.vehicle.list.VehicleListViewModel
import com.driverprofit.testing.FakeExpenseRepository
import com.driverprofit.testing.FakeVehicleRepository
import com.driverprofit.testing.MainDispatcherRule
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

@OptIn(ExperimentalCoroutinesApi::class)
class VehicleListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun vehicle(id: Long, name: String, createdAt: Instant) = Vehicle(
        id = id,
        name = name,
        fuel = VehicleFuel.FLEX,
        createdAt = createdAt,
    )

    private fun viewModel(
        repository: FakeVehicleRepository,
        expenses: FakeExpenseRepository = FakeExpenseRepository(),
    ) = VehicleListViewModel(
        observeVehicles = ObserveVehiclesUseCase(repository),
        observeVehicleOdometers = ObserveVehicleOdometersUseCase(expenses),
        deleteVehicle = DeleteVehicleUseCase(repository),
    )

    @Test
    fun `estado inicial e Loading`() = runTest {
        val viewModel = viewModel(FakeVehicleRepository())

        assertEquals(VehicleListUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `banco vazio produz estado Empty`() = runTest {
        val viewModel = viewModel(FakeVehicleRepository())

        val collectJob = launchCollector(viewModel)
        advanceUntilIdle()

        assertEquals(VehicleListUiState.Empty, viewModel.uiState.value)
        collectJob.cancel()
    }

    @Test
    fun `veiculos cadastrados produzem estado Content`() = runTest {
        val repository = FakeVehicleRepository(
            listOf(vehicle(1, "Onix", Instant.ofEpochMilli(1_000))),
        )
        val viewModel = viewModel(repository)

        val collectJob = launchCollector(viewModel)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is VehicleListUiState.Content)
        assertEquals(listOf("Onix"), (state as VehicleListUiState.Content).vehicles.map { it.name })
        collectJob.cancel()
    }

    @Test
    fun `lista vem do mais recente para o mais antigo`() = runTest {
        val repository = FakeVehicleRepository(
            listOf(
                vehicle(1, "Antigo", Instant.ofEpochMilli(1_000)),
                vehicle(2, "Recente", Instant.ofEpochMilli(9_000)),
            ),
        )
        val viewModel = viewModel(repository)

        val collectJob = launchCollector(viewModel)
        advanceUntilIdle()

        val state = viewModel.uiState.value as VehicleListUiState.Content
        assertEquals(listOf("Recente", "Antigo"), state.vehicles.map { it.name })
        collectJob.cancel()
    }

    @Test
    fun `pedir exclusao apenas marca o veiculo, sem excluir`() = runTest {
        val target = vehicle(1, "Onix", Instant.ofEpochMilli(1_000))
        val repository = FakeVehicleRepository(listOf(target))
        val viewModel = viewModel(repository)

        viewModel.onDeleteRequested(target)
        advanceUntilIdle()

        // A confirmacao ainda nao veio: o dado precisa continuar la.
        assertEquals(target, viewModel.vehiclePendingDeletion.value)
        assertEquals(1, repository.current.size)
    }

    @Test
    fun `cancelar exclusao limpa a marcacao e mantem o veiculo`() = runTest {
        val target = vehicle(1, "Onix", Instant.ofEpochMilli(1_000))
        val repository = FakeVehicleRepository(listOf(target))
        val viewModel = viewModel(repository)

        viewModel.onDeleteRequested(target)
        viewModel.onDeleteDismissed()
        advanceUntilIdle()

        assertNull(viewModel.vehiclePendingDeletion.value)
        assertEquals(1, repository.current.size)
    }

    @Test
    fun `confirmar exclusao remove o veiculo`() = runTest {
        val target = vehicle(1, "Onix", Instant.ofEpochMilli(1_000))
        val repository = FakeVehicleRepository(listOf(target))
        val viewModel = viewModel(repository)

        viewModel.onDeleteRequested(target)
        viewModel.onDeleteConfirmed()
        advanceUntilIdle()

        assertNull(viewModel.vehiclePendingDeletion.value)
        assertTrue(repository.current.isEmpty())
    }

    @Test
    fun `confirmar sem nada marcado nao faz nada`() = runTest {
        val repository = FakeVehicleRepository(
            listOf(vehicle(1, "Onix", Instant.ofEpochMilli(1_000))),
        )
        val viewModel = viewModel(repository)

        viewModel.onDeleteConfirmed()
        advanceUntilIdle()

        assertEquals(1, repository.current.size)
    }

    /**
     * `stateIn(WhileSubscribed)` só consulta a fonte enquanto houver coletor.
     * Sem isso, o estado ficaria em `Loading` para sempre no teste.
     */
    private fun TestScope.launchCollector(viewModel: VehicleListViewModel): Job =
        launch { viewModel.uiState.collect {} }
}
