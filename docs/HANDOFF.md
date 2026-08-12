# Handoff — estado do projeto

Documento de continuidade. Leia isto **antes** de qualquer coisa ao retomar o
projeto em uma sessão nova, junto com `PRD.md`, `ARCHITECTURE.md` e
`DEVELOPMENT.md`.

**Última atualização:** v0.4.1

---

## 1. Onde tudo está

| Item | Valor |
| --- | --- |
| Código | `C:\Users\pedro\Desktop\driver-profit` |
| Repositório | https://github.com/PedroDusek/driver-profit (público) |
| Branch estável | `main`, protegida |
| Última tag | `v0.4.1` |
| Versão do banco | **4** |

⚠️ **O caminho não pode conter acento.** O projeto nasceu em
`Desktop\RodAí` e o AGP recusa build com caractere não-ASCII no caminho. Se
essa pasta antiga ainda existir, pode apagar.

## 2. Ambiente

Já instalado e configurado nesta máquina:

- JDK 21, Android SDK 37 + Build-Tools 36, Android Studio
- Gradle 9.7 via wrapper · AGP 9.3.1 · Kotlin 2.4.10
- GitHub CLI autenticado como `PedroDusek`
- `local.properties` aponta para o SDK (não versionado)

**Armadilha do PowerShell 5.1:** `git commit -m` quebra quando a mensagem tem
aspas — o shell parte a string e o git lê os pedaços como pathspec. Escreva a
mensagem em um arquivo e use `git commit -F <arquivo>`. Vale para
`gh pr create --body-file` e `git tag -a -F`.

## 3. Como o produto é hoje

Aplicativo Android offline-first para medir **rentabilidade operacional** de
motorista de aplicativo. Quatro telas, todas alcançáveis pelos ícones do
dashboard:

- **Veículos** — nome + tipo de combustível, só isso
- **Ganhos** — jornada de trabalho: data, plataforma, valor, corridas, tempo,
  km. Histórico com totais e R$/hora, R$/km
- **Despesas** — abastecimento, carregamento, manutenção e outras. Filtro por
  natureza, totais que acompanham o filtro
- **Dashboard** — ainda um marcador; é a v0.5.0

## 4. Decisões de produto que divergem do PRD original

Estas foram tomadas pelo Pedro durante o desenvolvimento e **sobrepõem** o que
o PRD diz. Não "corrija" o código de volta para o PRD.

| Decisão | Onde estava no PRD | O que vale hoje |
| --- | --- | --- |
| Cadastro de veículo mínimo | §5, §14 pediam marca, modelo, ano, odômetro | Só nome + combustível |
| Combustível em lista plana | §13 pedia três eixos (powertrain + fuel + charging) | Um enum `VehicleFuel` com 7 opções, incluindo elétrico e híbrido |
| Quilometragem | §14 a punha no veículo | Não é atributo do veículo; volta na v0.6.0 como recurso de manutenção (troca de óleo, pneus) |
| Quantidade de combustível | §7 a tratava como essencial | **Opcional**. O indicador principal é custo/km, não consumo/km |
| `user_id` | §14 listava | Não existe — sem login no MVP |

**O indicador principal do produto é custo/km.** Consumo (km/L, km/kWh) é
secundário e depende do odômetro por lançamento, que só chega na v0.6.0.

## 5. Decisões técnicas que não devem ser revertidas

- **Dinheiro é `Long` em centavos** (`core/common/Money`). Nunca `Double`
- **Divisão devolve `null`**, não zero: um período sem km não tem R$/km. A UI
  exibe `—`
- **Tempo é `Long` em minutos** (`WorkDuration`); **quantidade é `Long` em
  milésimos** (`Quantity`)
- **A unidade viaja no tipo**: `FuelType.CNG.unit == CUBIC_METER`. Nunca tratar
  GNV em litros
- **Enums no banco são gravados por `name`**, nunca `ordinal`
- **Datas viram inteiros**: `LocalDate` → epoch day, `Instant` → epoch millis
- **Validação devolve o motivo, não a mensagem.** O domínio não conhece
  `Context`; `*Labels` na camada de apresentação traduzem
