# Handoff — estado do projeto

Documento de continuidade. Leia isto **antes** de qualquer coisa ao retomar o
projeto em uma sessão nova, junto com `PRD.md`, `ARCHITECTURE.md` e
`DEVELOPMENT.md`.

**Última atualização:** v0.13.0

---

## 0. Comece por aqui

**Está tudo na `main`, publicado.** v0.11.0 e v0.12.0 — pedidos de produto do
Pedro, não itens do roadmap original — foram mergeadas por fast-forward,
tagueadas, empurradas para `origin` e liberadas com sucesso (CI e workflow de
Release verdes, `gh release list` confirma os dois). v0.13.0 está pronta numa
branch de feature (`feature/backup-export-import`), aguardando o mesmo
caminho.

| Versão | Tag | Banco | Release |
| --- | --- | --- | --- |
| v0.7.0 Uso pessoal | ✅ | 6 | ✅ |
| v0.8.0 Consumo estimado | ✅ | 6 | ✅ |
| v0.9.0 Manutenção preventiva | ✅ | **7** | ✅ |
| v0.9.1 Ciclo do odômetro | ✅ | 7 | ✅ |
| v0.10.0 Custos fixos por competência | ✅ | **8** | ✅ |
| v0.10.1 Resolver a sobra do odômetro | ✅ | **9** | ✅ |
| v0.11.0 IPVA sem competência | ✅ | 9 | ✅ |
| v0.12.0 Veículo atual | ✅ | **10** | ✅ |
| v0.13.0 Exportar e importar arquivo | branch pronta | 10 | — |

O PRD §58 volta a valer: dá para fazer `git checkout v0.8.0` e reproduzir aquele
estado.

⚠️ **`app/build.gradle.kts` também ficou parado em `0.10.1` por duas versões**
— `versionCode`/`versionName` não acompanharam as tags v0.11.0 e v0.12.0,
apesar de `DEVELOPMENT.md` pedir isso a cada release. Corrigido num commit
avulso (`chore(release): bump versionCode/versionName to 0.12.0`) depois do
fato; a lição é conferir isso **antes** de taguear, não depois.

### O que fazer primeiro

**A última funcionalidade essencial do MVP já entrou.** O que resta é qualidade,
não escopo:

1. ✅ **Exportar e importar arquivo** — v0.13.0, pronta na branch
   `feature/backup-export-import`
2. **Crash handling.** Erro não tratado hoje fecha o app sem deixar rastro
3. **Testes de fluxo** — cadastrar veículo → lançar ganho → lançar despesa →
   conferir dashboard. Nenhuma tela tem verificação automatizada

**Robolectric foi descartado.** As quedas de cabo desta sessão foram físicas, não
falha de setup: com o aparelho parado, `connectedDebugAndroidTest` roda em menos
de um minuto. Montar uma segunda infraestrutura de teste para evitar um passo
manual de um minuto não se paga. O que resta do argumento — o CI nunca roda esses
testes, então eles dependem de alguém lembrar — virou regra no template de PR.

Se um dia isso mudar, a pista é o Room 2.8.4: desde a 2.7 o `MigrationTestHelper`
tem um construtor que roda na JVM com SQLite empacotado, **sem Robolectric**.

### O que já foi verificado, e o que não foi

- **76 testes instrumentados** confirmados em Redmi Note 8 Pro (Android 9),
  17/08/2026, banco na versão 10 — ver seção 7
- **Migração validada em dados reais**: aparelho com banco na versão 5 recebeu a
  v0.9.0 por cima, migrou 5→6→7, abriu sem exceção e manteve os dados. Como o
  Room confere o schema contra o hash esperado ao abrir, isso prova que as
  migrações escritas à mão produzem exatamente a estrutura que ele espera
- **Os onze indicadores do dashboard foram conferidos à mão** contra lançamentos
  reais, na v0.9.1. Bateram todos
- ⚠️ **Nenhuma tela tem teste automatizado.** O `androidTest` só cobre
  `data/local`. Toda verificação de interface até hoje foi feita a olho

### Armadilhas descobertas em 16/08/2026

- **O "erro vazio" do `adb install` não é da MIUI — é CRLF.** O adb emite
  `
`, e cortar a saída com `tail -1` ou `head -1` no Git Bash faz a linha
  parecer vazia. A mensagem `Success` está lá. Passar a saída por `tr -d '
'`
  resolve, e vale para todo comando do adb nesta máquina
- **Caminho remoto some no `adb push`.** O MSYS converte `/data/local/tmp` em
  `C:/Program Files/Git/data/...`. Use `MSYS_NO_PATHCONV=1` **e** caminho local
  no formato Windows, porque a variável desliga a conversão dos dois lados
