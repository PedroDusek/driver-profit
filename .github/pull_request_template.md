## Objetivo

<!-- O que foi implementado e por quê. Referencie a issue: Closes #123 -->

## Alterações

<!-- Quais arquivos/áreas foram alterados. -->

## Testes

<!-- Quais testes foram executados e o que eles cobrem. -->

- [ ] `./gradlew testDebugUnitTest`
- [ ] `./gradlew lintDebug`
- [ ] `./gradlew assembleDebug`
- [ ] Testes instrumentados (quando aplicável): `./gradlew connectedDebugAndroidTest`

## Impacto

**Existe impacto no banco?** <!-- Sim / Não -->

**Existe migration?** <!-- Sim / Não. Se sim, a versão do banco foi incrementada e a migration testada? -->

> ⚠️ **Se este PR mexe no banco, `connectedDebugAndroidTest` é obrigatório antes
> do merge.** O CI não roda teste de migração — ele exige aparelho. Sem essa
> execução, o gate verde não diz nada sobre a migração, e já aconteceu de uma
> migração atravessar duas versões sem ninguém exercitá-la.

## Checklist

- [ ] Build passa
- [ ] Testes passam
- [ ] Lint passa
- [ ] Sem credenciais, keystores ou tokens no código
- [ ] Documentação atualizada quando necessário (`docs/`)
- [ ] `docs/CHANGELOG.md` atualizado
- [ ] Nenhuma funcionalidade da lista de não-autorizadas foi adicionada (PRD §48)
- [ ] Commits seguem Conventional Commits
