# Desenvolvimento

Regras de trabalho no repositório. Valem para pessoas e para agentes.

## Antes de começar qualquer coisa

Nunca assuma que o projeto está limpo. Sempre:

1. Ler `README.md`, `docs/PRD.md`, `docs/ARCHITECTURE.md` e este arquivo
2. `git status` — existem alterações não commitadas?
3. `git branch --show-current` — em que branch você está?
4. `git tag --list` — qual a versão atual?
5. Entender o estado real antes de escrever a primeira linha

## Ambiente

| Requisito | Versão |
| --- | --- |
| JDK | 21 |
| Android SDK | 36 (Build-Tools 36) |
| Gradle | 9.7 (via wrapper — não instale à parte) |
| AGP | 9.3.1 |

⚠️ **O caminho do projeto não pode ter acento ou caractere não-ASCII.** O AGP
recusa o build no Windows nesse caso. Prefira caminhos curtos.

## Fluxo de uma feature

```
main
 ↓ criar branch
implementação
 ↓
testes
 ↓
commit
 ↓
push
 ↓
Pull Request
 ↓
CI verde
 ↓
revisão
 ↓
merge
 ↓
tag/release quando aplicável
```

Passo a passo (PRD §50):

1. Identificar a versão atual
2. Criar a branch apropriada
3. Definir o escopo da feature — uma versão por vez
4. Implementar
5. Criar/atualizar testes
6. `./gradlew testDebugUnitTest`
7. `./gradlew lintDebug`
8. `./gradlew assembleDebug`
9. Atualizar documentação e `CHANGELOG.md`
10. Commitar

## Branches

`main` é a branch estável e **nunca** recebe desenvolvimento direto. Ela está
protegida no GitHub: Pull Request obrigatório, sem force push, sem exclusão,
histórico linear.

| Tipo | Padrão | Exemplo |
| --- | --- | --- |
| Funcionalidade | `feature/<nome>` | `feature/vehicle-registration` |
| Correção | `fix/<problema>` | `fix/division-by-zero` |
| Correção urgente | `hotfix/<problema>` | `hotfix/crash-on-launch` |
| Documentação | `docs/<assunto>` | `docs/database-schema` |
| Infraestrutura | `chore/<assunto>` | `chore/ci-workflow` |

## Commits

[Conventional Commits](https://www.conventionalcommits.org/):

```
type(scope): description
```

Tipos: `feat`, `fix`, `docs`, `test`, `refactor`, `chore`, `perf`, `build`, `ci`.

Bons exemplos:

```
feat(vehicle): add vehicle registration form
feat(dashboard): calculate revenue per kilometer
fix(dashboard): prevent division by zero
test(metrics): add dashboard calculation tests
refactor(data): separate repository implementation
docs(readme): update setup instructions
chore(ci): configure Android build workflow
```

Não use: `update`, `changes`, `fix`, `stuff`, `teste`, `final`, `wip`.

### Tamanho

Um commit = uma alteração lógica. Evite um commit com 300 arquivos e 15
funcionalidades. Prefira:

```
commit 1 → entity + dao
commit 2 → repository
commit 3 → use case
commit 4 → UI
commit 5 → testes
```

## Pull Requests

Use o template. Todo PR informa objetivo, alterações, testes executados,
impacto no banco e checklist.

O CI roda testes, lint e build. **PR com CI vermelho não é mergeado.**

Merge por squash ou rebase — merge commit está desabilitado para manter o
histórico linear.

## Versionamento

[Semantic Versioning](https://semver.org/lang/pt-BR/): `MAJOR.MINOR.PATCH`.

| Incremento | Quando | Exemplo |
| --- | --- | --- |
| PATCH | Correção sem mudança de funcionalidade | corrigir cálculo de R$/km |
| MINOR | Nova funcionalidade compatível | adicionar manutenção |
| MAJOR | Mudança incompatível ou evolução arquitetural grande | mudança incompatível no modelo de dados |

Durante o desenvolvimento inicial: `0.x.y`. Após o primeiro lançamento
estável: `1.0.0`.

Atualize `versionName` em `app/build.gradle.kts` junto com a tag.

### Tags e releases

```bash
git tag -a v0.1.0 -m "v0.1.0 — MVP Foundation"
git push origin v0.1.0
```

A tag dispara o workflow de release. Toda tag aponta para um commit da `main`
já validado pelo CI.

## Rollback

Três níveis (PRD §44):

| Nível | Situação | Ação |
| --- | --- | --- |
| Branch | Feature errada, ainda em PR | Não fazer merge. `main` permanece intacta |
| Commit | Commit já entrou | `git revert <commit>` |
| Release | Versão problemática | Voltar para a tag estável anterior |

**Nunca** reescrever histórico compartilhado com force push.

## Alterações no banco

Se a feature mexe no banco, o PR precisa conter **tudo** isto:

```
Entity + DAO + Migration + Repository + Testes + docs/DATABASE.md
```

Alterar só a Entity e considerar pronto é erro. Ver
[DATABASE.md](DATABASE.md).

## Regras de código

Priorizar: simplicidade, legibilidade, testabilidade, baixo acoplamento, alta
coesão, tipagem forte, imutabilidade.

Evitar: god classes, singleton desnecessário, código duplicado, strings
mágicas, números mágicos, lógica financeira em Composables, lógica de banco em
ViewModels, dependências desnecessárias.

### Não regressão

Antes de concluir qualquer PR:

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

Se algum falhar, a feature **não** está concluída.

## Dependências

Antes de adicionar (PRD §55):

1. AndroidX ou Kotlin já resolvem?
2. A biblioteca é mantida?
3. Qual o impacto no projeto?
4. A decisão está documentada em `ARCHITECTURE.md`?

Versões só em `gradle/libs.versions.toml`.

## Segurança

Nunca versionar: API keys, senhas, tokens, secrets, keystores, credenciais.

O `.gitignore` já bloqueia `*.jks`, `*.keystore`, `keystore.properties`,
`secrets.properties`, `google-services.json`. Se precisar de um segredo no CI,
use GitHub Secrets.

## Escopo

Não adicionar sem autorização explícita: GPS, rastreamento, integração com
Uber/99, login, backend, Firebase, publicidade, assinatura, IA, pagamentos,
APIs externas (PRD §48).

Na dúvida, pergunte antes de implementar.
