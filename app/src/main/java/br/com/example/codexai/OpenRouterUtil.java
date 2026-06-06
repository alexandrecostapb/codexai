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
    private static final String MODEL = "openrouter/auto"; // Aqui você insere o nome da IA que vai ser utilizada


    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build();


    public interface PoeCallback {
        void onResponse(String resposta);
        void onError(String erro);
    }


    public void enviarMensagem(ArrayList<Mensagem> mensagens, PoeCallback callback) {


        try {


            JSONArray messagesArray = new JSONArray();


            JSONObject system = new JSONObject();
            system.put("role", "system");
            system.put("content",
                    "" //colocar a mensagem incial da ia depois
            );


            messagesArray.put(system);


            for (Mensagem m : mensagens) {


                String texto = m.getTexto();


                // ignora mensagem de carregamento
                if (texto.contains("Pensando")) {
                    continue;
                }


                JSONObject obj = new JSONObject();


                if (m.isUsuario()) {
                    obj.put("role", "user");
                } else {
                    obj.put("role", "assistant");
                }


                obj.put("content", texto);


                messagesArray.put(obj);
            }


            JSONObject body = new JSONObject();
            body.put("model", MODEL);
            body.put("messages", messagesArray);


            RequestBody requestBody = RequestBody.create(
                    body.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );


            Request request = new Request.Builder()
                    .url(BASE_URL)
                    .addHeader("Authorization", "Bearer " + POE_API_KEY)
                    .addHeader("Content-Type", "application/json")
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

