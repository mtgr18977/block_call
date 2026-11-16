# Filtro de Chamadas

App Android (Kotlin) que bloqueia ligações de números que não estão nos contatos do telefone, incluindo números ocultos. Usa o `CallScreeningService`.

## Como instalar

1. Abra a aba **Actions**, escolha o último build e baixe o artefato `filtro-chamadas-debug-apk`.
2. Extraia o zip e instale o `.apk` no celular (permita instalar de fontes desconhecidas).
3. No app, permita o acesso aos contatos e defina-o como app de triagem de chamadas.

## Regras

| Situação | Resultado |
|---|---|
| Número está nos contatos | Toca normal |
| Número fora dos contatos | Bloqueado e recusado (fica no histórico) |
| Número oculto/privado | Bloqueado |
| Sem permissão de contatos | Deixa tocar (evita bloquear tudo por engano) |
| Filtro desligado no app | Deixa tocar |
