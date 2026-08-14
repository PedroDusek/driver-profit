# PRD — Driver Profit

**Documento:** Product Requirements
**Plataforma inicial:** Android
**Versão da especificação:** 1.0.0

Este documento é a fonte de verdade do produto. As seções são referenciadas no
código e nos demais documentos como "PRD §n".

---

## 1. Visão do produto

Aplicativo Android para motoristas de aplicativos de transporte. Permite
registrar rapidamente ganhos, corridas, horas online, quilômetros rodados,
abastecimentos, recargas, manutenções e outras despesas — e a partir disso
calcular a **rentabilidade real**.

Perguntas que o app precisa responder:

- Quanto eu faturei hoje?
- Quanto gastei hoje?
- Quanto realmente lucrei?
- Quanto estou ganhando por hora?
- Quanto estou ganhando por quilômetro?
- Quanto cada corrida está rendendo?
- Quanto meu veículo custa por quilômetro?
- Meu desempenho está melhorando ou piorando?

**Offline-first.** No MVP não há dependência de internet para registrar ganhos,
despesas ou consultar o dashboard. Armazenamento local.

## 2. Princípio fundamental

O aplicativo **não** é um controle financeiro genérico. Ele mede
**rentabilidade operacional**.

```
FATURAMENTO → DESPESAS → CUSTO OPERACIONAL → LUCRO
FATURAMENTO → R$/HORA · R$/KM · R$/CORRIDA
```

Esses indicadores são o principal valor do produto.

## 3. Tecnologia

Kotlin · Jetpack Compose · Material 3 · Single Activity · Navigation Compose ·
ViewModel · Coroutines · Flow/StateFlow · Room · Repository Pattern · MVVM ·
Gradle Kotlin DSL · JUnit · AndroidX Test.

A UI não acessa DAO nem banco diretamente:

```
Compose UI → ViewModel → Use Case/Domain → Repository → DAO → Room
```

## 4. Objetivo do MVP

1. Cadastro do veículo
2. Configuração do tipo de propulsão
3. Registro de ganhos
4. Registro de abastecimentos/recargas
5. Registro de manutenção
6. Registro de outras despesas
7. Histórico
8. Dashboard
9. Filtros por período
10. Cálculo de indicadores
11. Persistência local
12. Testes automatizados
13. CI no GitHub
14. Versionamento por Git
15. Tags de release

Não implementar funcionalidades futuras antes de o MVP estar estável.

## 5. Veículo

Deve ser cadastrado antes que o usuário registre despesas relacionadas a ele.

Campos: marca, modelo, ano, quilometragem inicial, tipo de propulsão.

## 6–13. Combustível

O sistema usa enums, nunca strings espalhadas pelo código.

> **Revisão de produto (v0.2.1).** A especificação original pedia três eixos
> independentes — `powertrain` + `combustionFuel` + `chargingCapability`
> (§13). Isso foi substituído por um campo único:
>
> ```
> Vehicle
> ├── name    "Onix branco"
> └── fuel    GASOLINE | ETHANOL | FLEX | CNG | FLEX_CNG | ELECTRIC | HYBRID
> ```
>
> Motivo: o cadastro precisa do mínimo para calcular custo de abastecimento, e
> a complexidade dos três eixos não se pagava num formulário preenchido uma
> vez. `ELECTRIC` e `HYBRID` continuam na lista, então tudo o que as seções
> §11 e §12 exigem segue atendido. O que se perdeu foi a distinção explícita
> entre híbrido plug-in e convencional — se voltar a ser necessária, entra por
> migração. Ver `docs/DATABASE.md`.
>
> Marca, modelo, ano e odômetro inicial também saíram do cadastro: nenhum
> deles entra em conta de rentabilidade.

### Formulário de abastecimento (§7)

Dinâmico conforme o veículo cadastrado. O usuário nunca deve poder escolher um
combustível incompatível com o próprio veículo.

**Gasolina e etanol (§7, §8)** — data, litros, valor total, preço por litro,
odômetro, posto, observação.

```
preço/litro = valor total / litros
```