- **O aparelho cai e volta como `offline`.** `adb reconnect` não resolve; o que
  destrava é desbloquear a tela e reautorizar a depuração USB
- **Merge por rebase reescreve os SHAs**, então branches empilhadas ficam órfãs e
  o PR seguinte aparece como `CONFLICTING`. A saída é rebasear cada branch sobre
  a `main` nova antes do merge dela — o git descarta os commits duplicados pelo
  conteúdo sozinho. `--force-with-lease`, e **só em branch de feature**

---

## 1. Onde tudo está

| Item | Valor |
| --- | --- |
| Código | `C:\Users\pedro\Desktop\driver-profit` |
| Repositório | https://github.com/PedroDusek/driver-profit (público) |
| Branch estável | `main`, protegida |
| Última tag | `v0.12.0`, sem release publicado (ver aviso na seção 0) |
| Versão do banco | **10** |

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
motorista de aplicativo. Seis telas, todas alcançáveis pelos ícones do
dashboard:

- **Veículos** — nome + tipo de combustível, só isso
- **Ganhos** — jornada de trabalho: data, plataforma, valor, corridas, tempo,
  km. Histórico com totais e R$/hora, R$/km
- **Despesas** — abastecimento, carregamento, manutenção e outras. Filtro por
  natureza, totais que acompanham o filtro. Odômetro obrigatório onde há
  veículo em jogo
- **Uso pessoal** — quilômetros rodados fora do trabalho, por declaração ou
  por conciliação do odômetro
- **Manutenção** — óleo, filtros, freios, pneus e revisão, com intervalo
  editável por item. Diz "sem dados" quando não tem marco de onde contar
- **Dashboard** — tela principal. Indicadores do período escolhido: lucro em
  destaque, faturamento e despesas, volume (km, horas, corridas), R$/km,
  R$/hora, R$/corrida, custo/km, lucro/km, lucro/hora e despesas por natureza
- **Exportar e importar** (v0.13.0) — backup manual que o motorista vê,
  guarda e leva para outro aparelho. Exportar copia o banco; importar
  substitui tudo (sem mesclar) e pede para fechar e reabrir o app

No topo do dashboard aparecem dois avisos, e **nenhum dos dois pertence ao
período selecionado** — cada um declara na tela a que intervalo se refere:

- **Odômetro não confere**, quando há janela entre leituras com quilometragem
  sem explicação. Toca e cai no diálogo com três respostas: uso pessoal,
  jornada não lançada, ou **"Deixar de fora"** — a sobra sai dos totais e o
  app para de perguntar por aquela janela, com a consequência (custo/km um
  pouco mais alto) informada antes. Sobra negativa não aparece mais aqui: é
  inconsistência entre dois números do próprio motorista, não distância
  faltando, e segue calculada por baixo dos panos para as janelas se
  cancelarem
- **Manutenção pede atenção**, quando há item vencido ou próximo

## 4. Decisões de produto que divergem do PRD original

Estas foram tomadas pelo Pedro durante o desenvolvimento e **sobrepõem** o que
o PRD diz. Não "corrija" o código de volta para o PRD.

| Decisão | Onde estava no PRD | O que vale hoje |
| --- | --- | --- |
| Cadastro de veículo mínimo | §5, §14 pediam marca, modelo, ano, odômetro | Só nome + combustível |
| Combustível em lista plana | §13 pedia três eixos (powertrain + fuel + charging) | Um enum `VehicleFuel` com 7 opções, incluindo elétrico e híbrido |
| Quilometragem | §14 a punha no veículo | Não é atributo do veículo. Voltou na v0.6.0 como leitura **por lançamento**, obrigatória em abastecimento, recarga e manutenção |
| Quantidade de combustível | §7 a tratava como essencial | **Opcional**. O indicador principal é custo/km, não consumo/km |
| `user_id` | §14 listava | Não existe — sem login no MVP |

**O indicador principal do produto é custo/km.** Consumo (km/L, km/kWh) é
secundário; o odômetro por lançamento já existe desde a v0.6.0, e o consumo
estimado em si chega na v0.8.0.

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
- **Custo/km usa só despesa operacional.** Seguro, IPVA e financiamento entram
  no lucro, mas não na razão por quilômetro — eles não variam com o quanto se
  roda (PRD §22). O rateio deles é da v0.10.0
- **Custo/km divide pela distância total**, incluindo a pessoal — o rateio
  proporcional se cancela, então não é preciso estimar consumo nem preço médio
- **A semana do dashboard é ISO, segunda a domingo**, fixada no código e não
  lida do `Locale`
- **`Clock` é injetado** onde a regra depende da data corrente
  (`DashboardViewModel`, `VehicleValidator`)
