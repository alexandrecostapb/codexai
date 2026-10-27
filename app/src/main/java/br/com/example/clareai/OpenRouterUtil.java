package br.com.example.clareai;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class OpenRouterUtil {
    private static final String BASE_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String MODEL = "openrouter/free";
    static final int MAX_TOKENS = 2000;
    static final int MAX_TENTATIVAS = 3;
    private static final long ESPERA_MAXIMA_MS = 10_000;

    public enum Erro {
        SEM_CHAVE,
        SEM_CONEXAO,
        TEMPO_ESGOTADO,
        NAO_AUTORIZADO,
        LIMITE_EXCEDIDO,
        SERVIDOR,
        RESPOSTA_INVALIDA,
        OUTRO
    }

    public interface PoeCallback {
        void onResponse(String resposta);
        void onError(Erro erro);
    }

    private static OpenRouterUtil instancia;

    private final String apiKey;
    private final String url;
    private final OkHttpClient client;

    public static synchronized OpenRouterUtil getInstancia() {
        if (instancia == null) {
            instancia = new OpenRouterUtil(BuildConfig.OPENROUTER_API_KEY, BASE_URL, 2000);
        }
        return instancia;
    }

    @VisibleForTesting
    public static synchronized void setInstancia(@Nullable OpenRouterUtil nova) {
        instancia = nova;
    }

    public OpenRouterUtil(String apiKey, String url, long atrasoBaseMs) {
        this.apiKey = apiKey;
        this.url = url;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .callTimeout(120, TimeUnit.SECONDS)
                .addInterceptor(new RetryInterceptor(atrasoBaseMs))
                .build();
    }

    /**
     * Envia o histórico à IA. O callback é chamado em thread de rede.
     * Retorna a chamada para permitir cancelamento (null se nada foi enviado).
     */
    @Nullable
    public Call enviarMensagem(List<Mensagem> mensagens, @Nullable String base64Imagem, PoeCallback callback) {
        if (apiKey == null || apiKey.isEmpty()) {
            callback.onError(Erro.SEM_CHAVE);
            return null;
        }
        RequestBody requestBody;
        try {
            requestBody = RequestBody.create(
                    montarCorpo(mensagens, base64Imagem).toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );
        } catch (JSONException e) {
            callback.onError(Erro.OUTRO);
            return null;
        }

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("X-Title", "ClareAI")
                .post(requestBody)
                .build();

        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (call.isCanceled()) return;
                callback.onError(e instanceof SocketTimeoutException
                        ? Erro.TEMPO_ESGOTADO
                        : Erro.SEM_CONEXAO);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful()) {
                        callback.onError(erroDoStatus(response.code()));
                        return;
                    }
                    String conteudo = extrairResposta(body.string());
                    callback.onResponse(conteudo);
                } catch (JSONException e) {
                    callback.onError(Erro.RESPOSTA_INVALIDA);
                } catch (IOException e) {
                    if (!call.isCanceled()) callback.onError(Erro.SEM_CONEXAO);
                }
            }
        });
        return call;
    }

    static JSONObject montarCorpo(List<Mensagem> mensagens, @Nullable String base64Imagem) throws JSONException {
        JSONArray messagesArray = new JSONArray();
        messagesArray.put(new JSONObject()
                .put("role", "system")
                .put("content", Prompt.SYSTEM_PROMPT));

        for (Mensagem m : mensagens) {
            if (m.isPendente() || m.isErro()) continue;
            String texto = m.getTexto();
            if (texto == null || texto.trim().isEmpty()) continue;
            messagesArray.put(new JSONObject()
                    .put("role", m.isUsuario() ? "user" : "assistant")
                    .put("content", texto));
        }

        if (base64Imagem != null) {
            JSONArray conteudo = new JSONArray()
                    .put(new JSONObject()
                            .put("type", "text")
                            .put("text", "Analise esta imagem."))
                    .put(new JSONObject()
                            .put("type", "image_url")
                            .put("image_url", new JSONObject()
                                    .put("url", "data:image/jpeg;base64," + base64Imagem)));
            messagesArray.put(new JSONObject()
                    .put("role", "user")
                    .put("content", conteudo));
        }

        return new JSONObject()
                .put("model", MODEL)
                .put("max_tokens", MAX_TOKENS)
                .put("messages", messagesArray);
    }

    static String extrairResposta(String json) throws JSONException {
        String conteudo = new JSONObject(json)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .optString("content", "");
        if (conteudo.trim().isEmpty()) {
            throw new JSONException("Resposta vazia");
        }
        return conteudo;
    }

    static Erro erroDoStatus(int codigo) {
        if (codigo == 401 || codigo == 403) return Erro.NAO_AUTORIZADO;
        if (codigo == 429) return Erro.LIMITE_EXCEDIDO;
        if (codigo >= 500) return Erro.SERVIDOR;
        return Erro.OUTRO;
    }

    /** Repete a chamada em 429/503 respeitando Retry-After (limitado). */
    private static class RetryInterceptor implements Interceptor {
        private final long atrasoBaseMs;

        RetryInterceptor(long atrasoBaseMs) {
            this.atrasoBaseMs = atrasoBaseMs;
        }

        @NonNull
        @Override
        public Response intercept(@NonNull Chain chain) throws IOException {
            Response resposta = chain.proceed(chain.request());
            int tentativa = 1;
            while ((resposta.code() == 429 || resposta.code() == 503) && tentativa < MAX_TENTATIVAS) {
                long espera = esperaEmMs(resposta, tentativa);
                resposta.close();
                try {
                    Thread.sleep(espera);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrompido", e);
                }
                if (chain.call().isCanceled()) throw new IOException("Cancelado");
                resposta = chain.proceed(chain.request());
                tentativa++;
            }
            return resposta;
        }

        private long esperaEmMs(Response resposta, int tentativa) {
            String retryAfter = resposta.header("Retry-After");
            if (retryAfter != null) {
                try {
                    return Math.min(Long.parseLong(retryAfter.trim()) * 1000, ESPERA_MAXIMA_MS);
                } catch (NumberFormatException ignorado) {
                    // usa o backoff padrão
                }
            }
            return Math.min(atrasoBaseMs * tentativa, ESPERA_MAXIMA_MS);
        }
    }
}
