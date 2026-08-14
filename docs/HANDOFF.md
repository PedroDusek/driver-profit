# Handoff — estado do projeto

Documento de continuidade. Leia isto **antes** de qualquer coisa ao retomar o
projeto em uma sessão nova, junto com `PRD.md`, `ARCHITECTURE.md` e
`DEVELOPMENT.md`.

**Última atualização:** v0.8.0

---

## 1. Onde tudo está

| Item | Valor |
| --- | --- |
| Código | `C:\Users\pedro\Desktop\driver-profit` |
| Repositório | https://github.com/PedroDusek/driver-profit (público) |
| Branch estável | `main`, protegida |
| Última tag | `v0.6.0`. v0.7.0 e v0.8.0 prontas, **sem teste em aparelho** |
| Versão do banco | **6** |

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
  natureza, totais que acompanham o filtro. Odômetro obrigatório onde há
  veículo em jogo
- **Uso pessoal** — quilômetros rodados fora do trabalho, por declaração ou
  por conciliação do odômetro
- **Dashboard** — tela principal. Indicadores do período escolhido: lucro em
  destaque, faturamento e despesas, volume (km, horas, corridas), R$/km,
  R$/hora, R$/corrida, custo/km, lucro/km, lucro/hora e despesas por natureza

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
| **v0.9.0 Manutenção preventiva** | ⬅️ **próxima** |
| v0.10.0 Custos fixos · v0.11.0 Analytics | |
| v0.12.0 UX polish · v0.13.0 Hardening · v0.14.0 RC · v1.0.0 MVP | |

O bloco v0.6.0–v0.10.0 foi desenhado em conjunto: cada versão é pequena,
testável e reversível, e a ordem é de dependência, não de preferência. O
detalhamento com critério de saída e impacto no banco está em
[`ROADMAP.md`](ROADMAP.md); as regras de produto que sustentam tudo isso estão
no PRD §22 e §23.

### ⚠️ Duas versões esperando aparelho

`feature/personal-usage` (v0.7.0) e `feature/fuel-consumption` (v0.8.0) estão
empurradas e **não mergeadas**. A v0.8.0 foi empilhada sobre a v0.7.0.

Elas ficaram fora da `main` de propósito: a v0.7.0 traz a **migração 5→6**, e o
CI não roda teste de migração. Quando houver aparelho, rodar os instrumentados
uma vez cobre as duas — a v0.8.0 não mexe no banco.

### O que a v0.9.0 precisa fazer

**Manutenção preventiva** — alertas por quilometragem: óleo, filtros, pneus,
freios, revisão.

- Intervalo configurável por item
- **Distância mínima implícita por combustível comprado**, como piso
  independente do odômetro: litros × consumo histórico dá um chão que uma
  leitura desatualizada não esconde
- O alerta silencia, ou se declara incompleto, quando o dado não sustenta a
  afirmação

Assimetria que rege esta versão: subestimar km deixa o custo/km pessimista,
mas **atrasa** o alerta de troca de óleo — e isso desgasta motor. O alerta não
herda a degradação graciosa das outras funcionalidades; na dúvida ele pede a
leitura em vez de estimar.

O consumo histórico que o piso exige já existe: `ConsumptionEstimator`, v0.8.0.

### O que já está pronto e não deve ser refeito

- **Custo/km usa o quilômetro total** desde a v0.7.0, com a repartição
  trabalho/pessoal em reais. As duas partes somam exatamente a despesa
  operacional, e é assim de propósito — a tela precisa fechar contra o extrato
- **Financiamento, seguro e IPVA são 100% do trabalho** e ficam fora do rateio
- **Uso pessoal tem dois caminhos de entrada que se abatem**: declaração
  explícita e sobra da conciliação. Nunca somar os dois sem descontar
- **Consumo é tanque-a-tanque, e par com combustível diferente é descartado.**
  Alternar gasolina e etanol muda o consumo em ~30%; a média dos dois não
  descreve nenhum. E ele é **sempre** rotulado estimado (PRD §23)

## 7. Testes instrumentados

São a única verificação real das migrações. Cobrem os quatro DAOs e as seis
migrações encadeadas (1→2→3→4→5→6).

⚠️ **Os testes da migração 5→6 (v0.7.0) ainda não foram executados** — o
aparelho desconectou antes. Na v0.6.0 foram 42 testes, todos passando. Rodar
antes de mergear a v0.7.0.

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
