# Roadmap

Desenvolvimento em versões pequenas, verificáveis e reversíveis. Uma versão só
é concluída quando **build, testes, lint, revisão e tag** estão feitos
(PRD §51).

Nunca implementar duas versões ao mesmo tempo.

## MVP

### v0.1.0 — Foundation ✅ concluída

Fundação técnica. Nenhuma funcionalidade de produto.

- [x] Projeto Gradle Kotlin DSL + version catalog
- [x] Compose + Material 3 + tema claro/escuro
- [x] Single Activity + Navigation Compose
- [x] Estrutura de pacotes (core / data / domain / feature)
- [x] Room + schema exportado + conversores
- [x] Repository + injeção de dependências manual
- [x] Tipos base: `Money` (centavos), `WorkDuration` (minutos)
- [x] Formatação brasileira
- [x] Testes unitários dos tipos base
- [x] Testes de banco (DAO)
- [x] CI no GitHub Actions
- [x] Documentação inicial

**Critério de saída:** build passa, CI verde, app abre, banco inicial funciona.
Dashboard **não** entra aqui.

### v0.2.0 — Vehicle ✅ concluída

- [x] Cadastro de veículo
- [x] Edição e exclusão
- [x] Seleção de tipo de combustível
- [x] Validações
- [x] Testes: repository, use cases, ViewModel
- [x] Simplificação para nome + combustível (v0.2.1)

**Critério de saída:** o motorista consegue cadastrar, editar e excluir seu
veículo em dois campos.

Marca, modelo, ano e odômetro inicial foram removidos na v0.2.1 — nenhum deles
entra em conta de rentabilidade. Quilometragem volta na v0.6.0 como registro
por lançamento.

### v0.3.0 — Earnings ✅ concluída

- [x] Registro de sessão de trabalho: data, plataforma, corridas, valor, horas, km
- [x] Plataformas: Uber, 99, InDrive, Outra
- [x] Edição e exclusão
- [x] Histórico básico, com totais do período exibido
- [x] Testes dos cálculos básicos

**Critério de saída:** o motorista lança uma jornada e vê quanto ganhou por
hora e por quilômetro.

A plataforma é armazenada em cada registro desde já, para viabilizar
comparação entre plataformas depois (PRD §16) sem migração.

### v0.4.0 — Expenses ✅ concluída

- [x] Abastecimento: gasolina, etanol, flex, GNV (m³)
- [x] Carregamento elétrico (kWh), incluindo carga gratuita (kWh > 0, valor = 0)
- [x] Híbridos: abastecimento e/ou carregamento conforme configuração
- [x] Manutenção por categoria
- [x] Outras despesas
- [x] Formulários dinâmicos conforme o veículo cadastrado
- [x] Preço por unidade: R$/litro, R$/m³, R$/kWh
- [x] Histórico com filtro por natureza

**Critério de saída:** o motorista lança qualquer tipo de despesa, e o
formulário só oferece o que faz sentido para o veículo dele.

Odômetro por lançamento e consumo estimado ficaram para a v0.6.0.

### v0.5.0 — Dashboard ✅ concluída

- [x] Faturamento, despesas, lucro
- [x] Corridas, km, horas
- [x] R$/km, R$/hora, R$/corrida
- [x] Custo/km, lucro/km, lucro/hora
- [x] Filtros: hoje, ontem, semana, mês, mês anterior, personalizado
- [x] `DashboardMetrics` como classe pura, testável sem Android
- [x] Despesas do período separadas por natureza

**Critério de saída:** o motorista escolhe um período e vê quanto lucrou e
quanto lhe custa cada quilômetro.

Custo/km usa apenas despesa operacional: seguro, IPVA e financiamento entram
no lucro, mas não na razão por quilômetro (PRD §22). O rateio de custo fixo é
da v0.10.0.

