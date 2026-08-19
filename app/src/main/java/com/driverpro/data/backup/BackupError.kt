package com.driverpro.data.backup

/**
 * Motivo pelo qual uma exportação ou importação de backup falhou.
 *
 * Segue o mesmo padrão de `VehicleValidationError`/`ExpenseValidationError`:
 * o motivo, não a frase — quem traduz para texto é a camada de apresentação.
 */
enum class BackupError {
    /** O arquivo escolhido não é um banco deste app (ou está corrompido). */
    INVALID_FILE,

    /** O arquivo é de uma versão do app mais nova do que a instalada. */
    NEWER_APP_VERSION,

    /** Falha de leitura/escrita — cartão cheio, permissão negada, etc. */
    IO_ERROR,
}
