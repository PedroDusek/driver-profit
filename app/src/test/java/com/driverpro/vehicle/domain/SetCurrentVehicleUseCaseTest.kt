package com.driverpro.vehicle.domain

import com.driverpro.vehicle.domain.Vehicle
import com.driverpro.vehicle.domain.VehicleFuel
import com.driverpro.vehicle.domain.FakeVehicleRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SetCurrentVehicleUseCaseTest {

    private fun vehicle(id: Long, isCurrent: Boolean = false) = Vehicle(
        id = id,
        name = "Carro $id",
        fuel = VehicleFuel.FLEX,
        createdAt = Instant.EPOCH,
        isCurrent = isCurrent,
    )

    @Test
    fun `troca de atual e atomica - so um fica marcado`() = runTest {
        val repository = FakeVehicleRepository(
            listOf(vehicle(1, isCurrent = true), vehicle(2), vehicle(3)),
        )
        val setCurrent = SetCurrentVehicleUseCase(repository)

        setCurrent(3)

        val atuais = repository.current.filter { it.isCurrent }
        assertEquals(1, atuais.size)
        assertEquals(3L, atuais.single().id)
    }
}