**Flex (§9)** — permite escolher gasolina ou etanol a cada abastecimento. Cada
registro guarda qual combustível foi usado, mantendo histórico separado. Isso
viabiliza, no futuro: custo/km por combustível, consumo médio por combustível e
comparação entre eles.

**GNV (§10)** — unidade é **m³**, nunca litros.

```
preço/m³ = valor total / quantidade
```

**Elétrico (§11)** — não usa litros. Registra data, energia em kWh, valor
total, preço por kWh, odômetro, local e tipo de carregamento (residencial,
comercial, público, outro), observação.

```
preço/kWh = valor total / kWh
```

A estrutura deve permitir carregamento gratuito: `kWh > 0` e `valor = 0`.

**Híbrido (§12)** — possui motor elétrico **e** motor a combustão. Permite
registrar abastecimento e, quando aplicável, carregamento. O modelo **não**
presume que todo híbrido seja plug-in:

| Veículo | powertrain | chargingCapability |
| --- | --- | --- |
| Híbrido convencional | `HYBRID` | `NONE` |
| Híbrido plug-in | `HYBRID` | `PLUG_IN` |

A interface se adapta automaticamente.

## 14. Modelo de dados do veículo

Especificação original:

```
Vehicle
 ├── id · userId · brand · model · year · initialOdometer
 ├── powertrain · combustionFuel · chargingCapability
 └── createdAt
```

Implementado a partir da v0.2.1:

```
Vehicle
 ├── id
 ├── name
 ├── fuel
 └── createdAt
```

> Desvios registrados, ambos detalhados em `DATABASE.md`:
> - `userId` não foi criado — o MVP não tem login (§48)
> - marca, modelo, ano, odômetro inicial e os três eixos de propulsão foram
>   removidos na v0.2.1, por decisão de produto
>
> A quilometragem volta na v0.6.0 como registro por lançamento, servindo a
> controles de manutenção — não como atributo do veículo.

## 15–16. Registro de ganhos

Sessão de trabalho com: data, plataforma, número de corridas, valor recebido,
horas online, km rodados, observação.

Plataformas iniciais: Uber, 99, InDrive, Outra. A arquitetura deve permitir
adicionar plataformas sem alterar o banco.

A plataforma é armazenada em cada registro para viabilizar, no futuro,
R$/hora, R$/km e R$/corrida por plataforma e comparação entre elas. Essa
análise não precisa existir no MVP, mas o banco precisa permitir.

## 17. Despesas

Categorias: combustível, carregamento, manutenção, lavagem, pedágio,
estacionamento, seguro, IPVA, financiamento, outros.

Não usar tabela rígida que impeça adicionar categorias depois.

## 18. Manutenção

Categorias: óleo, filtros, pneus, freios, suspensão, bateria, correia, peças,
elétrica, mecânica, funilaria, revisão, outros.

Campos: data, categoria, descrição, valor, odômetro, oficina, observação.

## 19. Histórico

Lista todos os lançamentos com data, tipo, categoria, descrição e valor.
Permite editar, excluir, filtrar e ordenar.

Filtros: todos, ganhos, combustível, carregamento, manutenção, outras despesas.

## 20–22. Dashboard

Tela principal. Filtros de período: hoje, ontem, esta semana, este mês, mês
anterior, período personalizado.

### Indicadores (§21)

| Indicador | Cálculo |
| --- | --- |
| Faturamento | soma dos ganhos |
| Despesas | soma das despesas |
| Lucro líquido operacional | faturamento − despesas |
| Km rodados | soma dos quilômetros |
| Horas online | soma das horas |
| Corridas | soma das corridas |
| Ganho/km | faturamento / km |
| Ganho/hora | faturamento / horas |
| Ganho/corrida | faturamento / corridas |
| Custo/km | despesas / km |
| Lucro/km | lucro / km |
| Lucro/hora | lucro / horas |

**Nunca permitir divisão por zero.**

### Separação de custos (§22)

Custo de combustível, custo de energia, custo de manutenção e outros custos são
separados, permitindo análises como:

