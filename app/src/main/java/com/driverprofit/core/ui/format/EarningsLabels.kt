package com.driverprofit.core.ui.format

import androidx.annotation.StringRes
import com.driverprofit.R
import com.driverprofit.domain.model.Platform
import com.driverprofit.domain.model.WorkSessionField
import com.driverprofit.domain.model.WorkSessionValidationError

/**
 * Tradução dos enums de ganhos para textos da interface.
 *
 * Mesma divisão do resto do projeto: o domínio devolve o motivo do erro, a
 * apresentação escolhe a frase.
 */
object EarningsLabels {

    @StringRes
    fun platform(value: Platform): Int = when (value) {
        Platform.UBER -> R.string.platform_uber
        Platform.NINETY_NINE -> R.string.platform_99
        Platform.INDRIVE -> R.string.platform_indrive
        Platform.OTHER -> R.string.platform_other
    }

    @StringRes
    fun field(value: WorkSessionField): Int = when (value) {
        WorkSessionField.DATE -> R.string.session_date
        WorkSessionField.PLATFORM -> R.string.session_platform
        WorkSessionField.RIDES -> R.string.session_rides
        WorkSessionField.REVENUE -> R.string.session_revenue
        WorkSessionField.ONLINE_TIME -> R.string.session_online_time
        WorkSessionField.DISTANCE -> R.string.session_distance
        WorkSessionField.NOTE -> R.string.session_note
    }

    @StringRes
    fun error(value: WorkSessionValidationError): Int = when (value) {
        WorkSessionValidationError.REQUIRED -> R.string.error_required
        WorkSessionValidationError.DATE_IN_FUTURE -> R.string.error_date_in_future
        WorkSessionValidationError.NEGATIVE -> R.string.error_negative
        WorkSessionValidationError.ONLINE_TIME_TOO_LONG -> R.string.error_online_time_too_long
        WorkSessionValidationError.NOTE_TOO_LONG -> R.string.error_note_too_long
        WorkSessionValidationError.EMPTY_SESSION -> R.string.error_empty_session
    }
}
