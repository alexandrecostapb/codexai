# ClareAI

App Android (Java) que responde dúvidas de programação para estudantes de curso técnico integrado ao
ensino médio, usando IA generativa via [OpenRouter](https://openrouter.ai). O prompt de sistema limita o
escopo a programação, orienta a explicar em vez de entregar a solução pronta e é adequado a menores.

## Configurar a chave da API (sem versioná-la)

1. Crie uma chave no OpenRouter.
2. Em `local.properties` (já ignorado pelo git), adicione:
   ```
   OPENROUTER_API_KEY=sk-or-v1-...
   ```
   Alternativa: defina a variável de ambiente `OPENROUTER_API_KEY` antes de compilar.
3. Sem chave, o app abre e mostra uma mensagem de erro ao enviar.

> A chave é embutida no APK via `BuildConfig` e pode ser extraída por quem tiver o APK. Para distribuir
> o app, use um backend/proxy que guarde a chave.

## Build e testes

```
./gradlew assembleDebug        # gradlew.bat no Windows
./gradlew lint
./gradlew test                 # unitários (JUnit + MockWebServer, sem rede real)
./gradlew connectedDebugAndroidTest   # Espresso, exige emulador/dispositivo (API 31+)
```

## Arquitetura

- `MainActivity` — UI; observa o `ClareAIViewModel`.
- `ClareAIViewModel` — estado do chat (sobrevive à rotação), Room e rede fora da main thread.
- `OpenRouterUtil` — cliente OkHttp: retry em 429/503, erros tipados, cancelamento.
- `Prompt` — prompt de sistema. `BancoDeDados`/DAOs/entidades — Room (histórico local).
- Requisitos: minSdk 31, targetSdk 36, AGP 9.3.3, Gradle 9.5.
