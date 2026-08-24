package com.driverpro.feature.personal.list

import com.driverpro.core.common.Money
import com.driverpro.core.common.WorkDuration
import com.driverpro.domain.model.DateRange
import com.driverpro.domain.model.Expense
import com.driverpro.domain.model.ExpenseCategory
import com.driverpro.domain.model.PersonalUsage
import com.driverpro.domain.model.PersonalUsageSource
import com.driverpro.domain.model.Platform
import com.driverpro.domain.model.Vehicle
import com.driverpro.domain.model.VehicleFuel
import com.driverpro.domain.model.WorkSession
import com.driverpro.domain.repository.ExpenseRepository
import com.driverpro.domain.repository.PersonalUsageRepository
import com.driverpro.domain.repository.VehicleRepository
import com.driverpro.domain.repository.WorkSessionRepository
import com.driverpro.domain.usecase.DeletePersonalUsageUseCase
import com.driverpro.domain.usecase.DismissReconciliationUseCase
import com.driverpro.domain.usecase.ObserveOdometerReconciliationUseCase
import com.driverpro.domain.usecase.ObservePersonalUsageUseCase
import com.driverpro.domain.usecase.SaveReconciledPersonalUsageUseCase
import com.driverpro.testing.FakeExpenseRepository
import com.driverpro.testing.FakePersonalUsageRepository
import com.driverpro.testing.FakeReconciliationDismissalRepository
import com.driverpro.testing.FakeVehicleRepository
import com.driverpro.testing.FakeWorkSessionRepository
import com.driverpro.testing.MainDispatcherRule
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
import java.time.Instant
import java.time.LocalDate

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
