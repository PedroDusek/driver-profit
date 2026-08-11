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
└── feature/
    └── dashboard/               # uma pasta por tela
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

### Propulsão separada de combustível e de recarga

Três enums independentes em vez de um só (PRD §13):

```
Vehicle
├── powertrain          COMBUSTION | HYBRID | ELECTRIC
├── combustionFuel      GASOLINE | ETHANOL | FLEX | CNG | FLEX_CNG | null
└── chargingCapability  NONE | PLUG_IN | UNKNOWN | null
```

Um enum único (`FLEX`, `HYBRID`, `ELECTRIC`…) não representa "híbrido flex
plug-in" sem explodir em combinações. Com três eixos, `HYBRID + FLEX + PLUG_IN`
já é representável hoje, sem migração de schema amanhã.

`null` significa "não se aplica": elétrico puro não tem combustível; carro a
combustão não tem capacidade de recarga.

O formulário de abastecimento se monta a partir de `vehicle.refuelOptions` —
a UI nunca oferece um combustível incompatível com o veículo.

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

### Coerência entre propulsão e campos dependentes

`VehicleDraft.withPowertrain` zera combustível e capacidade de recarga quando
eles deixam de fazer sentido. A regra fica no domínio, e não na tela: um
elétrico com combustível cadastrado geraria um formulário de abastecimento
impossível na v0.4.0, independente de por qual caminho o dado entrou.

O validador cobre as duas direções — o que falta e o que sobra.

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