```
Combustível:       R$ 0,42/km
Manutenção:        R$ 0,11/km
Outros:            R$ 0,05/km
Custo operacional: R$ 0,58/km
```

#### Uso pessoal do veículo

> **Decisão de produto registrada.** Vale a partir da v0.7.0 e substitui o
> comportamento das v0.5.0 e v0.6.0.

O mesmo carro serve ao trabalho e à vida do motorista. Até a v0.6.0 o custo/km
divide **despesa total** por quilômetros **apenas profissionais** — o
combustível queimado no fim de semana entra no numerador e não no denominador,
inflando o indicador central do produto.

A correção não é classificar despesas. Combustível é fungível: um tanque não é
"pessoal" nem "profissional", ele queima nos dois. O rateio proporcional se
cancela algebricamente:

```
(E × Kp/K) ÷ Kp  =  E ÷ K
```

Ou seja: **basta o denominador ser o quilômetro total.** Não é necessário
estimar consumo (km/L) nem preço médio de combustível, e nenhuma despesa
gravada é alterada.

A tela exibe uma taxa e dois valores, que somam a despesa do período:

```
Custo real por km                  R$ 0,90

  Trabalho     800 km              R$ 720
  Pessoal      200 km              R$ 180
                                   ──────
                                   R$ 900
```

**A base do custo por km e a base do rateio são a mesma:** todo o custo
operacional — combustível, energia, manutenção, lavagem — e não apenas
combustível. Rodar por lazer também consome pneu e antecipa a troca de óleo;
deixar manutenção fora do rateio a cobraria integralmente do trabalho, que é a
mesma distorção que esta seção corrige.

O lucro desconta apenas a parcela profissional: `Faturamento − E × Kp ÷ K`.

**Como o quilômetro pessoal é conhecido.** Dois mecanismos que se abatem, para
não haver dupla contagem:

1. **Lançamento explícito** — data ou intervalo mais quilômetros. Põe a viagem
   no período em que ela aconteceu, em vez de borrá-la entre meses.
2. **Conciliação por odômetro** — o resíduo entre a leitura, os quilômetros de
   trabalho lançados e os pessoais já declarados. Captura o que ele nunca
   registrou, sem depender de memória.

O resíduo é distribuído proporcionalmente aos dias do intervalo, e o app
**pergunta** se ele é uso pessoal ou jornada não lançada — os dois têm sinais
opostos no custo/km.

Sem dado de uso pessoal, o app calcula com o que tem e **declara a limitação
na tela**. O estado "sem correção" é ausência de dado, nunca uma chave que o
motorista liga e desliga: um indicador que muda de definição conforme um botão
deixa de ser conferível, e a versão desligada é sempre a errada.

#### A janela da conciliação

> **Decisão registrada.** Desenho fechado para a v0.9.1.

A conciliação acontece **entre duas leituras de odômetro**, e não em recorte de
calendário.

O calendário não sabe nada sobre o carro. O único intervalo em que a diferença
de odômetro é um fato é o que vai de uma leitura à seguinte; amarrá-la ao mês
obrigaria a estimar o que aconteceu quando o mês termina no meio de dois
abastecimentos — precisamente o que esta seção existe para evitar.

Como o odômetro é obrigatório em abastecimento, recarga e manutenção, a leitura
chega sozinha, no ritmo em que o motorista usa o carro. Isso torna a cadência de
lançamento irrelevante: registrar diariamente, semanalmente ou depois de um mês
sumido produz a mesma conta, sem caso especial.

A conta da janela é:

```
sobra = (leitura nova − leitura anterior) − km de jornada − pessoal já declarado
```

**A sobra pode ser negativa, e isso é informação, não erro a esconder.** Uma
jornada alocada na janela errada infla uma sobra e desinfla a seguinte, e as
duas se cancelam — desde que a negativa seja preservada. Zerá-la quebra o
cancelamento e transforma um erro passageiro em quilometragem pessoal gravada
para sempre.