- **Subestimar km e superestimar km não custam a mesma coisa.** No custo/km o
  erro é só pessimismo; no alerta de manutenção ele atrasa a troca de óleo e
  desgasta motor. Por isso o `MaintenanceMonitor` usa o **maior** entre a
  diferença de odômetro e o piso por combustível comprado, e nunca afirma "em
  dia" sobre item sem marco
- **O lint não roda a checagem `Typos`**: ela usa dicionário inglês e acusava
  palavras portuguesas, quebrando o build com `warningsAsErrors`

## 6. Roadmap

| Versão | Status |
| --- | --- |
| v0.1.0 Foundation | ✅ |
| v0.2.0 / v0.2.1 Vehicle | ✅ |
| v0.3.0 / v0.3.1 Earnings | ✅ |
| v0.4.0 / v0.4.1 Expenses | ✅ |
| v0.5.0 Dashboard | ✅ |
| v0.6.0 Odômetro · v0.7.0 Uso pessoal · v0.8.0 Consumo | ✅ |
| v0.9.0 Manutenção preventiva · v0.9.1 Ciclo do odômetro | ✅ |
| v0.10.0 Custos fixos por competência | ✅ |
| v0.10.1 Resolver a sobra do odômetro | ✅ |
| v0.11.0 IPVA sem competência | ✅ |
| v0.12.0 Veículo atual | ✅ |
| **v0.13.0 Exportar e importar arquivo** | ⬅️ pronta, branch não mergeada |
| v0.14.0 Crash handling · v0.15.0 Testes de fluxo | |
| v0.16.0 Analytics · v0.17.0 UX polish · v0.18.0 Hardening · v0.19.0 RC · v1.0.0 MVP | |

O bloco v0.6.0–v0.10.0 foi desenhado em conjunto: cada versão é pequena,
testável e reversível, e a ordem é de dependência, não de preferência. O
detalhamento com critério de saída e impacto no banco está em
[`ROADMAP.md`](ROADMAP.md); as regras de produto que sustentam tudo isso estão
no PRD §22 e §23.

### O que falta para o MVP

Menos funcionalidade do que a contagem de versões sugere, e mais consolidação do
que o roadmap deixa transparecer.

**Funcionalidade essencial: nenhuma.** A v0.10.0 fechou a última. v0.11.0
(IPVA sem competência) e v0.12.0 (veículo atual) foram pedidos de produto do
Pedro, já mergeados e tagueados. v0.13.0 (analytics) é desejável e não
bloqueia o MVP.

**O que separa "funciona" de "lançável"**, e é o grosso:

- **Zero testes de interface.** Nenhuma tela tem verificação automatizada. Para
  um app cujo produto é a exatidão de um número, é a maior lacuna aberta
- **Sem exportação manual.** O Auto Backup do Android existe (e a v0.10.1
  corrigiu um bug em que ele perdia o WAL), mas é invisível — o motorista não
  tem como conferir se funcionou, e some se o backup do sistema estiver
  desligado. Exportar/importar arquivo é o item 1 da seção 0
- **Sem crash handling.** Erro não tratado fecha o app sem deixar rastro

**Três decisões que congelam na primeira instalação de terceiro** (PRD §48):

- **O nome do produto ainda é placeholder.** Trocar o `applicationId` depois
  **apaga os dados de quem já instalou**
- **Não há build de release assinado.** A release publica APK de *debug*, e
  assinatura de debug e de release não se atualizam entre si
- **Sem LICENSE**, por escolha do Pedro — efeito é todos os direitos reservados

### O que a v0.10.0 fez

**Custos fixos por competência** — separou "quando paguei" de "a que período o
valor se refere" (PRD §22).

- Início e fim de competência na despesa, anuláveis
- Valor diluído pelos dias do período
- Financiamento, seguro e IPVA atribuídos **100% ao trabalho**
- Custo fixo por km trabalhado
- Migração **7→8**

Histórico e "Despesas" continuam exibindo **caixa**, para conferir com o
extrato. Só os indicadores por km usam competência.

### O que a v0.10.1 fez

**Resolver a sobra do odômetro** — a conciliação tinha duas saídas (uso
pessoal ou jornada não lançada) e nenhuma servia para a sobra pequena e
inexplicável, o caso mais comum. Sem terceira opção, o aviso ficava para
sempre.

- **"Deixar de fora"**, gravando a quantidade dispensada (não só o intervalo)
  em `reconciliation_dismissals`. A dispensa caduca se a sobra crescer além
  do aceito
- **Sobra negativa deixa de ser exibida** — é inconsistência entre dois
  números do próprio motorista, não distância faltando, e não há fonte
  externa contra a qual conferir. Continua calculada por baixo dos panos,
  para janelas encadeadas se cancelarem
