package com.driverprofit.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * Garante que cada tipo de combustível produz as opções corretas de
 * abastecimento — é isso que monta o formulário de lançamento e determina a
 * unidade usada no cálculo de custo.
 */
class VehicleTest {

    private fun vehicle(fuel: VehicleFuel) = Vehicle(
        name = "Onix branco",
        fuel = fuel,
        createdAt = Instant.EPOCH,
    )

    @Test
    fun `flex oferece gasolina e etanol`() {
        val carro = vehicle(VehicleFuel.FLEX)

        assertEquals(listOf(FuelType.GASOLINE, FuelType.ETHANOL), carro.refuelOptions)
        assertFalse(carro.supportsChargingRecords)
    }

    @Test
    fun `gasolina oferece apenas gasolina`() {
        assertEquals(listOf(FuelType.GASOLINE), vehicle(VehicleFuel.GASOLINE).refuelOptions)
    }

    @Test
    fun `gnv e medido em metros cubicos e nunca em litros`() {
        assertEquals(listOf(FuelType.CNG), vehicle(VehicleFuel.CNG).refuelOptions)
        assertEquals(MeasurementUnit.CUBIC_METER, FuelType.CNG.unit)
        assertEquals(MeasurementUnit.LITER, FuelType.GASOLINE.unit)
    }

    @Test
    fun `flex com gnv oferece os tres insumos`() {
        assertEquals(
            listOf(FuelType.GASOLINE, FuelType.ETHANOL, FuelType.CNG),
            vehicle(VehicleFuel.FLEX_CNG).refuelOptions,
        )
    }

    @Test
    fun `eletrico e medido em kWh e aceita carregamento`() {
        val carro = vehicle(VehicleFuel.ELECTRIC)

        assertEquals(listOf(FuelType.ELECTRICITY), carro.refuelOptions)
        assertEquals(MeasurementUnit.KILOWATT_HOUR, FuelType.ELECTRICITY.unit)
        assertTrue(carro.supportsChargingRecords)
    }

    @Test
    fun `hibrido abastece e carrega`() {
        val carro = vehicle(VehicleFuel.HYBRID)

        assertEquals(
            listOf(FuelType.GASOLINE, FuelType.ETHANOL, FuelType.ELECTRICITY),
            carro.refuelOptions,
        )
        assertTrue(carro.supportsChargingRecords)
    }

    @Test
    fun `apenas eletrico e hibrido aceitam carregamento`() {
        val comCarga = VehicleFuel.entries.filter { it.supportsCharging }.toSet()

        assertEquals(setOf(VehicleFuel.ELECTRIC, VehicleFuel.HYBRID), comCarga)
    }

    @Test
    fun `todo combustivel oferece ao menos uma opcao de abastecimento`() {
        VehicleFuel.entries.forEach { fuel ->
            assertTrue("sem opcoes: $fuel", fuel.refuelOptions.isNotEmpty())
        }
    }
}
