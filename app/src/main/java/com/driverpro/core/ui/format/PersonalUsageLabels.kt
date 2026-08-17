package com.driverpro.core.ui.format

import androidx.annotation.StringRes
import com.driverpro.R
import com.driverpro.domain.model.PersonalUsageValidationError

/** Tradução dos erros de uso pessoal para textos da interface. */
object PersonalUsageLabels {

    @StringRes
    fun error(value: PersonalUsageValidationError): Int = when (value) {
        PersonalUsageValidationError.REQUIRED -> R.string.error_required
        PersonalUsageValidationError.DATE_IN_FUTURE -> R.string.error_date_in_future
        PersonalUsageValidationError.END_BEFORE_START -> R.string.error_end_before_start
        PersonalUsageValidationError.DISTANCE_OUT_OF_RANGE -> R.string.error_distance_out_of_range
        PersonalUsageValidationError.TEXT_TOO_LONG -> R.string.error_text_too_long
    }
}
