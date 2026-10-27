# Relatório de revisão — ClareAI

Branch: `revisao-clareai`. Achados da Fase 3 (todos verificados lendo o código, salvo onde marcado *suspeita*).

## Achados

| ID | Categoria | Sev. | Arquivo:linha | Problema | Status | Commit |
|----|-----------|------|---------------|----------|--------|--------|
| S1 | Segurança/IA | Crítica | OpenRouterUtil.java:17 | Chave de API hardcoded. Duas chaves já estiveram no código; a antiga (commits 2e36cba, 9eb99bb) está no GitHub (`origin/master`). | pendente (revogação é sua) | — |
| S2 | Privacidade | Alta | OpenRouterUtil.java:80,100; MainActivity.java:582 | `Log.d` do JSON enviado/recebido (perguntas e imagens base64 de menores) em qualquer build | — | — |
| B1 | Bug | Alta | MainActivity.java:530-535,607 | Mensagens novas ficam com `id=0` (insert não devolve id): editar logo após enviar atualiza 0 linhas; a resposta antiga da IA nunca sai do banco e reaparece duplicada ao reabrir o chat | — | — |
| B2 | Bug | Alta | MainActivity.java:576,602-621 | Callback da IA usa `conversaAtual` (pode ser `null` após "novo chat" → NPE) e `mensagens.size()-1` da lista errada se o usuário trocar de chat durante a resposta | — | — |
| B3 | Bug | Alta | MainActivity.java:73-111 | Todo o estado fica na Activity: girar a tela perde chat aberto e resposta em andamento (resposta é gravada, mas a UI some). `ClareAIViewModel` existe mas é código morto | — | — |
| B4 | Bug | Média | MainActivity.java:492-535,576 | Clique durante `enviando` grava a mensagem do usuário mas não chama a IA (fica sem resposta); botão não é desabilitado | — | — |
| B5 | Bug | Média | OpenRouterUtil.java:34 | `contains("Pensando")` descarta do contexto qualquer mensagem real que contenha a palavra | — | — |
| B6 | Bug | Média | MainActivity.java:166-189 | Excluir conversa não apaga suas mensagens (órfãs com dados pessoais) nem reinicia o chat se for o aberto | — | — |
| B7 | Bug | Média | MainActivity.java:338-343 | Permissão CAMERA declarada sem necessidade (ACTION_IMAGE_CAPTURE dispensa); após conceder, nada acontece (sem `onRequestPermissionsResult`). Também é o único erro do lint | — | — |
| B8 | Bug | Média | MainActivity.java:462-490,325-336 | `onActivityResult`/`getBitmap` depreciados, imagem em resolução cheia (risco de OOM), foto temporária nunca apagada | — | — |
| B9 | Bug | Baixa | Sobre.java:34-38 | Botão voltar cria nova MainActivity (empilha e perde o chat) | — | — |
| B10 | Bug | Baixa | MainActivity.java:572 | Editar mensagem com imagem descarta a imagem no reenvio; texto vazio é aceito | — | — |
| R1 | Robustez | Alta | OpenRouterUtil.java:107-141 | `response.body()` sem null-check/close; erro HTTP mostra só código; sem tratamento de 401/429/5xx, sem backoff, sem cancelamento ao sair; mensagens técnicas cruas para o aluno | — | — |
| R2 | Robustez | Média | OpenRouterUtil.java:73 | Sem `max_tokens`; sem limite de tamanho da entrada | — | — |
| P1 | IA | Média | Prompt.java | Sem instrução anti–prompt-injection / não revelar instruções; sem teste de escopo | — | — |
| P2 | IA | Média | OpenRouterUtil.java:16 | `openrouter/free` roteia para modelos gratuitos variáveis (moderação e política de dados imprevisíveis) — *decisão de produto* | pendente | — |
| P3 | IA | Baixa | OpenRouterUtil.java:90-91 | Cabeçalhos `HTTP-Referer`/`X-Title` com marca antiga "JavaCode" | — | — |
| P4 | IA | Baixa | MensagemAdapter.java:25 | Markwon: ok (sem HTML, links não clicáveis). Dependência `markwon:html` não usada | — | — |
| V1 | Privacidade | Alta | AndroidManifest.xml:8 | `allowBackup="true"` com histórico de chats/imagens de menores; regras de backup são o template | — | — |
| V2 | Privacidade | Média | — | LGPD/menores: sem aviso de privacidade nem consentimento; dados vão a terceiro (OpenRouter e modelo) — *decisão de produto* | pendente | — |
| V3 | Segurança | Média | app/build.gradle.kts:23 | R8/minify desligado em release | — | — |
| D1 | Dados | Média | BancoDeDados.java:32-33 | `allowMainThreadQueries` (imagens base64 lidas/gravadas na main thread → ANR) e `fallbackToDestructiveMigration` (apaga o histórico em qualquer mudança de schema) | — | — |
| Q1 | Qualidade | Baixa | vários | Código morto: `mostrarMenuChat`, `atualizarHistorico`, `onPause`, `OnItemLongClickListener.java`, `MensagemDao.removerTodas`, ramo duplicado `if (conversaAtual == null)` | — | — |
| Q2 | Qualidade | Baixa | app/build.gradle.kts:41-44 | Dependências não usadas (gson, mlkit text-recognition, markwon-html) e material duplicado | — | — |
| Q3 | Qualidade | Baixa | MainActivity/Adapters | 34 `HardcodedText` no lint + textos e cores (`Color.parseColor`) fixos no Java | — | — |
| U1 | Acessibilidade | Média | layouts | 6 `ContentDescription` ausentes; alvos de toque <48dp *suspeita* (a verificar no emulador) | — | — |
| G1 | Build | Média | app/build.gradle.kts:7,13 | `targetSdk=34` (lint `OldTargetApi`); SDK 37 está instalado | — | — |

Sem skills dos plugins carregadas nesta sessão (ver seção "Plugins"): análise feita manualmente com os mesmos critérios.
