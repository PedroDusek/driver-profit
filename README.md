# Driver Profit

Aplicativo Android que mede a **rentabilidade operacional real** de motoristas
de aplicativo.

> ⚠️ O nome do produto ainda não é definitivo. `Driver Profit` / `com.driverprofit`
> são placeholders usados até a definição do branding.

## O que é

Motorista de aplicativo costuma saber quanto **faturou**, mas não quanto
**lucrou**. Combustível, manutenção, pedágio e lavagem consomem o resultado sem
aparecer no extrato da plataforma.

O aplicativo registra ganhos e despesas e responde:

| Pergunta | Indicador |
| --- | --- |
| Quanto faturei? | Faturamento |
| Quanto gastei? | Despesas |
| Quanto realmente lucrei? | Lucro líquido operacional |
| Quanto ganho por hora trabalhada? | R$/hora |
| Quanto ganho por quilômetro? | R$/km |
| Quanto rende cada corrida? | R$/corrida |
| Quanto meu veículo custa por km? | Custo/km |
| Estou melhorando ou piorando? | Evolução por período |

## Objetivo

Não é um "controle financeiro" genérico. O produto existe para medir
rentabilidade:

```
FATURAMENTO → DESPESAS → CUSTO OPERACIONAL → LUCRO
FATURAMENTO → R$/HORA · R$/KM · R$/CORRIDA
```

**Offline-first**: registrar lançamentos e consultar o dashboard não depende de
internet. Todo dado vive no aparelho.

## Stack

| Camada | Tecnologia |
| --- | --- |
| Linguagem | Kotlin 2.4 |
| UI | Jetpack Compose + Material 3 |
| Arquitetura | MVVM + Repository, Single Activity |
| Navegação | Navigation Compose |
| Assincronismo | Coroutines + Flow / StateFlow |
| Persistência | Room (SQLite) |
| Injeção de dependências | Manual (`core/di/AppContainer`) |
| Build | Gradle Kotlin DSL 9.7 + AGP 9.3 |
| Testes | JUnit 4, kotlinx-coroutines-test, AndroidX Test |
| CI | GitHub Actions |

Versões exatas: [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

## Requisitos

- **JDK 21** (o Android Studio traz um JBR embutido que serve)
- **Android SDK 36** com Build-Tools 36
- **minSdk 26** (Android 8.0) — escolhido para ter `java.time` nativo

## Como executar

```bash
git clone https://github.com/PedroDusek/driver-profit.git
cd driver-profit
./gradlew assembleDebug
```

Instalar em um aparelho ou emulador conectado:

```bash
./gradlew installDebug
```

No Windows use `gradlew.bat` no lugar de `./gradlew`.

> O caminho do projeto **não pode conter caracteres não-ASCII** nem acentos —
> o AGP recusa o build no Windows nesse caso.

## Como testar

```bash
./gradlew testDebugUnitTest
```

Lint:

```bash
./gradlew lintDebug
```

Testes instrumentados (exigem aparelho ou emulador conectado):

```bash
./gradlew connectedDebugAndroidTest
```

Gate completo, o mesmo que roda no CI:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

## Estrutura

```
app/src/main/java/com/driverprofit/
├── core/          # Infra transversal: DI, navegação, tema, tipos base
│   ├── common/    # Money (centavos), WorkDuration (minutos)
│   ├── di/        # AppContainer
│   ├── navigation/
│   └── ui/        # theme, format
├── data/          # Room, DAOs, entities, repositories
│   ├── local/
│   └── repository/
├── domain/        # Modelos, contratos de repositório, use cases
│   ├── model/
│   ├── repository/
│   └── usecase/
└── feature/       # Uma pasta por tela
    └── dashboard/
```

Regra de dependência: `feature → domain → data`. A UI **nunca** acessa DAO.
Detalhes em [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Documentação

| Documento | Conteúdo |
| --- | --- |
| [PRD.md](docs/PRD.md) | Especificação de produto |
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | Camadas, regras e decisões técnicas |
| [DATABASE.md](docs/DATABASE.md) | Entidades, schema e migrações |
| [ROADMAP.md](docs/ROADMAP.md) | Versões planejadas |
| [CHANGELOG.md](docs/CHANGELOG.md) | Histórico de alterações |
| [DEVELOPMENT.md](docs/DEVELOPMENT.md) | Como contribuir, branches, commits |
| [HANDOFF.md](docs/HANDOFF.md) | Estado atual do projeto — leia ao retomar |

## Status atual

**v0.2.0 — Vehicle** (em desenvolvimento)

| Item | Status |
| --- | --- |
| Projeto Gradle + Compose + Material 3 | ✅ v0.1.0 |
| Navegação Single Activity | ✅ v0.1.0 |
| Room + schema exportado | ✅ v0.1.0 |
| Repository + DI manual | ✅ v0.1.0 |
| Tipos base (`Money`, `WorkDuration`) + testes | ✅ v0.1.0 |
| CI no GitHub Actions | ✅ v0.1.0 |
| Documentação inicial | ✅ v0.1.0 |
| Cadastro, edição e exclusão de veículo | ✅ v0.2.0 |
| Cadastro simplificado (nome + combustível) | ✅ v0.2.1 |
| Migração de schema 1→2 | ✅ v0.2.1 |
| Registro de ganhos e histórico | ✅ v0.3.0 |
| R$/hora e R$/km do histórico | ✅ v0.3.0 |
| Despesas: abastecimento, recarga, manutenção e outras | ✅ v0.4.0 |
| R$/litro, R$/m³ e R$/kWh | ✅ v0.4.0 |
| Dashboard com lucro e custo/km | ⏳ v0.5.0 |
| Registro de despesas | ⏳ v0.4.0 |
| Dashboard com indicadores | ⏳ v0.5.0 |

Roadmap completo em [`docs/ROADMAP.md`](docs/ROADMAP.md).