> ⚠️ **Limitação conhecida desta versão.** O custo/km divide despesa **total**
> por quilômetros **apenas profissionais**. O combustível queimado em uso
> pessoal está no numerador e não está no denominador, o que infla o indicador
> central do produto. Corrigido na v0.7.0 (PRD §22).

### v0.6.0 — Odômetro ✅ concluída

Fundação das três versões seguintes. Consumo estimado, uso pessoal e alertas
de manutenção dependem todos da leitura de odômetro, e nenhum deles existe sem
ela.

- [x] Odômetro por lançamento, **obrigatório** em abastecimento e recarga
- [x] Odômetro no lançamento de manutenção, fechando o PRD §18
- [x] Quilometragem atual do veículo visível na tela de veículos
- [x] Última leitura exibida no formulário, para o motorista conferir enquanto digita
- [x] Migração 4→5 + testes instrumentados

**Critério de saída:** todo abastecimento registra o km do painel, e o app sabe
quantos quilômetros o carro tem.

**Banco:** versão 5, migração aditiva.

Obrigatório, e não opcional, pelo mesmo motivo da v0.3.1: campo em branco
entrando como zero produz indicador errado e invisível. Acoplar o km ao
lançamento do dinheiro é o que garante leitura frequente sem exigir ritual do
motorista.

### v0.7.0 — Uso pessoal ✅ concluída

Corrige a limitação registrada na v0.5.0.

- [x] Lançamento de uso pessoal: data ou intervalo + km
- [x] Conciliação por odômetro — resíduo = leitura − km de trabalho − km
      pessoais já declarados
- [x] Pergunta explícita sobre o resíduo: uso pessoal ou jornada não lançada?
- [x] Resíduo distribuído proporcionalmente aos dias do intervalo
- [x] Custo real por km calculado sobre o km **total**
- [x] Repartição em reais entre trabalho e pessoal
- [x] Lucro descontando apenas a parcela profissional
- [x] Migração 5→6 + testes instrumentados

**Critério de saída:** o motorista que usa o carro no fim de semana vê um
custo/km que não pune o trabalho por isso, e os dois valores da repartição
somam a despesa do período.

**Banco:** versão 6.

Dois mecanismos de entrada porque um só não basta: a declaração explícita põe
a viagem no mês em que ela aconteceu, e a conciliação captura o que ele nunca
registrou. Um abate o outro para não haver dupla contagem (PRD §22).

### v0.8.0 — Consumo estimado ✅ concluída

- [x] km/L, km/m³ e km/kWh a partir de odômetro e quantidade
- [x] Sempre rotulado **estimado** (PRD §23)
- [x] Exibido no histórico de despesas, por abastecimento
- [ ] ~~Comparação com o consumo que o painel do carro indica~~ — adiada

**Critério de saída:** a partir do segundo abastecimento com quantidade
informada, o motorista vê quanto o carro está fazendo por litro.

**Banco:** sem alteração. Odômetro (v0.6.0) e quantidade (v0.4.0) já existiam.

A comparação com o painel foi adiada porque exige guardar o consumo declarado
pelo fabricante no veículo — ou seja, migração. Não valia acrescentar uma
sétima versão de schema por um número informativo; entra junto de outra
mudança de banco quando houver.

Pares com combustível diferente são descartados: num flex, alternar gasolina e
etanol muda o consumo em cerca de 30%, e uma média dos dois não descreveria
nenhum deles. Comparar consumo por combustível é pós-MVP (PRD §9).

Consumo é sempre rotulado como estimado: o cálculo por odômetro só é exato se
o tanque for abastecido em condições comparáveis.

Depende da quantidade em litros, que é opcional desde a v0.4.1 — o formulário
deve explicar o que se ganha ao preenchê-la.

### v0.9.0 — Manutenção preventiva ✅ concluída

- [x] Alertas por quilometragem: óleo, filtros, pneus, freios, revisão
- [x] Intervalo configurável por item, com padrão sugerido e caminho de volta
- [x] Distância mínima implícita por combustível comprado, como piso
      independente do odômetro
