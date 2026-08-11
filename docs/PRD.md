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

## 23. Consumo

Calculado pela diferença de odômetro entre abastecimentos:

```
50.350 km − 50.000 km = 350 km
350 km / 40 L = 8,75 km/L
```

Esse número **não** representa consumo exato se o tanque não foi abastecido em
condições comparáveis. No MVP, apresentar sempre como **consumo estimado**.

Para elétricos: km/kWh ou kWh/100 km — a unidade pode ser definida na camada de
apresentação.

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

v1.1 comparações · v1.2 metas · v1.3 manutenção preventiva · v1.4 custos fixos ·
v1.5 depreciação · v2.0 login, cloud backup, sincronização, backend.

Não implementar agora; manter a arquitetura preparada.

## 48. Funcionalidades não autorizadas

Não adicionar espontaneamente: **GPS, rastreamento, integração com Uber,
integração com 99, login, backend, Firebase, publicidade, assinatura, IA,
pagamentos, APIs externas.**

Podem ser discutidas depois. O objetivo é evitar aumento desnecessário da
complexidade do MVP.

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
