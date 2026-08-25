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

A dependência aponta sempre para dentro: `presentation` conhece `domain`,
`domain` não conhece `data` nem `presentation`. A inversão acontece porque a
interface `VehicleRepository` vive em `vehicle/domain` e a implementação
`OfflineVehicleRepository` vive em `vehicle/data` — mesmo padrão em toda
feature, não só veículo.

Isso vale **dentro** de uma feature. Entre features, a regra é outra: uma
feature pode depender do `domain` de outra quando o conceito é
genuinamente dela — `dashboard/domain` depende dos use cases de
`earnings`/`expenses`/`personal` porque agregar é o próprio trabalho do
dashboard, e `expenses/domain` depende de `maintenance/domain` porque uma
despesa de categoria manutenção referencia a categoria de serviço, que é
conceito de manutenção. O que não existe é o inverso — `vehicle` não
importa de `expenses`, `maintenance` não importa de `dashboard` — e toda
dependência cruzada real está registrada na seção "Estrutura de pacotes"
abaixo.

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

**Feature-first**, não por camada técnica. A pergunta que decide onde um
arquivo mora é sempre "que funcionalidade é dona deste código", nunca "que
tipo técnico é este arquivo" — a organização anterior (`core/`, `data/`,
`domain/`, `feature/` como pastas de topo, cada uma subdividida por tipo)
espalhava o domínio de veículo, por exemplo, por sete pastas diferentes.
Reorganizado numa branch dedicada, separada do redesign visual, depois da
v0.14.0.

```
com.driverpro/
├── DriverProApplication.kt   # cria o AppContainer
├── MainActivity.kt           # única Activity
│
├── core/                     # só o que é genuinamente compartilhado
│   ├── database/             # DriverProDatabase, Converters, Migrations
│   ├── di/                   # AppContainer, ViewModelFactory
│   ├── domain/                # Money, Quantity, WorkDuration, DateRange,
│   │                          # FuelType/MeasurementUnit — value objects
│   │                          # usados por toda feature, dono nenhuma
│   ├── navigation/            # rotas e NavHost
│   └── ui/
│       ├── component/         # IconChip, StatTile, DonutChart... sem
│       │                      # import de tipo de domínio nenhum
│       ├── format/             # BrazilianFormatter, MoneyInput,
│       │                       # QuantityInput — formatação genérica
│       └── theme/              # cores, forma, tipografia
│
├── vehicle/  expenses/  earnings/  maintenance/  personal/
│   ├── data/    (entidade, dao, repository impl)
│   ├── domain/  (modelo, repository interface, use cases)
│   └── presentation/  (screen, viewmodel; form/ e list/ onde existem)
│
├── dashboard/
│   ├── domain/        # agrega as outras — sem data/, não persiste nada
│   └── presentation/
│
├── backup/
│   ├── data/           # I/O de infraestrutura pura — sem domain/
│   └── presentation/
│
└── more/
    └── presentation/    # hub de navegação — só presentation/
```

Cada feature concentra o máximo possível do seu próprio código — para
trabalhar em "despesas", a maior parte do que importa está dentro de
`expenses/`. Nem toda feature tem as três camadas: `dashboard` não persiste
nada, `backup` não tem regra de negócio própria, `more` é puramente
navegação. Criar `data/`/`domain/` vazios "para completar" contrariaria o
próprio motivo da reorganização.

**Dependências cruzadas conhecidas** (uma feature importando o domínio de
outra, sempre na direção de quem é dono do conceito):

| De | Para | Por quê |
| --- | --- | --- |
| `dashboard/domain` | `earnings`, `expenses`, `personal` (`domain`) | Dashboard agrega os três — é o próprio trabalho dele |
| `expenses/domain` | `maintenance/domain` (`MaintenanceCategory`) | Uma despesa de categoria manutenção referencia a categoria de serviço; a classificação é conceito de manutenção |
| `maintenance/domain` | `expenses/domain` (`Consumption`, `Expense`) | O piso de quilometragem implícita vem do consumo calculado a partir de abastecimentos |
| `personal/domain` (`ReconcileOdometerUseCase`) | `earnings`, `expenses` (`domain`) | A conciliação lê sessões de trabalho e despesas para calcular a sobra do odômetro |
| toda feature | `core/domain`, `core/ui/*` | Value objects e design system genuinamente compartilhados |
| toda `*/presentation` | `core/di`, `core/navigation` | Fábrica de ViewModel e rotas são centralizadas |

