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

### v0.5.0 — Dashboard

- [ ] Faturamento, despesas, lucro
- [ ] Corridas, km, horas
- [ ] R$/km, R$/hora, R$/corrida
- [ ] Custo/km, lucro/km, lucro/hora
- [ ] Filtros: hoje, ontem, semana, mês, mês anterior, personalizado
- [ ] `DashboardMetrics` como classe pura, testável sem Android

### v0.6.0 — Analytics

- [ ] Gráficos
- [ ] Despesas por categoria
- [ ] Evolução de faturamento, R$/hora e R$/km
- [ ] Odômetro por lançamento
- [ ] Consumo estimado (km/L, km/kWh)
- [ ] Alertas de manutenção por quilometragem (óleo, pneus)

Consumo é sempre rotulado como **estimado**: o cálculo por odômetro só é exato
se o tanque for abastecido em condições comparáveis (PRD §23).

### v0.7.0 — UX Polish

- [ ] Estados vazios, loading e erro
- [ ] Animações moderadas
- [ ] Dark mode revisado
- [ ] Acessibilidade
- [ ] Formatação brasileira em toda a interface

### v0.8.0 — Hardening

- [ ] Cobertura de testes ampliada
- [ ] Performance
- [ ] Validações e tratamento de erros
- [ ] Migrações testadas
- [ ] Backup local
- [ ] Crash handling

### v0.9.0 — Release Candidate

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
| v1.3 | Manutenção preventiva e alertas (óleo, pneus, freios, revisão) |
| v1.4 | Custos fixos: seguro, IPVA, financiamento |
| v1.5 | Depreciação e custo real do veículo por km |
| v2.0 | Login, backup na nuvem, sincronização, multi-device, backend |

## Fora de escopo até autorização explícita

GPS, rastreamento, integração com Uber/99, login, backend, Firebase,
publicidade, assinatura, IA, pagamentos, APIs externas (PRD §48).
