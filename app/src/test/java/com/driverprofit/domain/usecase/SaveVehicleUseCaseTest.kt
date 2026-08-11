package com.driverprofit.domain.usecase

import com.driverprofit.domain.model.CombustionFuel
import com.driverprofit.domain.model.Vehicle
import com.driverprofit.domain.model.VehicleDraft
import com.driverprofit.domain.model.VehicleField
import com.driverprofit.domain.model.VehiclePowertrain
import com.driverprofit.testing.FakeVehicleRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class SaveVehicleUseCaseTest {

    private val clock = Clock.fixed(Instant.parse("2026-08-11T12:00:00Z"), ZoneId.of("UTC"))
    private val repository = FakeVehicleRepository()
    private val saveVehicle = SaveVehicleUseCase(repository, VehicleValidator(clock))

    private val validDraft = VehicleDraft(
        brand = "Chevrolet",
        model = "Onix",
        year = 2020,
        initialOdometerKm = 50_000,
        powertrain = VehiclePowertrain.COMBUSTION,
        combustionFuel = CombustionFuel.FLEX,
    )

    @Test
    fun `insere veiculo valido e devolve o id`() = runTest {
        val result = saveVehicle(validDraft)

        assertTrue(result is SaveVehicleResult.Success)
        assertEquals(1, repository.current.size)
        assertEquals("Onix", repository.current.single().model)
    }

    @Test
    fun `rascunho invalido nao grava nada`() = runTest {
        val result = saveVehicle(validDraft.copy(brand = ""))

        assertTrue(result is SaveVehicleResult.Invalid)
        assertEquals(
            listOf(VehicleField.BRAND),
            (result as SaveVehicleResult.Invalid).errors.map { it.field },
        )
        assertTrue(repository.current.isEmpty())
    }

    @Test
    fun `edicao atualiza em vez de inserir`() = runTest {
        val id = (saveVehicle(validDraft) as SaveVehicleResult.Success).id

        val result = saveVehicle(validDraft.copy(id = id, model = "Onix Plus"))

        assertEquals(SaveVehicleResult.Success(id), result)
        assertEquals(1, repository.current.size)
        assertEquals("Onix Plus", repository.current.single().model)
    }

    @Test
    fun `edicao preserva a data de cadastro original`() = runTest {
        val original = Vehicle(
            id = 7,
            brand = "Fiat",
            model = "Argo",
            year = 2019,
            initialOdometerKm = 80_000,
            powertrain = VehiclePowertrain.COMBUSTION,
            combustionFuel = CombustionFuel.FLEX,
            chargingCapability = null,
            createdAt = Instant.parse("2024-01-15T08:00:00Z"),
        )
        val repositoryComVeiculo = FakeVehicleRepository(listOf(original))
        val useCase = SaveVehicleUseCase(repositoryComVeiculo, VehicleValidator(clock))

        useCase(
            VehicleDraft(
                id = 7,
                brand = "Fiat",
                model = "Argo",
                year = 2019,
                initialOdometerKm = 95_000,
                powertrain = VehiclePowertrain.COMBUSTION,
                combustionFuel = CombustionFuel.FLEX,
            ),
        )

        // Trocar a quilometragem nao pode reescrever a data de entrada no app.
        val atualizado = repositoryComVeiculo.current.single()
        assertEquals(Instant.parse("2024-01-15T08:00:00Z"), atualizado.createdAt)
        assertEquals(95_000L, atualizado.initialOdometerKm)
    }

    @Test
    fun `edicao de veiculo ja excluido insere um novo em vez de perder o dado`() = runTest {
        val result = saveVehicle(validDraft.copy(id = 999))

        assertTrue(result is SaveVehicleResult.Success)
        assertEquals(1, repository.current.size)
    }

    @Test
    fun `eletrico com combustivel e rejeitado antes de tocar o repositorio`() = runTest {
        val result = saveVehicle(
            validDraft.copy(
                powertrain = VehiclePowertrain.ELECTRIC,
                combustionFuel = CombustionFuel.FLEX,
            ),
        )

        assertTrue(result is SaveVehicleResult.Invalid)
        assertTrue(repository.current.isEmpty())
    }
}
