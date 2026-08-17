package com.driverpro.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * Item de manutenção acompanhado por quilometragem (PRD §18, ROADMAP v0.9.0).
 *
 * Enum próprio, e não [MaintenanceCategory] inteira: das treze categorias que o
 * lançamento aceita, só estas cinco têm intervalo de troca previsível o
 * bastante para o app afirmar alguma coisa. Funilaria e elétrica acontecem
 * quando quebram, não a cada tantos quilômetros — monitorá-las produziria
 * alerta sem significado.
 *
 * [category] é o elo com o histórico: é o lançamento de manutenção daquela
 * categoria que estabelece o marco de onde a contagem recomeça.
 *
 * Os intervalos são **ponto de partida, não verdade**. Variam com motor, óleo,
 * uso e fabricante, e por isso o motorista pode alterar cada um. São valores
 * conservadores de uso urbano intenso, que é o regime de quem roda em
 * aplicativo.
 */
enum class MaintenanceItem(
    val category: MaintenanceCategory,
    val defaultIntervalKm: Long,
) {
    OIL(MaintenanceCategory.OIL, 10_000L),
    FILTERS(MaintenanceCategory.FILTERS, 10_000L),
    BRAKES(MaintenanceCategory.BRAKES, 30_000L),
    TIRES(MaintenanceCategory.TIRES, 40_000L),
    INSPECTION(MaintenanceCategory.INSPECTION, 10_000L),
    ;

    companion object {
        /** Menor intervalo aceitável — abaixo disso é engano de digitação. */
        const val MIN_INTERVAL_KM: Long = 500L

        /** Teto do intervalo, pelo mesmo motivo do teto do odômetro. */
        const val MAX_INTERVAL_KM: Long = 200_000L
    }
}

/**
 * Intervalo que o motorista definiu para um item, sobrepondo o padrão.
 *
 * **A ausência de registro significa "usar o padrão".** Só vira linha no banco
 * o que ele mudou de fato: intervalo diferente ou item silenciado. Isso mantém
 * a tabela pequena, faz um veículo novo já nascer acompanhado, e evita a
 * pergunta "estes valores são meus ou do app?" — quem tem linha, ele decidiu.
 */
data class MaintenanceSchedule(
    val id: Long = UNSAVED_ID,
    val vehicleId: Long,
    val item: MaintenanceItem,
    val intervalKm: Long,
    /** Falso quando o motorista desligou o acompanhamento deste item. */
    val monitored: Boolean = true,
    val createdAt: Instant,
) {
    companion object {
        const val UNSAVED_ID: Long = 0L
    }
}

/** Situação de um item acompanhado. */
enum class MaintenanceStatus {
    /** Ainda longe do intervalo. */
    OK,

    /** Dentro dos últimos 10% do intervalo. */
    DUE_SOON,

    /** Passou do intervalo. */
    OVERDUE,

    /**
     * O app não sabe, e diz que não sabe.
     *
     * Nunca houve lançamento de manutenção desta categoria com leitura de
     * odômetro, então não existe marco de onde contar. Sem marco não há
     * afirmação possível — nem "está em dia", que seria a mentira mais cara
     * das duas.
     */
    UNKNOWN,
}

/**
 * O que o app sabe sobre um item de manutenção agora.
 *
 * @param lastServiceKm odômetro do último serviço lançado nesta categoria.
 *   `null` quando não há marco, o que força [status] a [MaintenanceStatus.UNKNOWN].
 * @param traveledKm quilômetros rodados desde o marco, já considerando o piso
 *   por combustível comprado.
 * @param distanceIsImplied verdadeiro quando o piso por combustível superou a
 *   diferença de odômetro — ou seja, o painel está desatualizado e o número
 *   exibido é mínimo, não medido.
 */
data class MaintenanceAlert(
    val item: MaintenanceItem,
    val intervalKm: Long,
    val lastServiceKm: Long?,
    val lastServiceDate: LocalDate?,
    val traveledKm: Long?,
    val status: MaintenanceStatus,
    val distanceIsImplied: Boolean = false,
    /**
     * Falso quando o motorista desligou este item.
     *
     * Ele continua no resultado, e não some: a tela precisa oferecer o caminho
     * de volta, e um item que desaparece ao ser desligado não tem como ser
     * religado.
     */
    val monitored: Boolean = true,
) {
    /**
     * Quilometragem em que o serviço vence — **o número que a tela exibe**.
     *
     * É a soma de dois fatos: a leitura da última troca, lançada com a nota, e
     * o intervalo, que o motorista definiu. Não depende de onde o carro está
     * agora, então é exato mesmo com o painel atrasado.
     *
     * Essa é a diferença entre dizer "faltam 600 km" e "a próxima é aos
     * 110.000 km". A primeira é uma afirmação sobre o presente, que é
     * justamente o que o app não sabe com certeza — e um motorista que lê 600
     * quando faltam 550 para de confiar no aplicativo. A segunda ele confere
     * contra o próprio painel, e ela nunca está errada.
     *
     * `null` quando não há marco, o mesmo caso de [MaintenanceStatus.UNKNOWN].
     */
    val nextServiceKm: Long?
        get() = lastServiceKm?.plus(intervalKm)

    /**
     * Quanto falta para o intervalo. Negativo significa atraso.
     *
     * **Não é exibido**, de propósito — ver [nextServiceKm]. Serve para
     * ordenar por urgência e para decidir o estado, onde um erro de algumas
     * centenas de quilômetros adianta ou atrasa um lembrete discreto, em vez
     * de imprimir um número errado na tela.
     */
    val remainingKm: Long?
        get() = traveledKm?.let { intervalKm - it }

    /** Itens que merecem aparecer no dashboard. */
    val needsAttention: Boolean
        get() = monitored &&
            (status == MaintenanceStatus.OVERDUE || status == MaintenanceStatus.DUE_SOON)
}

/** Um veículo e a situação de cada item acompanhado dele. */
data class VehicleMaintenance(
    val vehicle: Vehicle,
    val alerts: List<MaintenanceAlert>,
) {
    val needingAttention: List<MaintenanceAlert>
        get() = alerts.filter { it.needsAttention }
}