- [x] Alerta se declara sem dados quando não há marco de onde contar
- [x] Tela própria e aviso no dashboard só quando há item vencido ou próximo
- [x] Migração 6→7 + testes instrumentados

**Critério de saída:** o app avisa da troca de óleo sem nunca afirmar com base
em quilometragem que ele não tem.

**Banco:** versão 7.

O marco vem do histórico de manutenção que já existia — nenhuma coluna nova
guarda "quando foi a última troca", porque duplicá-la criaria duas verdades que
divergiriam na primeira correção de lançamento. Consequência assumida: um item
só passa a alertar depois do primeiro serviço lançado **com odômetro**.

Intervalo é preferência, e só o que o motorista alterou vira linha no banco.
Isso faz veículo novo nascer acompanhado e permite revisar um padrão numa versão
futura sem sobrescrever escolha de ninguém.

Assimetria que rege esta versão: subestimar km infla o custo/km, o que é
apenas pessimista; mas **atrasa** o alerta de manutenção, o que desgasta
motor. Por isso o alerta não herda a degradação graciosa das outras
funcionalidades — na dúvida ele pede a leitura, em vez de estimar.

### v0.9.1 — Fechar o ciclo do odômetro ✅ concluída

O ciclo que a v0.7.0 construiu **não gira sozinho**. A conciliação existe, mas o
gatilho é manual, roda sobre o mês corrente e só sobre o primeiro veículo da
lista. Quem nunca apertar o botão fica com `km pessoal = 0`, e aí
`km total = km de trabalho` — o custo/km volta a ser o da v0.5.0, inflado, que é
exatamente o defeito que a v0.7.0 existe para corrigir.

**Sem alteração de banco.** Os cinco itens são domínio e interface; a coluna
anulável de odômetro e a tabela `personal_usage` já suportam tudo. Isso importa
para o fluxo do projeto: esta versão não depende de mais uma rodada de teste de
migração em aparelho.

Os itens têm **dependência entre si**, e a ordem abaixo é de dependência, não de
preferência.

#### 1. Discrepância negativa tratada — pré-requisito

Hoje a sobra da conciliação é `coerceAtLeast(0L)`. Isso precisa mudar antes de
qualquer automação, porque quebra uma propriedade da qual o resto depende.

Com janelas encadeadas, um lançamento alocado na janela errada **deveria se
cancelar sozinho**:

```
Janela 1   leitura 100.000 → 101.000   delta 1.000   corridas 900   sobra +100
Janela 2   leitura 101.000 → 101.700   delta   700   corridas 800   sobra −100
                                                                    ────────
                                                          pessoal real:  0
```

Com o piso em zero, a janela 2 vira 0 em vez de −100 e o motorista termina com
100 km de uso pessoal que nunca existiram, gravados para sempre. Com
conciliação manual e mensal isso quase nunca acontecia; com conciliação a cada
leitura, acontece toda semana.

Sobra negativa não é uso pessoal negativo: é **sinal de erro de lançamento** —
km de jornada inflado, ou leitura digitada baixa. É o único sinal que o app tem
de que um número foi digitado errado, e hoje ele é engolido em silêncio.

**Critério de saída:** sobra negativa é exibida como divergência a resolver,
nunca gravada como uso pessoal e nunca descartada calada.

#### 2. Conciliação por janela entre leituras

A janela deixa de ser o mês e passa a ser **de uma leitura de odômetro até a
próxima**, por veículo, disparada pela chegada da leitura nova.

O calendário não sabe nada sobre o carro. O único intervalo em que a diferença
de odômetro é um fato é o que vai de uma leitura à seguinte — amarrar ao mês
obriga a estimar o que aconteceu quando o mês termina entre dois
abastecimentos, que é justamente o que o projeto evita.

O ganho colateral é que a cadência do motorista deixa de importar. Lançar
diariamente, semanalmente ou depois de um mês sumido produz a mesma conta, sem
caso especial: a leitura é que define a fronteira.

