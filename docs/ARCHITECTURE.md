# Arquitetura

Documento de referência técnica. Toda decisão estrutural do projeto deve estar
registrada aqui — se uma escolha não está explicada neste arquivo, ela ainda
não foi decidida de verdade.

## Princípio

A prioridade do projeto, nesta ordem (PRD §59):

```
CORREÇÃO → ESTABILIDADE → MANUTENIBILIDADE → UX → PERFORMANCE → NOVAS FUNCIONALIDADES
```

Nenhuma otimização entra antes de o comportamento estar correto e testado.

## Camadas

```
Compose UI
    ↓
ViewModel
    ↓
Use Case / Domain
    ↓
Repository (interface no domínio)
    ↓
DAO
    ↓
Room
```

A dependência aponta sempre para dentro: `feature` conhece `domain`, `domain`
não conhece `data` nem `feature`. A inversão acontece porque a interface
`VehicleRepository` vive em `domain/repository` e a implementação
`OfflineVehicleRepository` vive em `data/repository`.

### Responsabilidades

| Camada | Faz | Não faz |
| --- | --- | --- |
| **UI** (Composables) | Desenhar estado, emitir eventos | Acessar DAO, rodar SQL, calcular indicadores, manipular entidades do banco |
| **ViewModel** | Estado de tela (`StateFlow`), tradução de eventos, chamar use cases | Conter regra financeira, conhecer Room |
| **Domain** | Regras de negócio, cálculos, validações, use cases | Depender de Android ou de Room |
| **Data** | Room, DAOs, entities, repositories, persistência | Conter regra de negócio |

A camada de domínio é Kotlin puro. É isso que permite testar todos os cálculos
de rentabilidade com JUnit, sem emulador.

## Estrutura de pacotes

```
com.driverprofit/
├── DriverProfitApplication.kt   # cria o AppContainer
├── MainActivity.kt              # única Activity
├── core/
│   ├── common/                  # Money, WorkDuration — tipos base do domínio
│   ├── di/                      # AppContainer
│   ├── navigation/              # rotas e NavHost
│   └── ui/
│       ├── format/              # BrazilianFormatter
│       └── theme/               # cores, tipografia, tema
├── data/
│   ├── local/
│   │   ├── dao/
│   │   ├── database/            # RoomDatabase, TypeConverters
│   │   └── entity/              # entidades + mapeamento para domínio
│   └── repository/              # implementações
├── domain/
│   ├── model/                   # Vehicle, enums de propulsão
│   ├── repository/              # contratos
│   └── usecase/                 # regras de negócio (a partir de v0.2.0)
└── feature/                     # uma pasta por tela
    ├── dashboard/
    ├── earnings/                # list/ e form/
    ├── expenses/                # list/ e form/
    └── vehicle/                 # list/ e form/
```

A estrutura é proporcional ao projeto: módulo Gradle único, sem camadas
cerimoniais. Multi-módulo só se o tempo de build virar problema real.

## Decisões técnicas

### Dinheiro é `Long` em centavos

`core/common/Money.kt`. `R$ 286,40` é `28640`.

Ponto flutuante acumula erro: somar cem lançamentos de `0.07` em `Double` não
dá `7.00`. Em um app cujo produto **é** a exatidão do número, isso é
inaceitável. A formatação para texto acontece só em
`core/ui/format/BrazilianFormatter`.

### Divisão nunca estoura, retorna `null`

`Money.per(quantity)` devolve `null` quando o divisor é zero, negativo ou não
finito. Um período sem quilômetros rodados **não tem** R$/km — não é `R$ 0,00/km`.
A UI exibe `—`. Isso resolve o requisito "nunca permitir divisão por zero"
(PRD §21) no tipo, e não em cada tela.

### Tempo é `Long` em minutos

`core/common/WorkDuration.kt`. `8h20` é `500`. Horas decimais existem apenas
como divisor em R$/hora, via `toHours()`, e nunca são persistidas.

### Unidades são parte do tipo

