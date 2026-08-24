package com.driverpro.vehicle.presentation

import androidx.lifecycle.SavedStateHandle
import com.driverpro.core.navigation.DriverProDestination
import com.driverpro.vehicle.domain.Vehicle
import com.driverpro.vehicle.domain.VehicleField
import com.driverpro.vehicle.domain.VehicleFuel
import com.driverpro.vehicle.domain.VehicleValidationError
import com.driverpro.vehicle.domain.GetVehicleUseCase
import com.driverpro.vehicle.domain.SaveVehicleUseCase
import com.driverpro.vehicle.domain.VehicleValidator
import com.driverpro.vehicle.presentation.form.VehicleFormViewModel
import com.driverpro.vehicle.domain.FakeVehicleRepository
import com.driverpro.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class VehicleFormViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-08-11T12:00:00Z"), ZoneId.of("UTC"))

    private val existingVehicle = Vehicle(
        id = 1,
        name = "Onix branco",
        fuel = VehicleFuel.FLEX,
        createdAt = Instant.parse("2024-01-15T08:00:00Z"),
    )

    private fun viewModel(
        repository: FakeVehicleRepository = FakeVehicleRepository(),
        vehicleId: Long? = null,
    ): VehicleFormViewModel {
        val handle = SavedStateHandle(
            vehicleId?.let { mapOf(DriverProDestination.ARG_VEHICLE_ID to it) } ?: emptyMap(),
        )
        return VehicleFormViewModel(
            savedStateHandle = handle,
            getVehicle = GetVehicleUseCase(repository),
            saveVehicle = SaveVehicleUseCase(repository, VehicleValidator(clock)),
        )
    }

    @Test
    fun `estado inicial de cadastro comeca vazio`() = runTest {
        val state = viewModel().uiState.value

        assertFalse(state.isEditing)
        assertFalse(state.isLoading)
        assertEquals("", state.name)
        assertNull(state.fuel)
        assertTrue(state.errors.isEmpty())
    }

    @Test
    fun `edicao carrega os dados do veiculo`() = runTest {
        val viewModel = viewModel(FakeVehicleRepository(listOf(existingVehicle)), vehicleId = 1)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isEditing)
        assertFalse(state.isLoading)
        assertEquals("Onix branco", state.name)
        assertEquals(VehicleFuel.FLEX, state.fuel)
    }

    @Test
    fun `veiculo excluido enquanto a tela abria vira cadastro novo`() = runTest {
        val viewModel = viewModel(FakeVehicleRepository(), vehicleId = 99)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isEditing)
    }

    @Test
    fun `salvar formulario vazio expoe os erros por campo`() = runTest {
        val viewModel = viewModel()

        viewModel.onSave()
        advanceUntilIdle()

        val errors = viewModel.uiState.value.errors
        assertEquals(VehicleValidationError.REQUIRED, errors[VehicleField.NAME])
        assertEquals(VehicleValidationError.REQUIRED, errors[VehicleField.FUEL])
        assertNull(viewModel.uiState.value.savedVehicleId)
    }

    @Test
    fun `editar um campo limpa o erro daquele campo`() = runTest {
        val viewModel = viewModel()

        viewModel.onSave()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorFor(VehicleField.NAME))

        viewModel.onNameChange("Onix branco")

        // O erro reaparece no proximo save se ainda existir; manter a mensagem
        // enquanto o usuario digita e so ruido.
        assertNull(viewModel.uiState.value.errorFor(VehicleField.NAME))
        assertNotNull(viewModel.uiState.value.errorFor(VehicleField.FUEL))
    }

    @Test
    fun `cadastro valido persiste e sinaliza navegacao`() = runTest {
        val repository = FakeVehicleRepository()
        val viewModel = viewModel(repository)

        viewModel.onNameChange("Onix branco")
        viewModel.onFuelChange(VehicleFuel.FLEX)
        viewModel.onSave()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.savedVehicleId)
        assertEquals(1, repository.current.size)
        assertEquals("Onix branco", repository.current.single().name)
    }

    @Test
    fun `onNavigatedBack limpa o sinal para nao navegar duas vezes`() = runTest {
        val viewModel = viewModel()

        viewModel.onNameChange("Onix branco")
        viewModel.onFuelChange(VehicleFuel.FLEX)
        viewModel.onSave()
        advanceUntilIdle()
        viewModel.onNavigatedBack()

        assertNull(viewModel.uiState.value.savedVehicleId)
    }

    @Test
    fun `edicao atualiza o veiculo existente em vez de criar outro`() = runTest {
        val repository = FakeVehicleRepository(listOf(existingVehicle))
        val viewModel = viewModel(repository, vehicleId = 1)
        advanceUntilIdle()

        viewModel.onNameChange("Onix prata")
        viewModel.onSave()
        advanceUntilIdle()

        assertEquals(1, repository.current.size)
        assertEquals("Onix prata", repository.current.single().name)
        assertEquals(1L, repository.current.single().id)
    }

    @Test
    fun `trocar o combustivel na edicao persiste a troca`() = runTest {
        val repository = FakeVehicleRepository(listOf(existingVehicle))
        val viewModel = viewModel(repository, vehicleId = 1)
        advanceUntilIdle()

        viewModel.onFuelChange(VehicleFuel.ELECTRIC)
        viewModel.onSave()
        advanceUntilIdle()

        assertEquals(VehicleFuel.ELECTRIC, repository.current.single().fuel)
    }
}