Sobra negativa não é uso pessoal negativo: é sinal de que um número foi lançado
errado — km de jornada inflado, ou leitura digitada baixa. É o único sinal de
erro de digitação que o app consegue produzir sozinho, e por isso ele avisa em
vez de engolir.

#### Declaração de ignorância no odômetro

> **Decisão registrada.** Desenho fechado para a v0.9.1.

O odômetro é obrigatório em abastecimento, recarga e manutenção (§23). A
obrigatoriedade vale para o lançamento do dia, quando o painel está à mão.

Para **lançamento retroativo** — data anterior à última leitura conhecida
daquele veículo — o formulário oferece declarar explicitamente que a leitura é
desconhecida, gravando ausência de valor. É o caso de quem instala o app e lança
o histórico do mês passado, ou de quem lança a semana toda no domingo: nota de
posto não traz odômetro, e o número não existe para ser lembrado.

Isso **não** afrouxa a regra do campo obrigatório, que proíbe branco tratado
como zero em silêncio (§16, v0.3.1). Uma declaração explícita é o oposto de um
branco: ausência já significa "não sei" em todo o cálculo — o consumo pula o
par, o alerta de manutenção não usa como marco, a conciliação não conta a
janela.

A opção **não** aparece no lançamento do dia. Disponível sempre, viraria saída
fácil rotineira, e a leitura por lançamento — fundação de três funcionalidades —
deixaria de existir na prática.

O que se perde é assumido: despesa antiga sem leitura conta no dinheiro e não
conta na distância. É melhor que a alternativa, porque odômetro inventado
envenena consumo, quilômetro pessoal e alerta de manutenção de uma vez — e neste
último produz um alvo falso exibido com confiança.

#### Atribuição por natureza de custo

Nem todo custo se rateia da mesma forma, porque nem todo custo é causado pelo
uso:

| Natureza | Atribuição | Motivo |
| --- | --- | --- |
| Combustível, energia, manutenção, lavagem, pedágio, estacionamento | proporcional aos km | o uso é que consome |
| **Financiamento, seguro, IPVA** | **100% ao trabalho** | existem pela decisão de ter o carro para trabalhar |

Dirigir no domingo consome combustível e desgasta pneu, então esses custos se
rateiam. Mas levar o carro ao mercado **não gera parcela de financiamento**,
não aumenta o prêmio do seguro e não muda o IPVA — esses valores existiriam
idênticos se o carro ficasse parado. Ratear um custo fixo pelo uso é atribuí-lo
a algo que não o causou.

Os três custos fixos seguem uma regra única, e não duas, para que a explicação
ao motorista caiba numa frase e o cálculo caiba num teste.

> **Simplificação assumida.** Pedágio e estacionamento são rateados junto com o
> combustível, embora sejam atribuíveis — o motorista sabe de qual viagem foram.
> Marcá-los individualmente é refinamento pós-MVP; o erro é pequeno diante do
> combustível.

#### Competência dos custos fixos

Custo fixo é pago em bloco e serve a um período. Sem separar as duas coisas, o
IPVA de R$ 1.200 pago em janeiro faz janeiro parecer catastrófico e o resto do
ano parecer isento — e nenhum dos doze meses diz a verdade.

Toda despesa tem uma data (quando o dinheiro saiu). Custos fixos ganham também
um **período de competência** (a que intervalo o valor se refere), e o custo
diário é o valor dividido pelos dias desse intervalo:

| Lançamento | Pago em | Competência | Custo diário |
| --- | --- | --- | --- |
| IPVA R$ 1.200 | 15/01 | 01/01 – 31/12 | R$ 3,29 |
| Seguro R$ 300 | 05/03 | 01/03 – 31/03 | R$ 9,68 |
| Parcela R$ 1.200 | 10/04 | 01/04 – 30/04 | R$ 40,00 |
| Combustível R$ 150 | 12/04 | — | conta no dia |

Sem competência declarada, a despesa conta no próprio dia — o comportamento
que já existe.

**Histórico e "Despesas" continuam exibindo caixa**, para conferir com o
extrato; apenas os indicadores por quilômetro usam competência. Resultado por
competência é assunto pós-MVP.

