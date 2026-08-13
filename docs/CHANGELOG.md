# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/).
Versionamento conforme [Semantic Versioning](https://semver.org/lang/pt-BR/).

## [Não publicado]

### Decisões de produto registradas

Nenhuma alteração de código — apenas o desenho das próximas versões, fechado
antes de implementar para não custar migração dupla.

- **Uso pessoal do veículo** (PRD §22). O custo/km hoje divide despesa total
  por km apenas profissionais; a correção é o denominador virar o km total,
  porque o rateio proporcional se cancela. Sem estimar consumo, sem preço
  médio, sem alterar despesa gravada
- **Financiamento, seguro e IPVA são 100% do trabalho.** Custo fixo não é
  causado pelo uso: levar o carro ao mercado não gera parcela nem aumenta o
  IPVA. Os demais custos operacionais se rateiam por quilometragem
- **A base do custo/km e a base do rateio são a mesma** — todo o custo
  operacional, não só combustível. Rodar por lazer também consome pneu
- **Competência dos custos fixos**, separando quando o dinheiro saiu de a que
  período ele se refere
- **Odômetro obrigatório no abastecimento**, e combustível comprado como prova
  independente de distância para proteger os alertas de manutenção (PRD §23)
- **Sem chave de ligar/desligar** em indicador. O estado "sem correção" é
  ausência de dado, nunca um botão — um indicador que muda de definição
  conforme um switch deixa de ser conferível
- **Roadmap resegmentado**: v0.6.0 Odômetro · v0.7.0 Uso pessoal · v0.8.0
  Consumo estimado · v0.9.0 Manutenção preventiva · v0.10.0 Custos fixos ·
  v0.11.0 Analytics. Manutenção preventiva e custos fixos foram antecipados do
  pós-MVP, porque sem eles o custo/km fica incompleto

## [0.5.1] — Cursor ancorado e formulários acima do teclado

Primeiros defeitos encontrados usando o app em aparelho — nenhuma tela tinha
sido aberta de verdade até a v0.5.0.

### Corrigido

**O cursor andava sozinho no campo de valor.**

O campo exibe o texto já formatado, então ele muda de comprimento a cada
tecla: `R$ 3,00` vira `R$ 32,00`. Passando uma `String` simples ao
`OutlinedTextField`, a seleção nunca é controlada, e o Compose preserva o
**deslocamento numérico** do cursor — que no texto reformatado cai num lugar
visual diferente.

A lógica de dígitos da v0.4.1 estava correta; faltava fixar o cursor.

**O teclado cobria o campo em digitação.**

O manifesto declara `adjustResize`, mas `enableEdgeToEdge()` faz a janela
deixar de encaixar nas system windows, e `adjustResize` para de encolher o
layout. Quem precisa consumir o inset passa a ser o Compose, com
`imePadding()`.

### Decisões registradas

- **Cursor ancorado no fim**, como aplicativo de banco, em vez de calcular a
  posição correta. Não existe posição correta: ao teclar `5` em `R$ 3,00` os
  dígitos deslizam e a vírgula fica parada, então toda regra de mapeamento erra
  em algum canto. Ancorar elimina o problema em vez de administrá-lo — vírgula
  e separador de milhar viram puramente visuais, e o número só cresce ou
  encolhe pela direita
- **Só em campo numérico.** Texto livre — nome, observação, posto, oficina —
  mantém edição no meio, onde ela é legítima
- **`imePadding()` antes do `verticalScroll`**, para encolher a área rolável em
  vez do conteúdo: assim o campo focado entra em cena sozinho

### Não coberto por teste

As duas correções são comportamento de Compose. `MoneyInputTest` cobre a
lógica de dígitos e continua verde, mas passaria igual com o cursor quebrado.
O projeto ainda não tem teste de UI — é o candidato natural agora que os
testes instrumentados provaram rodar.

## [0.5.0] — Dashboard

Os indicadores de rentabilidade. Ganhos e despesas já estavam no banco desde a
v0.4.0; esta versão é a conta entre eles — a razão de o produto existir.

### Adicionado

**Domínio**
- `DashboardMetrics` — classe pura com os doze indicadores do PRD §21:
  faturamento, despesas, lucro, km, horas, corridas, R$/km, R$/hora,
  R$/corrida, custo/km, lucro/km e lucro/hora (PRD §29)
- `DashboardPeriod` — hoje, ontem, esta semana, este mês, mês anterior e
  personalizado, com o dia de referência recebido por parâmetro
- `DateRange` — intervalo de dias fechado nas duas pontas
- `ObserveDashboardUseCase` — combina ganhos e despesas do mesmo período num
  `Flow` só

**Interface**
- Dashboard substitui o marcador da v0.1.0: filtros de período, lucro em
  destaque, volume, quanto rendeu, custo e lucro por unidade, e despesas
  separadas por natureza
- Seletor de intervalo personalizado
- `BrazilianFormatter.moneyOrUnavailable` — indicador que não pode ser
  calculado vira `—`, nunca `R$ 0,00`

**Testes**
- 52 testes novos (270 no total)

### Decisões registradas

- **Custo/km usa apenas despesa operacional** (PRD §22). Seguro, IPVA e
  financiamento não variam com a distância: lançar o seguro anual num dia de
  trabalho jogaria o custo/km daquele dia para as alturas e faria o motorista
  concluir que rodar não compensa. Os três continuam dentro de "Despesas" e do
  lucro — só ficam fora da razão por quilômetro, e a tela diz isso quando eles
  existem no período.
- **Lucro negativo é exibido como número negativo**, com cor de erro, e não
  como zero. Prejuízo escrito com a mesma tinta do lucro passa despercebido
  justamente no dia em que mais importa.
- **`DashboardPeriod` é `sealed`, não `enum`.** "Personalizado" carrega um
  intervalo e os demais não; com enum, esse intervalo viajaria num campo
  anulável que só faz sentido para uma das constantes.
- **A semana é ISO, de segunda a domingo**, fixada no código e não deduzida do
  `Locale`. Um indicador que muda de intervalo conforme a configuração do
  aparelho é impossível de conferir.
- **Os presets devolvem a semana e o mês inteiros**, não até hoje. Data futura
  é recusada na validação, então nenhum dia à frente tem registro e o número é
  o mesmo — com a vantagem de "este mês" ter sempre o mesmo começo e fim.
- **O `Clock` é injetado na ViewModel.** Com `LocalDate.now()` fixo no código,
  o teste de "ontem" passaria hoje e falharia na virada do mês.
- **Intervalo personalizado invertido é reordenado**, não recusado: tocar na
  data final antes da inicial é engano de toque.
- **`R$ 0,00` e "indisponível" são coisas diferentes.** Período sem
  quilômetros não tem custo/km igual a zero; a tela exibe `—`.

### Alterado

- A verificação `Typos` do lint foi desligada. Ela compara cada palavra dos
  textos com um dicionário **inglês**, e acusou "eles" como erro de digitação
  de "eels". Num app inteiramente em português não existe acerto possível nessa
  checagem, só falso positivo — e com `warningsAsErrors` o custo é uma frase
  nova da interface derrubar o CI. `tools:locale="pt-BR"` não a desarma nesta
  versão do lint.

### Não implementado nesta versão

- **Gráficos e evolução entre períodos** — v0.6.0.
- **Custo por km separado por natureza** (o quadro do PRD §22 com
  combustível R$ 0,42/km, manutenção R$ 0,11/km). A tela mostra as despesas
  por natureza em reais; a razão por quilômetro de cada uma entra com os
  gráficos da v0.6.0.
- **Custos fixos rateados** — v1.4, como o PRD §47 já previa.

### Corrigido

**Os testes de migração nunca conseguiam começar.** `kotlinx-serialization`
resolvia quebrado no classpath de `androidTest`: o `json` em 1.8.1 e o `core`
em 1.7.3.

A causa é a *consistent resolution* do AGP, que obriga o classpath de teste a
acompanhar o do app. `room-testing` pede 1.8.1, mas só existe em
`androidTest` — no classpath do app quem manda são navigation e lifecycle, que
trazem 1.7.3. As classes de schema do Room, compiladas contra 1.8.x, chamavam
`GeneratedSerializer.typeParametersSerializers()`, que em 1.7.3 ainda é
abstrato: `AbstractMethodError` ao ler o schema exportado, antes de qualquer
migração rodar.

O BOM do `kotlinx-serialization` alinha as duas pontas em 1.8.1.

### Verificado em aparelho

Primeira execução dos testes instrumentados na história do projeto:
**36 testes, todos passando** num Redmi Note 8 Pro com Android 9 — os três
DAOs e as quatro migrações encadeadas (1→2→3→4).

Continua sem verificação: os fluxos de interface nunca foram usados de verdade
num aparelho.

## [0.4.1] — Campo de valor corrigido e quantidade opcional

### Corrigido

**O campo de valor travava em `R$ 0,00`.**

O campo exibe o valor já formatado, e o `onValueChange` reextraía *todos* os
dígitos do texto devolvido. Só que `"R$ 0,00"` contém **três** dígitos, não um
— estado e tela deixavam de ser reversíveis:

- apagar um caractere de `R$ 0,00` devolvia `"00"`, que normalizava de volta
  para `"0"`: o campo travava e nunca esvaziava;
- tocar no meio do texto e digitar `3` produzia `R$ 30,00` em vez de `R$ 0,03`,
  porque a posição do dígito no texto formatado mudava o resultado.

`MoneyInput` passa a raciocinar por diferença: compara os dígitos do texto
exibido com os do texto devolvido para saber se o motorista digitou ou apagou,
e identifica o trecho inserido por prefixo/sufixo em comum. Vale para o campo
de ganhos e para o de despesas.

### Alterado

- **Quantidade de combustível virou opcional.** O indicador principal do
  produto é **custo/km**, que sai do valor pago e dos quilômetros rodados —
  não de quantos litros entraram no tanque. Exigir a quantidade cobrava um
  dado a cada abastecimento em troca de R$/litro, que é secundário
- Quando informada, a quantidade continua habilitando o preço por unidade.
  Zero segue rejeitado: ou não foi informado, e aí fica em branco, ou é erro
  de digitação
- `ExpenseDetail.Refuel.quantity` e `ExpenseDetail.Charging.energy` agora são
  anuláveis. **Sem mudança de schema** — as colunas já eram anuláveis
- Lint deixa de tratar `GradleDependency` e `NewerVersionAvailable` como erro:
  com `warningsAsErrors`, um commit que passava hoje falharia amanhã sozinho
  ao ser publicada uma release de dependência, quebrando o critério do PRD §58

### Adicionado

- `docs/HANDOFF.md` — estado do projeto para retomar em sessão nova
- `MoneyInputTest` com o grupo de regressão do defeito acima

## [0.4.0] — Expenses

Registro de despesas. Com ganhos e despesas no mesmo banco, a v0.5.0 pode
finalmente calcular lucro em vez de só faturamento.

### Adicionado

**Domínio**
- `Expense` com `ExpenseDetail` selado: `Refuel`, `Charging` e `Maintenance`
- `pricePerUnit` — R$/litro, R$/m³ ou R$/kWh, conforme o insumo (PRD §7, §10, §11)
- `ExpenseCategory` com as dez naturezas do PRD §17; `MaintenanceCategory` com
  os treze itens do §18; `ChargingLocation` com os quatro tipos do §11
- `Quantity` — quantidade em milésimos da unidade, para não usar `Double` num
  divisor
- `ExpenseValidator` e os use cases de escrita e leitura, incluindo consulta
  por período

**Dados**
- Banco vai para a **versão 4**, com `expenses`, FK para `vehicles` e índices
  sobre `date` e `vehicle_id`
- Migração 3→4 aditiva

**Interface**
- Histórico com filtro por natureza e totais que acompanham o filtro
- Formulário dinâmico: os campos mudam conforme a categoria escolhida
- `QuantityInput` — entrada decimal com vírgula, sem depender de `Locale`

**Testes**
- 85 testes novos (218 no total)
- Testes instrumentados de `ExpenseDao`, incluindo a exclusão de veículo que
  preserva a despesa, e das migrações 3→4 e 1→4

### Decisões registradas

- **Uma tabela para todas as despesas.** O PRD §17 pede que adicionar
  categoria não exija mudança estrutural; com tabelas separadas, "pedágio"
  seria uma migração.
- **`ON DELETE SET NULL` no veículo.** Trocar de carro não pode apagar o
  histórico financeiro.
- **Valor zero é válido, quantidade zero não.** Recarga gratuita é R$ 0,00 com
  kWh maior que zero (PRD §11); abastecimento sem quantidade não tem preço por
  litro, que é o motivo de ser uma categoria separada.
- **O combustível é validado contra o veículo.** Etanol num carro a GNV é dado
  impossível e contaminaria o custo por unidade.
- **Seguro, IPVA e financiamento são marcados como custo fixo.** Eles não
  variam com o quanto se roda; misturá-los no custo/km de um dia distorceria o
  indicador (PRD §22).
- **Quantidade em milésimos**, porque a bomba exibe três casas decimais.
- **Quantidade é digitada com vírgula**, e não em dígitos puros como o
  dinheiro: teclar `35` esperando 35 litros e obter 0,035 seria a pior
  armadilha possível.

### Não implementado nesta versão

- **Odômetro por lançamento**, e portanto **consumo estimado** (PRD §23).
  Ficaram para a v0.6.0 por decisão de produto: a quilometragem deixou de ser
  atributo do veículo na v0.2.1 e volta como recurso de manutenção.

### Não verificado

Testes instrumentados exigem emulador e **não foram executados**.

## [0.3.1] — Campos da jornada obrigatórios

Corrige uma regra de validação que produziria indicadores errados no dashboard.

### O problema

Na v0.3.0, faturamento, corridas, tempo online e distância eram
individualmente opcionais. O dashboard agrega período assim:

```
R$/hora do período = soma(faturamento) ÷ soma(horas)
```

Uma sessão com valor preenchido e horas em branco entraria com o valor no
numerador e zero no denominador — inflando o R$/hora e exibindo o resultado com
a mesma confiança de um número correto. A regra antiga trocava um dado ausente
e visível por um indicador errado e invisível, contra o PRD §59.

### Alterado

- Faturamento, corridas, tempo online e distância passam a ser obrigatórios
- **Zero continua sendo resposta válida**: seis horas online sem nenhuma
  corrida é um dia ruim que existe. O que se recusa é o campo em branco
- `EMPTY_SESSION` só é acusado quando os quatro campos foram informados e todos
  valem zero; com campos em branco, o erro correto é "campo obrigatório"
- O campo de valor fica vazio em vez de mostrar `R$ 0,00`, que dava aparência
  de preenchido
- `WorkSessionValidator.toSession` deixa de preencher zeros implícitos

### Corrigido

- Digitar `0` no valor era descartado junto com os zeros à esquerda, tornando
  "campo vazio" e "valor zero" indistinguíveis
- Editar um campo qualquer limpava o erro do faturamento, mesmo quando esse
  erro era do próprio campo

## [0.3.0] — Earnings

Registro de ganhos: o motorista já consegue lançar quanto trabalhou e quanto
recebeu, e ver R$/hora e R$/km do histórico.

### Adicionado

**Domínio**
- `WorkSession` — sessão de trabalho com data, plataforma, corridas,
  faturamento, tempo online, distância e observação
- `revenuePerHour`, `revenuePerKm` e `revenuePerRide` como propriedades da
  sessão, devolvendo `null` quando o divisor é zero (PRD §21)
- `Platform` — Uber, 99, InDrive e Outra
- `WorkSessionValidator`, `SaveWorkSessionUseCase` e os use cases de leitura
- `ObserveWorkSessionsBetweenUseCase` — consulta por período, já pronta para os
  filtros do dashboard da v0.5.0

**Dados**
- Banco vai para a **versão 3**, com `work_sessions` e índice sobre `date`
- Migração 2→3 puramente aditiva

**Interface**
- Histórico de lançamentos com card de totais, edição e exclusão
- Formulário com seletor de data, plataforma, valor, corridas, tempo e
  distância
- Acesso pelo ícone de ganhos no dashboard

**Testes**
- 53 testes novos (126 no total)
- Testes instrumentados de `WorkSessionDao`, incluindo consulta por período,
  e de migração 2→3 e 1→3

### Decisões registradas

- **Registro por sessão, não por corrida.** Pedir para lançar corrida a corrida
  seria trabalho demais para quem dirige o dia inteiro; o motorista consulta o
  extrato da plataforma e lança o consolidado.
- **Valor digitado em centavos.** Teclar `32050` mostra `R$ 320,50` — evita a
  briga com vírgula, ponto e teclado numérico.
- **Distância em quilômetros inteiros.** Meio quilômetro muda R$/km na terceira
  casa decimal; não vale a complexidade de fracionar.
- **Campos numéricos são individualmente opcionais**, mas a sessão inteira não
  pode estar vazia. Forçar o preenchimento de km faria o motorista inventar um
  número — pior que não ter o dado.
- **Data de hoje já vem preenchida** no lançamento novo, que é o caso comum.
- **Minutos acima de 59 são recusados na digitação**, não no save.

### Não verificado

Testes instrumentados exigem emulador e **não foram executados**.

## [0.2.1] — Cadastro de veículo simplificado

Reduz o cadastro a **nome + tipo de combustível**. Decisão de produto: nenhum
dos campos removidos entrava em conta de rentabilidade, e cada campo a mais é
uma barreira entre o motorista e o primeiro lançamento.

> ⚠️ **Desvio de versionamento assumido.** Pelo PRD §41, alteração de schema e
> remoção de funcionalidade não caberiam em um PATCH. Numerado como 0.2.1 por
> decisão explícita, para manter o roadmap intacto (Earnings segue como
> v0.3.0). Registrado aqui para que o histórico não engane quem vier depois.

### Alterado

- `Vehicle` agora tem apenas `name`, `fuel` e `createdAt`
- `VehicleFuel` — lista plana com gasolina, etanol, flex, GNV, flex+GNV,
  elétrico e híbrido, substituindo os três eixos `VehiclePowertrain` +
  `CombustionFuel` + `ChargingCapability`
- `FuelType` ganha `ELECTRICITY`, medido em kWh
- Formulário passa a ter dois campos; some a lógica de campos condicionais
- Banco vai para a **versão 2**, com migração 1→2 que preserva os veículos
  existentes (`name` recebe "marca modelo", `fuel` deriva da propulsão antiga)

### Removido

- `brand`, `model`, `year` e `initialOdometerKm` do veículo
- `VehiclePowertrain`, `CombustionFuel` e `ChargingCapability`
- `VehicleDraft.withPowertrain` e as validações de coerência entre eixos
- Validações de ano e de odômetro

Quilometragem volta depois como registro por lançamento, servindo a controles
de manutenção (troca de óleo, pneus) — não como atributo do veículo.

### Adicionado

- `Migrations.kt` com a migração 1→2 registrada no builder do Room
- `MigrationTest` — três casos cobrindo dados preservados, derivação de
  combustível a partir da propulsão antiga e banco vazio
- Schemas exportados de ambas as versões versionados no Git

### Não verificado

`MigrationTest` exige emulador ou aparelho e **não foi executado**. O gate de
CI cobre apenas testes de unidade.

## [0.2.0] — Vehicle

Cadastro de veículo completo: o primeiro fluxo de produto do aplicativo.

### Adicionado

**Domínio**
- `VehicleDraft` — veículo em preenchimento, com todo campo obrigatório
  anulável, porque "ainda não escolhido" é um estado legítimo do formulário
- `VehicleValidator` — validação pura, com `Clock` injetado para tornar a
  regra de ano determinística nos testes
- `SaveVehicleUseCase` — insert e update no mesmo use case, decidido pelo id
  do rascunho; preserva o `createdAt` original ao editar
- `ObserveVehiclesUseCase`, `GetVehicleUseCase`, `DeleteVehicleUseCase`

**Interface**
- Lista de veículos com estados de carregamento, vazio e conteúdo
- Formulário de cadastro e edição, com campos que aparecem conforme a
  propulsão escolhida (PRD §7)
- Exclusão com diálogo de confirmação
- Acesso à lista pelo ícone de veículo no dashboard
- Todos os textos em português, com os enums traduzidos em `VehicleLabels`

**Testes**
- 46 testes novos (85 no total): validação, use case, ViewModel de lista e
  ViewModel de formulário
- `FakeVehicleRepository` e `MainDispatcherRule` para testar ViewModels na JVM

### Decisões registradas

- **A validação devolve o motivo, não a mensagem.** O domínio retorna
  `VehicleValidationError.REQUIRED`; a camada de apresentação escolhe o texto.
  Assim o domínio continua sem `Context` e testável sem Android.
- **Todos os erros de uma vez**, e não o primeiro encontrado. Corrigir um
  campo por vez, com um erro novo aparecendo a cada tentativa, é a forma mais
  eficiente de irritar quem preenche formulário.
- **Trocar a propulsão limpa o que deixou de se aplicar.** A regra vive em
  `VehicleDraft.withPowertrain`, no domínio, e não na tela — assim vale para
  qualquer caminho que altere um veículo.
- **Uma rota só para cadastro e edição.** A tela é a mesma; muda apenas se ela
  começa preenchida.

## [0.1.0] — Foundation

Fundação técnica do projeto. Nenhuma funcionalidade de produto: o objetivo
desta versão é que o build passe, o CI fique verde, o aplicativo abra e o banco
inicial funcione.

### Adicionado

**Build**
- Projeto Gradle Kotlin DSL com version catalog (`gradle/libs.versions.toml`)
- Gradle 9.7.0 via wrapper, AGP 9.3.1, Kotlin 2.4.10, JDK 21
- `compileSdk`/`targetSdk` 36, `minSdk` 26 (Android 8.0, `java.time` nativo)
- Lint com `warningsAsErrors` no gate de build

**Aplicação**
- Single Activity com Jetpack Compose e Material 3
- Tema claro/escuro com Material You no Android 12+
- Navigation Compose com destino de dashboard (marcador)

**Domínio**
- `Money` — valor monetário em `Long` de centavos, com divisão segura que
  retorna `null` em vez de estourar
- `WorkDuration` — tempo online em `Long` de minutos
- `Vehicle` com propulsão, combustível e capacidade de recarga em eixos
  independentes
- Enums `VehiclePowertrain`, `CombustionFuel`, `FuelType`, `MeasurementUnit`,
  `ChargingCapability`, com a unidade de medida embutida no tipo

**Dados**
- Room v1 com a tabela `vehicles` e schema exportado em `app/schemas/`
- `TypeConverters` para `Instant`, `LocalDate` e enums (persistidos por `name`)
- `VehicleDao` e `OfflineVehicleRepository`
- Injeção de dependências manual via `AppContainer`

**Apresentação**
- `BrazilianFormatter` — moeda, duração, data e quilometragem no padrão
  brasileiro, com saída determinística

**Testes**
- Testes unitários de `Money`, `WorkDuration`, `BrazilianFormatter` e `Vehicle`,
  cobrindo divisão por zero, valores negativos, acúmulo sem perda de precisão e
  combinações de propulsão
- Testes instrumentados de `VehicleDao` (inserção, atualização, exclusão,
  consulta)

**Infraestrutura**
- CI no GitHub Actions: validação do wrapper, testes, lint e assemble a cada PR
- Workflow de release disparado por tag `v*.*.*`
- Templates de issue e de Pull Request
- `main` protegida: PR obrigatório, histórico linear, sem force push
- Documentação: README, PRD, ARCHITECTURE, DATABASE, ROADMAP, DEVELOPMENT

### Decisões registradas

- Dinheiro nunca em `Double` — centavos em `Long` (PRD §26)
- Divisão por zero devolve `null`, e a UI exibe `—`, em vez de `R$ 0,00` (PRD §21)
- Propulsão, combustível e recarga em eixos separados, permitindo
  `HYBRID + FLEX + PLUG_IN` sem migração futura (PRD §13)
- DI manual em vez de Hilt, proporcional ao tamanho do projeto (PRD §55)
- `fallbackToDestructiveMigration` proibido (PRD §45)
- `user_id` não criado em `vehicles`: sem login no MVP — ver `DATABASE.md`
