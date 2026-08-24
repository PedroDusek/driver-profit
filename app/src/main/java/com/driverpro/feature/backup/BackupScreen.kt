package com.driverpro.feature.backup

import android.os.Process
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.driverpro.R
import com.driverpro.core.di.DriverProViewModelFactory
import com.driverpro.core.ui.format.BackupLabels
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Exportar e importar arquivo (v0.13.0).
 *
 * O backup que o motorista **vê, guarda e leva para outro aparelho** — ao
 * contrário do Auto Backup do Android, que é invisível e depende de
 * configuração do sistema (PRD §47).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BackupViewModel = viewModel(factory = DriverProViewModelFactory.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val suggestedName = remember {
        "driverpro-${LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}.backup"
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri -> uri?.let(viewModel::onExportRequested) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::onImportPicked) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.backup_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.backup_export_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { exportLauncher.launch(suggestedName) },
                        enabled = !uiState.isBusy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.backup_action_export))
                    }
                }
            }

            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.backup_import_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("*/*")) },
                        enabled = !uiState.isBusy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.backup_action_import))
                    }
                }
            }

            if (uiState.isBusy) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
            }
        }
    }

    uiState.pendingImportUri?.let {
        AlertDialog(
            onDismissRequest = viewModel::onImportDismissed,
            title = { Text(stringResource(R.string.backup_import_confirm_title)) },
            text = { Text(stringResource(R.string.backup_import_confirm_message)) },
            confirmButton = {
                TextButton(onClick = viewModel::onImportConfirmed) {
                    Text(stringResource(R.string.backup_action_import))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onImportDismissed) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    uiState.message?.let { message ->
        if (uiState.importCompleted) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(stringResource(R.string.backup_import_success_title)) },
                text = { Text(stringResource(R.string.backup_import_success_message)) },
                confirmButton = {
                    TextButton(onClick = { Process.killProcess(Process.myPid()) }) {
                        Text(stringResource(R.string.backup_action_close_app))
                    }
                },
            )
        } else {
            AlertDialog(
                onDismissRequest = viewModel::onMessageShown,
                title = { Text(stringResource(BackupLabels.title(message))) },
                text = { Text(stringResource(BackupLabels.message(message))) },
                confirmButton = {
                    TextButton(onClick = viewModel::onMessageShown) {
                        Text(stringResource(R.string.action_confirm))
                    }
                },
            )
        }
    }
}