`Money`, `Quantity`, `WorkDuration`, `DateRange`, `FuelType` e
`MeasurementUnit` vivem em `core/domain` — não em `core/common` — porque são
usados por toda feature e não pertencem a nenhuma sozinha.

A estrutura é proporcional ao projeto: módulo Gradle único, sem camadas
cerimoniais. Multi-módulo só se o tempo de build virar problema real.

## Decisões técnicas

### Dinheiro é `Long` em centavos

`core/domain/Money.kt`. `R$ 286,40` é `28640`.

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

`core/domain/WorkDuration.kt`. `8h20` é `500`. Horas decimais existem apenas
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

`dashboard/domain/DashboardMetrics.kt`. Recebe as sessões e as despesas de um
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

`maintenance/domain/MaintenanceMonitor.kt`. É a única funcionalidade que se recusa a
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

`dashboard/domain/DashboardPeriod.kt`. "Personalizado" carrega um intervalo e os
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
`vehicle/presentation/VehicleLabels`, na camada de apresentação.

Se o domínio devolvesse a frase pronta, ele precisaria de `Context` para ler
string resources — e deixaria de rodar em teste JUnit puro. Como bônus, mudar
o texto de um erro não toca em regra de negócio.

A validação também devolve **todos** os erros de uma vez, não o primeiro.
Corrigir um campo por vez, com um erro novo aparecendo a cada tentativa, é a
forma mais eficiente de irritar quem preenche formulário.

### ViewModel factory centralizada

`core/di/ViewModelFactory.kt`. Com DI manual, cada ViewModel precisa de uma
fábrica que monte suas dependências; concentrá-las num arquivo evita espalhar
`viewModelFactory { }` dentro dos Composables.

### Dispatcher injetado

Repositories recebem `CoroutineDispatcher` como parâmetro com default
`Dispatchers.IO`. Testes substituem por um dispatcher determinístico.

### Sem `fallbackToDestructiveMigration`

Apagar o banco do motorista para resolver mudança de schema não é uma opção.
Ver [DATABASE.md](DATABASE.md).

### Backup toca Context e SQLite bruto de propósito

`backup/data/ExportBackupUseCase.kt` e `ImportBackupUseCase.kt` (v0.13.0)
vivem em `backup/data/` — a feature `backup` nem tem pasta `domain/` — e
recebem `Context` e `DriverProDatabase` diretamente, desvio deliberado da
regra "domínio não conhece Android" que vale para o resto do projeto.

Motivo: a funcionalidade inteira **é** infraestrutura. Exportar é um
checkpoint de WAL (`PRAGMA wal_checkpoint(FULL)`, mesmo motivo do
`backup_rules.xml`) seguido de copiar bytes de um arquivo para um `Uri` do
Storage Access Framework. Importar é o inverso, com uma validação antes:
copia para um arquivo temporário, abre só leitura, confere
`PRAGMA user_version` contra `DriverProDatabase.VERSION` e a presença da
tabela `vehicles`. Não há regra de negócio para isolar em domínio puro — só
haveria uma camada de indireção sem função.

O arquivo exportado é uma **cópia crua do banco**, não um formato próprio.
Isso resolve compatibilidade de versão de graça: um arquivo de schema mais
antigo é aceito e migra sozinho — pelas `Migrations.ALL` já cadastradas em
`AppContainer` — na próxima vez que o Room abrir o arquivo trocado, no
próximo lançamento do app. Um arquivo de schema mais novo é rejeitado antes
de tocar no banco vivo, porque não dá para abrir versão futura com app mais
velho.

**Depois de importar, `database.close()` é chamado e a instância fica
inutilizável — o app pede para fechar e reabrir, em vez de tentar se
reiniciar sozinho.** `AppContainer` e `database` são singletons vivos no
processo; trocar o arquivo por baixo deles sem reiniciar o processo não é
seguro (DAOs de repositórios já instanciados ficariam presos à conexão
antiga). Reiniciar o processo automaticamente (`AlarmManager` + `killProcess`)
é possível, mas é exatamente o tipo de mecanismo que se comporta diferente
entre fabricantes — a mesma categoria de instabilidade que o MIUI já causou
neste projeto (`docs/HANDOFF.md`) — e não vale o risco para uma tela que o
motorista abre uma vez a cada troca de aparelho.

