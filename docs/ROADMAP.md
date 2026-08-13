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

### v0.7.0 — Uso pessoal

Corrige a limitação registrada na v0.5.0.

- [ ] Lançamento de uso pessoal: data ou intervalo + km
- [ ] Conciliação por odômetro — resíduo = leitura − km de trabalho − km
      pessoais já declarados
- [ ] Pergunta explícita sobre o resíduo: uso pessoal ou jornada não lançada?
- [ ] Resíduo distribuído proporcionalmente aos dias do intervalo
- [ ] Custo real por km calculado sobre o km **total**
- [ ] Repartição em reais entre trabalho e pessoal
- [ ] Lucro descontando apenas a parcela profissional
- [ ] Migração 5→6 + testes instrumentados

**Critério de saída:** o motorista que usa o carro no fim de semana vê um
custo/km que não pune o trabalho por isso, e os dois valores da repartição
somam a despesa do período.

**Banco:** versão 6.

Dois mecanismos de entrada porque um só não basta: a declaração explícita põe
a viagem no mês em que ela aconteceu, e a conciliação captura o que ele nunca
registrou. Um abate o outro para não haver dupla contagem (PRD §22).

### v0.8.0 — Consumo estimado

- [ ] km/L, km/m³ e km/kWh a partir de odômetro e quantidade
- [ ] Sempre rotulado **estimado** (PRD §23)
- [ ] Comparação com o consumo que o painel do carro indica

Consumo é sempre rotulado como estimado: o cálculo por odômetro só é exato se
o tanque for abastecido em condições comparáveis.

Depende da quantidade em litros, que é opcional desde a v0.4.1 — o formulário
deve explicar o que se ganha ao preenchê-la.

### v0.9.0 — Manutenção preventiva

- [ ] Alertas por quilometragem: óleo, filtros, pneus, freios, revisão
- [ ] Intervalo configurável por item
- [ ] Distância mínima implícita por combustível comprado, como piso
      independente do odômetro
- [ ] Alerta silencia, ou se declara incompleto, quando o dado não sustenta a
      afirmação

**Critério de saída:** o app avisa da troca de óleo sem nunca afirmar com base
em quilometragem que ele não tem.

Assimetria que rege esta versão: subestimar km infla o custo/km, o que é
apenas pessimista; mas **atrasa** o alerta de manutenção, o que desgasta
motor. Por isso o alerta não herda a degradação graciosa das outras
funcionalidades — na dúvida ele pede a leitura, em vez de estimar.

### v0.10.0 — Custos fixos por competência

- [ ] Período de competência na despesa (início e fim), separando "quando
      paguei" de "a que período se refere"
- [ ] Diluição do valor pelos dias do período
- [ ] Financiamento, seguro e IPVA atribuídos **100% ao trabalho** (PRD §22)
- [ ] Custo fixo por km trabalhado
- [ ] Migração 6→7 + testes instrumentados

**Critério de saída:** o IPVA pago em janeiro não faz janeiro parecer
catastrófico nem o resto do ano parecer isento.

**Banco:** versão 7. Colunas anuláveis — despesas existentes ficam com `NULL`,
que significa "competência é a própria data", o comportamento atual.

Histórico e "Despesas" continuam exibindo **caixa**, para conferir com o
extrato. Só os indicadores por km usam competência.

### v0.11.0 — Analytics

- [ ] Gráficos
- [ ] Custo por km separado por natureza (PRD §22)
- [ ] Evolução de faturamento, R$/hora e R$/km entre períodos

### v0.12.0 — UX Polish

- [ ] Estados vazios, loading e erro
- [ ] Animações moderadas
- [ ] Dark mode revisado
- [ ] Acessibilidade
- [ ] Formatação brasileira em toda a interface

### v0.13.0 — Hardening

- [ ] Cobertura de testes ampliada
- [ ] Performance
- [ ] Validações e tratamento de erros
- [ ] Migrações testadas
- [ ] Backup local
- [ ] Crash handling

### v0.14.0 — Release Candidate

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
> Antes de distribuir a **qualquer terceiro**, ver a seção de distribuição no
> PRD §48: o `applicationId` congela na primeira instalação, e trocá-lo depois
> apaga os dados dos testadores.

> Manutenção preventiva e custos fixos saíram do pós-MVP: viraram v0.9.0 e
> v0.10.0. Sem eles o custo/km fica incompleto, e o custo/km é o produto.

## Fora de escopo até autorização explícita

GPS, rastreamento, integração com Uber/99, login, backend, Firebase,
publicidade, assinatura, IA, pagamentos, APIs externas (PRD §48).
