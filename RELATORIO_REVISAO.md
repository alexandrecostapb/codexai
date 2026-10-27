# Relatório de revisão — ClareAI

Branch: `revisao-clareai`. Achados da Fase 3 (todos verificados lendo o código, salvo onde marcado *suspeita*).

## Achados

| ID | Categoria | Sev. | Arquivo:linha | Problema | Status | Commit |
|----|-----------|------|---------------|----------|--------|--------|
| S1 | Segurança/IA | Crítica | OpenRouterUtil.java:17 | Chave de API hardcoded. Duas chaves já estiveram no código; a antiga (commits 2e36cba, 9eb99bb) está no GitHub (`origin/master`). | parcial: removida do código (c5498e5); revogar chaves no OpenRouter é pendência sua | c5498e5 |
| S2 | Privacidade | Alta | OpenRouterUtil.java:80,100; MainActivity.java:582 | `Log.d` do JSON enviado/recebido (perguntas e imagens base64 de menores) em qualquer build | corrigido | c5498e5, aa44bc7 |
| B1 | Bug | Alta | MainActivity.java:530-535,607 | Mensagens novas ficam com `id=0` (insert não devolve id): editar logo após enviar atualiza 0 linhas; a resposta antiga da IA nunca sai do banco e reaparece duplicada ao reabrir o chat | corrigido | aa44bc7 |
| B2 | Bug | Alta | MainActivity.java:576,602-621 | Callback da IA usa `conversaAtual` (pode ser `null` após "novo chat" → NPE) e `mensagens.size()-1` da lista errada se o usuário trocar de chat durante a resposta | corrigido | aa44bc7 |
| B3 | Bug | Alta | MainActivity.java:73-111 | Todo o estado fica na Activity: girar a tela perde chat aberto e resposta em andamento (resposta é gravada, mas a UI some). `ClareAIViewModel` existe mas é código morto | corrigido | aa44bc7 |
| B4 | Bug | Média | MainActivity.java:492-535,576 | Clique durante `enviando` grava a mensagem do usuário mas não chama a IA (fica sem resposta); botão não é desabilitado | corrigido | aa44bc7 |
| B5 | Bug | Média | OpenRouterUtil.java:34 | `contains("Pensando")` descarta do contexto qualquer mensagem real que contenha a palavra | corrigido | aa44bc7 |
| B6 | Bug | Média | MainActivity.java:166-189 | Excluir conversa não apaga suas mensagens (órfãs com dados pessoais) nem reinicia o chat se for o aberto | corrigido | aa44bc7 |
| B7 | Bug | Média | MainActivity.java:338-343 | Permissão CAMERA declarada sem necessidade (ACTION_IMAGE_CAPTURE dispensa); após conceder, nada acontece (sem `onRequestPermissionsResult`). Também é o único erro do lint | corrigido (INTERNET declarada explicitamente em 4a22262) | c5498e5, 4a22262 |
| B8 | Bug | Média | MainActivity.java:462-490,325-336 | `onActivityResult`/`getBitmap` depreciados, imagem em resolução cheia (risco de OOM), foto temporária nunca apagada | corrigido | aa44bc7 |
| B9 | Bug | Baixa | Sobre.java:34-38 | Botão voltar cria nova MainActivity (empilha e perde o chat) | corrigido | aa44bc7 |
| B10 | Bug | Baixa | MainActivity.java:572 | Editar mensagem com imagem descarta a imagem no reenvio; texto vazio é aceito | corrigido | aa44bc7 |
| R1 | Robustez | Alta | OpenRouterUtil.java:107-141 | `response.body()` sem null-check/close; erro HTTP mostra só código; sem tratamento de 401/429/5xx, sem backoff, sem cancelamento ao sair; mensagens técnicas cruas para o aluno | corrigido | aa44bc7 |
| R2 | Robustez | Média | OpenRouterUtil.java:73 | Sem `max_tokens`; sem limite de tamanho da entrada | corrigido | aa44bc7 |
| P1 | IA | Média | Prompt.java | Sem instrução anti–prompt-injection / não revelar instruções; sem teste de escopo | corrigido | aa44bc7 |
| P2 | IA | Média | OpenRouterUtil.java:16 | `openrouter/free` roteia para modelos gratuitos variáveis (moderação e política de dados imprevisíveis) — *decisão de produto* | pendente | — |
| P3 | IA | Baixa | OpenRouterUtil.java:90-91 | Cabeçalhos `HTTP-Referer`/`X-Title` com marca antiga "JavaCode" | corrigido | aa44bc7 |
| P4 | IA | Baixa | MensagemAdapter.java:25 | Markwon: ok (sem HTML, links não clicáveis). Dependência `markwon:html` não usada | corrigido | aa44bc7 |
| V1 | Privacidade | Alta | AndroidManifest.xml:8 | `allowBackup="true"` com histórico de chats/imagens de menores; regras de backup são o template | corrigido | c5498e5, aa44bc7 |
| V2 | Privacidade | Média | — | LGPD/menores: sem aviso de privacidade nem consentimento; dados vão a terceiro (OpenRouter e modelo) — *decisão de produto* | pendente | — |
| V3 | Segurança | Média | app/build.gradle.kts:23 | R8/minify desligado em release | corrigido | c5498e5, aa44bc7 |
| D1 | Dados | Média | BancoDeDados.java:32-33 | `allowMainThreadQueries` (imagens base64 lidas/gravadas na main thread → ANR) e `fallbackToDestructiveMigration` (apaga o histórico em qualquer mudança de schema) | corrigido | aa44bc7 |
| Q1 | Qualidade | Baixa | vários | Código morto: `mostrarMenuChat`, `atualizarHistorico`, `onPause`, `OnItemLongClickListener.java`, `MensagemDao.removerTodas`, ramo duplicado `if (conversaAtual == null)` | corrigido | aa44bc7 |
| Q2 | Qualidade | Baixa | app/build.gradle.kts:41-44 | Dependências não usadas (gson, mlkit text-recognition, markwon-html) e material duplicado | corrigido | aa44bc7 |
| Q3 | Qualidade | Baixa | MainActivity/Adapters | 34 `HardcodedText` no lint + textos e cores (`Color.parseColor`) fixos no Java | corrigido | aa44bc7 |
| U1 | Acessibilidade | Média | layouts | 6 `ContentDescription` ausentes; alvos de toque <48dp *suspeita* (a verificar no emulador) | parcial: contentDescription do layout principal; auditoria WCAG completa pendente | aa44bc7 |
| G1 | Build | Média | app/build.gradle.kts:7,13 | `targetSdk=34` (lint `OldTargetApi`); SDK 37 está instalado | corrigido | c5498e5, aa44bc7 |


