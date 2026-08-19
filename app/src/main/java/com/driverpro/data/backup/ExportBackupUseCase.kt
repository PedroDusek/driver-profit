package com.driverpro.data.backup

import android.content.Context
import android.net.Uri
import com.driverpro.data.local.database.DriverProDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Resultado de uma tentativa de exportar o backup. */
sealed interface ExportBackupResult {
    data object Success : ExportBackupResult
    data class Failure(val reason: BackupError) : ExportBackupResult
}

/**
 * Exporta o banco para um arquivo que o motorista escolhe, vê e guarda
 * (v0.13.0) — a ponte manual que falta entre o Auto Backup invisível do
 * Android e a nuvem da v2.0 (PRD §47).
 *
 * O arquivo exportado é uma **cópia crua do banco Room**, não um formato
 * novo: ele já é auto-descritivo (schema e versão inclusos via
 * `PRAGMA user_version`), e importá-lo de volta reaproveita as migrações que
 * já existem — não precisa de nenhum código de (des)serialização por tabela.
 *
 * Toca `Context` e SQLite bruto de propósito — ver a entrada correspondente
 * em `docs/ARCHITECTURE.md`. A funcionalidade inteira é infraestrutura; não
 * há regra de negócio para isolar em domínio puro.
 */
class ExportBackupUseCase(
    private val context: Context,
    private val database: DriverProDatabase,
) {
    suspend operator fun invoke(destination: Uri): ExportBackupResult =
        withContext(Dispatchers.IO) {
            try {
                // Força as escritas do WAL para o .db principal. Sem isto a
                // cópia pode levar um SQLite válido e quase vazio — o mesmo
                // bug que o backup_rules.xml documenta e a v0.10.1 corrigiu
                // para o Auto Backup.
                database.openHelper.writableDatabase
                    .query("PRAGMA wal_checkpoint(FULL)")
                    .use { it.moveToFirst() }

                val source = context.getDatabasePath(DriverProDatabase.NAME)
                val output = context.contentResolver.openOutputStream(destination)
                    ?: return@withContext ExportBackupResult.Failure(BackupError.IO_ERROR)

                output.use { out -> source.inputStream().use { it.copyTo(out) } }
                ExportBackupResult.Success
            } catch (e: java.io.IOException) {
                ExportBackupResult.Failure(BackupError.IO_ERROR)
            }
        }
}
