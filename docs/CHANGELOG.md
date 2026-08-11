# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/).
Versionamento conforme [Semantic Versioning](https://semver.org/lang/pt-BR/).

## [Não publicado]

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
