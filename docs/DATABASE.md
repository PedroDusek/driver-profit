# Banco de dados

Room sobre SQLite, local ao aparelho. Nome do arquivo: `driver_profit.db`.

**Versão atual do schema: 7**

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

### `expenses` (v5)

Todas as despesas em **uma tabela só**, com as colunas de detalhe anuláveis.

| Coluna | Tipo SQL | Nulo | Descrição |
| --- | --- | --- | --- |
| `id` | INTEGER PK AUTOINCREMENT | não | Identificador |
| `vehicle_id` | INTEGER | **sim** | FK para `vehicles`, `ON DELETE SET NULL` |
| `date` | INTEGER | não | Epoch day |
| `category` | TEXT | não | `FUEL` \| `CHARGING` \| `MAINTENANCE` \| `CAR_WASH` \| `TOLL` \| `PARKING` \| `INSURANCE` \| `VEHICLE_TAX` \| `FINANCING` \| `OTHER` |
| `amount_cents` | INTEGER | não | Valor pago, em centavos. **Zero é válido** |
| `description` | TEXT | não | Observação; string vazia quando não informada |
| `fuel_type` | TEXT | **sim** | `GASOLINE` \| `ETHANOL` \| `CNG` \| `ELECTRICITY` |
| `quantity_thousandths` | INTEGER | **sim** | Milésimos da unidade (litro, m³ ou kWh). Opcional desde a v0.4.1 |
| `charging_location` | TEXT | **sim** | `RESIDENTIAL` \| `COMMERCIAL` \| `PUBLIC` \| `OTHER` |
| `maintenance_category` | TEXT | **sim** | Item da manutenção |
| `place` | TEXT | **sim** | Posto, eletroposto ou oficina, conforme a categoria |
| `odometer_km` | INTEGER | **sim** | Leitura do painel, em km inteiros. Desde a v0.6.0 |
| `created_at` | INTEGER | não | Epoch millis (UTC) |

**Índices:** `date` e `vehicle_id`.

**`odometer_km` é anulável no schema, mas obrigatório no domínio** para
abastecimento, recarga e manutenção — as categorias que exigem veículo. A
coluna aceita nulo por duas razões: pedágio e estacionamento não têm leitura, e
as despesas gravadas antes da v0.6.0 nunca tiveram. Exigir `NOT NULL`
obrigaria a inventar um valor na migração, e valor inventado de odômetro
contamina consumo estimado, quilômetro pessoal e alerta de manutenção.

A obrigatoriedade vive em `ExpenseValidator`: ela vale para o que se grava de
agora em diante e não invalida retroativamente o histórico.

**Por que uma tabela só:** o PRD §17 pede explicitamente que adicionar
categoria não exija mudança estrutural. Com uma tabela por natureza de
despesa, "pedágio" seria uma migração; assim é uma constante de enum. O preço
são colunas nulas em boa parte das linhas — barato — e a coerência entre
categoria e detalhe passar a ser responsabilidade do domínio
(`ExpenseValidator` + `sealed ExpenseDetail`), não do schema.

**`ON DELETE SET NULL`:** excluir um veículo não pode apagar o histórico
financeiro. A despesa fica órfã e continua somando no dashboard, que é o
comportamento correto para quem trocou de carro.

**`amount_cents` aceita zero:** recarga gratuita é despesa de R$ 0,00 com kWh
maior que zero (PRD §11).

**Quantidade em milésimos**, não centésimos: a bomba de combustível exibe três
casas decimais, e arredondar na entrada já introduziria erro no preço por
litro.

**`place` é uma coluna só** para posto, eletroposto e oficina. São três nomes
para "onde isso aconteceu" e nunca coexistem na mesma linha; três colunas
seriam duas sempre nulas.

### `personal_usage` (v6)

Quilômetros rodados fora do trabalho (PRD §22).

| Coluna | Tipo SQL | Nulo | Descrição |
| --- | --- | --- | --- |
| `id` | INTEGER PK AUTOINCREMENT | não | Identificador |
| `vehicle_id` | INTEGER | **sim** | FK para `vehicles`, `ON DELETE SET NULL` |
| `start_date` | INTEGER | não | Epoch day do primeiro dia |
| `end_date` | INTEGER | não | Epoch day do último dia, inclusive |
| `distance_km` | INTEGER | não | Km do intervalo inteiro, não por dia |
| `source` | TEXT | não | `DECLARED` \| `RECONCILED` |
| `note` | TEXT | não | Observação; string vazia quando não informada |
| `created_at` | INTEGER | não | Epoch millis (UTC) |

**Índices:** `start_date`, `end_date` e `vehicle_id`.

**Tabela própria, e não coluna em `work_sessions`:** uso pessoal não tem
plataforma, corridas nem faturamento, e não é uma jornada. Enfiá-lo lá
obrigaria a deixar metade das colunas nulas e a filtrar toda consulta de
ganhos.

**Intervalo, e não um dia:** uma viagem de fim de semana cobre vários dias, e a
sobra da conciliação cobre todo o período entre duas leituras de odômetro. O
recorte proporcional por dias é do domínio (`PersonalUsage.kilometersWithin`),
não do SQL.

**`source` distingue declarado de estimado.** A sobra da conciliação não tem a
mesma confiança de uma viagem que o motorista lançou, e a tela precisa dizer
qual é qual antes de ele decidir corrigir.

### `maintenance_schedules` (v7)

Intervalo de manutenção que o motorista definiu, sobrepondo o padrão do app.

