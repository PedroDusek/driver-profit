package com.driverpro.maintenance.presentation

import androidx.annotation.StringRes
import com.driverpro.R
import com.driverpro.expenses.presentation.ExpenseLabels
import com.driverpro.maintenance.domain.MaintenanceItem
import com.driverpro.maintenance.domain.MaintenanceStatus
import com.driverpro.maintenance.domain.MaintenanceValidationError

/** Tradução dos enums de manutenção preventiva para textos da interface. */
object MaintenanceLabels {

    /**
     * O item é nomeado pela categoria de despesa correspondente.
     *
     * "Óleo" é a mesma coisa no lançamento e no alerta — dois textos diferentes
     * para o mesmo item fariam o motorista duvidar se são a mesma coisa.
     */
    @StringRes
    fun item(value: MaintenanceItem): Int = ExpenseLabels.maintenance(value.category)

    @StringRes
    fun status(value: MaintenanceStatus): Int = when (value) {
        MaintenanceStatus.OK -> R.string.maintenance_status_ok
        MaintenanceStatus.DUE_SOON -> R.string.maintenance_status_due_soon
        MaintenanceStatus.OVERDUE -> R.string.maintenance_status_overdue
        MaintenanceStatus.UNKNOWN -> R.string.maintenance_status_unknown
    }

    @StringRes
    fun error(value: MaintenanceValidationError): Int = when (value) {
        MaintenanceValidationError.REQUIRED -> R.string.error_required
        MaintenanceValidationError.INTERVAL_OUT_OF_RANGE -> R.string.error_interval_out_of_range
    }
}
