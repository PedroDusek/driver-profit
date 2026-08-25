package com.driverpro.personal.domain

import com.driverpro.core.domain.DateRange

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class PersonalUsageTest {

    private fun usage(start: LocalDate, end: LocalDate, km: Long) = PersonalUsage(
        vehicleId = 1,
        range = DateRange(start, end),
        distanceKm = km,
        createdAt = Instant.EPOCH,
    )

    @Test
    fun `viagem de um dia inteira dentro do periodo conta tudo`() {
        val viagem = usage(LocalDate.of(2026, 7, 12), LocalDate.of(2026, 7, 12), 300)

        assertEquals(
            300L,
            viagem.kilometersWithin(DateRange(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31))),
        )
    }

    @Test
    fun `viagem fora do periodo nao conta nada`() {
        val viagem = usage(LocalDate.of(2026, 7, 12), LocalDate.of(2026, 7, 14), 1_200)

        assertEquals(
            0L,
            viagem.kilometersWithin(DateRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31))),
        )
    }

    @Test
    fun `viagem de tres dias contribui um terco para um dia`() {
        // 1.200 km entre 12 e 14 de julho: um periodo que cobre so o dia 13
        // recebe 400 km.
        val viagem = usage(LocalDate.of(2026, 7, 12), LocalDate.of(2026, 7, 14), 1_200)

        assertEquals(
            400L,
            viagem.kilometersWithin(DateRange(LocalDate.of(2026, 7, 13), LocalDate.of(2026, 7, 13))),
        )
    }

    @Test
    fun `viagem que atravessa a virada do mes entra nos dois meses`() {
        // Este e o caso que uma consulta "comeca dentro do periodo" perderia,
        // deixando o custo/km de agosto inflado.
        val viagem = usage(LocalDate.of(2026, 7, 30), LocalDate.of(2026, 8, 2), 400)

        val julho = viagem.kilometersWithin(
            DateRange(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)),
        )
        val agosto = viagem.kilometersWithin(
            DateRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)),
        )

        assertEquals(200L, julho)
        assertEquals(200L, agosto)
        // Nada se perde e nada se duplica na virada.
        assertEquals(400L, julho + agosto)
    }

    @Test
    fun `periodo que contem a viagem inteira recebe todos os quilometros`() {
        val viagem = usage(LocalDate.of(2026, 7, 12), LocalDate.of(2026, 7, 14), 1_200)

        assertEquals(
            1_200L,
            viagem.kilometersWithin(DateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))),
        )
    }

    @Test
    fun `intervalo de um dia so no limite do periodo conta`() {
        val viagem = usage(LocalDate.of(2026, 7, 31), LocalDate.of(2026, 7, 31), 120)

        assertEquals(
            120L,
            viagem.kilometersWithin(DateRange(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31))),
        )
    }
}