**Critério de saída:** o motorista que registra abastecimentos e jornadas, e
nunca abre a tela de uso pessoal, vê o custo/km correto.

#### 3. "Não sei a leitura" em lançamento retroativo

O odômetro é obrigatório em abastecimento, recarga e manutenção, sem exceção por
data. Quem baixa o app e lança o histórico do mês passado — ou lança a semana
toda no domingo — encontra um campo obrigatório cuja resposta ele não tem: nota
de posto não traz odômetro.

As duas saídas de hoje são ruins. Desistir dos abastecimentos antigos deixa o
custo/km sem numerador. Inventar um número envenena três coisas: o consumo
estimado ordena por odômetro, o `MAX(odometer_km)` vira a quilometragem corrente
do carro, e o marco de manutenção sai errado — este último produzindo um **alvo
falso exibido com confiança**, que é pior que a contagem regressiva que a v0.9.0
removeu.

A saída é uma declaração explícita de ignorância, gravando `NULL`, **disponível
apenas quando o lançamento é retroativo** — data anterior à última leitura
conhecida daquele veículo. No lançamento de hoje ela não aparece, porque hoje o
painel está à mão; disponível sempre, viraria escotilha de fuga diária e a
fundação do odômetro desmoronaria.

Isso não fura a regra da v0.6.0, preserva-a. O que ela proíbe é campo em branco
que o cálculo trata como zero em silêncio (lição da v0.3.1). Uma declaração
explícita é o oposto, e `NULL` já significa "não sei" em todo o domínio.

**Critério de saída:** dá para lançar um mês de histórico sem inventar um único
número.

#### 4. Nota de limitação quando falta dado de uso pessoal

O PRD §22 manda o app declarar a limitação na tela quando não há dado de uso
pessoal. **Não está implementado.** A nota explicativa do custo/km só é
desenhada quando já existe uso pessoal registrado — ou seja, ela aparece quando
o número está certo e some quando está incompleto, que é o inverso do
necessário.

É a primeira coisa que deveria aparecer para quem acabou de instalar o app e
lançou histórico sem odômetro.

**Critério de saída:** custo/km sem dado de uso pessoal diz, na tela, que está
calculado com o que existe.

#### 5. Repartição do custo/km mostrando a proporção

O foco principal da tela é o **custo/km total** — é o número que serve a quem
roda só a trabalho, que é a maioria. A repartição entra abaixo, e é útil
sobretudo para quem usa bastante o carro fora do trabalho: ela dá a ideia real
de quanto a operação divide o veículo com a vida pessoal.

A repartição ganha a proporção ao lado dos reais:

```
Custo real por km                  R$ 1,00
  Trabalho     300 km   86%        R$ 300
  Pessoal       50 km   14%        R$  50
                                   ──────
                                   R$ 350
```

**Percentual, e não centavos por km.** Escrever "R$ 0,86/km profissional"
convidaria a multiplicar por 300 km de trabalho e chegar a R$ 258, que não é
valor nenhum: os 86 centavos são por km **total**, não por km trabalhado. O
custo por km é o mesmo nos dois usos — o que muda é quantos quilômetros cada um
consumiu. Os reais continuam porque são o que fecha com o extrato.

**Critério de saída:** as duas linhas somam a despesa operacional e a proporção
responde "quanto do carro o trabalho divide com a vida pessoal".

### v0.10.0 — Custos fixos por competência ✅ concluída

- [x] Período de competência na despesa (início e fim), separando "quando
      paguei" de "a que período se refere"
- [x] Diluição do valor pelos dias do período
- [x] Financiamento, seguro e IPVA atribuídos **100% ao trabalho** (PRD §22)
- [x] Custo fixo por km trabalhado
- [x] Migração 7→8 + testes instrumentados escritos