## 23. Consumo

Calculado pela diferença de odômetro entre abastecimentos:

```
50.350 km − 50.000 km = 350 km
350 km / 40 L = 8,75 km/L
```

Esse número **não** representa consumo exato se o tanque não foi abastecido em
condições comparáveis. No MVP, apresentar sempre como **consumo estimado**.

> O odômetro por lançamento chega na **v0.6.0**, e é obrigatório em
> abastecimento e recarga. Ele é a fundação de três funcionalidades — consumo
> estimado, uso pessoal (§22) e alertas de manutenção — e nenhuma delas existe
> sem ele. Ser obrigatório, e não opcional, segue a regra da v0.3.1: campo em
> branco entrando como zero produz indicador errado e invisível.
>
> O consumo continua **secundário**. O indicador principal é custo/km, que sai
> do valor pago e dos quilômetros rodados, sem depender de quantos litros
> entraram no tanque.
>
> **Combustível comprado é prova de distância percorrida.** Litros multiplicados
> pelo consumo histórico dão uma distância mínima implícita, independente de o
> motorista atualizar o odômetro ou não. Esse piso protege os alertas de
> manutenção, onde subestimar quilometragem atrasa o aviso — e alerta de troca
> de óleo atrasado desgasta motor, enquanto quilometragem subestimada no
> custo/km apenas o deixa pessimista. As duas tolerâncias a erro são
> diferentes, e o tratamento também.

> **Lançamento retroativo deixa o consumo otimista.** Tanque-a-tanque pressupõe
> leitura tirada na bomba. Quem lança a semana inteira no domingo registra a
> leitura de domingo num abastecimento de terça, e os dias a mais de rodagem
> entram no trecho sem litro correspondente — o km/L sobe. O dinheiro e a
> repartição trabalho/pessoal não sofrem com isso; só o consumo. Como ele é
> secundário e já é sempre rotulado estimado, a distorção é aceitável — mas não
> deve ser apresentada como precisão.

Para elétricos: km/kWh ou kWh/100 km — a unidade pode ser definida na camada de
apresentação.

### Alerta de manutenção: alvo, não contagem regressiva

> **Decisão registrada na v0.9.0.**

O alerta exibe **a quilometragem em que o serviço vence** — "a próxima troca de
óleo é aos 110.000 km" — e nunca quanto falta.

O alvo é a soma de dois fatos: a leitura da última troca, lançada com a nota, e
o intervalo, definido pelo motorista. Não depende de onde o carro está agora, e
por isso é exato mesmo com o painel vários tanques atrasado.

"Faltam 600 km" seria uma afirmação sobre o presente, que é a única coisa que o
app não sabe com certeza. Um motorista que lê 600 quando faltam 550 confere no
painel, encontra a divergência e para de confiar no aplicativo inteiro — não só
naquele número.

A incerteza não desaparece: ela migra do número exibido para o **momento do
lembrete**, onde errar algumas centenas de quilômetros custa um aviso um pouco
adiantado, e não um número falso. Por isso a banda de aviso nunca é menor que a
defasagem que o painel consegue acumular entre dois abastecimentos.

O app não disputa com o painel quem sabe a quilometragem. Ele afirma um fato
conferível e deixa a comparação para o motorista — que é quem acompanha o carro.

## 24–25. Arquitetura

```
UI → Presentation → Domain → Data
```

A estrutura deve ser escalável, mas proporcional ao projeto. Não criar
complexidade apenas para parecer "enterprise".

| Camada | Responsabilidade | Proibições |
| --- | --- | --- |
| UI | Desenhar estado | Acessar DAO, rodar SQL, calcular finanças, manipular entidades do banco |
| ViewModel | Estado da tela, eventos, chamar use cases, expor `StateFlow` | Regras financeiras complexas |
| Domain | Regras de negócio, cálculos, validações, use cases | — |
| Data | Room, DAO, entities, repositories, persistência | — |

## 26. Dinheiro e precisão