## Resumo executivo

Chave de API real estava no código (e uma anterior no histórico público do GitHub). Foi removida do código versionado, mas **precisa ser revogada por você**. O app foi renomeado para ClareAI, o fluxo de envio/edição/rotação foi reescrito sobre um ViewModel (corrigindo 10 bugs verificados, entre eles resposta duplicada ao editar e NPE ao trocar de chat), a rede ganhou retry em 429, erros amigáveis e prompt anti-injeção, e o app deixou de gravar logs e backups com dados de alunos. 14 testes unitários e 5 Espresso passam; validação no emulador (modo avião e rotação) feita com capturas em `docs/evidencias/`.

## Resultados reais

| Verificação | Resultado |
|---|---|
| Linha de base (antes) | assembleDebug OK; lint: 1 erro (CAMERA) + 93 avisos |
| `assembleDebug` / `assembleRelease` (R8) | OK |
| `lint` | OK (0 erros) |
| `test` | 14 testes, 0 falhas (MockWebServer: sucesso, 401, 500, 429 com retry/desistência, JSON inválido, conteúdo vazio, sem conexão, caracteres especiais, prompt) |
| `connectedDebugAndroidTest` (Medium_Phone, API 37) | 5/5 passam (IA simulada) |
| Emulador | modo avião → "Sem conexão..." sem crash; rotação durante a resposta → conversa e resposta preservadas; chamada real à API funcionou |
| Entrada vazia / longa | vazia coberta por Espresso; limite de 4000 caracteres via `InputFilter` + validação (não exercitado em teste automatizado) |
| Varredura de segurança | **não executada com `claude-security`** (skill indisponível na sessão); revisão manual cobriu S1, S2, V1, V3, B7, FileProvider |

Observações honestas: os testes foram escritos junto com o código, não "vermelho primeiro"; o clique por coordenada do Espresso não alcança o botão enviar (ele fica sobreposto ao campo; toque manual funciona), então os testes usam `performClick()`.

## Pendências suas

1. **Revogar no OpenRouter** a chave `sk-or-v1-1d3e…` (no histórico público: commits 2e36cba, 9eb99bb) e a atual (`sk-or-v1-f7de…`, que esteve só no seu working tree e agora está em `local.properties`). Remover do histórico exigiria reescrever o git — não fiz.
2. Decisão de produto (P2): `openrouter/free` roteia para modelos variáveis; na validação uma resposta veio como "User Safety: safe" (modelo de moderação). Escolha um modelo fixo e verifique a política de dados dele (menores).
3. LGPD (V2): aviso de privacidade/consentimento dos responsáveis; imagens e perguntas vão a terceiros.
4. Auditoria de acessibilidade completa (tamanho de toque, contraste, fonte grande) e textos fixos restantes nos layouts `activity_sobre`, `item_*`, `dialog_renomear`.
5. Atualizar dependências (lint `GradleDependency`) e `compileOptions` Java 8 → 17.

## Recomendações futuras

Backend/proxy para guardar a chave (a chave no APK é extraível); moderação de entrada/saída; botão "tentar novamente" após erro; copiar bloco de código; testes de ViewModel.

## Plugins e skills

Todos os plugins pedidos estavam instalados e ativos (`claude plugin list`, v2.1.310), sem necessidade de `/reload-plugins`. Porém **as skills deles não apareceram na lista de skills desta sessão**, então **não** usei `engineering:*`, `mattpocock-skills:*`, `design-skills:*`, `claude-security`, `code-simplifier`, `claude-android-qa-plugin` nem `claude-code-setup`. Apliquei os mesmos critérios manualmente (análise, testes, emulador via adb, README, CLAUDE.md). Não foram executados: code-simplifier, code-review por plugin e deploy-checklist formal.
