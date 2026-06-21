package br.com.example.codexai;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class OpenRouterUtil {
    private static final String POE_API_KEY = ""; // Aqui você insere sua própria API
    private static final String BASE_URL = "https://openrouter.ai/api/v1/chat/completions"; // URL padrão da API
    private static final String MODEL = "openai/gpt-4o-mini"; // Aqui você insere o nome da IA que vai ser utilizada

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    public interface PoeCallback {
        void onResponse(String resposta);
        void onError(String erro);
    }

    public void enviarMensagem(ArrayList<Mensagem> mensagens,String base64Imagem, PoeCallback callback) {

        try {
            JSONArray messagesArray = new JSONArray();
            JSONObject system = new JSONObject();
            system.put("role", "system");
            system.put("content",
                    "Você é o CodexAI, uma IA que ajuda estudantes a aprender programação. " +
                            "Responda sempre em português, de forma clara, didática e objetiva. \n" +
                            "Não repita apresentações ou saudações." //colocar a mensagem incial da ia depois
            );

            messagesArray.put(system);

            for (Mensagem m : mensagens) {
                if (m.getTexto() != null && m.getTexto().contains("Pensando")) {
                    continue;
                }
                JSONObject obj = new JSONObject();

                if (m.isUsuario()) {
                    obj.put("role", "user");
                } else {
                    obj.put("role", "assistant");
                }

                String texto = m.getTexto();
                if (texto != null && !texto.trim().isEmpty()) {
                    obj.put("content", texto);
                    messagesArray.put(obj);
                }
            }

            //envio da imagem (se existir)
            if(base64Imagem !=null){
                JSONObject obj = new JSONObject();
                obj.put("role", "user");

                JSONArray contentArray = new JSONArray();

                JSONObject textoObj = new JSONObject();
                textoObj.put("type", "text");
                textoObj.put("text", "Analise esta imagem.");

                JSONObject imgObj = new JSONObject();
                imgObj.put("type", "image_url");

                JSONObject imgUrl = new JSONObject();
                imgUrl.put("url", "data:image/jpeg;base64," + base64Imagem);

                imgObj.put("image_url", imgUrl);

                contentArray.put(textoObj);
                contentArray.put(imgObj);

                obj.put("content", contentArray);

                messagesArray.put(obj);
            }

            JSONObject body = new JSONObject();
            body.put("model", MODEL);
            body.put("messages", messagesArray);

            android.util.Log.d("JSON_ENVIADO", body.toString());

            RequestBody requestBody = RequestBody.create(
                    body.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(BASE_URL)
                    .addHeader("Authorization", "Bearer " + POE_API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("HTTP-Referer", "https://javacode.app")
                    .addHeader("X-Title", "JavaCode")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                    callback.onError("Falha na conexão: " + e.getMessage());
                }
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String respostaJson = response.body().string();

                    android.util.Log.d("OPENROUTER_RESPOSTA", respostaJson);

                    if (!response.isSuccessful()) {
                        callback.onError("Erro HTTP: " + response.code());
                        return;
                    }

                    try {
                        JSONObject json = new JSONObject(respostaJson);

                        JSONArray choices = json.getJSONArray("choices");

                        JSONObject message = choices
                                .getJSONObject(0)
                                .getJSONObject("message");

                        String conteudo = message.getString("content");
                        callback.onResponse(conteudo);

                    } catch (Exception e) {
                        callback.onError("Erro ao ler resposta: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            callback.onError("Erro geral: " + e.getMessage());
        }
    }
}