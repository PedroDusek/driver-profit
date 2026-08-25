package com.driverpro.vehicle.domain

import com.driverpro.vehicle.domain.Vehicle
import com.driverpro.vehicle.domain.VehicleDraft
import com.driverpro.vehicle.domain.VehicleField
import com.driverpro.vehicle.domain.VehicleFuel
import com.driverpro.vehicle.domain.FakeVehicleRepository
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

    private val validDraft = VehicleDraft(name = "Onix branco", fuel = VehicleFuel.FLEX)

    @Test
    fun `insere veiculo valido e devolve o id`() = runTest {
        val result = saveVehicle(validDraft)

        assertTrue(result is SaveVehicleResult.Success)
        assertEquals(1, repository.current.size)
        assertEquals("Onix branco", repository.current.single().name)
    }

    @Test
    fun `primeiro veiculo cadastrado nasce atual`() = runTest {
        saveVehicle(validDraft)

        assertTrue(repository.current.single().isCurrent)
    }

    @Test
    fun `segundo veiculo cadastrado nao mexe em quem e o atual`() = runTest {
        saveVehicle(validDraft)
        saveVehicle(validDraft.copy(name = "Civic prata"))

        val atuais = repository.current.filter { it.isCurrent }
        assertEquals(1, atuais.size)
        assertEquals("Onix branco", atuais.single().name)
    }

    @Test
    fun `editar nao mexe em isCurrent`() = runTest {
        val id = (saveVehicle(validDraft) as SaveVehicleResult.Success).id
        saveVehicle(validDraft.copy(name = "Segundo carro"))

        saveVehicle(validDraft.copy(id = id, name = "Onix branco renomeado"))

        assertTrue(repository.current.first { it.id == id }.isCurrent)
    }

    @Test
    fun `rascunho invalido nao grava nada`() = runTest {
        val result = saveVehicle(validDraft.copy(name = ""))

        assertTrue(result is SaveVehicleResult.Invalid)
        assertEquals(
            listOf(VehicleField.NAME),
            (result as SaveVehicleResult.Invalid).errors.map { it.field },
        )
        assertTrue(repository.current.isEmpty())
    }

    @Test
    fun `edicao atualiza em vez de inserir`() = runTest {
        val id = (saveVehicle(validDraft) as SaveVehicleResult.Success).id

        val result = saveVehicle(validDraft.copy(id = id, name = "Onix prata"))

        assertEquals(SaveVehicleResult.Success(id), result)
        assertEquals(1, repository.current.size)
        assertEquals("Onix prata", repository.current.single().name)
    }

    @Test
    fun `edicao preserva a data de cadastro original`() = runTest {
        val original = Vehicle(
            id = 7,
            name = "Argo",
            fuel = VehicleFuel.FLEX,
            createdAt = Instant.parse("2024-01-15T08:00:00Z"),
        )
        val repositoryComVeiculo = FakeVehicleRepository(listOf(original))
        val useCase = SaveVehicleUseCase(repositoryComVeiculo, VehicleValidator(clock))

        useCase(VehicleDraft(id = 7, name = "Argo", fuel = VehicleFuel.GASOLINE))

        // Trocar o combustivel nao pode reescrever a data de entrada no app.
        val atualizado = repositoryComVeiculo.current.single()
        assertEquals(Instant.parse("2024-01-15T08:00:00Z"), atualizado.createdAt)
        assertEquals(VehicleFuel.GASOLINE, atualizado.fuel)
    }

    @Test
    fun `edicao de veiculo ja excluido insere um novo em vez de perder o dado`() = runTest {
        val result = saveVehicle(validDraft.copy(id = 999))

        assertTrue(result is SaveVehicleResult.Success)
        assertEquals(1, repository.current.size)
    }

    @Test
    fun `sem combustivel escolhido e rejeitado antes de tocar o repositorio`() = runTest {
        val result = saveVehicle(validDraft.copy(fuel = null))

        assertTrue(result is SaveVehicleResult.Invalid)
        assertTrue(repository.current.isEmpty())
    }
}
