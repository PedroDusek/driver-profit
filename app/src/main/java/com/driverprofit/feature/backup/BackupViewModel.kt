package com.driverprofit.feature.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driverprofit.data.backup.BackupError
import com.driverprofit.data.backup.ExportBackupResult
import com.driverprofit.data.backup.ExportBackupUseCase
import com.driverprofit.data.backup.ImportBackupResult
import com.driverprofit.data.backup.ImportBackupUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** O que a tela tem para dizer depois de uma tentativa de exportar ou importar. */
sealed interface BackupMessage {
    data object ExportSuccess : BackupMessage
    data object ImportSuccess : BackupMessage
    data class ExportFailed(val reason: BackupError) : BackupMessage
    data class ImportFailed(val reason: BackupError) : BackupMessage
}

data class BackupUiState(
    val isBusy: Boolean = false,
    /** Uri escolhido para importar, aguardando confirmação — a operação é destrutiva. */
    val pendingImportUri: Uri? = null,
    val message: BackupMessage? = null,
) {
    /** `true` quando a importação já terminou e o app precisa ser reaberto. */
    val importCompleted: Boolean get() = message is BackupMessage.ImportSuccess
}

/**
 * Exportar e importar arquivo (v0.13.0).
 *
 * Sem lista para observar — ao contrário das outras telas do app, o estado
 * aqui é só o andamento da operação em curso, não um recorte do banco.
 */
class BackupViewModel(
    private val exportBackup: ExportBackupUseCase,
    private val importBackup: ImportBackupUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun onExportRequested(destination: Uri) {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            val result = exportBackup(destination)
            _uiState.update { state ->
                state.copy(
                    isBusy = false,
                    message = when (result) {
                        is ExportBackupResult.Success -> BackupMessage.ExportSuccess
                        is ExportBackupResult.Failure -> BackupMessage.ExportFailed(result.reason)
                    },
                )
            }
        }
    }

    /** Só guarda o arquivo escolhido — [onImportConfirmed] é quem de fato importa. */
    fun onImportPicked(source: Uri) {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(pendingImportUri = source) }
    }

    fun onImportDismissed() {
        _uiState.update { it.copy(pendingImportUri = null) }
    }

    fun onImportConfirmed() {
        val source = _uiState.value.pendingImportUri ?: return
        _uiState.update { it.copy(isBusy = true, pendingImportUri = null) }
        viewModelScope.launch {
            val result = importBackup(source)
            _uiState.update { state ->
                state.copy(
                    isBusy = false,
                    message = when (result) {
                        is ImportBackupResult.Success -> BackupMessage.ImportSuccess
                        is ImportBackupResult.Rejected -> BackupMessage.ImportFailed(result.reason)
                    },
                )
            }
        }
    }

    /** Consumido pela UI depois de mostrar a mensagem, para não mostrar duas vezes. */
    fun onMessageShown() {
        // Sucesso de importação não some sozinho: o app já fechou o banco e
        // só o botão de fechar o processo pode tirar essa mensagem da tela.
        if (_uiState.value.message is BackupMessage.ImportSuccess) return
        _uiState.update { it.copy(message = null) }
    }
}
