package com.driverpro.personal.presentation

import com.driverpro.core.domain.DateRange
import com.driverpro.core.domain.Money
import com.driverpro.core.domain.WorkDuration
import com.driverpro.earnings.domain.FakeWorkSessionRepository
import com.driverpro.earnings.domain.Platform
import com.driverpro.earnings.domain.WorkSession
import com.driverpro.earnings.domain.WorkSessionRepository
import com.driverpro.expenses.domain.Expense
import com.driverpro.expenses.domain.ExpenseCategory
import com.driverpro.expenses.domain.ExpenseRepository
import com.driverpro.expenses.domain.FakeExpenseRepository
import com.driverpro.personal.domain.DeletePersonalUsageUseCase
import com.driverpro.personal.domain.DismissReconciliationUseCase
import com.driverpro.personal.domain.FakePersonalUsageRepository
import com.driverpro.personal.domain.FakeReconciliationDismissalRepository
import com.driverpro.personal.domain.ObserveOdometerReconciliationUseCase
import com.driverpro.personal.domain.ObservePersonalUsageUseCase
import com.driverpro.personal.domain.PersonalUsage
import com.driverpro.personal.domain.PersonalUsageRepository
import com.driverpro.personal.domain.PersonalUsageSource
import com.driverpro.personal.domain.SaveReconciledPersonalUsageUseCase
import com.driverpro.personal.presentation.list.PersonalUsageListViewModel
import com.driverpro.testing.MainDispatcherRule
import com.driverpro.vehicle.domain.FakeVehicleRepository
import com.driverpro.vehicle.domain.Vehicle
import com.driverpro.vehicle.domain.VehicleFuel
import com.driverpro.vehicle.domain.VehicleRepository
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/**
 * Reproduz o relato do Pedro: testando no aparelho, adicionar e remover
 * lançamentos de km pessoal/profissional fez o aviso de conciliação sumir e
 * não voltar.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PersonalUsageListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private var nextId = 1L

    private fun vehicle() = Vehicle(
        id = 1,
        name = "Onix",
        fuel = VehicleFuel.FLEX,
        createdAt = Instant.EPOCH,
        isCurrent = true,
    )

    private fun reading(odometerKm: Long, date: LocalDate) = Expense(
        id = nextId++,
        vehicleId = 1,
        date = date,
        category = ExpenseCategory.FUEL,
        amount = Money.of(200, 0),
        odometerKm = odometerKm,
        createdAt = Instant.EPOCH,
    )

    private fun session(date: LocalDate, km: Long) = WorkSession(
        id = nextId++,
        date = date,
        platform = Platform.UBER,
        rides = 10,
        revenue = Money.of(300, 0),
        onlineTime = WorkDuration.of(8, 0),
        distanceKm = km,
        createdAt = Instant.EPOCH,
    )

    private fun personal(start: LocalDate, end: LocalDate, km: Long) = PersonalUsage(
        id = nextId++,
        vehicleId = 1,
        range = DateRange(start, end),
        distanceKm = km,
        source = PersonalUsageSource.DECLARED,
        createdAt = Instant.EPOCH,
    )

    private fun viewModel(
        vehicleRepository: VehicleRepository = FakeVehicleRepository(listOf(vehicle())),
        expenseRepository: ExpenseRepository,
        workSessionRepository: WorkSessionRepository = FakeWorkSessionRepository(),
        personalUsageRepository: PersonalUsageRepository = FakePersonalUsageRepository(),
        dismissalRepository: FakeReconciliationDismissalRepository = FakeReconciliationDismissalRepository(),
    ) = PersonalUsageListViewModel(
        observePersonalUsage = ObservePersonalUsageUseCase(personalUsageRepository),
        observeReconciliation = ObserveOdometerReconciliationUseCase(
            vehicleRepository = vehicleRepository,
            expenseRepository = expenseRepository,
            workSessionRepository = workSessionRepository,
            personalUsageRepository = personalUsageRepository,
            dismissalRepository = dismissalRepository,
        ),
        deletePersonalUsage = DeletePersonalUsageUseCase(personalUsageRepository),
        saveReconciled = SaveReconciledPersonalUsageUseCase(personalUsageRepository),
        dismissReconciliation = DismissReconciliationUseCase(dismissalRepository),
    )

    private fun TestScope.launchCollector(viewModel: PersonalUsageListViewModel): Job =
        launch { viewModel.pendingReconciliation.collect {} }

    @Test
    fun `fechar o dialogo sem responder nao apaga a divergencia para sempre`() = runTest {
        // 1.000 km de painel, so 600 km de jornada: sobra 400.
        val expenseRepository = FakeExpenseRepository(
            listOf(reading(100_000, DIA_1), reading(101_000, DIA_4)),
        )
        val workSessionRepository = FakeWorkSessionRepository(listOf(session(DIA_2, 600)))
        val viewModel = viewModel(
            expenseRepository = expenseRepository,
            workSessionRepository = workSessionRepository,
        )
        val job = launchCollector(viewModel)
        advanceUntilIdle()

        assertEquals(400L, viewModel.pendingReconciliation.value?.unexplainedKilometers)

        // Motorista toca fora do dialogo (ou aperta voltar) sem escolher nada.
        viewModel.onReconcileDismissed()
        advanceUntilIdle()
        assertNull(viewModel.pendingReconciliation.value)
        job.cancel()
    }

    @Test
    fun `remover a jornada que soma na sobra deveria trazer o aviso de volta maior`() = runTest {
        val expenseRepository = FakeExpenseRepository(
            listOf(reading(100_000, DIA_1), reading(101_000, DIA_4)),
        )
        val workSessionRepository = FakeWorkSessionRepository(listOf(session(DIA_2, 600)))
        val viewModel = viewModel(
            expenseRepository = expenseRepository,
            workSessionRepository = workSessionRepository,
        )
        val job = launchCollector(viewModel)
        advanceUntilIdle()

        assertEquals(400L, viewModel.pendingReconciliation.value?.unexplainedKilometers)

        // Fecha o dialogo sem responder.
        viewModel.onReconcileDismissed()
        advanceUntilIdle()
        assertNull(viewModel.pendingReconciliation.value)

        // Motorista percebe que lançou a jornada errada e apaga — agora a
        // sobra real é 1.000 km, bem maior que os 400 originais. O aviso
        // precisa voltar, porque é uma divergência estritamente pior.
        workSessionRepository.deleteSession(workSessionRepository.current.single().id)
        advanceUntilIdle()

        assertNotNull(
            "aviso deveria reaparecer: a sobra cresceu de 400 para 1000 km, " +
                "e o motorista nunca decidiu nada sobre ela",
            viewModel.pendingReconciliation.value,
        )
        assertEquals(1_000L, viewModel.pendingReconciliation.value?.unexplainedKilometers)
        job.cancel()
    }

    @Test
    fun `adicionar uso pessoal que resolve a sobra some com o aviso`() = runTest {
        val expenseRepository = FakeExpenseRepository(
            listOf(reading(100_000, DIA_1), reading(101_000, DIA_4)),
        )
        val workSessionRepository = FakeWorkSessionRepository(listOf(session(DIA_2, 600)))
        val personalUsageRepository = FakePersonalUsageRepository()
        val viewModel = viewModel(
            expenseRepository = expenseRepository,
            workSessionRepository = workSessionRepository,
            personalUsageRepository = personalUsageRepository,
        )
        val job = launchCollector(viewModel)
        advanceUntilIdle()

        assertEquals(400L, viewModel.pendingReconciliation.value?.unexplainedKilometers)

        personalUsageRepository.addUsage(personal(DIA_3, DIA_3, 400))
        advanceUntilIdle()

        assertNull(viewModel.pendingReconciliation.value)
        job.cancel()
    }

    @Test
    fun `remover o uso pessoal que resolvia a sobra traz o aviso de volta`() = runTest {
        val expenseRepository = FakeExpenseRepository(
            listOf(reading(100_000, DIA_1), reading(101_000, DIA_4)),
        )
        val workSessionRepository = FakeWorkSessionRepository(listOf(session(DIA_2, 600)))
        val personalUsageRepository = FakePersonalUsageRepository(
            listOf(personal(DIA_3, DIA_3, 400)),
        )
        val viewModel = viewModel(
            expenseRepository = expenseRepository,
            workSessionRepository = workSessionRepository,
            personalUsageRepository = personalUsageRepository,
        )
        val job = launchCollector(viewModel)
        advanceUntilIdle()

        // Resolvida: a declaração de 400 km ja cobre a sobra.
        assertNull(viewModel.pendingReconciliation.value)

        // Motorista percebe que lançou o uso pessoal errado (era outro
        // veículo, por exemplo) e apaga.
        personalUsageRepository.deleteUsage(personalUsageRepository.current.single().id)
        advanceUntilIdle()

        assertNotNull(
            "aviso deveria reaparecer: sem o uso pessoal declarado a sobra " +
                "volta a 400 km sem explicação",
            viewModel.pendingReconciliation.value,
        )
        assertEquals(400L, viewModel.pendingReconciliation.value?.unexplainedKilometers)
        job.cancel()
    }

    private companion object {
        val DIA_1: LocalDate = LocalDate.of(2026, 8, 1)
        val DIA_2: LocalDate = LocalDate.of(2026, 8, 2)
        val DIA_3: LocalDate = LocalDate.of(2026, 8, 3)
        val DIA_4: LocalDate = LocalDate.of(2026, 8, 4)
    }
}
