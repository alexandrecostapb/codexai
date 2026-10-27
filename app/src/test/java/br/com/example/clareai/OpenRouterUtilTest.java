package br.com.example.clareai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/** Nenhum teste aqui acessa a API real: tudo vai para um MockWebServer local. */
public class OpenRouterUtilTest {

    private static final String RESPOSTA_OK =
            "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"Olá, aluno\"}}]}";

    private MockWebServer servidor;
    private OpenRouterUtil util;

    @Before
    public void setUp() throws Exception {
        servidor = new MockWebServer();
        servidor.start();
        util = new OpenRouterUtil("chave-de-teste", servidor.url("/v1/chat").toString(), 0);
    }

    @After
    public void tearDown() throws Exception {
        servidor.shutdown();
    }

    private static class Resultado implements OpenRouterUtil.PoeCallback {
        final CountDownLatch pronto = new CountDownLatch(1);
        final AtomicReference<String> resposta = new AtomicReference<>();
        final AtomicReference<OpenRouterUtil.Erro> erro = new AtomicReference<>();

        @Override
        public void onResponse(String r) {
            resposta.set(r);
            pronto.countDown();
        }

        @Override
        public void onError(OpenRouterUtil.Erro e) {
            erro.set(e);
            pronto.countDown();
        }

        Resultado aguardar() throws InterruptedException {
            assertTrue("callback não chamado", pronto.await(10, TimeUnit.SECONDS));
            return this;
        }
    }

    private Resultado enviar(String texto) throws InterruptedException {
        Resultado r = new Resultado();
        List<Mensagem> msgs = new ArrayList<>();
        msgs.add(new Mensagem(texto, true));
        util.enviarMensagem(msgs, null, r);
        return r.aguardar();
    }

    @Test
    public void sucesso_devolveConteudoEEnviaAutorizacao() throws Exception {
        servidor.enqueue(new MockResponse().setBody(RESPOSTA_OK));

        Resultado r = enviar("O que é uma variável?");

        assertEquals("Olá, aluno", r.resposta.get());
        RecordedRequest req = servidor.takeRequest();
        assertEquals("Bearer chave-de-teste", req.getHeader("Authorization"));
        JSONObject corpo = new JSONObject(req.getBody().readUtf8());
        assertEquals(OpenRouterUtil.MAX_TOKENS, corpo.getInt("max_tokens"));
        JSONArray msgs = corpo.getJSONArray("messages");
        assertEquals("system", msgs.getJSONObject(0).getString("role"));
        assertEquals("O que é uma variável?", msgs.getJSONObject(1).getString("content"));
    }

    @Test
    public void semChave_naoFazRequisicao() throws Exception {
        OpenRouterUtil semChave = new OpenRouterUtil("", servidor.url("/").toString(), 0);
        Resultado r = new Resultado();
        semChave.enviarMensagem(new ArrayList<>(), null, r);
        r.aguardar();

        assertEquals(OpenRouterUtil.Erro.SEM_CHAVE, r.erro.get());
        assertEquals(0, servidor.getRequestCount());
    }

    @Test
    public void http401_viraNaoAutorizado() throws Exception {
        servidor.enqueue(new MockResponse().setResponseCode(401).setBody("{}"));
        assertEquals(OpenRouterUtil.Erro.NAO_AUTORIZADO, enviar("oi").erro.get());
    }

    @Test
    public void http500_viraServidor() throws Exception {
        servidor.enqueue(new MockResponse().setResponseCode(500));
        assertEquals(OpenRouterUtil.Erro.SERVIDOR, enviar("oi").erro.get());
    }

    @Test
    public void http429_repeteEDepoisSucede() throws Exception {
        servidor.enqueue(new MockResponse().setResponseCode(429));
        servidor.enqueue(new MockResponse().setResponseCode(429).setHeader("Retry-After", "0"));
        servidor.enqueue(new MockResponse().setBody(RESPOSTA_OK));

        Resultado r = enviar("oi");

        assertEquals("Olá, aluno", r.resposta.get());
        assertEquals(3, servidor.getRequestCount());
    }