**Critério de saída:** o IPVA pago em janeiro não faz janeiro parecer
catastrófico nem o resto do ano parecer isento.

**Banco:** versão 8. Colunas anuláveis — despesas existentes ficam com `NULL`,
que significa "competência é a própria data", o comportamento atual.

Histórico e "Despesas" continuam exibindo **caixa**, para conferir com o
extrato. Só os indicadores por km usam competência.

### v0.11.0 — IPVA sem competência ✅ concluída

- [x] `ExpenseCategory.allowsAccrual`: só seguro e financiamento
- [x] Formulário para de oferecer competência para IPVA
- [x] Validação de domínio rejeita competência fora de `allowsAccrual`
- [x] Dashboard filtra custo fixo rateado por `allowsAccrual`, não por
      `accrual` estar vazio — robusto a dado legado

**Critério de saída:** IPVA lançado conta inteiro no mês do lançamento, sem
diluir pelos outros onze meses. Seguro e financiamento continuam usando
competência.

**Banco:** sem migração — mudança só de regra de negócio e validação.

### v0.12.0 — Veículo atual ✅ concluída

- [x] `vehicles.is_current`: automático com 1 veículo, botão com 2+
- [x] `work_sessions.vehicle_id`: ganhos passam a gravar o veículo atual
- [x] Despesas pré-selecionam o veículo atual (qualquer quantidade de
      veículos, não só quando há exatamente um)
- [x] Migração 9→10 + 76 testes instrumentados, confirmados em Redmi Note 8
      Pro (Android 9)

**Critério de saída:** trocar de veículo não obriga escolher o carro toda vez
em ganhos e despesas, e cada lançamento mantém o veículo que tinha quando foi
criado — a base para comparar histórico entre carros depois.

**Banco:** versão 10.

### v0.13.0 — Exportar e importar arquivo ✅ concluída

- [x] Exportação copia o banco (checkpoint de WAL + cópia de arquivo) para um
      `Uri` escolhido via Storage Access Framework — sem permissão nova
- [x] Importação valida antes de trocar: `PRAGMA user_version` contra
      `DriverProDatabase.VERSION`, presença da tabela `vehicles`
- [x] Importação substitui tudo (sem mesclar); tela avisa a consequência antes
      de confirmar
- [x] Sexto ícone no dashboard; sem migração de banco — só I/O
- [x] `BackupTest` instrumentado (5 testes), confirmado em Redmi Note 8 Pro
      (Android 9)

**Critério de saída:** o motorista consegue exportar um arquivo, guardá-lo, e
importá-lo de volta (no mesmo aparelho ou em outro) recuperando o histórico
completo.

**Banco:** sem migração — o formato de exportação é o próprio arquivo do
banco, não um formato novo.

### v0.14.0 — Nome e redesign visual (DriverPro)

- [x] `com.driverprofit` → `com.driverpro` em todo o projeto, resolvendo a
      pendência do nome (PRD §10) — seguro porque nenhuma instalação de
      terceiro tinha acontecido ainda
- [x] Cor de marca separada da cor semântica de lucro/prejuízo
      (`core/ui/theme/Color.kt`)
- [x] Componentes novos (`core/ui/component`): `IconChip`, `StatTile`,
      `CategoryBarRow`, `ListItemCard`
- [x] Navegação: barra inferior no lugar de seis ícones na TopAppBar; uso
      pessoal, manutenção e backup viram entradas em "Mais"
      (`more/presentation/MoreScreen.kt`)
- [x] Ícone do launcher redesenhado na cor de marca nova
- [ ] `connectedDebugAndroidTest` no aparelho físico (a pasta de schemas do
      Room mudou de nome — ver `DATABASE.md`)

**Critério de saída:** o app se chama DriverPro em todo lugar (pacote, banco,
ícone, documentação), e a interface deixa de ser Material 3 "de fábrica".

