package com.driverprofit.feature.expenses

import androidx.lifecycle.SavedStateHandle
import com.driverprofit.core.common.Money
import com.driverprofit.core.common.Quantity
import com.driverprofit.core.navigation.DriverProfitDestination
import com.driverprofit.domain.model.ChargingLocation
import com.driverprofit.domain.model.Expense
import com.driverprofit.domain.model.ExpenseCategory
import com.driverprofit.domain.model.ExpenseDetail
import com.driverprofit.domain.model.ExpenseDetailKind
import com.driverprofit.domain.model.ExpenseField
import com.driverprofit.domain.model.FuelType
import com.driverprofit.domain.model.MaintenanceCategory
import com.driverprofit.domain.model.MeasurementUnit
import com.driverprofit.domain.model.Vehicle
import com.driverprofit.domain.model.VehicleFuel
import com.driverprofit.domain.usecase.ExpenseValidator
import com.driverprofit.domain.usecase.GetExpenseUseCase
import com.driverprofit.domain.usecase.ObserveVehicleOdometersUseCase
import com.driverprofit.domain.usecase.ObserveVehiclesUseCase
import com.driverprofit.domain.usecase.SaveExpenseUseCase
import com.driverprofit.feature.expenses.form.ExpenseFormViewModel
import com.driverprofit.testing.FakeExpenseRepository
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
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseFormViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val hoje = LocalDate.of(2026, 8, 11)
    private val clock = Clock.fixed(Instant.parse("2026-08-11T12:00:00Z"), ZoneId.of("UTC"))

    private val flexCar = Vehicle(1, "Onix branco", VehicleFuel.FLEX, Instant.EPOCH)
    private val electricCar = Vehicle(2, "Dolphin", VehicleFuel.ELECTRIC, Instant.EPOCH)
    private val cngCar = Vehicle(3, "Kwid GNV", VehicleFuel.CNG, Instant.EPOCH)

    private fun viewModel(
        vehicles: List<Vehicle> = listOf(flexCar),
        expenses: FakeExpenseRepository = FakeExpenseRepository(),
        expenseId: Long? = null,
    ): ExpenseFormViewModel {
        val vehicleRepository = FakeVehicleRepository(vehicles)
        val handle = SavedStateHandle(
            expenseId?.let { mapOf(DriverProfitDestination.ARG_EXPENSE_ID to it) } ?: emptyMap(),
        )
        return ExpenseFormViewModel(
            savedStateHandle = handle,
            observeVehicles = ObserveVehiclesUseCase(vehicleRepository),
            observeVehicleOdometers = ObserveVehicleOdometersUseCase(expenses),
            getExpense = GetExpenseUseCase(expenses),
            saveExpense = SaveExpenseUseCase(
                expenses,
                vehicleRepository,
                ExpenseValidator(clock),
            ),
            clock = clock,
        )
    }

    /**
     * Simula o motorista limpando o campo e digitando os centavos, um toque
     * por vez — que é como um `TextField` real se comporta.
     */
    private fun ExpenseFormViewModel.setAmount(digits: String) {
        repeat(uiState.value.amountDigits.length) {
            onAmountChange(uiState.value.amountText.dropLast(1))
        }
        digits.forEach { onAmountChange(uiState.value.amountText + it) }
    }

    @Test
    fun `lancamento novo comeca com a data de hoje`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(hoje, viewModel.uiState.value.date)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `com um veiculo so ele ja vem selecionado`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        // Pedir que selecione o unico carro que existe e burocracia.
        assertEquals(1L, viewModel.uiState.value.vehicleId)
    }

    @Test
    fun `com varios veiculos nenhum vem selecionado`() = runTest {
        val viewModel = viewModel(vehicles = listOf(flexCar, electricCar))
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.vehicleId)
    }

    @Test
    fun `pedagio nao mostra campo de veiculo`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onCategoryChange(ExpenseCategory.TOLL)

        assertFalse(viewModel.uiState.value.showVehicle)
        assertEquals(ExpenseDetailKind.NONE, viewModel.uiState.value.detailKind)
    }

    @Test
    fun `abastecimento oferece so os combustiveis do veiculo`() = runTest {
        val viewModel = viewModel(vehicles = listOf(cngCar))
        advanceUntilIdle()

        viewModel.onCategoryChange(ExpenseCategory.FUEL)

        assertEquals(listOf(FuelType.CNG), viewModel.uiState.value.availableFuelTypes)
    }

    @Test
    fun `eletricidade nao aparece como combustivel de abastecimento`() = runTest {
        // Carregar nao e abastecer: sao categorias diferentes.
        val viewModel = viewModel(vehicles = listOf(electricCar))
        advanceUntilIdle()

        viewModel.onCategoryChange(ExpenseCategory.FUEL)

        assertTrue(viewModel.uiState.value.availableFuelTypes.isEmpty())
    }

    @Test
    fun `unidade do campo acompanha o combustivel escolhido`() = runTest {
        val viewModel = viewModel(vehicles = listOf(cngCar))
        advanceUntilIdle()

        viewModel.onCategoryChange(ExpenseCategory.FUEL)
        viewModel.onFuelTypeChange(FuelType.CNG)

        assertEquals(MeasurementUnit.CUBIC_METER, viewModel.uiState.value.quantityUnit)
    }

    @Test
    fun `carregamento usa kWh como unidade`() = runTest {
        val viewModel = viewModel(vehicles = listOf(electricCar))
        advanceUntilIdle()

        viewModel.onCategoryChange(ExpenseCategory.CHARGING)

        assertEquals(MeasurementUnit.KILOWATT_HOUR, viewModel.uiState.value.quantityUnit)
    }

    @Test
    fun `trocar de categoria limpa os campos que deixaram de existir`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onCategoryChange(ExpenseCategory.FUEL)
        viewModel.onFuelTypeChange(FuelType.ETHANOL)
        viewModel.onQuantityChange("35,4")
        viewModel.onCategoryChange(ExpenseCategory.TOLL)

        // Senao salvaria um pedagio com combustivel pendurado.
        assertNull(viewModel.uiState.value.fuelType)
        assertEquals("", viewModel.uiState.value.quantityInput)
    }

    @Test
    fun `trocar de veiculo descarta combustivel que o novo nao aceita`() = runTest {
        val viewModel = viewModel(vehicles = listOf(flexCar, cngCar))
        advanceUntilIdle()

        viewModel.onCategoryChange(ExpenseCategory.FUEL)
        viewModel.onVehicleChange(1)
        viewModel.onFuelTypeChange(FuelType.ETHANOL)
        viewModel.onVehicleChange(3)

        assertNull(viewModel.uiState.value.fuelType)
    }

    @Test
    fun `valor zero digitado e preservado`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.setAmount("0")

        // Recarga gratuita e R$ 0,00 (PRD 11), diferente de campo vazio.
        assertEquals(Money.ZERO, viewModel.uiState.value.amount)
    }

    @Test
    fun `quantidade aceita virgula`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onQuantityChange("35,4")

        assertEquals(Quantity.of(35, 400), viewModel.uiState.value.quantity)
    }

    @Test
    fun `o campo de odometro so aparece onde ha veiculo`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onCategoryChange(ExpenseCategory.FUEL)
        assertTrue(viewModel.uiState.value.showOdometer)

        viewModel.onCategoryChange(ExpenseCategory.TOLL)
        assertFalse(viewModel.uiState.value.showOdometer)
    }

    @Test
    fun `odometro aceita so digitos`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        // Quilometro e inteiro: sem separador, sem fracao, sem sinal.
        viewModel.onOdometerChange("45.200,7 km")

        assertEquals("452007", viewModel.uiState.value.odometerInput)
        assertEquals(452_007L, viewModel.uiState.value.odometerKm)
    }

    @Test
    fun `a ultima leitura do veiculo fica visivel no formulario`() = runTest {
        // Ver "ultima leitura" enquanto digita e o que faz um digito trocado
        // saltar aos olhos na hora.
        val expenses = FakeExpenseRepository(
            listOf(
                Expense(
                    id = 1,
                    vehicleId = 1,
                    date = hoje,
                    category = ExpenseCategory.FUEL,
                    amount = Money.of(200, 0),
                    odometerKm = 44_000,
                    createdAt = Instant.EPOCH,
                ),
                Expense(
                    id = 2,
                    vehicleId = 1,
                    date = hoje.minusDays(5),
                    category = ExpenseCategory.FUEL,
                    amount = Money.of(180, 0),
                    odometerKm = 45_500,
                    createdAt = Instant.EPOCH,
                ),
            ),
        )
        val viewModel = viewModel(expenses = expenses)
        advanceUntilIdle()

        // A maior, e nao a mais recente por data: o motorista pode lancar o
        // abastecimento da semana passada hoje.
        assertEquals(45_500L, viewModel.uiState.value.lastOdometerKm)
    }

    @Test
    fun `sem leitura anterior o formulario nao inventa referencia`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.lastOdometerKm)
    }

    @Test
    fun `abastecimento sem odometro nao salva`() = runTest {
        val expenses = FakeExpenseRepository()
        val viewModel = viewModel(expenses = expenses)
        advanceUntilIdle()

        viewModel.onCategoryChange(ExpenseCategory.FUEL)
        viewModel.setAmount("21000")
        viewModel.onFuelTypeChange(FuelType.ETHANOL)
        viewModel.onSave()
        advanceUntilIdle()

        assertTrue(expenses.current.isEmpty())
        assertNotNull(viewModel.uiState.value.errorFor(ExpenseField.ODOMETER))
    }

    @Test
    fun `abastecimento valido persiste com o detalhe certo`() = runTest {
        val expenses = FakeExpenseRepository()
        val viewModel = viewModel(expenses = expenses)
        advanceUntilIdle()

        viewModel.onCategoryChange(ExpenseCategory.FUEL)
        viewModel.setAmount("21000")
        viewModel.onOdometerChange("45200")
        viewModel.onFuelTypeChange(FuelType.ETHANOL)
        viewModel.onQuantityChange("35,4")
        viewModel.onPlaceChange("Posto Shell")
        viewModel.onSave()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.savedExpenseId)
        val saved = expenses.current.single()
        assertEquals(Money.of(210, 0), saved.amount)
        assertEquals(
            ExpenseDetail.Refuel(FuelType.ETHANOL, Quantity.of(35, 400), "Posto Shell"),
            saved.detail,
        )
    }

    @Test
    fun `o campo de local vai para o campo certo conforme a categoria`() = runTest {
        val expenses = FakeExpenseRepository()
        val viewModel = viewModel(expenses = expenses)
        advanceUntilIdle()

        viewModel.onCategoryChange(ExpenseCategory.MAINTENANCE)
        viewModel.setAmount("32000")
        viewModel.onOdometerChange("45200")
        viewModel.onMaintenanceCategoryChange(MaintenanceCategory.OIL)
        viewModel.onPlaceChange("Oficina do Zé")
        viewModel.onSave()
        advanceUntilIdle()

        assertEquals(
            ExpenseDetail.Maintenance(MaintenanceCategory.OIL, "Oficina do Zé"),
            expenses.current.single().detail,
        )
    }

    @Test
    fun `recarga gratuita e salva com valor zero`() = runTest {
        val expenses = FakeExpenseRepository()
        val viewModel = viewModel(vehicles = listOf(electricCar), expenses = expenses)
        advanceUntilIdle()

        viewModel.onCategoryChange(ExpenseCategory.CHARGING)
        viewModel.setAmount("0")
        viewModel.onOdometerChange("12800")
        viewModel.onQuantityChange("42")
        viewModel.onChargingLocationChange(ChargingLocation.COMMERCIAL)
        viewModel.onSave()
        advanceUntilIdle()

        assertEquals(Money.ZERO, expenses.current.single().amount)
        assertEquals(Money.ZERO, expenses.current.single().pricePerUnit)
    }

    @Test
    fun `salvar sem categoria expoe o erro`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onSave()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorFor(ExpenseField.CATEGORY))
        assertNull(viewModel.uiState.value.savedExpenseId)
    }

    @Test
    fun `edicao carrega os dados da despesa`() = runTest {
        val existing = Expense(
            id = 1,
            vehicleId = 1,
            date = hoje.minusDays(1),
            category = ExpenseCategory.FUEL,
            amount = Money.of(210, 0),
            description = "cheio",
            detail = ExpenseDetail.Refuel(FuelType.GASOLINE, Quantity.of(30), "Ipiranga"),
            createdAt = Instant.EPOCH,
        )
        val viewModel = viewModel(expenses = FakeExpenseRepository(listOf(existing)), expenseId = 1)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isEditing)
        assertEquals(ExpenseCategory.FUEL, state.category)
        assertEquals(Money.of(210, 0), state.amount)
        assertEquals(FuelType.GASOLINE, state.fuelType)
        assertEquals("30", state.quantityInput)
        assertEquals("Ipiranga", state.place)
        assertEquals("cheio", state.description)
    }

    @Test
    fun `edicao atualiza em vez de criar outra`() = runTest {
        val existing = Expense(
            id = 1,
            date = hoje,
            category = ExpenseCategory.TOLL,
            amount = Money.of(12, 50),
            createdAt = Instant.EPOCH,
        )
        val expenses = FakeExpenseRepository(listOf(existing))
        val viewModel = viewModel(expenses = expenses, expenseId = 1)
        advanceUntilIdle()

        viewModel.setAmount("1500")
        viewModel.onSave()
        advanceUntilIdle()

        assertEquals(1, expenses.current.size)
        assertEquals(Money.of(15, 0), expenses.current.single().amount)
    }

    // --- "Nao sei a leitura" em lancamento retroativo ---

    @Test
    fun `nao sei a leitura so e oferecida em data anterior a hoje`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onCategoryChange(ExpenseCategory.FUEL)

        viewModel.onDateChange(hoje)
        assertFalse(viewModel.uiState.value.allowsUnknownOdometer(hoje))

        // No lancamento do dia o painel esta a mao; oferecer a saida ali a
        // transformaria em rotina.
        viewModel.onDateChange(hoje.minusDays(3))
        assertTrue(viewModel.uiState.value.allowsUnknownOdometer(hoje))
    }

    @Test
    fun `voltar a data para hoje desfaz a declaracao`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onCategoryChange(ExpenseCategory.FUEL)
        viewModel.onDateChange(hoje.minusDays(3))
        viewModel.onOdometerUnknownChange(true)
        assertTrue(viewModel.uiState.value.odometerUnknown)

        viewModel.onDateChange(hoje)

        // Sem isso a declaracao ficaria pendurada num estado que nao a permite:
        // a caixa some, o campo continua escondido por causa dela, e o
        // motorista recebe "campo obrigatorio" sem ter onde preencher.
        assertFalse(viewModel.uiState.value.odometerUnknown)
    }

    @Test
    fun `digitar a leitura desmarca a declaracao`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onCategoryChange(ExpenseCategory.FUEL)
        viewModel.onDateChange(hoje.minusDays(3))
        viewModel.onOdometerUnknownChange(true)

        viewModel.onOdometerChange("100000")

        assertFalse(viewModel.uiState.value.odometerUnknown)
        assertEquals("100000", viewModel.uiState.value.odometerInput)
    }

}