Não usar `Double` para dinheiro. Usar **`Long` em centavos**: `R$ 286,40` →
`28640`. A formatação para texto acontece **somente** na camada de
apresentação.

## 27. Unidades

| Grandeza | Unidade |
| --- | --- |
| Distância | quilômetros |
| Combustível líquido | litros |
| GNV | m³ |
| Energia | kWh |
| Tempo | minutos |
| Dinheiro | centavos |

Horas online são armazenadas como `Long` de minutos: `8h20` → `500`. Na
interface: `8h 20min`.

## 28. Datas

Usar `java.time.LocalDate` para o dia de trabalho. Nunca strings como
`"11/08/2026"` como representação interna.

## 29. Cálculos do dashboard

Camada específica, independente da UI, testável sem iniciar o Android:

```
DashboardMetrics
  totalRevenue · totalExpenses · netProfit
  totalRides · totalKilometers · totalOnlineMinutes
  revenuePerKm · revenuePerHour · revenuePerRide
  costPerKm · profitPerKm · profitPerHour
```

## 30. Testes

Obrigatórios.

**Unitários:** faturamento, despesas, lucro, R$/km, R$/hora, R$/corrida,
custo/km, lucro/km, lucro/hora, divisão por zero, períodos sem registros,
combustível, energia, consumo estimado.

**Banco:** inserção, atualização, exclusão, consultas por período.

**ViewModel:** estado inicial, inserção, atualização do dashboard, erros de
validação.

**UI:** gradualmente, para os fluxos críticos — cadastrar veículo → registrar
ganho → registrar despesa → visualizar dashboard.

## 31–43. Processo

Ver [`DEVELOPMENT.md`](DEVELOPMENT.md) para o detalhamento operacional de
branches, commits, PRs, versionamento, tags e releases.

Resumo:

- `main` estável e protegida; nunca desenvolver diretamente nela
- Branches `feature/`, `fix/`, `hotfix/`
- Conventional Commits; um commit por alteração lógica
- Todo PR informa objetivo, alterações, testes, impacto e checklist
- CI obrigatório: checkout → JDK → build → testes → lint → assemble debug
- Semantic Versioning; tags `vX.Y.Z`; GitHub Release + CHANGELOG

## 44. Rollback

| Nível | Situação | Ação |
| --- | --- | --- |
| Branch | Feature errada | Não fazer merge |
| Commit | Commit já entrou | `git revert` |
| Release | Versão problemática | Voltar à tag estável anterior |

Nunca apagar histórico compartilhado com force push.

## 45. Banco e rollback

Mudanças no Room usam **migrations**. Nunca apagar o banco para resolver
alteração de schema. `fallbackToDestructiveMigration` não é solução permanente.

Toda mudança de schema: alterar Entity → criar Migration → testar a Migration →
atualizar `DATABASE.md` → rodar testes → commit separado quando apropriado.

## 46. Roadmap

Ver [`ROADMAP.md`](ROADMAP.md).

## 47. Versões futuras

v1.1 comparações · v1.2 metas · v1.3 pedágio e estacionamento marcáveis como
pessoais · v1.4 meta de quilometragem para o carro se pagar · v1.5 depreciação ·
v2.0 login, cloud backup, sincronização, backend.

Não implementar agora; manter a arquitetura preparada.

> **Manutenção preventiva e custos fixos foram antecipados** para v0.9.0 e
> v0.10.0. Sem eles o custo/km fica incompleto, e o custo/km é o produto
> (§2). Ver [`ROADMAP.md`](ROADMAP.md).

## 48. Funcionalidades não autorizadas

Não adicionar espontaneamente: **GPS, rastreamento, integração com Uber,
integração com 99, login, backend, Firebase, publicidade, assinatura, IA,
pagamentos, APIs externas.**

Podem ser discutidas depois. O objetivo é evitar aumento desnecessário da
complexidade do MVP.

O manifesto não declara **nenhuma** permissão. `INTERNET` entraria em PR
próprio, justificado.

### Autorização condicional: cadastro e assinatura

