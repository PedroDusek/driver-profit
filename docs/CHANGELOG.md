# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/).
Versionamento conforme [Semantic Versioning](https://semver.org/lang/pt-BR/).

## [Não publicado]

### Alterado

- **IPVA sai do rateio por competência.** O valor lançado passa a contar
  inteiro no mês do lançamento, em vez de diluído pelos dias de um intervalo —
  o dinheiro sai à vista ou em poucas parcelas, nunca em doze fatias iguais.
  Continua **100% ao trabalho** e fora do custo/km. Seguro e financiamento
  continuam usando competência normalmente. Sem migração de banco:
  `Expense.amountWithin` já tinha o comportamento certo para quando não há
  competência

## [0.10.1] — Resolver a sobra do odômetro

A conciliação tinha duas saídas — classificar como uso pessoal ou lançar a
jornada — e nenhuma servia para o caso mais comum: uma sobra pequena que o
motorista não consegue explicar. Sem terceira opção, o aviso ficava para sempre.

**Banco na versão 9.**

### Corrigido

- **O backup automático não incluía o arquivo `-wal`.** O Room usa write-ahead
  logging, e as escritas recentes ficam no WAL até um checkpoint movê-las para o
  `.db`. As regras copiavam só o `.db`, então o backup podia levar um SQLite
  válido e quase vazio — e a restauração devolveria um histórico incompleto sem
  erro nenhum. Medido em 16/08/2026: `.db` com 4 KB e `-wal` com 206 KB. O
  `-shm` segue de fora, porque o SQLite o regenera a partir do WAL.

### Adicionado

- **"Deixar de fora"** — a sobra sai de todos os totais e o app para de perguntar
  por aquela janela. A tela informa a consequência antes: o custo por km fica um
  pouco mais alto que o real
- `reconciliation_dismissals`, guardando **a quantidade dispensada** e não só o
  intervalo
- **"Resolver depois"** no lugar de "Cancelar" — o aviso continua lá, e o rótulo
  agora diz isso

### Alterado

- **Sobra negativa deixa de ser exibida.** Quando os lançamentos somam mais que o
  painel não há distância faltando, só inconsistência entre dois números do
  próprio motorista — e como o app **é** a anotação dele, não existe fonte contra
  a qual conferir. O cartão e o diálogo de divergência introduzidos na v0.9.1 são
  removidos: foram construídos com boa intenção e não tinham ação associada
- A sobra negativa **continua sendo calculada e preservada**, só não é mostrada.
  É ela que faz janelas encadeadas se cancelarem
- `OdometerWindow` passa a expor `windows()` além de `pending()`, separando o
  cálculo da política de exibição

### Decisões registradas

- **A dispensa caduca quando a sobra muda.** Aceitar 15 km é aceitar um fato, não
  um intervalo de tempo. Se a sobra crescer além disso, apareceu distância nova
  sobre a qual ninguém opinou; se encolher dentro dela, cabe no que já foi aceito
- **Nunca sugerir um valor corrigido para a leitura.** O número sugerido viria das
  jornadas, e ajustar o odômetro para concordar com elas destruiria a
  independência que faz dele um sinal — a conciliação passaria a confirmar a si
  mesma
- **Sem saldo global entre janelas.** Netting esconderia erro de digitação e
  deixaria o número de cada período errado do seu jeito
- **Alerta sem ação possível é imposto sobre atenção.** Ele ensina a fechar aviso
  sem ler, e gasta a atenção que manutenção vencida e quilômetro sem explicação
  precisam ter

## [0.10.0] — Custos fixos por competência

Custo fixo é pago em bloco e serve a um período. Sem separar as duas coisas, o
IPVA de R$ 1.200 pago em janeiro faz janeiro parecer catastrófico e o resto do
ano parecer isento — e nenhum dos doze meses diz a verdade.

### Adicionado

- **Período de competência na despesa** (`accrual_start`, `accrual_end`),
  separando "quando paguei" de "a que período o valor se refere"
- `Expense.amountWithin(period)` — rateio pelos dias do intervalo
- **Custo fixo do período** e **custo fixo por km trabalhado** no dashboard
- Campos de competência no formulário, oferecidos **só em custo fixo**
- `maintenance_schedules` intacta; **banco na versão 8**, migração aditiva

### Decisões registradas

- **Só os indicadores por quilômetro usam competência.** Histórico, "Despesas" e
  lucro continuam exibindo **caixa**, para conferir com o extrato. Resultado por
  competência é assunto pós-MVP (PRD §22).
- **O divisor do custo fixo por km é o quilômetro trabalhado**, não o total.
  Financiamento, seguro e IPVA existem pela decisão de ter o carro para
  trabalhar; levar o carro ao mercado não gera parcela. Por isso ele é um
  indicador separado, e não uma parcela do custo/km — os dois têm denominadores
  diferentes, e somá-los seria somar razões de bases distintas.
- **O rateio é por dias iguais**, não por dias trabalhados: seguro e IPVA correm
  no calendário, não no uso. Um mês parado custa o mesmo que um mês rodando, que
  é o que faz deles custo fixo.
- **A consulta é de sobreposição**, não de contenção: o IPVA pago em 15/01 com
  competência anual precisa aparecer em agosto, e a data dele está a sete meses
  de distância.
- **Competência não pela metade.** Uma ponta só não define intervalo, e aceitá-la
  obrigaria o cálculo a inventar a outra.
- **`NULL` é o caso comum**, e significa "conta no próprio dia" — o comportamento
  de todo custo variável e de todo lançamento anterior a esta versão.

## [0.9.1] — Fechar o ciclo do odômetro

O ciclo que a v0.7.0 construiu existia mas **não girava sozinho**: a conciliação
era manual, mensal e só do primeiro veículo. Quem nunca apertasse o botão ficava
com uso pessoal zerado e custo/km inflado — o defeito que a v0.7.0 existia para
corrigir. Encontrado no primeiro teste com dados reais em aparelho.

**Sem alteração de banco.**

### Corrigido

- **Sobra negativa não é mais zerada em silêncio.** O `coerceAtLeast(0L)` saiu.
  Lançamento acima do painel vira divergência declarada, com a instrução de
  conferir os dois números
- **A conciliação acontece sozinha**, por janela entre leituras de odômetro, e
  aparece no dashboard — ao lado do número que ela afeta
- **O custo/km declara quando está incompleto** (PRD §22, exigido desde a
  v0.7.0 e nunca implementado): sem dado de uso pessoal, a tela diz que toda a
  distância está sendo cobrada do trabalho

### Decisões registradas

- **A janela vai de uma leitura à seguinte**, não de mês de calendário. O único
  intervalo em que a diferença de odômetro é um fato é esse; e como a leitura é
  obrigatória em todo abastecimento, ela chega sozinha. A cadência do motorista
  deixa de importar — diária, semanal ou depois de um mês sumido dão a mesma
  conta, sem caso especial.
- **A janela começa no dia seguinte à leitura anterior**, para que janelas
  consecutivas não contem a mesma jornada duas vezes.
- **Preservar a negativa restaura o cancelamento entre janelas.** Uma jornada
  alocada na janela errada infla uma sobra e desinfla a seguinte; com piso em
  zero, o desequilíbrio passageiro virava quilometragem pessoal gravada para
  sempre.
- **Duas leituras no mesmo dia** caem numa janela de um dia só, e a jornada
  daquele dia pode ser contada duas vezes — o que **encolhe** a sobra. Errar
  para menos devolve o comportamento antigo em vez de inventar quilometragem
  pessoal que ninguém rodou.

### Também nesta versão

- **Todas as janelas pendentes são conferidas**, não só a última. Dois
  abastecimentos lançados antes de abrir o app fecham duas janelas, e conferir
  só a mais nova abandonava a anterior para sempre — o que atinge em cheio quem
  lança em lote, semanalmente.
- **Coerência entre data e odômetro na validação.** Uma leitura que contradiz as
  vizinhas por data é recusada com motivo: odômetro só cresce, e um lançamento
  de 12/08 tem que caber entre a leitura de 10/08 e a de 16/08. Leituras do
  mesmo dia ficam de fora da comparação, porque duas paradas no mesmo dia não
  têm ordem conhecida.
- **"Não sei a leitura"** em lançamento com data anterior a hoje, gravando
  ausência. Nota de posto não traz odômetro, e exigir um número que não existe
  empurra o motorista a inventar — o que envenena consumo, marco de manutenção e
  conciliação de uma vez. A opção não aparece no lançamento do dia, quando o
  painel está à mão; disponível sempre, viraria rotina e a leitura por
  lançamento deixaria de existir na prática.
- **Proporção na repartição do custo/km** — percentual, e não centavos por km.
  "R$ 0,86/km profissional" convidaria a multiplicar por quilômetro trabalhado e
  chegar a um valor que não existe: os 86 centavos são por quilômetro **total**.
  O custo por km é o mesmo nos dois usos; o que muda é quantos quilômetros cada
  um consumiu.

- **Cada bloco diz a que intervalo se refere.** O dashboard empilha três
  semânticas de tempo — o período selecionado, a janela entre duas leituras de
  odômetro e a quilometragem acumulada do carro — e só a primeira era rotulada.
  A divergência passa a mostrar as datas da janela, o aviso de manutenção diz
  que não depende do seletor, e o diálogo de conciliação abre dizendo qual
  intervalo está conferindo. Este último virou necessidade quando mais de uma
  janela passou a ficar pendente: dois diálogos seguidos eram idênticos exceto
  pelos números.

### Decisão mantida

A sobra **continua sendo uma pergunta**, não uma atribuição automática (PRD §22).
Chegou-se a implementar a atribuição automática e ela foi revertida: passeio e
jornada não lançada têm sinais opostos no custo/km, e presumir apagaria a
diferença justamente no indicador central do produto.

## [0.9.0] — Manutenção preventiva

Alertas por quilometragem para óleo, filtros, freios, pneus e revisão. É a
primeira funcionalidade do app que **não** degrada com elegância quando falta
dado — e isso é deliberado.

### Adicionado

- `MaintenanceItem` — cinco itens acompanhados, cada um com intervalo padrão
  editável e ligação com a categoria de manutenção que estabelece o marco
- `MaintenanceMonitor` — decide o estado de cada item: em dia, se aproximando,
  vencido ou **sem dados**
- **Piso de distância por combustível comprado** (PRD §23): litros multiplicados
  pelo consumo histórico da v0.8.0 provam quilometragem independente do painel
- Tela de manutenção, com um cartão por veículo, intervalo editável por item e
  o marco de onde a contagem parte
- Aviso no dashboard, só quando existe item vencido ou próximo
- `maintenance_schedules` — **banco na versão 7**, migração aditiva

### Decisões registradas

- **A tela exibe o alvo, não a contagem regressiva.** "A próxima é aos 110.000
  km" é a soma de dois fatos — a leitura da última troca e o intervalo
  escolhido — e por isso é exata mesmo com o painel atrasado. "Faltam 600 km"
  seria uma afirmação sobre o presente, que é justamente o que o app não sabe
  com certeza; um motorista que lê 600 quando faltam 550 para de confiar no
  aplicativo inteiro. A incerteza migra do número exibido para o **momento do
  lembrete**, onde errar algumas centenas de quilômetros custa um aviso
  adiantado em vez de um número falso.
- **Os rótulos de estado são neutros em gênero** — "Em atraso", e não
  "vencido", que não concorda com "revisão".
- **A assimetria que rege a versão.** Subestimar quilometragem deixa o custo/km
  pessimista, o que é apenas chato; mas **atrasa** o alerta de troca de óleo, o
  que desgasta motor. As duas tolerâncias a erro são diferentes, e o tratamento
  também: aqui, na dúvida, o alerta pede a leitura em vez de estimar.
- **Sem marco, sem afirmação.** Item que nunca teve manutenção lançada com
  odômetro fica em `UNKNOWN` e diz isso na tela. Nunca "em dia" — essa seria a
  mentira cara das duas.
- **Ausência de registro significa intervalo padrão.** Só vira linha no banco o
  que o motorista alterou. Veículo novo já nasce acompanhado, a tabela fica com
  meia dúzia de linhas, e continua sendo possível distinguir escolha de omissão.
- **Voltar ao padrão apaga a preferência**, em vez de gravar o valor padrão:
  assim o veículo acompanha uma eventual revisão do número.
- **Consumo pela mediana**, não pela média nem pelo máximo. Um par tanque-a-tanque
  estragado desloca a média e domina o máximo; a mediana o ignora.
- **O piso recorta abastecimentos por data, não por odômetro.** Ele existe
  justamente para o caso em que a leitura está atrasada — filtrar por ela
  devolveria o defeito para dentro da correção.
- **Item desligado continua na lista.** Sumir da tela ao ser desligado o tornaria
  impossível de religar.
- **`ON DELETE CASCADE`**, diferente de `expenses` e `personal_usage`: aquelas
  guardam histórico financeiro e sobrevivem à troca de carro; esta guarda uma
  preferência sobre um carro que deixou de existir.

## [0.8.0] — Consumo estimado

O número que o PRD §23 pede desde o começo e que só ficou possível quando o
odômetro por lançamento chegou, na v0.6.0.

### Adicionado

- `Consumption` — quilômetros por unidade, em **milésimos**, pelo mesmo motivo
  de `Quantity`: manter o número inteiro até a hora de exibir
- `ConsumptionEstimator` — método tanque-a-tanque, com a distância vindo da
  diferença de odômetro e o divisor sendo a quantidade do **segundo**
  abastecimento, que é o que repõe o queimado no trecho
- O consumo aparece no histórico de despesas, sempre rotulado **estimado**
- `BrazilianFormatter.consumption` — `"8,75 km/L"`, com zero à direita cortado

**Sem alteração de banco.** Odômetro e quantidade já existiam.

### Decisões registradas

- **Sempre rotulado estimado** (PRD §23). O número só seria exato se os dois
  abastecimentos tivessem enchido o tanque nas mesmas condições, e ninguém
  garante isso na vida real.
- **Par com combustível diferente é descartado.** Num flex, alternar gasolina e
  etanol muda o consumo em cerca de 30%; uma média dos dois não descreveria
  nenhum deles. Comparar consumo por combustível é pós-MVP (PRD §9).
- **A ordem é a do odômetro, não a da data.** Lançar hoje o abastecimento da
  semana passada é comum, e a ordem física do carro é a do painel.
- **Ausência de dado descarta o par em silêncio.** Sem odômetro nos dois, sem
  quantidade no segundo, ou com odômetro repetido, não há estimativa — e não
  há número inventado no lugar dela.
- **Calculado sobre o histórico inteiro**, não sobre o que o filtro mostra: o
  trecho nasce da diferença para o abastecimento anterior, que pode estar fora
  do filtro.

### Não implementado nesta versão

**Comparação com o consumo declarado pelo painel do carro.** Exige guardar esse
valor no veículo, ou seja, migração. Não valia uma sétima versão de schema por
um número informativo — entra junto de outra mudança de banco quando houver.

## [0.7.0] — Uso pessoal

Corrige a distorção registrada como limitação conhecida na v0.5.0: o custo/km
dividia despesa **total** por quilômetros **apenas profissionais**, então o
combustível queimado no fim de semana inflava o indicador central do produto.

### Corrigido

**O custo por km agora usa a distância total.**

A correção não classifica despesa nenhuma — combustível é fungível, um tanque
não é "pessoal" nem "profissional". Basta o denominador ser o quilômetro
total, porque o rateio proporcional se cancela algebricamente:

```
(E × Kp/K) ÷ Kp = E ÷ K
```

Sem estimar consumo, sem preço médio, e sem alterar nenhuma despesa gravada.

Com R$ 900 de custo operacional em 1.000 km — 800 de trabalho, 200 pessoais —
o custo/km sai de R$ 1,125 para R$ 0,90.

### Adicionado

**Domínio**
- `PersonalUsage` — intervalo de dias mais quilômetros, com
  `kilometersWithin` distribuindo proporcionalmente aos dias
- `PersonalUsageValidator` e os use cases de escrita e leitura
- `ReconcileOdometerUseCase` — confere o painel contra o lançado e **abate** o
  que já foi declarado
- `DashboardMetrics` ganha `personalKilometers`, `workOperationalCost`,
  `personalOperationalCost` e `workExpenses`

**Dados**
- Banco vai para a **versão 6**, com `personal_usage`
- Migração 5→6 aditiva
- Consulta por **sobreposição** de intervalos, não por contenção

**Interface**
- Tela de uso pessoal com histórico, lançamento e exclusão
- Conciliação por odômetro, mostrando a conta inteira antes de perguntar
- Repartição trabalho/pessoal em reais no dashboard

### Decisões registradas

- **Dois mecanismos de entrada, e um abate o outro.** A declaração explícita
  põe a viagem no mês em que aconteceu; a conciliação captura o que nunca foi
  registrado. Sem o abatimento, o motorista descontaria duas vezes.
- **A pergunta do resíduo não é presumida.** Uso pessoal e jornada esquecida
  têm sinais opostos no custo/km.
- **Jornada esquecida não é gravada aqui.** O lugar de corrigir é o formulário
  de ganhos, com plataforma, corridas e valor — registrar só a distância
  inflaria o R$/km, que é o defeito que a v0.3.1 corrigiu.
- **A parte pessoal é calculada por diferença**, não por proporção própria:
  dois arredondamentos independentes deixariam um centavo órfão e a tela
  deixaria de fechar contra o extrato.
- **Custo fixo não entra no rateio.** Passear no domingo não gera parcela de
  financiamento (PRD §22).
- **Sem quilômetro nenhum, o custo inteiro fica com o trabalho** — o
  comportamento conservador, e o mesmo de antes desta versão.
- **Consulta por sobreposição.** Uma viagem de 28/07 a 03/08 precisa aparecer
  nos dois meses; filtrar por "começa dentro do período" perderia a segunda
  metade e deixaria agosto inflado.

### Não coberto

O lançamento e a conciliação não têm teste de interface — o projeto ainda não
tem nenhum. A lógica que sustenta os dois está coberta por teste de domínio.

## [0.6.0] — Odômetro

Fundação das três versões seguintes: consumo estimado, uso pessoal e alertas
de manutenção dependem todos da leitura de odômetro, e nenhum deles existe sem
ela.

### Adicionado

**Domínio**
- `Expense.odometerKm` — leitura do painel no lançamento (PRD §23)
- `ExpenseValidator` passa a exigir a leitura em abastecimento, recarga e
  manutenção, com teto de `MAX_ODOMETER_KM` para pegar dígito a mais
- `ObserveVehicleOdometerUseCase` e `ObserveVehicleOdometersUseCase`

**Dados**
- Banco vai para a **versão 5**, com `odometer_km` em `expenses`
- Migração 4→5 aditiva
- `MAX(odometer_km)` por veículo, não a leitura do lançamento mais recente

**Interface**
- Campo de odômetro nas categorias ligadas ao veículo
- Última leitura conhecida exibida enquanto o motorista digita
- Quilometragem atual de cada carro na lista de veículos

### Decisões registradas

- **Obrigatório, e não opcional.** Mesmo motivo da v0.3.1: campo em branco que
  o cálculo trata como ausente produz indicador incompleto sem avisar. Aqui o
  preço é maior — um alerta de troca de óleo atrasado desgasta motor.
- **Fora de `ExpenseDetail`.** A leitura vale igualmente para abastecimento,
  recarga e manutenção; dentro do `sealed` estaria repetida nas três variantes.
- **Coluna anulável, regra no domínio.** As despesas anteriores à v0.6.0 não
  têm leitura, e inventar um valor na migração envenenaria exatamente o que o
  odômetro serve para calcular. `NULL` diz a verdade.
- **`MAX`, não "a mais recente por data".** Odômetro só cresce, e lançar hoje o
  abastecimento da semana passada é comum — ordenar por data devolveria uma
  leitura menor que a real.
- **A última leitura fica visível no formulário.** É o que faz um dígito
  trocado saltar aos olhos na hora, em vez de virar consumo absurdo depois.
- **Leitura digitada e depois abandonada não é gravada.** Trocar a categoria
  para pedágio descarta o odômetro, que não tem veículo a que se referir.

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
