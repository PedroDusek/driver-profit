package com.driverprofit.feature.vehicle

import androidx.lifecycle.SavedStateHandle
import com.driverprofit.core.navigation.DriverProfitDestination
import com.driverprofit.domain.model.ChargingCapability
import com.driverprofit.domain.model.CombustionFuel
import com.driverprofit.domain.model.Vehicle
import com.driverprofit.domain.model.VehicleField
import com.driverprofit.domain.model.VehiclePowertrain
import com.driverprofit.domain.model.VehicleValidationError
import com.driverprofit.domain.usecase.GetVehicleUseCase
import com.driverprofit.domain.usecase.SaveVehicleUseCase
import com.driverprofit.domain.usecase.VehicleValidator
import com.driverprofit.feature.vehicle.form.VehicleFormViewModel
import com.driverprofit.testing.FakeVehicleRepository
import com.driverprofit.testing.MainDispatcherRule
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
        brand = "Chevrolet",
        model = "Onix",
        year = 2020,
        initialOdometerKm = 50_000,
        powertrain = VehiclePowertrain.COMBUSTION,
        combustionFuel = CombustionFuel.FLEX,
        chargingCapability = null,
        createdAt = Instant.parse("2024-01-15T08:00:00Z"),
    )

    private fun viewModel(
        repository: FakeVehicleRepository = FakeVehicleRepository(),
        vehicleId: Long? = null,
    ): VehicleFormViewModel {
        val handle = SavedStateHandle(
            vehicleId?.let { mapOf(DriverProfitDestination.ARG_VEHICLE_ID to it) } ?: emptyMap(),
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
        assertEquals("", state.brand)
        assertNull(state.powertrain)
        assertTrue(state.errors.isEmpty())
    }

    @Test
    fun `edicao carrega os dados do veiculo`() = runTest {
        val viewModel = viewModel(FakeVehicleRepository(listOf(existingVehicle)), vehicleId = 1)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isEditing)
        assertFalse(state.isLoading)
        assertEquals("Chevrolet", state.brand)
        assertEquals("2020", state.yearInput)
        assertEquals("50000", state.odometerInput)
        assertEquals(CombustionFuel.FLEX, state.combustionFuel)
    }

    @Test
    fun `campos numericos descartam caracteres nao numericos`() = runTest {
        val viewModel = viewModel()

        viewModel.onYearChange("20a2b0")
        viewModel.onOdometerChange("50.000")

        assertEquals("2020", viewModel.uiState.value.yearInput)
        assertEquals("50000", viewModel.uiState.value.odometerInput)
    }

    @Test
    fun `ano e limitado a quatro digitos`() = runTest {
        val viewModel = viewModel()

        viewModel.onYearChange("202020")

        assertEquals("2020", viewModel.uiState.value.yearInput)
    }

    @Test
    fun `combustao mostra combustivel e esconde recarga`() = runTest {
        val viewModel = viewModel()

        viewModel.onPowertrainChange(VehiclePowertrain.COMBUSTION)

        val state = viewModel.uiState.value
        assertTrue(state.showCombustionFuel)
        assertFalse(state.showChargingCapability)
    }

    @Test
    fun `eletrico esconde combustivel e mostra recarga`() = runTest {
        val viewModel = viewModel()

        viewModel.onPowertrainChange(VehiclePowertrain.ELECTRIC)

        val state = viewModel.uiState.value
        assertFalse(state.showCombustionFuel)
        assertTrue(state.showChargingCapability)
    }

    @Test
    fun `hibrido mostra os dois campos`() = runTest {
        val viewModel = viewModel()

        viewModel.onPowertrainChange(VehiclePowertrain.HYBRID)

        val state = viewModel.uiState.value
        assertTrue(state.showCombustionFuel)
        assertTrue(state.showChargingCapability)
    }

    @Test
    fun `trocar para eletrico limpa o combustivel escolhido antes`() = runTest {
        val viewModel = viewModel()

        viewModel.onPowertrainChange(VehiclePowertrain.COMBUSTION)
        viewModel.onCombustionFuelChange(CombustionFuel.FLEX)
        viewModel.onPowertrainChange(VehiclePowertrain.ELECTRIC)

        // Sem isso, o motorista salvaria um eletrico com combustivel - dado
        // incoerente que quebraria o formulario de abastecimento na v0.4.0.
        assertNull(viewModel.uiState.value.combustionFuel)
    }

    @Test
    fun `trocar para combustao limpa a recarga escolhida antes`() = runTest {
        val viewModel = viewModel()

        viewModel.onPowertrainChange(VehiclePowertrain.ELECTRIC)
        viewModel.onChargingCapabilityChange(ChargingCapability.PLUG_IN)
        viewModel.onPowertrainChange(VehiclePowertrain.COMBUSTION)

        assertNull(viewModel.uiState.value.chargingCapability)
    }

    @Test
    fun `salvar formulario vazio expoe os erros por campo`() = runTest {
        val viewModel = viewModel()

        viewModel.onSave()
        advanceUntilIdle()

        val errors = viewModel.uiState.value.errors
        assertEquals(VehicleValidationError.REQUIRED, errors[VehicleField.BRAND])
        assertEquals(VehicleValidationError.REQUIRED, errors[VehicleField.POWERTRAIN])
        assertNull(viewModel.uiState.value.savedVehicleId)
    }

    @Test
    fun `editar um campo limpa o erro daquele campo`() = runTest {
        val viewModel = viewModel()

        viewModel.onSave()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorFor(VehicleField.BRAND))

        viewModel.onBrandChange("Chevrolet")

        // O erro reaparece no proximo save se ainda existir; manter a mensagem
        // enquanto o usuario digita e so ruido.
        assertNull(viewModel.uiState.value.errorFor(VehicleField.BRAND))
        assertNotNull(viewModel.uiState.value.errorFor(VehicleField.MODEL))
    }

    @Test
    fun `cadastro valido persiste e sinaliza navegacao`() = runTest {
        val repository = FakeVehicleRepository()
        val viewModel = viewModel(repository)

        preencherFormularioValido(viewModel)
        viewModel.onSave()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.savedVehicleId)
        assertEquals(1, repository.current.size)
        assertEquals("Onix", repository.current.single().model)
    }

    @Test
    fun `onNavigatedBack limpa o sinal para nao navegar duas vezes`() = runTest {
        val viewModel = viewModel()

        preencherFormularioValido(viewModel)
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

        viewModel.onModelChange("Onix Plus")
        viewModel.onSave()
        advanceUntilIdle()

        assertEquals(1, repository.current.size)
        assertEquals("Onix Plus", repository.current.single().model)
        assertEquals(1L, repository.current.single().id)
    }

    private fun preencherFormularioValido(viewModel: VehicleFormViewModel) {
        viewModel.onBrandChange("Chevrolet")
        viewModel.onModelChange("Onix")
        viewModel.onYearChange("2020")
        viewModel.onOdometerChange("50000")
        viewModel.onPowertrainChange(VehiclePowertrain.COMBUSTION)
        viewModel.onCombustionFuelChange(CombustionFuel.FLEX)
    }
}