`FuelType.CNG.unit == CUBIC_METER`, `FuelType.GASOLINE.unit == LITER`. Isso
impede, em tempo de compilação, o erro clássico de tratar GNV em litros
(PRD §10).

| Grandeza | Unidade | Tipo |
| --- | --- | --- |
| Distância | quilômetro | `Long` |
| Combustível líquido | litro | — |
| GNV | m³ | — |
| Energia | kWh | — |
| Tempo | minuto | `WorkDuration` |
| Dinheiro | centavo | `Money` |

### Cadastro de veículo mínimo

O veículo guarda **nome e combustível**, nada mais:

```
Vehicle
├── name    "Onix branco"
└── fuel    GASOLINE | ETHANOL | FLEX | CNG | FLEX_CNG | ELECTRIC | HYBRID
```

Marca, modelo e ano ficaram de fora porque nenhum deles entra em qualquer
conta de rentabilidade — e cada campo a mais é uma barreira entre o motorista
e o primeiro lançamento. Quilometragem também não é atributo do veículo: ela
será registrada por lançamento, servindo a controles de manutenção.

O que o cálculo realmente precisa do veículo é a **unidade de medida do
abastecimento**, e é isso que `fuel` determina.

`FLEX` e `HYBRID` não são insumos, são capacidades. O insumo efetivo é
escolhido no lançamento, a partir de `vehicle.refuelOptions`, e cada
lançamento guarda qual foi usado — o que mantém histórico separado por insumo
(PRD §9) e viabiliza comparar custo/km entre gasolina e etanol depois.

A unidade viaja no tipo (`FuelType.CNG.unit == CUBIC_METER`), o que impede em
tempo de compilação tratar GNV em litros ou energia em litros.

> **Desvio registrado.** O PRD §13 pedia três eixos independentes
> (`powertrain` + `combustionFuel` + `chargingCapability`) para permitir
> combinações futuras. A v0.2.1 substituiu isso por uma lista plana, por
> decisão de produto: a complexidade dos três eixos não se pagava num
> cadastro que o motorista preenche uma vez. `ELECTRIC` e `HYBRID` continuam
> na lista, então os casos do PRD §11 e §12 seguem atendidos.

### O veículo atual é um invariante, não um estado livre

Desde a v0.12.0, `Vehicle.isCurrent` garante: **exatamente um veículo é atual
quando há pelo menos um cadastrado.** Três pontos mantêm isso, e nenhum outro
lugar do código escreve em `isCurrent`:

- `SaveVehicleUseCase` marca o veículo recém-criado como atual quando ele é o
  primeiro (`repository.countVehicles() == 0` antes do insert). Cadastros
  seguintes não mexem em quem é o atual.
- `DeleteVehicleUseCase` chama `promoteOldestToCurrentIfNone()` depois de toda
  exclusão, incondicionalmente. É idempotente — só age se a exclusão deixou o
  banco sem nenhum atual — e promove o mais antigo dos que sobraram.
- `SetCurrentVehicleUseCase` faz a troca manual (tela de veículos, com dois ou
  mais veículos). No DAO, `clearCurrent()` + `markCurrent(id)` andam dentro de
  um `@Transaction`, para nunca existir um instante com dois atuais nem com
  nenhum.

`VehicleValidator.toVehicle` recebe `isCurrent` como parâmetro com default
`false` — o mesmo padrão de `createdAt` — para que editar nome ou combustível
não resete a marcação: `SaveVehicleUseCase` passa `existing.isCurrent` na
edição.

É o veículo atual que `ExpenseFormViewModel` e `EarningsFormViewModel`
pré-selecionam automaticamente ao abrir um lançamento novo
(`vehicles.firstOrNull { it.isCurrent }`), substituindo a heurística anterior
de "só pré-seleciona com exatamente um veículo cadastrado". Ganhos gravam o
vínculo em `WorkSession.vehicleId` (novo em v0.12.0, nulo e imutável depois de
salvo) — o mesmo padrão que despesa já usava desde a v0.4.0.

### O cálculo do dashboard vive no domínio

`domain/model/DashboardMetrics.kt`. Recebe as sessões e as despesas de um
período e devolve os doze indicadores do PRD §21. Kotlin puro: sem Android,
sem Room, sem `Context`.

