# Banco de dados

Room sobre SQLite, local ao aparelho. Nome do arquivo: `driver_profit.db`.

**Versão atual do schema: 3**

O JSON do schema é exportado em `app/schemas/` e **é versionado no Git**. Ele é
o que torna possível escrever testes de migração de verdade. Os schemas
antigos são mantidos: sem o `1.json`, não há como testar a migração 1→2.

## Entidades

### `vehicles` (v2)

Veículo do motorista. Guarda apenas o mínimo necessário para identificar o
carro e calcular custo de abastecimento.

| Coluna | Tipo SQL | Nulo | Descrição |
| --- | --- | --- | --- |
| `id` | INTEGER PK AUTOINCREMENT | não | Identificador |
| `name` | TEXT | não | Como o motorista chama o carro: "Onix branco" |
| `fuel` | TEXT | não | `GASOLINE` \| `ETHANOL` \| `FLEX` \| `CNG` \| `FLEX_CNG` \| `ELECTRIC` \| `HYBRID` |
| `created_at` | INTEGER | não | Epoch millis (UTC) |

**Por que só isso:** marca, modelo e ano não entram em nenhuma conta de
rentabilidade. `fuel` é o único atributo do veículo que o cálculo consome —
ele determina a unidade de medida do abastecimento (litro, m³ ou kWh).

**Quilometragem não vive aqui.** Ela será registrada por lançamento, servindo
a controles de manutenção (troca de óleo, pneus), e não como atributo do
veículo.

### `work_sessions` (v3)

Uma sessão de trabalho: o que o motorista fez em um dia, numa plataforma
(PRD §15). Rodar na Uber e na 99 no mesmo dia são dois registros — é isso que
torna a comparação entre plataformas possível depois (PRD §16).

| Coluna | Tipo SQL | Nulo | Descrição |
| --- | --- | --- | --- |
| `id` | INTEGER PK AUTOINCREMENT | não | Identificador |
| `date` | INTEGER | não | Epoch day do dia de trabalho |
| `platform` | TEXT | não | `UBER` \| `NINETY_NINE` \| `INDRIVE` \| `OTHER` |
| `rides` | INTEGER | não | Número de corridas |
| `revenue_cents` | INTEGER | não | Valor recebido, em centavos |
| `online_minutes` | INTEGER | não | Tempo online, em minutos |
| `distance_km` | INTEGER | não | Quilômetros rodados, inteiros |
| `note` | TEXT | não | Observação; string vazia quando não informada |
| `created_at` | INTEGER | não | Epoch millis (UTC) |

**Índice:** `index_work_sessions_date` sobre `date`. Toda consulta do dashboard
filtra por período (PRD §20), e como `date` é epoch day o `BETWEEN` é
comparação numérica que usa o índice.

**Campos numéricos não são anuláveis.** Um dia sem quilometragem anotada grava
`0`, não `NULL`: para somar no dashboard, "não anotei" e "zero" dão no mesmo, e
`NULL` em coluna numérica só produziria `COALESCE` espalhado por toda query.
O que a validação impede é a sessão inteira estar zerada.

**A plataforma é gravada desde já**, mesmo sem análise por plataforma no MVP.
Adicionar uma plataforma nova é acrescentar uma constante no enum — não mexe no
banco, porque a coluna guarda o `name`.

## Convenções

### Nomes

- Tabelas: `snake_case`, plural (`vehicles`, `earnings`, `expenses`)
- Colunas: `snake_case`
- Unidade no nome quando houver ambiguidade: `total_cents`, `online_minutes`,
  `odometer_km`

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
| `VehicleFuel` | `TEXT` |
| `Platform` | `TEXT` |

## Migrações

**`fallbackToDestructiveMigration()` é proibido.** Apagar o histórico
financeiro do motorista para resolver mudança de schema não é uma opção.

As migrações vivem em `data/local/database/Migrations.kt` e são registradas no
builder em `core/di/AppContainer.kt`.

Toda alteração de schema exige, no mesmo PR:

1. Alterar a `@Entity`
2. Incrementar `DriverProfitDatabase.VERSION`
3. Escrever a `Migration(n, n+1)` e adicioná-la a `Migrations.ALL`
4. Escrever o teste da migração em `app/src/androidTest/.../MigrationTest.kt`
5. Atualizar este documento
6. Rodar `./gradlew testDebugUnitTest connectedDebugAndroidTest`

Um PR que altera apenas a Entity está incompleto.

### Histórico

| Versão | Quando | Alteração |
| --- | --- | --- |
| 1 | v0.1.0 | Schema inicial: `vehicles` com marca, modelo, ano, odômetro inicial, propulsão, combustível e capacidade de recarga |
| 2 | v0.2.1 | Cadastro simplificado: remove `brand`, `model`, `year`, `initial_odometer_km`, `powertrain` e `charging_capability`; introduz `name` e `fuel` |
| 3 | v0.3.0 | Adiciona `work_sessions` e o índice sobre `date` |

#### Migração 1 → 2

SQLite não suporta `DROP COLUMN` nas versões de Android que o app atende, então
a migração usa o padrão tabela-nova + cópia + troca.

Os dados existentes **não** são descartados:

| Coluna nova | Origem |
| --- | --- |
| `name` | `TRIM(brand \|\| ' ' \|\| model)` — é como o motorista já reconhecia o carro na lista |
| `fuel` | `ELECTRIC`/`HYBRID` quando a propulsão era essa; senão o `combustion_fuel` antigo; `FLEX` como último recurso |

`FLEX` é o padrão de último recurso porque é a configuração mais comum na frota
brasileira de aplicativo — e porque a coluna não aceita nulo.

As colunas `initial_odometer_km` e `charging_capability` são descartadas sem
destino. Isso é perda de informação assumida: o odômetro passa a ser registrado
por lançamento, e a distinção plug-in deixou de existir no modelo.

#### Migração 2 → 3

Puramente aditiva: cria `work_sessions` e seu índice. Nenhuma tabela existente
é tocada, então não há risco para os dados já gravados.

## Desvios registrados em relação ao PRD

### `user_id` não existe em `vehicles`

O PRD §14 lista `userId` no modelo do veículo. A coluna **não** foi criada.

Motivo: o MVP não tem login, e login está na lista de funcionalidades não
autorizadas (PRD §48) até v2.0. Uma coluna preenchida sempre com o mesmo valor
constante é peso morto que precisa ser mantido em toda query e todo teste.

Quando houver multiusuário (v2.0), ela entra por migração normal — adicionar
uma coluna com default é justamente o tipo de migração mais simples que existe.

### Marca, modelo, ano e odômetro inicial não existem em `vehicles`

O PRD §5 e §14 listavam esses campos. Foram removidos na v0.2.1 por decisão de
produto: nenhum deles entra em conta de rentabilidade, e o cadastro precisa ser
rápido o bastante para não desistirem dele.

### Propulsão, combustível e recarga viraram um campo só

O PRD §13 pedia três eixos independentes. A v0.2.1 substituiu por `fuel`, uma
lista plana. `ELECTRIC` e `HYBRID` continuam representáveis, então os casos do
PRD §11 e §12 seguem atendidos — o que se perdeu foi a distinção explícita
entre híbrido plug-in e convencional.

Se essa distinção voltar a ser necessária, ela entra por migração.
