package com.driverpro.data.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.net.Uri
import com.driverpro.core.database.DriverProDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/** Resultado de uma tentativa de importar um backup. */
sealed interface ImportBackupResult {
    data object Success : ImportBackupResult
    data class Rejected(val reason: BackupError) : ImportBackupResult
}

/**
 * Importa um backup escolhido pelo motorista, substituindo o banco atual
 * (v0.13.0).
 *
 * **Substitui tudo — não existe "mesclar".** Um swap de arquivo SQLite não
 * tem como reconciliar com o banco atual; a tela chama isto só depois de o
 * motorista confirmar um aviso explícito.
 *
 * O arquivo escolhido é validado **antes** de tocar no banco vivo: copiado
 * para um temporário, aberto só leitura, conferido contra
 * `DriverProDatabase.VERSION` e a presença da tabela `vehicles`. Um
 * arquivo de versão igual ou mais antiga é aceito sem precisar de código de
 * migração novo — `Migrations.ALL`, já cadastradas em `AppContainer`, rodam
 * sozinhas na próxima vez que o Room abrir o arquivo, no próximo lançamento
 * do app.
 *
 * Depois de trocar o arquivo, `database` é fechado e não deve ser usado de
 * novo neste processo — a tela pede para o motorista fechar e reabrir o app.
 * Ver a decisão registrada em `docs/ARCHITECTURE.md`: reiniciar o processo
 * sozinho é mecanismo frágil entre fabricantes, e não vale o risco para uma
 * tela que se abre uma vez a cada troca de aparelho.
 */
class ImportBackupUseCase(
    private val context: Context,
    private val database: DriverProDatabase,
) {
    suspend operator fun invoke(source: Uri): ImportBackupResult =
        withContext(Dispatchers.IO) {
            val temp = File(context.cacheDir, "import-${System.currentTimeMillis()}.db")
            try {
                val input = context.contentResolver.openInputStream(source)
                    ?: return@withContext ImportBackupResult.Rejected(BackupError.IO_ERROR)
                input.use { stream -> temp.outputStream().use { stream.copyTo(it) } }

                val rejection = validate(temp)
                if (rejection != null) return@withContext ImportBackupResult.Rejected(rejection)

                database.close()
                val target = context.getDatabasePath(DriverProDatabase.NAME)
                temp.copyTo(target, overwrite = true)
                // O WAL antigo não pertence ao banco novo; deixá-lo para trás
                // reaplicaria transações erradas na próxima abertura.
                File("${target.path}-wal").delete()
                File("${target.path}-shm").delete()
                ImportBackupResult.Success
            } catch (e: IOException) {
                ImportBackupResult.Rejected(BackupError.IO_ERROR)
            } finally {
                temp.delete()
            }
        }

    /**
     * `null` quando o arquivo é aceitável; o motivo da rejeição caso
     * contrário.
     *
     * SQLite não recusa `openDatabase` num arquivo que não é banco nenhum —
     * o erro só aparece na primeira consulta de verdade
     * (`SQLiteDatabaseCorruptException`, que é uma `SQLiteException`). Por
     * isso o `try` cobre abrir **e** consultar, não só abrir.
     */
    private fun validate(file: File): BackupError? = try {
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use {
            val hasVehiclesTable = it.rawQuery(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'vehicles'",
                null,
            ).use { cursor -> cursor.moveToFirst() }
            if (!hasVehiclesTable) return@use BackupError.INVALID_FILE

            val fileVersion = it.version
            if (fileVersion > DriverProDatabase.VERSION) return@use BackupError.NEWER_APP_VERSION

            null
        }
    } catch (e: SQLiteException) {
        BackupError.INVALID_FILE
    }
}