É o cálculo que **é** o produto, então ele precisa cair na primeira linha de
teste, com JUnit, sem emulador (PRD §29). Colocá-lo em ViewModel ou em
Composable o tornaria verificável apenas com um aparelho ligado — e é
justamente o número que ninguém pode errar.

A classe guarda só somas; tudo que é razão entre duas somas é propriedade
derivada. Assim não existe a possibilidade de um total e um indicador
discordarem: o segundo é sempre calculado do primeiro.

### Custo/km usa apenas despesa operacional

`ExpenseCategory.isOperationalCost` separa seguro, IPVA e financiamento do
resto, e é essa soma reduzida que entra no `costPerKm`.

Custo fixo não varia com a distância. Lançar o seguro anual num dia de
trabalho jogaria o custo/km daquele dia para as alturas e faria o motorista
concluir que rodar não compensa — o que seria falso (PRD §22). Os três
continuam dentro de "Despesas" e do lucro; ficam fora apenas da razão por
quilômetro, e a tela informa isso quando eles existem no período.

O rateio de custo fixo ao longo do tempo é da v1.4 (PRD §47).

### O alerta de manutenção não degrada como o resto do app

`domain/model/MaintenanceMonitor.kt`. É a única funcionalidade que se recusa a
trabalhar com o que tem.

Todas as outras degradam com elegância: sem uso pessoal, o custo/km sai
pessimista e a tela avisa. Aqui a mesma postura seria errada, porque os dois
lados do erro custam coisas diferentes — subestimar quilometragem deixa o
custo/km pessimista, mas **atrasa** o alerta de troca de óleo, e óleo velho
desgasta motor.

Duas regras saem daí:

- **Piso por combustível comprado.** Litros multiplicados pelo consumo histórico
  (`ConsumptionEstimator`, v0.8.0) dão distância que o carro necessariamente
  percorreu, independente de o painel estar atualizado. Quando o piso passa da
  diferença de odômetro, é ele que vale — e a tela declara o número como mínimo,
  pedindo a leitura.
- **Sem marco, sem afirmação.** Item que nunca teve manutenção lançada com
  odômetro fica em `UNKNOWN`. Nunca "em dia": das duas mentiras possíveis, essa é
  a que custa motor.

E uma terceira, que é de apresentação mas muda o desenho: **a tela exibe o
alvo, não quanto falta.** `MaintenanceAlert.nextServiceKm` é a leitura da
última troca mais o intervalo — dois fatos somados, exatos mesmo com o painel
três tanques atrasado. Contagem regressiva carregaria para dentro do número
toda a incerteza sobre a posição atual do carro.

Isso reposiciona o problema inteiro: a incerteza sai do **número**, onde errar
destrói a confiança no app, e vai para o **momento do lembrete**, onde errar
algumas centenas de quilômetros custa um aviso um pouco adiantado. É também o
que permite ao app ser conferível — ele afirma um fato e deixa a comparação com
o painel para o motorista, em vez de disputar com ele quem sabe a quilometragem.

O consumo usado no piso é a **mediana** por combustível, não a média nem o
máximo: um par tanque-a-tanque estragado desloca a média e domina o máximo. E os
abastecimentos são recortados por data, não por odômetro — o piso existe
justamente para quando a leitura está atrasada, e filtrar por ela devolveria o
defeito para dentro da correção.

O marco não é persistido. Ele é derivado do último lançamento de manutenção da
categoria, em `expenses`. Uma coluna "última troca" criaria uma segunda verdade
que divergiria da primeira na primeira correção de lançamento.

### Período é `sealed`, e o dia de referência é parâmetro

`domain/model/DashboardPeriod.kt`. "Personalizado" carrega um intervalo e os
demais não; com `enum`, esse intervalo viajaria por fora, num campo anulável
que só faz sentido para uma das constantes, e cada leitor do estado precisaria
lembrar dessa combinação.

