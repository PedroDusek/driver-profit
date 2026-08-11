# Banco de dados

Room sobre SQLite, local ao aparelho. Nome do arquivo: `driver_profit.db`.

**Versão atual do schema: 1**

O JSON do schema é exportado em `app/schemas/` e **é versionado no Git**. Ele é
o que torna possível escrever testes de migração de verdade.

## Entidades

### `vehicles` (v1)

Veículo do motorista. Precisa existir antes de qualquer despesa vinculada a ele.

| Coluna | Tipo SQL | Nulo | Descrição |
| --- | --- | --- | --- |
| `id` | INTEGER PK AUTOINCREMENT | não | Identificador |
| `brand` | TEXT | não | Marca |
| `model` | TEXT | não | Modelo |
| `year` | INTEGER | não | Ano |
| `initial_odometer_km` | INTEGER | não | Odômetro no cadastro, em km |
| `powertrain` | TEXT | não | `COMBUSTION` \| `HYBRID` \| `ELECTRIC` |
| `combustion_fuel` | TEXT | **sim** | `GASOLINE` \| `ETHANOL` \| `FLEX` \| `CNG` \| `FLEX_CNG` |
| `charging_capability` | TEXT | **sim** | `NONE` \| `PLUG_IN` \| `UNKNOWN` |
| `created_at` | INTEGER | não | Epoch millis (UTC) |

**Por que `combustion_fuel` e `charging_capability` são anuláveis:** um elétrico
puro não tem combustível; um carro a combustão não tem capacidade de recarga.
`null` significa "não se aplica", e não "desconhecido" — para desconhecido
existe `ChargingCapability.UNKNOWN`.

## Convenções

### Nomes

- Tabelas: `snake_case`, plural (`vehicles`, `earnings`, `expenses`)
- Colunas: `snake_case`
- Unidade no nome quando houver ambiguidade: `initial_odometer_km`,
  `total_cents`, `online_minutes`

### Tipos

| Conceito | Armazenado como | Nunca |
| --- | --- | --- |
| Dinheiro | `INTEGER` em centavos | `REAL` |
| Duração | `INTEGER` em minutos | horas decimais |
| Data (dia de trabalho) | `INTEGER` epoch day | `TEXT` `"11/08/2026"` |
| Timestamp | `INTEGER` epoch millis | `TEXT` |
| Enum | `TEXT` com o `name` | `INTEGER` com o `ordinal` |

Enum por `name` e não por `ordinal`: reordenar constantes no código não pode
mudar o significado de dados já gravados.

Data como epoch day mantém consulta por período como comparação numérica —
`WHERE date BETWEEN :start AND :end` usa índice.

## Conversores

`data/local/database/Converters.kt`:

| Kotlin | SQLite |
| --- | --- |
| `Instant` | `INTEGER` epoch millis |
| `LocalDate` | `INTEGER` epoch day |
| `VehiclePowertrain` | `TEXT` |
| `CombustionFuel` | `TEXT` |
| `ChargingCapability` | `TEXT` |

## Migrações

**`fallbackToDestructiveMigration()` é proibido.** Apagar o histórico
financeiro do motorista para resolver mudança de schema não é uma opção.

Toda alteração de schema exige, no mesmo PR:

1. Alterar a `@Entity`
2. Incrementar `DriverProfitDatabase.VERSION`
3. Escrever a `Migration(n, n+1)`
4. Escrever o teste da migração (`MigrationTestHelper` + schema exportado)
5. Atualizar este documento
6. Rodar `./gradlew testDebugUnitTest connectedDebugAndroidTest`

Um PR que altera apenas a Entity está incompleto.

### Histórico

| Versão | Quando | Alteração |
| --- | --- | --- |
| 1 | v0.1.0 | Schema inicial: `vehicles` |

### Planejado

| Versão | Alvo | Alteração prevista |
| --- | --- | --- |
| 2 | v0.3.0 | `earnings` — sessões de trabalho por plataforma |
| 3 | v0.4.0 | `expenses`, `fuel_records`, `charging_records`, `maintenance_records` |

Essas tabelas ainda **não** existem. A lista serve para orientar o desenho,
não para criar colunas antes da hora.

## Desvios registrados em relação ao PRD

### `user_id` não existe em `vehicles`

O PRD §14 lista `userId` no modelo do veículo. A coluna **não** foi criada.

Motivo: o MVP não tem login, e login está na lista de funcionalidades não
autorizadas (PRD §48) até v2.0. Uma coluna preenchida sempre com o mesmo valor
constante é peso morto que precisa ser mantido em toda query e todo teste.

Quando houver multiusuário (v2.0), ela entra por migração normal — adicionar
uma coluna com default é justamente o tipo de migração mais simples que existe.
