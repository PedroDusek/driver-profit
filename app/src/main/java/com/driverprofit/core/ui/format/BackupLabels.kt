package com.driverprofit.core.ui.format

import androidx.annotation.StringRes
import com.driverprofit.R
import com.driverprofit.data.backup.BackupError
import com.driverprofit.feature.backup.BackupMessage

/** Tradução de [BackupMessage] para textos da interface. */
object BackupLabels {

    @StringRes
    fun title(message: BackupMessage): Int = when (message) {
        is BackupMessage.ExportSuccess -> R.string.backup_export_success_title
        is BackupMessage.ImportSuccess -> R.string.backup_import_success_title
        is BackupMessage.ExportFailed -> R.string.backup_export_failed_title
        is BackupMessage.ImportFailed -> R.string.backup_import_failed_title
    }

    @StringRes
    fun message(message: BackupMessage): Int = when (message) {
        is BackupMessage.ExportSuccess -> R.string.backup_export_success_message
        is BackupMessage.ImportSuccess -> R.string.backup_import_success_message
        is BackupMessage.ExportFailed -> error(message.reason)
        is BackupMessage.ImportFailed -> error(message.reason)
    }

    @StringRes
    private fun error(reason: BackupError): Int = when (reason) {
        BackupError.INVALID_FILE -> R.string.backup_error_invalid_file
        BackupError.NEWER_APP_VERSION -> R.string.backup_error_newer_version
        BackupError.IO_ERROR -> R.string.backup_error_io
    }
}