`rangeAt(today)` recebe o dia em vez de ler `LocalDate.now()` internamente. É
isso que torna "ontem" e "mês anterior" testáveis sem depender do relógio da
máquina — o mesmo motivo pelo qual `DashboardViewModel` recebe um `Clock`, e
pelo qual `VehicleValidator` já recebia.

A semana é ISO — **segunda a domingo** — fixada no código e não deduzida do
`Locale`. O primeiro dia da semana muda de país para país, e um indicador que
troca de intervalo conforme a configuração do aparelho é impossível de
conferir.

### Datas

`java.time.LocalDate` para o dia de trabalho, `java.time.Instant` para
carimbos de criação. Nunca strings como `"11/08/2026"`.

No banco: `LocalDate` vira **epoch day** e `Instant` vira **epoch millis**,
ambos `Long`. Consultas por período viram comparação numérica em SQL, que usa
índice e não depende de parsing de texto.

`minSdk 26` foi escolhido justamente para ter `java.time` nativo, sem
desugaring.

### Enums persistidos por `name`, não por `ordinal`

`Converters.kt`. Reordenar constantes de um enum não pode corromper dados já
gravados.

### Injeção de dependências manual

`core/di/AppContainer.kt`. Sem Hilt nem Koin.

Com um módulo e poucas dependências, um container manual resolve o problema sem
custo de processamento de anotações no build nem curva de aprendizado. Se o
grafo crescer a ponto de o arquivo ficar difícil de ler, reavaliar — e
atualizar esta seção.

### Validação devolve o motivo, não a mensagem

`VehicleValidator` retorna `VehicleFieldError(field, error)`, onde `error` é um
enum como `REQUIRED` ou `YEAR_OUT_OF_RANGE`. Quem traduz para texto é
`core/ui/format/VehicleLabels`, na camada de apresentação.

Se o domínio devolvesse a frase pronta, ele precisaria de `Context` para ler
string resources — e deixaria de rodar em teste JUnit puro. Como bônus, mudar
o texto de um erro não toca em regra de negócio.

A validação também devolve **todos** os erros de uma vez, não o primeiro.
Corrigir um campo por vez, com um erro novo aparecendo a cada tentativa, é a
forma mais eficiente de irritar quem preenche formulário.

### ViewModel factory centralizada

`core/ui/ViewModelFactory.kt`. Com DI manual, cada ViewModel precisa de uma
fábrica que monte suas dependências; concentrá-las num arquivo evita espalhar
`viewModelFactory { }` dentro dos Composables.

### Dispatcher injetado

Repositories recebem `CoroutineDispatcher` como parâmetro com default
`Dispatchers.IO`. Testes substituem por um dispatcher determinístico.

### Sem `fallbackToDestructiveMigration`

Apagar o banco do motorista para resolver mudança de schema não é uma opção.
Ver [DATABASE.md](DATABASE.md).

## Regras de dependência

Antes de adicionar uma biblioteca (PRD §55):

1. AndroidX ou a stdlib do Kotlin já resolvem?
2. A biblioteca é mantida ativamente?
3. Qual o impacto em tamanho de APK e tempo de build?
4. A decisão está documentada aqui?

Todas as versões ficam em `gradle/libs.versions.toml`. Nenhuma versão
hardcoded em `build.gradle.kts`.

## Funcionalidades fora de escopo

Não adicionar sem autorização explícita (PRD §48): GPS, rastreamento,
integração com Uber/99, login, backend, Firebase, publicidade, assinatura, IA,
pagamentos, APIs externas.

O manifesto não declara **nenhuma** permissão. Toda permissão nova precisa ser
justificada em um PR próprio.

## Testes

| Tipo | Onde | Roda no CI |
| --- | --- | --- |
| Unitários (domínio, cálculos, formatação) | `app/src/test/` | ✅ |
| Banco (DAO, migrações) | `app/src/androidTest/` | ❌ exige emulador |
| ViewModel | `app/src/test/` | ✅ |
| UI (Compose) | `app/src/androidTest/` | ❌ exige emulador |

Cálculos de indicadores ficam em classes puras justamente para caírem na
primeira linha — testáveis sem iniciar o Android.
