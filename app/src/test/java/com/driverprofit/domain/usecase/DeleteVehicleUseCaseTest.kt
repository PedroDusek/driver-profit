package com.driverprofit.domain.usecase

import com.driverprofit.domain.model.Vehicle
import com.driverprofit.domain.model.VehicleFuel
import com.driverprofit.testing.FakeVehicleRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class DeleteVehicleUseCaseTest {

    private fun vehicle(id: Long, createdAt: Instant, isCurrent: Boolean = false) = Vehicle(
        id = id,
        name = "Carro $id",
        fuel = VehicleFuel.FLEX,
        createdAt = createdAt,
        isCurrent = isCurrent,
    )

    @Test
    fun `apagar o veiculo atual promove o mais antigo dos que sobraram`() = runTest {
        val repository = FakeVehicleRepository(
            listOf(
                vehicle(1, Instant.parse("2024-01-01T00:00:00Z")),
                vehicle(2, Instant.parse("2024-06-01T00:00:00Z"), isCurrent = true),
                vehicle(3, Instant.parse("2025-01-01T00:00:00Z")),
            ),
        )
        val deleteVehicle = DeleteVehicleUseCase(repository)

        deleteVehicle(2)

        val restantes = repository.current
        assertEquals(setOf(1L, 3L), restantes.map { it.id }.toSet())
        assertTrue("o mais antigo deveria virar atual", restantes.single { it.id == 1L }.isCurrent)
        assertTrue(restantes.none { it.id != 1L && it.isCurrent })
    }

    @Test
    fun `apagar um veiculo que nao e o atual nao muda quem e o atual`() = runTest {
        val repository = FakeVehicleRepository(
            listOf(
                vehicle(1, Instant.parse("2024-01-01T00:00:00Z"), isCurrent = true),
                vehicle(2, Instant.parse("2024-06-01T00:00:00Z")),
            ),
        )
        val deleteVehicle = DeleteVehicleUseCase(repository)

        deleteVehicle(2)

        assertEquals(1L, repository.current.single().id)
        assertTrue(repository.current.single().isCurrent)
    }

    @Test
    fun `apagar o unico veiculo deixa a lista vazia sem quebrar`() = runTest {
        val repository = FakeVehicleRepository(
            listOf(vehicle(1, Instant.EPOCH, isCurrent = true)),
        )
        val deleteVehicle = DeleteVehicleUseCase(repository)

        deleteVehicle(1)

        assertTrue(repository.current.isEmpty())
        assertNull(repository.current.firstOrNull())
    }
}