### Identidade visual (v0.14.0)

O app tinha Material 3 "de fábrica": cor primária = verde de resultado
financeiro, cards sem hierarquia, zero visualização de dado além de texto,
seis ícones sem rótulo na TopAppBar do Dashboard como navegação. A v0.14.0
resolveu isso junto com o nome do produto (`HANDOFF.md §0`), porque as duas
coisas — nome e identidade visual — precisavam existir de propósito antes da
primeira instalação de terceiro, não só o nome.

**Cor de marca separada da cor semântica de resultado.** `core/ui/theme/
Color.kt` define `BrandIndigo*` (primária — botão, FAB, chip selecionado,
navegação) e `ProfitColors` (verde/vermelho — só onde o número exibido é
lucro ou prejuízo em si). Antes, a primária **era** o verde de `ProfitColors`,
o que misturava as duas coisas: qualquer botão comum "parecia" um resultado
positivo. `ProfitColors` ganhou pares contêiner/on-contêiner por tema
(`Theme.kt: ProfitColors.container()/onContainer()`) para o card de lucro do
Dashboard usar a cor de fundo em si como sinal, não só o texto.

**Material You (`dynamicColor`) desligado por padrão.** Um app que acabou de
fixar identidade visual própria não deveria trocar de cor conforme o papel de
parede do aparelho. O parâmetro continua existindo em `DriverProTheme` para
quem quiser ligar, e para preview/teste pedirem resultado determinístico.