> **Decisão registrada.** Login e assinatura mensal **estão autorizados a
> partir da v2.0**, e seguem proibidos até lá. A intenção fica registrada para
> ser rastreável; a proibição continua valendo para toda versão anterior.

A arquitetura já comporta isso sem retrabalho: os cálculos são Kotlin puro e
não sabem de onde vêm os dados, os contratos de repositório vivem no domínio,
e telas novas são destinos novos no grafo de navegação. **Assinatura é uma
verificação de direito de uso na frente de um app que continua funcionando
igual** — não exige `user_id` nas tabelas, porque um celular tem um motorista.

O que é caro é **sincronização**, não assinatura: ids globais estáveis, marcas
de exclusão e `updated_at` por linha. Nada disso é exigido para cobrar, e
mesmo para sincronizar existe a saída de começar do presente, sem reconstruir
o passado. Por isso **não** se antecipam colunas de sincronização: seria
complexidade sem uso, contra o §54.

### Distribuição para grupo de teste

Antes do lançamento oficial, o app será entregue a um grupo restrito. Três
armadilhas, todas com prazo na **primeira instalação de terceiro** — até lá
tudo é reversível de graça:

1. **Teste interno e fechado da Play Console são upload para a Play**, e
   reservam o `applicationId` para sempre naquela conta, mesmo sem publicar e
   mesmo apagando o rascunho depois. Distribuir por fora (link da release,
   Drive, App Distribution) não reserva nada.
2. **Trocar o `applicationId` apaga os dados dos testadores**, porque o banco
   vive no armazenamento privado do pacote.
3. **Assinatura de debug e de release não se atualizam entre si.** Hoje a
   release publica um APK de debug (`com.driverprofit.debug`); um build de
   release assinado seria outro pacote, sem herdar nada.

Se o grupo for usar o app para valer, entregar **build de release assinado**
desde o primeiro dia — o que exige keystore, `signingConfig` e o segredo no CI
(§56). E o nome do produto, hoje um placeholder, precisa estar decidido antes
disso.

## 49–53. Regras de trabalho

Antes de implementar: ler a documentação, verificar branch, status do Git,
alterações não commitadas e versão atual. **Nunca assumir que o projeto está
limpo.**

Antes de cada feature: identificar a versão, criar a branch, definir escopo,
implementar, testar, rodar lint e build, atualizar documentação, commitar.
Nunca implementar múltiplas versões simultaneamente.

Depois de cada versão: `BUILD → TEST → LINT → REVIEW → TAG`.

Não regressão: uma feature nova não pode quebrar funcionalidades anteriores.

Alteração de banco exige Entity + DAO + Migration + Repository + Tests +
`DATABASE.md` no mesmo PR.

## 54. Regras de código

**Priorizar:** simplicidade, legibilidade, testabilidade, baixo acoplamento,
alta coesão, reutilização quando fizer sentido, tipagem forte, imutabilidade.

**Evitar:** god classes, singleton desnecessário, código duplicado, strings
mágicas, números mágicos, lógica financeira em Composables, lógica de banco em
ViewModels, dependências desnecessárias.

## 55. Dependências

Antes de adicionar: verificar se AndroidX/Kotlin já resolvem, avaliar a
manutenção da biblioteca, avaliar o impacto e documentar a decisão.

## 56. Segurança

Nunca versionar API keys, senhas, tokens, secrets, keystores ou credenciais.
Usar `.gitignore` e mecanismos apropriados de secrets.

## 58. Critério de sucesso

Deve ser possível fazer `git checkout v0.1.0` e reproduzir aquele estado, e o
mesmo para versões seguintes. O histórico deve permitir identificar quando uma
funcionalidade entrou, qual PR a introduziu, qual commit alterou uma regra,
qual versão introduziu um bug e qual versão estava estável antes.

## 59. Princípio final

```
CORREÇÃO → ESTABILIDADE → MANUTENIBILIDADE → UX → PERFORMANCE → NOVAS FUNCIONALIDADES
```

Não sacrificar estabilidade para adicionar funcionalidades rapidamente. Cada
versão deve ser pequena, testável, documentada e reversível.
