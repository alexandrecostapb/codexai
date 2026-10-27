# ClareAI — convenções

- Java (sem Kotlin), pacote `br.com.example.clareai`, minSdk 31, targetSdk 36.
- Nomes de domínio em português (Mensagem, Conversa, enviar...); classes PascalCase, métodos camelCase,
  constantes UPPER_SNAKE_CASE, recursos snake_case. Textos de UI sempre em `res/values/strings.xml`.
- Estado do chat só no `ClareAIViewModel`, alterado na main thread; banco e rede nunca na main thread
  (Room sem `allowMainThreadQueries`).
- Nunca versionar a chave: `OPENROUTER_API_KEY` vem de `local.properties`/ambiente → `BuildConfig`.
  Não logar perguntas, respostas ou imagens dos alunos (menores de idade).
- Nenhum teste automatizado chama a API real: use `OpenRouterUtil.setInstancia(fake)` ou MockWebServer.
- Antes de concluir: `gradlew assembleDebug assembleRelease lint test`.
- Um commit por etapa, mensagens em português; não misturar renomeação com correção.