**Forma e tipografia.** `core/ui/theme/Shape.kt` define um raio de canto maior
que o padrão M3 (20–24dp contra ~12dp) — a assinatura visual que diferencia o
app do Material 3 puro. A fonte do sistema continua (ver acima, "Cadastro de
veículo mínimo" não mexe nisso) — o que mudou foi passar a usar algarismo
tabular (`fontFeatureSettings = "tnum"`, `core/ui/theme/Type.kt`) nos números
financeiros grandes, para os dígitos não mudarem de largura ao recompor.

**Componentes novos em `core/ui/component/`** substituem padrões que estavam
duplicados tela a tela: `IconChip` (círculo tonal colorido, deriva o fundo de
uma cor semente por composição em vez de guardar um par claro/escuro por
categoria — ver `ExpenseCategoryVisuals.kt`), `StatTile` (grade de
indicadores 2 colunas), `CategoryBarRow` (barra proporcional — primeira
visualização de dado do app além de texto) e `ListItemCard` (padroniza
ícone-chip + conteúdo + ação, usado pelas quatro telas de lista).

**Navegação: barra inferior substitui a fileira de ícones.** `DashboardScreen`
ganhou `bottomBar` com `NavigationBar` (Dashboard, Ganhos, Despesas, Veículos,
Mais). Mudança só de apresentação — cada item chama exatamente o
`navController.navigate(...)` que já existia. Uso pessoal, manutenção e backup
saíram da TopAppBar e viraram entradas em `more/presentation/MoreScreen.kt`, novo
destino (`DriverProDestination.MORE`) que não existia antes; nenhuma tela de
destino mudou de comportamento, só como se chega até ela.

### Revisão da identidade visual (ainda na v0.14.0, antes de mergear)

O Pedro trouxe uma logo e uma referência visual próprias (`IMAGENS/`) durante
a mesma branch da v0.14.0, antes de qualquer tag ou merge. Parte do que a
primeira passada decidiu foi revertido para seguir essa referência —
registrado aqui, e não apagado do histórico acima, porque o raciocínio de
separar marca de sinal financeiro continua correto **em tese**; só perdeu
para uma referência visual concreta que faz diferente.

- **Marca voltou a ser verde.** `BrandIndigo*` virou `BrandGreen*`
  (`core/ui/theme/Color.kt`), e `ProfitColors.positive*` agora usa
  intencionalmente os mesmos tons — a logo e a referência usam **um verde só**
  para marca e resultado positivo ("verde = seu lucro crescendo"). Os
  extensions `ProfitColors.container()/onContainer()` do parágrafo acima não
  existem mais: o card de lucro do Dashboard voltou a ser branco/neutro, com
  o **texto** do valor colorido (`ProfitColors.content()`, nova função) — a
  referência não pinta o card inteiro, só o número.
- **Tema escuro é navy, não neutro indigo-tintado.** `DarkBackground`/
  `DarkSurface` (`#0A0F1C`/`#121B2E`) seguem a cor de fundo da própria logo.
- **Página cinza-clara, cards brancos com elevação visível.** `background`
  (`#F4F6F4`) e `surface`/`surfaceContainerLowest` (branco) são tokens
  distintos agora — antes eram quase a mesma cor, e o card tonal
  (`surfaceContainerLow`) que os componentes usavam explicitamente somava a
  isso para o card "sumir" no fundo. `StatTile`, `ListItemCard` e os cards de
  resumo usam o branco padrão do `Card` mais `CardDefaults.cardElevation()`
  explícito (2–3dp) em vez de um preenchimento tonal.
- **`CategoryBarRow` foi substituído por `DonutChart` + `CategoryLegendRow`.**
  A referência usa gráfico de rosca no breakdown de despesas por categoria —
  primeiro gráfico de verdade do app (`Canvas` puro, sem biblioteca nova, PRD
  §55) — com uma legenda simples (bolinha colorida + rótulo + valor +
  percentual) embaixo; a barra proporcional ficou redundante com a rosca já
  mostrando a proporção.
- **Novo: breakdown de ganhos por plataforma.** `EarningsSummary.byPlatform`
  (`EarningsListViewModel.kt`) espelha exatamente `ExpensesSummary.byCategory`
  — soma agrupada da lista já carregada, mesmo padrão, nenhum use case novo.
  Exibido com crachá quadrado colorido (iniciais) em vez de bolinha, para não
  confundir com a legenda de categoria de despesa na mesma tela do app.
- **Ícone redesenhado de novo**, agora com gradiente azul→verde reproduzindo a
  logo (`ic_launcher_foreground.xml`), sobre fundo navy sólido.

### Reorganização feature-first (branch separada, depois da v0.14.0)

O Pedro pediu a reestruturação que resultou na seção "Estrutura de pacotes"
lá em cima — trocar organização por camada técnica (`core/`, `data/`,
`domain/`, `feature/`, cada uma subdividida por tipo de arquivo) por
organização por funcionalidade. Numa branch própria, separada da v0.14.0 de
propósito: são mudanças de natureza completamente diferente (visual vs.
estrutura de pacotes), e misturá-las tornaria qualquer uma das duas mais
difícil de revisar.

**Critério para o que fica em `core`:** só código que não pertence a uma
feature específica e é usado por múltiplas. Antes da reorganização,
`core/common/` já guardava `Money`/`Quantity`/`WorkDuration` por esse motivo
— o problema era o resto: `core/ui/format/` também guardava `VehicleLabels`,
`ExpenseLabels`, `MaintenanceLabels` e todos os outros `*Labels.kt`,
específicos de uma feature cada, só porque `BrazilianFormatter` (esse sim
genuinamente compartilhado) morava ali do lado. Cada `*Labels.kt` foi para a
`presentation/` da sua feature; `ExpenseCategoryVisuals.kt` (acoplado a
`ExpenseCategory`) saiu de `core/ui/component/` para `expenses/presentation/`
pelo mesmo motivo. O teste aplicado em cada caso duvidoso está registrado na
tabela de dependências cruzadas, acima.

**`DriverProDatabase` mudou de FQCN**, de `com.driverpro.data.local.database`
para `com.driverpro.core.database` — exceção explícita do critério acima,
porque banco, conversores e migrations são infraestrutura genuinamente
compartilhada. Isso arrastou a pasta de schemas exportados do Room
(`app/schemas/`), que o `MigrationTestHelper` localiza pelo FQCN da classe —
mesma mecânica do rename `com.driverprofit` → `com.driverpro` da v0.14.0.
Entidades e DAOs também mudaram de pacote ao seguir cada feature, mas isso
**não** afeta schema nenhum: Room identifica tabela pelo nome SQL
(`@Entity(tableName = ...)`), não pelo FQCN da classe Kotlin.

**Nenhuma regra de negócio, cálculo, tela, navegação ou schema mudou** — só
o endereço dos arquivos e as declarações `package`/`import`. O gate
(`testDebugUnitTest`, `lintDebug`, `assembleDebug`) ficou verde a cada etapa,
uma feature por vez, exatamente como pedido.

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
