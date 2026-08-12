package com.driverprofit.domain.usecase

import com.driverprofit.core.common.Money
import com.driverprofit.core.common.WorkDuration
import com.driverprofit.domain.model.Platform
import com.driverprofit.domain.model.WorkSession
import com.driverprofit.domain.model.WorkSessionDraft
import com.driverprofit.domain.model.WorkSessionField
import com.driverprofit.domain.model.WorkSessionFieldError
import com.driverprofit.domain.model.WorkSessionValidationError
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
    fun `rascunho vazio acusa data, plataforma e sessao vazia`() {
        val errors = validator.validate(WorkSessionDraft())

        assertEquals(
            setOf(
                WorkSessionFieldError(WorkSessionField.DATE, WorkSessionValidationError.REQUIRED),
                WorkSessionFieldError(
                    WorkSessionField.PLATFORM,
                    WorkSessionValidationError.REQUIRED,
                ),
                WorkSessionFieldError(
                    WorkSessionField.REVENUE,
                    WorkSessionValidationError.EMPTY_SESSION,
                ),
            ),
            errors.toSet(),
        )
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
    fun `apenas o faturamento preenchido ja basta`() {
        // Um dia pode ter sido lancado so com o valor do extrato, sem que o
        // motorista tenha anotado horas ou quilometragem.
        assertEquals(
            emptyList<WorkSessionFieldError>(),
            validator.validate(
                WorkSessionDraft(
                    date = hoje,
                    platform = Platform.UBER,
                    revenue = Money.of(320, 50),
                ),
            ),
        )
    }

    @Test
    fun `apenas as corridas preenchidas ja basta`() {
        assertEquals(
            emptyList<WorkSessionFieldError>(),
            validator.validate(
                WorkSessionDraft(date = hoje, platform = Platform.UBER, rides = 5),
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
    fun `toSession preenche com zero o que nao foi informado`() {
        val session = validator.toSession(
            WorkSessionDraft(date = hoje, platform = Platform.UBER, revenue = Money.of(100, 0)),
        )

        // Para o dashboard, "nao anotei" e "zero" somam igual - o que nao pode
        // e o dia inteiro estar em branco, e isso a validacao ja barrou.
        assertEquals(0, session.rides)
        assertEquals(WorkDuration.ZERO, session.onlineTime)
        assertEquals(0L, session.distanceKm)
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
}
