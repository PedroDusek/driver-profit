package com.driverprofit.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.driverprofit.core.common.Money
import com.driverprofit.core.common.Quantity
import com.driverprofit.domain.model.ChargingLocation
import com.driverprofit.domain.model.Expense
import com.driverprofit.domain.model.ExpenseCategory
import com.driverprofit.domain.model.ExpenseDetail
import com.driverprofit.domain.model.FuelType
import com.driverprofit.domain.model.MaintenanceCategory
import java.time.Instant
import java.time.LocalDate

/**
 * Despesa no banco.
 *
 * **Uma tabela só**, com as colunas de detalhe anuláveis, em vez de uma tabela
 * por natureza de despesa (PRD §17: nada de estrutura rígida que impeça
 * categorias novas). Acrescentar "pedágio" é uma constante de enum; com
 * tabelas separadas seria uma migração.
 *
 * O preço disso são colunas nulas em boa parte das linhas — barato — e a
 * coerência entre categoria e detalhe passar a ser responsabilidade do
 * domínio, não do schema. `ExpenseValidator` e o `sealed ExpenseDetail`
 * cuidam disso.
 *
 * `ON DELETE SET NULL` no veículo: excluir um carro não pode apagar o
 * histórico financeiro. A despesa fica órfã, mas continua somando no
 * dashboard — que é o comportamento correto para quem trocou de veículo.
 */
@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicle_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index(value = ["date"]), Index(value = ["vehicle_id"])],
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "vehicle_id")
    val vehicleId: Long? = null,

    /** Epoch day. */
    @ColumnInfo(name = "date")
    val date: LocalDate,

    @ColumnInfo(name = "category")
    val category: ExpenseCategory,

    /** Centavos (PRD §26). Zero é válido: recarga gratuita. */
    @ColumnInfo(name = "amount_cents")
    val amountCents: Long,

    @ColumnInfo(name = "description")
    val description: String,

    // --- Detalhe: preenchido conforme a categoria ---

    @ColumnInfo(name = "fuel_type")
    val fuelType: FuelType? = null,

    /** Milésimos da unidade indicada por [fuelType] ou por kWh na recarga. */
    @ColumnInfo(name = "quantity_thousandths")
    val quantityThousandths: Long? = null,

    @ColumnInfo(name = "charging_location")
    val chargingLocation: ChargingLocation? = null,

    @ColumnInfo(name = "maintenance_category")
    val maintenanceCategory: MaintenanceCategory? = null,

    /** Posto, local de recarga ou oficina, conforme a categoria. */
    @ColumnInfo(name = "place")
    val place: String? = null,

    /**
     * Leitura do painel, em quilômetros inteiros.
     *
     * Anulável no schema mesmo sendo obrigatória no domínio para as categorias
     * ligadas ao veículo: pedágio e estacionamento não a têm, e as despesas
     * gravadas antes da v0.6.0 nunca tiveram. Exigir NOT NULL obrigaria a
     * inventar um valor na migração — e valor inventado de odômetro
     * contaminaria consumo estimado e alerta de manutenção.
     */
    @ColumnInfo(name = "odometer_km")
    val odometerKm: Long? = null,

    /** Epoch millis em UTC. */
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
)

fun ExpenseEntity.toDomain(): Expense = Expense(
    id = id,
    vehicleId = vehicleId,
    date = date,
    category = category,
    amount = Money(amountCents),
    description = description,
    detail = toDetail(),
    odometerKm = odometerKm,
    createdAt = createdAt,
)

/**
 * Reconstrói o detalhe a partir das colunas.
 *
 * Devolve `null` quando falta o que a categoria exige, em vez de estourar:
 * uma linha inconsistente — vinda de migração ou de bug antigo — deve
 * degradar para "despesa sem detalhe" e continuar somando no dashboard, nunca
 * derrubar a tela de histórico.
 */
private fun ExpenseEntity.toDetail(): ExpenseDetail? = when (category.detailKind) {
    // A quantidade é opcional, então sua ausência não impede reconstruir o
    // detalhe — só deixa o preço por unidade indisponível.
    com.driverprofit.domain.model.ExpenseDetailKind.REFUEL ->
        fuelType?.let {
            ExpenseDetail.Refuel(it, quantityThousandths?.let(::Quantity), place.orEmpty())
        }

    com.driverprofit.domain.model.ExpenseDetailKind.CHARGING ->
        chargingLocation?.let {
            ExpenseDetail.Charging(quantityThousandths?.let(::Quantity), it, place.orEmpty())
        }

    com.driverprofit.domain.model.ExpenseDetailKind.MAINTENANCE ->
        maintenanceCategory?.let { ExpenseDetail.Maintenance(it, place.orEmpty()) }

    com.driverprofit.domain.model.ExpenseDetailKind.NONE -> null
}

fun Expense.toEntity(): ExpenseEntity {
    val base = ExpenseEntity(
        id = id,
        vehicleId = vehicleId,
        date = date,
        category = category,
        amountCents = amount.cents,
        description = description,
        odometerKm = odometerKm,
        createdAt = createdAt,
    )
    return when (val detail = detail) {
        is ExpenseDetail.Refuel -> base.copy(
            fuelType = detail.fuelType,
            quantityThousandths = detail.quantity?.thousandths,
            place = detail.station,
        )
        is ExpenseDetail.Charging -> base.copy(
            quantityThousandths = detail.energy?.thousandths,
            chargingLocation = detail.location,
            place = detail.place,
        )
        is ExpenseDetail.Maintenance -> base.copy(
            maintenanceCategory = detail.category,
            place = detail.workshop,
        )
        null -> base
    }
}
