package com.driverprofit.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * Garante que a separação powertrain / combustível / recarga (PRD §13)
 * produz o formulário certo para cada configuração de veículo.
 */
class VehicleTest {

    private fun vehicle(
        powertrain: VehiclePowertrain,
        combustionFuel: CombustionFuel?,
        chargingCapability: ChargingCapability?,
    ) = Vehicle(
        brand = "Marca",
        model = "Modelo",
        year = 2020,
        initialOdometerKm = 50_000,
        powertrain = powertrain,
        combustionFuel = combustionFuel,
        chargingCapability = chargingCapability,
        createdAt = Instant.EPOCH,
    )

    @Test
    fun `veiculo flex oferece gasolina e etanol no abastecimento`() {
        val carro = vehicle(VehiclePowertrain.COMBUSTION, CombustionFuel.FLEX, null)

        assertEquals(listOf(FuelType.GASOLINE, FuelType.ETHANOL), carro.refuelOptions)
        assertFalse(carro.supportsChargingRecords)
    }

    @Test
    fun `veiculo a gasolina oferece apenas gasolina`() {
        val carro = vehicle(VehiclePowertrain.COMBUSTION, CombustionFuel.GASOLINE, null)

        assertEquals(listOf(FuelType.GASOLINE), carro.refuelOptions)
    }

    @Test
    fun `gnv e medido em metros cubicos e nunca em litros`() {
        val carro = vehicle(VehiclePowertrain.COMBUSTION, CombustionFuel.CNG, null)

        assertEquals(listOf(FuelType.CNG), carro.refuelOptions)
        assertEquals(MeasurementUnit.CUBIC_METER, FuelType.CNG.unit)
        assertEquals(MeasurementUnit.LITER, FuelType.GASOLINE.unit)
    }

    @Test
    fun `eletrico puro nao tem combustivel e aceita carregamento`() {
        val carro = vehicle(VehiclePowertrain.ELECTRIC, null, ChargingCapability.PLUG_IN)

        assertTrue(carro.refuelOptions.isEmpty())
        assertTrue(carro.supportsChargingRecords)
    }

    @Test
    fun `hibrido convencional abastece mas nao carrega`() {
        val carro = vehicle(
            VehiclePowertrain.HYBRID,
            CombustionFuel.GASOLINE,
            ChargingCapability.NONE,
        )

        assertEquals(listOf(FuelType.GASOLINE), carro.refuelOptions)
        assertFalse(carro.supportsChargingRecords)
    }

    @Test
    fun `hibrido plug-in flex abastece e carrega`() {
        val carro = vehicle(
            VehiclePowertrain.HYBRID,
            CombustionFuel.FLEX,
            ChargingCapability.PLUG_IN,
        )

        assertEquals(listOf(FuelType.GASOLINE, FuelType.ETHANOL), carro.refuelOptions)
        assertTrue(carro.supportsChargingRecords)
    }

    @Test
    fun `capacidade de recarga desconhecida nao habilita o formulario`() {
        val carro = vehicle(
            VehiclePowertrain.HYBRID,
            CombustionFuel.GASOLINE,
            ChargingCapability.UNKNOWN,
        )

        assertFalse(carro.supportsChargingRecords)
    }
}
