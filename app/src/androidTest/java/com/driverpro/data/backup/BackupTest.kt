package com.driverpro.data.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.driverpro.core.database.DriverProDatabase
import com.driverpro.core.database.Migrations
import com.driverpro.vehicle.data.VehicleEntity
import com.driverpro.vehicle.domain.VehicleFuel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant

/**
 * Testes de exportar/importar backup (v0.13.0).
 *
 * Ao contrário dos outros testes de banco, estes precisam de um arquivo de
 * verdade no disco — `Room.inMemoryDatabaseBuilder` não serve, porque
 * exportar e importar são operações sobre o **arquivo**, não sobre a conexão.
 * Por isso usam o mesmo caminho que `AppContainer` usa em produção
 * (`context.getDatabasePath(DriverProDatabase.NAME)`), como
 * `ExportBackupUseCase`/`ImportBackupUseCase` fazem — e limpam esse arquivo
 * antes e depois de cada teste.
 */
@RunWith(AndroidJUnit4::class)
class BackupTest {

    private lateinit var context: Context
    private lateinit var database: DriverProDatabase
    private lateinit var dbFile: File

    @Before
    fun createDatabase() {
        context = ApplicationProvider.getApplicationContext()
        dbFile = context.getDatabasePath(DriverProDatabase.NAME)
        deleteDatabaseFiles()
        database = Room.databaseBuilder(context, DriverProDatabase::class.java, DriverProDatabase.NAME)
            .addMigrations(*Migrations.ALL)
            .build()
    }

    @After
    fun closeDatabase() {
        runCatching { database.close() }
        deleteDatabaseFiles()
    }

    private fun deleteDatabaseFiles() {
        dbFile.delete()
        File("${dbFile.path}-wal").delete()
        File("${dbFile.path}-shm").delete()
    }

    private suspend fun insertVehicle(name: String) {
        database.vehicleDao().insert(
            VehicleEntity(name = name, fuel = VehicleFuel.FLEX, createdAt = Instant.EPOCH),
        )
    }

    @Test
    fun exportaEArquivoContemOsDados() = runTest {
        insertVehicle("Onix branco")
        val exportFile = File(context.cacheDir, "export-test-${System.currentTimeMillis()}.backup")

        val result = ExportBackupUseCase(context, database).invoke(Uri.fromFile(exportFile))

        assertTrue(result is ExportBackupResult.Success)
        assertTrue("arquivo exportado deveria ter conteúdo", exportFile.length() > 0)

        val exported = SQLiteDatabase.openDatabase(exportFile.path, null, SQLiteDatabase.OPEN_READONLY)
        exported.rawQuery("SELECT name FROM vehicles", null).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Onix branco", cursor.getString(0))
        }
        exported.close()
        exportFile.delete()
    }

    @Test
    fun importaArquivoDeVersaoMaisNovaERejeitadoSemTocarNoBancoVivo() = runTest {
        insertVehicle("Onix branco")
        val futureFile = File(context.cacheDir, "future-${System.currentTimeMillis()}.backup")
        // Um banco vazio, mas com a tabela vehicles e uma versão inalcançável —
        // isola exatamente a checagem de versão, sem depender do schema real.
        SQLiteDatabase.openOrCreateDatabase(futureFile, null).use { db ->
            db.execSQL("CREATE TABLE vehicles (id INTEGER PRIMARY KEY)")
            db.version = DriverProDatabase.VERSION + 1
        }

        val result = ImportBackupUseCase(context, database).invoke(Uri.fromFile(futureFile))

        assertTrue(result is ImportBackupResult.Rejected)
        assertEquals(BackupError.NEWER_APP_VERSION, (result as ImportBackupResult.Rejected).reason)
        // O banco vivo continua intacto — a rejeição aconteceu antes de qualquer troca.
        assertEquals(1, database.vehicleDao().count())
        futureFile.delete()
    }

    @Test
    fun importaArquivoQueNaoEBancoDeDadosERejeitado() = runTest {
        val garbage = File(context.cacheDir, "garbage-${System.currentTimeMillis()}.backup")
        garbage.writeText("isto não é um banco SQLite")

        val result = ImportBackupUseCase(context, database).invoke(Uri.fromFile(garbage))

        assertTrue(result is ImportBackupResult.Rejected)
        assertEquals(BackupError.INVALID_FILE, (result as ImportBackupResult.Rejected).reason)
        garbage.delete()
    }

    @Test
    fun importaArquivoSemTabelaVehiclesERejeitado() = runTest {
        val notABackup = File(context.cacheDir, "not-a-backup-${System.currentTimeMillis()}.backup")
        SQLiteDatabase.openOrCreateDatabase(notABackup, null).use { db ->
            db.execSQL("CREATE TABLE outra_coisa (id INTEGER PRIMARY KEY)")
        }

        val result = ImportBackupUseCase(context, database).invoke(Uri.fromFile(notABackup))

        assertTrue(result is ImportBackupResult.Rejected)
        assertEquals(BackupError.INVALID_FILE, (result as ImportBackupResult.Rejected).reason)
        notABackup.delete()
    }

    @Test
    fun importaBackupValidoTrocaOArquivoNoDisco() = runTest {
        insertVehicle("Carro original")
        val exportFile = File(context.cacheDir, "roundtrip-${System.currentTimeMillis()}.backup")
        val exportResult = ExportBackupUseCase(context, database).invoke(Uri.fromFile(exportFile))
        assertTrue(exportResult is ExportBackupResult.Success)

        // Muda o banco vivo depois do backup, para provar que importar
        // descarta essa mudança e volta ao estado do arquivo.
        insertVehicle("Carro adicionado depois do backup")
        assertEquals(2, database.vehicleDao().count())

        val importResult = ImportBackupUseCase(context, database).invoke(Uri.fromFile(exportFile))
        assertTrue(importResult is ImportBackupResult.Success)

        // `database` foi fechado pelo import — reabre um Room novo apontando
        // para o mesmo arquivo, como o app faz no próximo lançamento.
        val reopened = Room.databaseBuilder(context, DriverProDatabase::class.java, DriverProDatabase.NAME)
            .addMigrations(*Migrations.ALL)
            .build()
        assertEquals(1, reopened.vehicleDao().count())
        assertEquals("Carro original", reopened.vehicleDao().findById(1L)?.name)
        reopened.close()
        exportFile.delete()
    }
}
