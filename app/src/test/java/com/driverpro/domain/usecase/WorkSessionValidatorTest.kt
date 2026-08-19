package com.driverpro.domain.usecase

import com.driverpro.core.common.Money
import com.driverpro.core.common.WorkDuration
import com.driverpro.domain.model.Platform
import com.driverpro.domain.model.WorkSession
import com.driverpro.domain.model.WorkSessionDraft
import com.driverpro.domain.model.WorkSessionField
import com.driverpro.domain.model.WorkSessionFieldError
import com.driverpro.domain.model.WorkSessionValidationError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class WorkSessionValidatorTest {

    // Relogio fixo em 11/08/2026: a checagem de data futura precisa ser
    // deterministica.
    private val hoje = LocalDate.of(2026, 8, 11)
    private val clock = Clock.fixed(Instant.parse("2026-08-11T12:00:00Z"), ZoneId.of("UTC"))
    private val validator = WorkSessionValidator(clock)

    private val validDraft = WorkSessionDraft(
        date = hoje,
        platform = Platform.UBER,
        rides = 18,
        revenue = Money.of(320, 50),
        onlineTime = WorkDuration.of(8, 20),
        distanceKm = 210,
    )

    @Test
    fun `rascunho completo e valido`() {
        assertEquals(emptyList<WorkSessionFieldError>(), validator.validate(validDraft))
    }

    @Test
    fun `rascunho vazio acusa todos os campos obrigatorios de uma vez`() {
        val errors = validator.validate(WorkSessionDraft())

        assertEquals(
            setOf(
                WorkSessionField.DATE,
                WorkSessionField.PLATFORM,
                WorkSessionField.REVENUE,
                WorkSessionField.RIDES,
                WorkSessionField.DISTANCE,
                WorkSessionField.ONLINE_TIME,
            ),
            errors.map { it.field }.toSet(),
        )
        assertTrue(errors.all { it.error == WorkSessionValidationError.REQUIRED })
    }

    @Test
    fun `data de hoje e aceita`() {
        assertEquals(emptyList<WorkSessionFieldError>(), validator.validate(validDraft.copy(date = hoje)))
    }

    @Test
    fun `data no passado e aceita`() {
        // Lancar o dia anterior de noite e o caso mais comum de uso.
        assertEquals(
            emptyList<WorkSessionFieldError>(),
            validator.validate(validDraft.copy(date = hoje.minusDays(30))),
        )
    }

    @Test
    fun `data no futuro e rejeitada`() {
        assertEquals(
            listOf(
                WorkSessionFieldError(
                    WorkSessionField.DATE,
                    WorkSessionValidationError.DATE_IN_FUTURE,
                ),
            ),
            validator.validate(validDraft.copy(date = hoje.plusDays(1))),
        )
    }

    @Test
    fun `faturamento em branco e rejeitado`() {
        // O dashboard divide soma(faturamento) por soma(horas). Deixar um
        // campo em branco entrar como zero produziria indicador errado com
        // cara de indicador certo.
        assertEquals(
            listOf(
                WorkSessionFieldError(
                    WorkSessionField.REVENUE,
                    WorkSessionValidationError.REQUIRED,
                ),
            ),
            validator.validate(validDraft.copy(revenue = null)),
        )
    }

    @Test
    fun `tempo online em branco e rejeitado`() {
        assertEquals(
            listOf(
                WorkSessionFieldError(
                    WorkSessionField.ONLINE_TIME,
                    WorkSessionValidationError.REQUIRED,
                ),
            ),
            validator.validate(validDraft.copy(onlineTime = null)),
        )
    }

    @Test
    fun `corridas em branco sao rejeitadas`() {
        assertEquals(
            listOf(
                WorkSessionFieldError(WorkSessionField.RIDES, WorkSessionValidationError.REQUIRED),
            ),
            validator.validate(validDraft.copy(rides = null)),
        )
    }

    @Test
    fun `distancia em branco e rejeitada`() {
        assertEquals(
            listOf(
                WorkSessionFieldError(
                    WorkSessionField.DISTANCE,
                    WorkSessionValidationError.REQUIRED,
                ),
            ),
            validator.validate(validDraft.copy(distanceKm = null)),
        )
    }

    @Test
    fun `zero preenchido e resposta valida`() {
        // Seis horas online sem nenhuma corrida e um dia ruim que existe.
        // O que se recusa e o campo em branco, nao o valor zero.
        assertEquals(
            emptyList<WorkSessionFieldError>(),
            validator.validate(
                validDraft.copy(
                    revenue = Money.ZERO,
                    rides = 0,
                    distanceKm = 0,
                    onlineTime = WorkDuration.of(6, 0),
                ),
            ),
        )
    }

    @Test
    fun `sessao com todos os numeros zerados e rejeitada`() {
        val errors = validator.validate(
            validDraft.copy(
                revenue = Money.ZERO,
                rides = 0,
                onlineTime = WorkDuration.ZERO,
                distanceKm = 0,
            ),
        )

        assertTrue(
            errors.contains(
                WorkSessionFieldError(
                    WorkSessionField.REVENUE,
                    WorkSessionValidationError.EMPTY_SESSION,
                ),
            ),
        )
    }

    @Test
    fun `faturamento negativo e rejeitado`() {
        assertTrue(
            validator.validate(validDraft.copy(revenue = Money(-1))).contains(
                WorkSessionFieldError(
                    WorkSessionField.REVENUE,
                    WorkSessionValidationError.NEGATIVE,
                ),
            ),
        )
    }

    @Test
    fun `corridas negativas sao rejeitadas`() {
        assertTrue(
            validator.validate(validDraft.copy(rides = -1)).contains(
                WorkSessionFieldError(WorkSessionField.RIDES, WorkSessionValidationError.NEGATIVE),
            ),
        )
    }

    @Test
    fun `distancia negativa e rejeitada`() {
        assertTrue(
            validator.validate(validDraft.copy(distanceKm = -1)).contains(
                WorkSessionFieldError(
                    WorkSessionField.DISTANCE,
                    WorkSessionValidationError.NEGATIVE,
                ),
            ),
        )
    }

    @Test
    fun `jornada de exatamente 24 horas e aceita`() {
        assertEquals(
            emptyList<WorkSessionFieldError>(),
            validator.validate(validDraft.copy(onlineTime = WorkDuration.of(24, 0))),
        )
    }

    @Test
    fun `jornada acima de 24 horas e rejeitada`() {
        assertEquals(
            listOf(
                WorkSessionFieldError(
                    WorkSessionField.ONLINE_TIME,
                    WorkSessionValidationError.ONLINE_TIME_TOO_LONG,
                ),
            ),
            validator.validate(validDraft.copy(onlineTime = WorkDuration(24 * 60 + 1))),
        )
    }

    @Test
    fun `observacao acima do limite e rejeitada`() {
        val nota = "a".repeat(WorkSession.MAX_NOTE_LENGTH + 1)

        assertEquals(
            listOf(
                WorkSessionFieldError(
                    WorkSessionField.NOTE,
                    WorkSessionValidationError.NOTE_TOO_LONG,
                ),
            ),
            validator.validate(validDraft.copy(note = nota)),
        )
    }

    @Test
    fun `toSession copia os valores informados sem inventar defaults`() {
        val session = validator.toSession(validDraft)

        assertEquals(18, session.rides)
        assertEquals(Money.of(320, 50), session.revenue)
        assertEquals(WorkDuration.of(8, 20), session.onlineTime)
        assertEquals(210L, session.distanceKm)
    }

    @Test
    fun `toSession remove espacos das bordas da observacao`() {
        assertEquals(
            "corrida longa",
            validator.toSession(validDraft.copy(note = "  corrida longa  ")).note,
        )
    }

    @Test
    fun `toSession usa o relogio injetado`() {
        assertEquals(
            Instant.parse("2026-08-11T12:00:00Z"),
            validator.toSession(validDraft).createdAt,
        )
    }

    @Test
    fun `toSession carrega o veiculo sem exigir nada dele`() {
        // O vinculo e automatico (v0.12.0): nulo e uma resposta valida, ja
        // que nem sempre ha um veiculo atual quando o ganho e lancado.
        assertEquals(null, validator.toSession(validDraft).vehicleId)
        assertEquals(5L, validator.toSession(validDraft.copy(vehicleId = 5)).vehicleId)
    }
}
