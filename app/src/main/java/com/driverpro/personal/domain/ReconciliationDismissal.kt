package com.driverpro.personal.domain

import com.driverpro.core.domain.DateRange

import java.time.Instant

/**
 * Uma sobra de odômetro que o motorista aceitou deixar de fora da conta.
 *
 * Não é uso pessoal nem quilômetro de trabalho: é distância que o painel
 * registrou, que os lançamentos não explicam, e que ele decidiu não classificar.
 * Os quilômetros continuam fora de todos os totais — o custo por km fica um
 * pouco mais alto que o real, e é isso que a tela informa antes de ele decidir.
 *
 * **A dispensa guarda quanto foi dispensado, e não só o intervalo.** É o que a
 * torna válida sobre um *fato*, e não sobre um pedaço do calendário: se a sobra
 * daquela janela mudar, a dispensa deixa de descrever a situação e a pergunta
 * volta. Sem isso, um lançamento retroativo entraria calado numa janela já
 * resolvida e ninguém saberia.
 */
data class ReconciliationDismissal(
    val id: Long = UNSAVED_ID,
    val vehicleId: Long,
    val window: DateRange,
    /** Quantos quilômetros foram aceitos fora da conta. Sempre positivo. */
    val dismissedKm: Long,
    val createdAt: Instant,
) {
    companion object {
        const val UNSAVED_ID: Long = 0L
    }
}