- **Todos os erros de uma vez**, nunca o primeiro
- **`fallbackToDestructiveMigration` é proibido.** Toda mudança de schema exige
  migração + teste + `DATABASE.md`
- **DI manual** (`core/di/AppContainer`), sem Hilt
- **Campos da jornada são obrigatórios** — zero é resposta válida, branco não.
  Isso existe porque campo em branco entrando como zero inflava o R$/hora
  agregado

## 6. Roadmap

| Versão | Status |
| --- | --- |
| v0.1.0 Foundation | ✅ |
| v0.2.0 / v0.2.1 Vehicle | ✅ |
| v0.3.0 / v0.3.1 Earnings | ✅ |
| v0.4.0 / v0.4.1 Expenses | ✅ |
| **v0.5.0 Dashboard** | ⬅️ **próxima** |
| v0.6.0 Analytics + odômetro por lançamento + alertas de manutenção | |
| v0.7.0 UX polish · v0.8.0 Hardening · v0.9.0 RC · v1.0.0 MVP | |

### O que a v0.5.0 precisa fazer

Indicadores por período (PRD §21), com filtros: hoje, ontem, semana, mês, mês
anterior, personalizado.

```
Faturamento · Despesas · Lucro
Km · Horas · Corridas
R$/km · R$/hora · R$/corrida
Custo/km · Lucro/km · Lucro/hora
```

**Tudo de que ela precisa já existe no banco.** Os dois use cases de consulta
por período estão prontos e testados, sem tela que os use ainda:

- `ObserveWorkSessionsBetweenUseCase`
- `ObserveExpensesBetweenUseCase`

Criar uma classe `DashboardMetrics` pura (PRD §29), testável sem Android, que
receba as duas listas e produza os indicadores. **Não colocar cálculo em
ViewModel nem em Composable.**

Dois pontos de atenção:

1. **Custos fixos.** `ExpenseCategory.isOperationalCost` já separa seguro, IPVA
   e financiamento do resto. Custo/km deve usar só o operacional — seguro não
   varia com o quanto se roda, e jogá-lo no custo/km de um dia distorce o
   indicador (PRD §22).
2. **Divisão por zero.** Use `Money.per()`, que devolve `null`. Exiba `—`, e
   nunca `R$ 0,00`.

## 7. Débito conhecido

**Nenhum teste instrumentado jamais foi executado.** São 4 arquivos em
`app/src/androidTest/`, cobrindo os três DAOs e as quatro migrações
encadeadas (1→2→3→4). Eles compilam e estão no repositório, mas exigem
emulador ou aparelho conectado, e o CI não os roda.

Com um aparelho em depuração USB:

```bash
cd C:/Users/pedro/Desktop/driver-profit && ./gradlew connectedDebugAndroidTest
```

Isso é o item de maior risco do projeto: as migrações nunca tocaram um SQLite
real.

Além disso: **nenhuma tela foi aberta em um aparelho.** Tudo o que existe foi
verificado por teste unitário, lint e build.

## 8. Fluxo de trabalho

```
main → branch → implementa → testes → commits → PR → CI verde → merge rebase → tag
```

- Branches: `feature/`, `fix/`, `chore/`, `docs/`
- Conventional Commits, um commit por alteração lógica
- Merge por **rebase** (preserva os commits individuais), nunca squash
- Gate obrigatório antes de fechar qualquer PR:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

- Cada versão vira tag `vX.Y.Z`, e a tag dispara o workflow de release que
  publica o APK

## 9. Funcionalidades proibidas sem autorização (PRD §48)

GPS, rastreamento, integração com Uber/99, login, backend, Firebase,
publicidade, assinatura, IA, pagamentos, APIs externas.

O manifesto não declara **nenhuma** permissão. Manter assim.

## 10. Pendências de decisão

- **Nome do produto** ainda não é definitivo. `Driver Profit` /
  `com.driverprofit` são placeholders
- **Sem LICENSE**, por escolha do Pedro. Efeito legal: todos os direitos
  reservados
- **Repositório é público** de propósito: no plano Free, proteção de branch só
  funciona em repo público. Se assinar GitHub Pro, dá para voltar a privado
  mantendo as regras
