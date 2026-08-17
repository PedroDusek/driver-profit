package com.driverprofit.domain.repository

import com.driverprofit.domain.model.DateRange
import com.driverprofit.domain.model.Expense
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Contrato de acesso às despesas.
 *
 * A interface vive no domínio e a implementação em `data.repository`.
 */
interface ExpenseRepository {

    /** Emite todas as despesas, da mais recente para a mais antiga. */
    fun observeExpenses(): Flow<List<Expense>>

    /**
     * Despesas de um período, inclusive nas duas pontas.
     *
     * Base dos filtros do dashboard (PRD §20) e do custo por quilômetro.
     */
    fun observeExpensesBetween(start: LocalDate, end: LocalDate): Flow<List<Expense>>

    /**
     * Despesas cuja competência encosta no período (PRD §22).
     *
     * Sobreposição, e não contenção — um custo fixo se refere a um intervalo
     * que costuma ser bem maior que o dia em que foi pago.
     */
    fun observeAccruedBetween(start: LocalDate, end: LocalDate): Flow<List<Expense>>

    /**
     * Maior leitura de odômetro registrada para um veículo, ou `null` quando
     * ainda não há nenhuma.
     *
     * É a quilometragem atual do carro segundo o app — a base do consumo
     * estimado, do quilômetro pessoal e dos alertas de manutenção.
     */
    fun observeLatestOdometer(vehicleId: Long): Flow<Long?>

    /** Última leitura de cada veículo, indexada por id. */
    fun observeOdometers(): Flow<Map<Long, Long>>

    /**
     * Quilômetros que o painel diz que o carro andou durante [period].
     *
     * Parte da última leitura **anterior** ao período, para não perder o
     * trecho entre ela e a primeira leitura de dentro. Sem leitura anterior,
     * usa a menor de dentro como linha de partida.
     *
     * `null` quando não há leituras suficientes para afirmar qualquer coisa.
     */
    suspend fun odometerDistanceIn(vehicleId: Long, period: DateRange): Long?

    suspend fun getExpense(id: Long): Expense?

    suspend fun addExpense(expense: Expense): Long

    suspend fun updateExpense(expense: Expense)

    suspend fun deleteExpense(id: Long)
}
