package com.driverpro.domain.usecase

import com.driverpro.core.common.Money
import com.driverpro.core.common.WorkDuration
import com.driverpro.domain.model.Platform
import com.driverpro.domain.model.WorkSession
import com.driverpro.domain.model.WorkSessionDraft
import com.driverpro.domain.model.WorkSessionField
import com.driverpro.testing.FakeWorkSessionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class SaveWorkSessionUseCaseTest {

    private val hoje = LocalDate.of(2026, 8, 11)
    private val clock = Clock.fixed(Instant.parse("2026-08-11T12:00:00Z"), ZoneId.of("UTC"))
    private val repository = FakeWorkSessionRepository()
    private val saveSession = SaveWorkSessionUseCase(repository, WorkSessionValidator(clock))

    private val validDraft = WorkSessionDraft(
        date = hoje,
        platform = Platform.UBER,
        rides = 18,
        revenue = Money.of(320, 50),
        onlineTime = WorkDuration.of(8, 20),
        distanceKm = 210,
    )

    @Test
    fun `registra sessao valida e devolve o id`() = runTest {
        val result = saveSession(validDraft)

        assertTrue(result is SaveWorkSessionResult.Success)
        assertEquals(1, repository.current.size)
        assertEquals(Money.of(320, 50), repository.current.single().revenue)
    }

    @Test
    fun `rascunho invalido nao grava nada`() = runTest {
        val result = saveSession(validDraft.copy(date = hoje.plusDays(1)))

        assertTrue(result is SaveWorkSessionResult.Invalid)
        assertEquals(
            listOf(WorkSessionField.DATE),
            (result as SaveWorkSessionResult.Invalid).errors.map { it.field },
        )
        assertTrue(repository.current.isEmpty())
    }

    @Test
    fun `duas plataformas no mesmo dia sao dois registros`() = runTest {
        saveSession(validDraft.copy(platform = Platform.UBER))
        saveSession(validDraft.copy(platform = Platform.NINETY_NINE))

        // E isso que torna a comparacao entre plataformas possivel depois.
        assertEquals(2, repository.current.size)
        assertEquals(
            setOf(Platform.UBER, Platform.NINETY_NINE),
            repository.current.map { it.platform }.toSet(),
        )
    }

    @Test
    fun `edicao atualiza em vez de inserir`() = runTest {
        val id = (saveSession(validDraft) as SaveWorkSessionResult.Success).id

        val result = saveSession(validDraft.copy(id = id, revenue = Money.of(400, 0)))

        assertEquals(SaveWorkSessionResult.Success(id), result)
        assertEquals(1, repository.current.size)
        assertEquals(Money.of(400, 0), repository.current.single().revenue)
    }

    @Test
    fun `edicao preserva a data de criacao original`() = runTest {
        val original = WorkSession(
            id = 7,
            date = hoje.minusDays(3),
            platform = Platform.UBER,
            rides = 10,
            revenue = Money.of(200, 0),
            onlineTime = WorkDuration.of(5, 0),
            distanceKm = 100,
            createdAt = Instant.parse("2024-01-15T08:00:00Z"),
        )
        val repositoryComSessao = FakeWorkSessionRepository(listOf(original))
        val useCase = SaveWorkSessionUseCase(repositoryComSessao, WorkSessionValidator(clock))

        useCase(
            WorkSessionDraft(
                id = 7,
                date = hoje.minusDays(3),
                platform = Platform.UBER,
                rides = 12,
                revenue = Money.of(250, 0),
                onlineTime = WorkDuration.of(5, 0),
                distanceKm = 100,
            ),
        )

        // Corrigir o valor de um dia nao muda quando o lancamento entrou.
        val atualizada = repositoryComSessao.current.single()
        assertEquals(Instant.parse("2024-01-15T08:00:00Z"), atualizada.createdAt)
        assertEquals(Money.of(250, 0), atualizada.revenue)
    }

    @Test
    fun `edicao de sessao ja excluida insere uma nova em vez de perder o dado`() = runTest {
        val result = saveSession(validDraft.copy(id = 999))

        assertTrue(result is SaveWorkSessionResult.Success)
        assertEquals(1, repository.current.size)
    }

    @Test
    fun `campos em branco sao rejeitados antes de tocar o repositorio`() = runTest {
        val result = saveSession(
            WorkSessionDraft(date = hoje, platform = Platform.UBER),
        )

        assertTrue(result is SaveWorkSessionResult.Invalid)
        assertTrue(repository.current.isEmpty())
    }

    @Test
    fun `sessao com tudo preenchido em zero e rejeitada`() = runTest {
        // Tudo zero e um dia que nao aconteceu: nao informa nada e poluiria o
        // historico.
        val result = saveSession(
            validDraft.copy(
                revenue = Money.ZERO,
                rides = 0,
                onlineTime = WorkDuration.ZERO,
                distanceKm = 0,
            ),
        )

        assertTrue(result is SaveWorkSessionResult.Invalid)
        assertTrue(repository.current.isEmpty())
    }
}
