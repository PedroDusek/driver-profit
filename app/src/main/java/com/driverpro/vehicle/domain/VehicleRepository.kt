package com.driverpro.vehicle.domain

import com.driverpro.vehicle.domain.Vehicle
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de acesso a veículos.
 *
 * A interface vive no domínio e a implementação em `data.repository`: o
 * domínio não conhece Room, e trocar a fonte de dados no futuro (backup,
 * sincronização) não obriga a mexer em regra de negócio.
 */
interface VehicleRepository {

    /** Emite a lista completa a cada alteração no banco. */
    fun observeVehicles(): Flow<List<Vehicle>>

    fun observeVehicle(id: Long): Flow<Vehicle?>

    suspend fun getVehicle(id: Long): Vehicle?

    /** Quantidade de veículos cadastrados — usado para decidir a tela inicial. */
    suspend fun countVehicles(): Int

    /** Persiste um novo veículo e devolve o id gerado. */
    suspend fun addVehicle(vehicle: Vehicle): Long

    suspend fun updateVehicle(vehicle: Vehicle)

    suspend fun deleteVehicle(id: Long)

    /**
     * Marca [id] como o veículo atual, e nenhum outro (v0.12.0).
     *
     * Troca atômica: entre limpar o atual anterior e marcar o novo não existe
     * um instante sem nenhum veículo atual visível a outra consulta.
     */
    suspend fun setCurrent(id: Long)

    /**
     * Garante que, havendo veículo cadastrado, exista um atual.
     *
     * Sem efeito se já existir um atual ou se não houver veículo nenhum.
     * Chamado depois de excluir um veículo: se o excluído era o atual, o mais
     * antigo dos que sobraram assume — o formulário de ganhos e despesas
     * depende de sempre haver um veículo atual quando existe pelo menos um.
     */
    suspend fun promoteOldestToCurrentIfNone()
}
