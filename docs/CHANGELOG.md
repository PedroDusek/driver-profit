# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/).
Versionamento conforme [Semantic Versioning](https://semver.org/lang/pt-BR/).

## [Não publicado]

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