    @Test
    public void http429_persistente_desisteAposTentativas() throws Exception {
        for (int i = 0; i < OpenRouterUtil.MAX_TENTATIVAS; i++) {
            servidor.enqueue(new MockResponse().setResponseCode(429));
        }

        Resultado r = enviar("oi");

        assertEquals(OpenRouterUtil.Erro.LIMITE_EXCEDIDO, r.erro.get());
        assertEquals(OpenRouterUtil.MAX_TENTATIVAS, servidor.getRequestCount());
    }

    @Test
    public void jsonInvalido_viraRespostaInvalida() throws Exception {
        servidor.enqueue(new MockResponse().setBody("isto não é json"));
        assertEquals(OpenRouterUtil.Erro.RESPOSTA_INVALIDA, enviar("oi").erro.get());
    }

    @Test
    public void respostaSemChoices_viraRespostaInvalida() throws Exception {
        servidor.enqueue(new MockResponse().setBody("{\"choices\":[]}"));
        assertEquals(OpenRouterUtil.Erro.RESPOSTA_INVALIDA, enviar("oi").erro.get());
    }

    @Test
    public void conteudoVazioOuNulo_viraRespostaInvalida() throws Exception {
        servidor.enqueue(new MockResponse().setBody("{\"choices\":[{\"message\":{\"content\":\"  \"}}]}"));
        assertEquals(OpenRouterUtil.Erro.RESPOSTA_INVALIDA, enviar("oi").erro.get());

        servidor.enqueue(new MockResponse().setBody("{\"choices\":[{\"message\":{\"content\":null}}]}"));
        assertEquals(OpenRouterUtil.Erro.RESPOSTA_INVALIDA, enviar("oi").erro.get());
    }

    @Test
    public void servidorIndisponivel_viraErroDeConexao() throws Exception {
        servidor.shutdown();
        Resultado r = enviar("oi");
        assertNotNull(r.erro.get());
        assertEquals(OpenRouterUtil.Erro.SEM_CONEXAO, r.erro.get());
    }

    @Test
    public void corpo_ignoraPendentesErrosEVazios_eIncluiImagem() throws Exception {
        List<Mensagem> msgs = Arrays.asList(
                new Mensagem("Pensando em loops", true),
                Mensagem.criarPendente("Pensando..."),
                Mensagem.criarErro("Erro qualquer"),
                new Mensagem("   ", true),
                new Mensagem("Resposta anterior", false));

        JSONArray m = OpenRouterUtil.montarCorpo(msgs, "QUJD").getJSONArray("messages");

        // system + "Pensando em loops" + "Resposta anterior" + imagem
        assertEquals(4, m.length());
        assertEquals("Pensando em loops", m.getJSONObject(1).getString("content"));
        assertEquals("assistant", m.getJSONObject(2).getString("role"));
        JSONArray partes = m.getJSONObject(3).getJSONArray("content");
        assertTrue(partes.getJSONObject(1).getJSONObject("image_url")
                .getString("url").endsWith("base64,QUJD"));
    }

    @Test
    public void corpo_caracteresEspeciaisECodigoColado_sobrevivemAoJson() throws Exception {
        String codigo = "System.out.println(\"olá \\n\"); // ç \"aspas\" {}[] \ud83d\ude00\n\tint x = 1;";

        JSONObject corpo = OpenRouterUtil.montarCorpo(
                Arrays.asList(new Mensagem(codigo, true)), null);
        String relido = new JSONObject(corpo.toString())
                .getJSONArray("messages").getJSONObject(1).getString("content");

        assertEquals(codigo, relido);
    }

    @Test
    public void corpo_semImagem_naoTemMensagemExtra() throws Exception {
        JSONArray m = OpenRouterUtil.montarCorpo(
                Arrays.asList(new Mensagem("oi", true)), null).getJSONArray("messages");
        assertEquals(2, m.length());
        assertNull(m.getJSONObject(1).optJSONArray("content"));
    }

    @Test
    public void promptDeSistema_temRegrasDeEscopoEAntiInjecao() {
        assertTrue(Prompt.SYSTEM_PROMPT.contains("exclusivamente em programação"));
        assertTrue(Prompt.SYSTEM_PROMPT.contains("confidenciais"));
        assertTrue(Prompt.SYSTEM_PROMPT.contains("Não forneça a versão completa corrigida"));
    }
}
