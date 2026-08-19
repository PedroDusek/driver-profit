package com.driverpro.feature.maintenance

import com.driverpro.core.common.Money
import com.driverpro.domain.model.Expense
import com.driverpro.domain.model.ExpenseCategory
import com.driverpro.domain.model.ExpenseDetail
import com.driverpro.domain.model.MaintenanceCategory
import com.driverpro.domain.model.MaintenanceItem
import com.driverpro.domain.model.MaintenanceStatus
import com.driverpro.domain.model.Vehicle
import com.driverpro.domain.model.VehicleFuel
import com.driverpro.domain.usecase.MaintenanceValidationError
import com.driverpro.domain.usecase.ObserveMaintenanceUseCase
import com.driverpro.domain.usecase.ResetMaintenanceIntervalUseCase
import com.driverpro.domain.usecase.SaveMaintenanceIntervalUseCase
import com.driverpro.testing.FakeExpenseRepository
import com.driverpro.testing.FakeMaintenanceScheduleRepository
import com.driverpro.testing.FakeVehicleRepository
import com.driverpro.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class MaintenanceViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val vehicle = Vehicle(
        id = 1,
        name = "Onix branco",
        fuel = VehicleFuel.FLEX,
        createdAt = Instant.EPOCH,
    )

    private val vehicles = FakeVehicleRepository(listOf(vehicle))
    private val schedules = FakeMaintenanceScheduleRepository()

    private val expenses = FakeExpenseRepository(
        listOf(
            Expense(
                id = 1,
                vehicleId = 1,
                date = LocalDate.of(2026, 2, 1),
                category = ExpenseCategory.MAINTENANCE,
                amount = Money.of(300, 0),
                detail = ExpenseDetail.Maintenance(category = MaintenanceCategory.OIL),
                odometerKm = 100_000,
                createdAt = Instant.EPOCH,
            ),
            Expense(
                id = 2,
                vehicleId = 1,
                date = LocalDate.of(2026, 3, 1),
                category = ExpenseCategory.TOLL,
                amount = Money.of(9, 0),
                odometerKm = 105_000,
                createdAt = Instant.EPOCH,
            ),
        ),
    )

    private fun viewModel() = MaintenanceViewModel(
        observeMaintenance = ObserveMaintenanceUseCase(vehicles, expenses, schedules),
        saveInterval = SaveMaintenanceIntervalUseCase(schedules),
        resetInterval = ResetMaintenanceIntervalUseCase(schedules),
    )

    private fun TestScope.collecting(viewModel: MaintenanceViewModel): Job =
        launch { viewModel.uiState.collect { } }

    @Test
    fun `lista os itens do veiculo com o estado de cada um`() = runTest {
        val viewModel = viewModel()
        val job = collecting(viewModel)
        advanceUntilIdle()

        val state = viewModel.uiState.value as MaintenanceUiState.Content
        val oleo = state.vehicles.single().alerts.single { it.item == MaintenanceItem.OIL }

        assertEquals(MaintenanceItem.entries.size, state.vehicles.single().alerts.size)
        // 105.000 - 100.000 = 5.000 km desde a troca, num intervalo de 10.000.
        assertEquals(5_000L, oleo.traveledKm)
        assertEquals(MaintenanceStatus.OK, oleo.status)

        job.cancel()
    }

    @Test
    fun `sem veiculo cadastrado nao ha o que acompanhar`() = runTest {
        val viewModel = MaintenanceViewModel(
            observeMaintenance = ObserveMaintenanceUseCase(
                FakeVehicleRepository(),
                expenses,
                schedules,
            ),
            saveInterval = SaveMaintenanceIntervalUseCase(schedules),
            resetInterval = ResetMaintenanceIntervalUseCase(schedules),
        )
        val job = launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        assertEquals(MaintenanceUiState.Empty, viewModel.uiState.value)

        job.cancel()
    }

    @Test
    fun `alterar o intervalo grava a preferencia e reflete na lista`() = runTest {
        val viewModel = viewModel()
        val job = collecting(viewModel)
        advanceUntilIdle()

        val oleo = (viewModel.uiState.value as MaintenanceUiState.Content)
            .vehicles.single().alerts.single { it.item == MaintenanceItem.OIL }

        viewModel.onEditRequested(vehicleId = 1, alert = oleo)
        viewModel.onIntervalChange("5000")
        viewModel.onEditConfirmed()
        advanceUntilIdle()

        assertNull(viewModel.intervalEdit.value)
        assertEquals(5_000L, schedules.current.single().intervalKm)

        val depois = (viewModel.uiState.value as MaintenanceUiState.Content)
            .vehicles.single().alerts.single { it.item == MaintenanceItem.OIL }
        // 5.000 rodados num intervalo de 5.000 agora esta vencido.
        assertEquals(MaintenanceStatus.OVERDUE, depois.status)

        job.cancel()
    }

    @Test
    fun `intervalo fora de faixa e recusado e nada e gravado`() = runTest {
        val viewModel = viewModel()
        val job = collecting(viewModel)
        advanceUntilIdle()

        val oleo = (viewModel.uiState.value as MaintenanceUiState.Content)
            .vehicles.single().alerts.single { it.item == MaintenanceItem.OIL }

        viewModel.onEditRequested(vehicleId = 1, alert = oleo)
        viewModel.onIntervalChange("50")
        viewModel.onEditConfirmed()
        advanceUntilIdle()

        assertEquals(
            MaintenanceValidationError.INTERVAL_OUT_OF_RANGE,
            viewModel.intervalEdit.value?.error,
        )
        assertTrue(schedules.current.isEmpty())

        job.cancel()
    }

    @Test
    fun `o campo so aceita digito`() = runTest {
        val viewModel = viewModel()
        val job = collecting(viewModel)
        advanceUntilIdle()

        val oleo = (viewModel.uiState.value as MaintenanceUiState.Content)
            .vehicles.single().alerts.single { it.item == MaintenanceItem.OIL }

        viewModel.onEditRequested(vehicleId = 1, alert = oleo)
        viewModel.onIntervalChange("10.000 km")

        assertEquals("10000", viewModel.intervalEdit.value?.input)

        job.cancel()
    }

    @Test
    fun `voltar ao padrao apaga a preferencia`() = runTest {
        val viewModel = viewModel()
        val job = collecting(viewModel)
        advanceUntilIdle()

        val oleo = (viewModel.uiState.value as MaintenanceUiState.Content)
            .vehicles.single().alerts.single { it.item == MaintenanceItem.OIL }

        viewModel.onEditRequested(vehicleId = 1, alert = oleo)
        viewModel.onIntervalChange("5000")
        viewModel.onEditConfirmed()
        advanceUntilIdle()

        viewModel.onResetRequested(vehicleId = 1, item = MaintenanceItem.OIL)
        advanceUntilIdle()

        // Apagar, e nao gravar 10.000: assim o veiculo acompanha uma eventual
        // revisao do padrao, e continua sendo possivel distinguir escolha de
        // omissao.
        assertTrue(schedules.current.isEmpty())

        val depois = (viewModel.uiState.value as MaintenanceUiState.Content)
            .vehicles.single().alerts.single { it.item == MaintenanceItem.OIL }
        assertEquals(MaintenanceItem.OIL.defaultIntervalKm, depois.intervalKm)

        job.cancel()
    }

    @Test
    fun `desligar um item o mantem na lista sem pedir atencao`() = runTest {
        val viewModel = viewModel()
        val job = collecting(viewModel)
        advanceUntilIdle()

        val oleo = (viewModel.uiState.value as MaintenanceUiState.Content)
            .vehicles.single().alerts.single { it.item == MaintenanceItem.OIL }

        viewModel.onMonitoredChange(vehicleId = 1, alert = oleo, monitored = false)
        advanceUntilIdle()

        val depois = (viewModel.uiState.value as MaintenanceUiState.Content)
            .vehicles.single().alerts.single { it.item == MaintenanceItem.OIL }

        assertFalse(depois.monitored)
        assertFalse(depois.needsAttention)

        job.cancel()
    }
}