| Coluna | Tipo SQL | Nulo | Descrição |
| --- | --- | --- | --- |
| `id` | INTEGER PK AUTOINCREMENT | não | Identificador |
| `vehicle_id` | INTEGER | não | FK para `vehicles`, `ON DELETE CASCADE` |
| `item` | TEXT | não | `OIL` \| `FILTERS` \| `BRAKES` \| `TIRES` \| `INSPECTION` |
| `interval_km` | INTEGER | não | A cada quantos quilômetros |
| `monitored` | INTEGER | não | 0 quando o motorista desligou o acompanhamento |
| `created_at` | INTEGER | não | Epoch millis (UTC) |

**Índice:** `(vehicle_id, item)`, **único**.

**A tabela guarda só o que ele mudou.** Linha ausente significa "intervalo
padrão do app", que vive no enum `MaintenanceItem`. Três consequências, todas
desejadas: um veículo recém-cadastrado já nasce acompanhado sem ritual de
configuração; a tabela tem meia dúzia de linhas em vez de cinco por veículo; e
continua sendo possível distinguir "ele escolheu 10.000" de "ele nunca mexeu" —
o que permite revisar um padrão numa versão futura sem sobrescrever escolha de
ninguém.

**Por isso não existe "gravar o padrão".** Devolver um item ao padrão é apagar a
linha, e é assim que `resetToDefault` funciona.

**`vehicle_id` é NOT NULL e a exclusão é `CASCADE`**, ao contrário de `expenses`
e `personal_usage`, que são anuláveis com `SET NULL`. Aquelas guardam histórico
financeiro, que precisa sobreviver à troca de carro; esta guarda uma preferência
sobre um carro específico, e sem o carro ela não significa nada.

**`monitored` é coluna e não ausência de linha:** desligar um item precisa
preservar o intervalo escolhido, para que religá-lo devolva o número dele e não
o padrão. E o item continua aparecendo na tela — um item que some ao ser
desligado não tem como ser religado.

**Um índice só.** Ele é único, o que impede dois intervalos para o mesmo item, e
como começa por `vehicle_id` também atende a exigência do Room para a chave
estrangeira.

**O marco não vive aqui.** De onde a contagem parte é o último lançamento de
manutenção daquela categoria com odômetro, em `expenses` — dado que já existe
desde a v0.4.0, com a leitura desde a v0.6.0. Duplicá-lo numa coluna criaria
duas verdades que divergiriam na primeira correção de lançamento.

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
| `ExpenseCategory` | `TEXT` |
| `FuelType` | `TEXT` |
| `ChargingLocation` | `TEXT` |
| `MaintenanceCategory` | `TEXT` |
| `Quantity` | `INTEGER` em milésimos |

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
| 4 | v0.4.0 | Adiciona `expenses`, com FK para `vehicles` e índices sobre `date` e `vehicle_id` |
| 5 | v0.6.0 | Adiciona `odometer_km` em `expenses` (odômetro por lançamento) |
| 6 | v0.7.0 | Adiciona `personal_usage` (quilometragem fora do trabalho) |
| 7 | v0.9.0 | Adiciona `maintenance_schedules` (intervalos de manutenção) |

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

#### Migração 3 → 4

Aditiva: cria `expenses` com a chave estrangeira para `vehicles` e os dois
índices. Nenhuma tabela existente é alterada.

O índice sobre `vehicle_id` não é opcional — o Room o exige para a chave
estrangeira, e sem ele apagar um veículo faria varredura completa de
`expenses`.

#### Migração 4 → 5

`ALTER TABLE expenses ADD COLUMN odometer_km INTEGER`. Aditiva e sem reescrita
de tabela — barata mesmo com histórico grande.

A coluna é **anulável e sem default**, de propósito. As despesas já gravadas
não têm leitura de painel, e preencher com zero ou repetir a anterior
envenenaria justamente o que o odômetro serve para calcular. `NULL` diz a
verdade: "não sei".

Consequência prática: o consumo estimado (v0.8.0) e os alertas de manutenção
(v0.9.0) só passam a funcionar a partir do primeiro lançamento com leitura.
Isso é correto — não há como reconstruir um dado que nunca foi coletado.

#### Migração 5 → 6

Aditiva: cria `personal_usage` com a chave estrangeira para `vehicles` e três
índices. Nenhuma tabela existente é tocada.

Índices em `start_date` **e** `end_date` porque a consulta do dashboard é de
**sobreposição** de intervalos, não de contenção: uma viagem de 28/07 a 03/08
precisa aparecer em julho e em agosto, cada mês com a fatia de quilômetros que
lhe cabe. Filtrar por "começa dentro do período" perderia a segunda metade
dela, e o custo/km de agosto voltaria a ficar inflado — que é o defeito que
esta versão existe para corrigir.

`vehicle_id` é indexado porque o Room o exige para a chave estrangeira.

#### Migração 6 → 7

Aditiva: cria `maintenance_schedules` com a chave estrangeira para `vehicles` e
um índice único. Nenhuma tabela existente é tocada.

**A tabela nasce vazia, e isso é o comportamento correto.** Como linha ausente
significa "intervalo padrão", todo veículo já cadastrado passa a ser acompanhado
sem que a migração precise inserir cinco linhas por carro — o que seria gravar
no banco uma decisão que o motorista não tomou.

Os alertas também não precisam de dado novo: o marco de cada item vem do
histórico de manutenção que já está em `expenses`, com odômetro desde a v0.6.0.
Consequência assumida: um item só passa a alertar depois do primeiro serviço
lançado **com leitura**. Antes disso ele se declara sem dados, que é a verdade.

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