- Migração **8→9**

### O que já está pronto e não deve ser refeito

- **Custo/km usa o quilômetro total** desde a v0.7.0, com a repartição
  trabalho/pessoal em reais. As duas partes somam exatamente a despesa
  operacional, e é assim de propósito — a tela precisa fechar contra o extrato
- **Financiamento, seguro e IPVA são 100% do trabalho** e ficam fora do rateio
- **Uso pessoal tem dois caminhos de entrada que se abatem**: declaração
  explícita e sobra da conciliação. Nunca somar os dois sem descontar
- **O alerta de manutenção não estima na dúvida** — é a única funcionalidade
  que se recusa a trabalhar com o que tem. Item sem marco diz "sem dados", e
  o piso por combustível comprado vence odômetro atrasado. Ver seção 5
- **Intervalo de manutenção só vira linha no banco quando o motorista o
  altera.** Ausência significa padrão do app; voltar ao padrão é apagar a linha
- **Consumo é tanque-a-tanque, e par com combustível diferente é descartado.**
  Alternar gasolina e etanol muda o consumo em ~30%; a média dos dois não
  descreve nenhum. E ele é **sempre** rotulado estimado (PRD §23)

## 7. Testes instrumentados

São a única verificação real das migrações — e, desde a v0.13.0, também de
exportar/importar backup, que é I/O de verdade e não cabe em teste unitário.
Cobrem cinco dos seis DAOs, as dez migrações encadeadas
(1→2→3→4→5→6→7→8→9→10) e o roundtrip de backup.

✅ **Última execução confirmada em dispositivo: 17/08/2026, 81 testes, todos
passando**, em Redmi Note 8 Pro com Android 9, na branch
`feature/backup-export-import` (banco 10, igual ao de `main`):
`MigrationTest` 27, `ExpenseDaoTest` 14, `WorkSessionDaoTest` 10,
`VehicleDaoTest` 12, `MaintenanceScheduleDaoTest` 8, `PersonalUsageDaoTest` 5,
`BackupTest` 5 (novo).

⚠️ **`ReconciliationDismissalDao` não tem teste instrumentado dedicado.** É
o único DAO dos seis sem cobertura própria — a tabela nova da v0.10.1 é
exercitada só via `MigrationTest`.

⚠️ **`BackupTest` opera no caminho real do banco** (`context.getDatabasePath
(DriverProfitDatabase.NAME)`), o mesmo que `AppContainer` usa em produção —
diferente dos DAOs, que usam `Room.inMemoryDatabaseBuilder`. É proposital:
exportar/importar são operações sobre o **arquivo**, não sobre a conexão, e
banco em memória não tem arquivo para copiar. O teste limpa antes e depois
(`@Before`/`@After`), então não interfere com os outros — mas por isso mesmo
não pode rodar em paralelo com outro teste que também toque
`driver_profit.db` fora de memória.

⚠️ **Ele desinstala os dois pacotes ao terminar, e isso apaga os dados.** Se
você quer exercitar uma migração em cima de dados reais, faça isso **antes** de
rodar os instrumentados — ou tenha o banco salvo. Puxar uma cópia com
`adb exec-out run-as <pacote> cat databases/driver_profit.db` custa segundos e
já salvou esta sessão.

```bash
cd C:/Users/pedro/Desktop/driver-profit && ./gradlew connectedDebugAndroidTest
```

⚠️ **O CI não roda esses testes** — eles exigem aparelho ou emulador. Cada
mudança de schema precisa dessa execução manual antes do merge; o gate verde
do PR não cobre migração.

Para rodar num Xiaomi/MIUI, três chaves precisam estar ligadas em Opções do
desenvolvedor: **Depuração USB**, **Instalar via USB** e **Depuração USB
(Configurações de segurança)**. Se o `adb` mostrar `offline` e não sair desse
estado, reiniciar o aparelho destrava — alternar a chave já não resolve depois
que o `adbd` engasga.

**Quando o Gradle falhar com `INSTALL_FAILED_USER_RESTRICTED`** — a MIUI
recusando a instalação — instale os dois APKs à mão e chame o runner direto:

```bash
adb install -r -t app/build/outputs/apk/debug/app-debug.apk
adb install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.driverprofit.debug.test/androidx.test.runner.AndroidJUnitRunner
```

⚠️ O `connectedDebugAndroidTest` **desinstala os dois pacotes ao terminar**. Se
o app sumir do celular depois de rodar os testes, não é defeito — é só
reinstalar.

O que **continua sem verificação**: nenhum fluxo de interface tem teste. O app
é usável, mas as telas só foram exercitadas por teste de ViewModel, lint e
build.

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