**Banco:** sem migração de schema — a classe e o arquivo do banco só mudaram
de nome (`DriverProDatabase`, `driverpro.db`); os schemas exportados
(versões 1–10) foram movidos para a pasta com o novo nome de classe, não
alterados.

### v0.15.0 — Crash handling

- [ ] Erro não tratado hoje fecha o app sem deixar rastro
- [ ] Sem Firebase/Crashlytics (PRD §48) — precisa ser 100% local

### v0.16.0 — Testes de fluxo

- [ ] Cadastrar veículo → lançar ganho → lançar despesa → conferir dashboard
- [ ] Nenhuma tela tem verificação automatizada hoje

### v0.17.0 — Analytics

- [ ] Gráficos
- [ ] Custo por km separado por natureza (PRD §22)
- [ ] Evolução de faturamento, R$/hora e R$/km entre períodos
- [ ] Filtro/comparação do dashboard por veículo (ficou de fora da v0.12.0 de
      propósito, para já ter dado histórico correto acumulado)
- [ ] **Verificar se sobras negativas de odômetro são frequentes.** A v0.10.1 as
      desconsidera em silêncio, porque uma isolada não tem ação possível. Mas um
      motorista que anota quilômetro a mais sistematicamente teria o custo/km
      permanentemente otimista sem nada dizer. É observação sobre um conjunto,
      não alerta — e só faz sentido com histórico acumulado

### v0.18.0 — UX Polish

- [ ] Estados vazios, loading e erro
- [ ] Animações moderadas
- [ ] Dark mode revisado
- [ ] Acessibilidade
- [ ] Formatação brasileira em toda a interface

### v0.19.0 — Hardening

- [ ] Cobertura de testes ampliada
- [ ] Performance
- [ ] Validações e tratamento de erros
- [ ] Migrações testadas

### v0.20.0 — Release Candidate

Congelamento de funcionalidades. A partir daqui: apenas correções,
performance, segurança, UX, testes e estabilidade.

### v1.0.0 — MVP Release

Critérios: build estável, CI verde, testes passando, banco estável, fluxos
principais funcionando, sem bugs críticos conhecidos.

## Pós-MVP

Não implementar agora. A arquitetura já está preparada para receber:

| Versão | Escopo |
| --- | --- |
| v1.1 | Comparação entre meses e entre plataformas; histórico de consumo |
| v1.2 | Metas diárias e mensais |
| v1.3 | Pedágio e estacionamento marcáveis como pessoais, em vez de rateados |
| v1.4 | Meta de quilometragem: quanto rodar para o carro se pagar |
| v1.5 | Depreciação e custo real do veículo por km |
| v2.0 | Login, backup na nuvem, sincronização, multi-device, backend |
| v2.1 | Assinatura mensal (Play Billing) |

> **Login e assinatura estão autorizados a partir da v2.0**, e proibidos até
> lá (PRD §48). Assinatura é verificação de direito de uso e não exige
> `user_id` nas tabelas; o que é caro é sincronização — ids estáveis, marcas
> de exclusão e `updated_at`. Nada disso é antecipado.
>
> A conta é vinculada à **conta Google**, sem CPF nem outro identificador
> pessoal. Limitar uma conta por CPF foi considerado e descartado: validação de
> dígito verificador é trivialmente burlável, verificação real custa e cria
> fricção, e o Play Billing já amarra a assinatura a uma conta. Motivo completo
> no PRD §48.
>
> Antes de distribuir a **qualquer terceiro**, ver a seção de distribuição no
> PRD §48: o `applicationId` congela na primeira instalação, e trocá-lo depois
> apaga os dados dos testadores.

> Manutenção preventiva e custos fixos saíram do pós-MVP: viraram v0.9.0 e
> v0.10.0. Sem eles o custo/km fica incompleto, e o custo/km é o produto.

## Fora de escopo até autorização explícita

GPS, rastreamento, integração com Uber/99, login, backend, Firebase,
publicidade, assinatura, IA, pagamentos, APIs externas (PRD §48).
