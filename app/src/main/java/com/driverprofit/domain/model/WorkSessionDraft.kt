package com.driverprofit.domain.model

import com.driverprofit.core.common.Money
import com.driverprofit.core.common.WorkDuration
import java.time.LocalDate

/**
 * Sessão de trabalho em preenchimento, ainda não validada.
 *
 * Campos obrigatórios são anuláveis porque "ainda não preenchido" é um estado
 * legítimo do formulário. A conversão para [WorkSession] só acontece depois
 * que `WorkSessionValidator` aprova o rascunho.
 */
data class WorkSessionDraft(
    val id: Long = WorkSession.UNSAVED_ID,
    val date: LocalDate? = null,
    val platform: Platform? = null,
    val rides: Int? = null,
    val revenue: Money? = null,
    val onlineTime: WorkDuration? = null,
    val distanceKm: Long? = null,
    val note: String = "",
) {
    /** `true` quando o rascunho representa a edição de uma sessão já salva. */
    val isEditing: Boolean get() = id != WorkSession.UNSAVED_ID
}

/** Constrói um rascunho a partir de uma sessão já persistida, para edição. */
fun WorkSession.toDraft(): WorkSessionDraft = WorkSessionDraft(
    id = id,
    date = date,
    platform = platform,
    rides = rides,
    revenue = revenue,
    onlineTime = onlineTime,
    distanceKm = distanceKm,
    note = note,
)
