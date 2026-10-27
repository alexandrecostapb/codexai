package br.com.example.clareai;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import android.view.View;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import okhttp3.Call;

/** Fluxo de UI com IA simulada: nenhum teste chama a API real. */
@RunWith(AndroidJUnit4.class)
public class ChatFluxoTest {

    /** IA falsa: responde de forma fixa ou com erro, em outra thread. */
    private static class IaFalsa extends OpenRouterUtil {
        volatile OpenRouterUtil.Erro erro;
        volatile int chamadas;

        IaFalsa() {
            super("falsa", "http://localhost/", 0);
        }

        @Nullable
        @Override
        public Call enviarMensagem(List<Mensagem> mensagens, @Nullable String base64Imagem, PoeCallback callback) {
            chamadas++;
            new Thread(() -> {
                if (erro != null) callback.onError(erro);
                else callback.onResponse("Resposta simulada da IA");
            }).start();
            return null;
        }
    }

    private IaFalsa ia;

    @Before
    public void setUp() {
        Context ctx = ApplicationProvider.getApplicationContext();
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .grantRuntimePermission(ctx.getPackageName(), android.Manifest.permission.RECORD_AUDIO);
        BancoDeDados.getDatabase(ctx).clearAllTables();
        ia = new IaFalsa();
        OpenRouterUtil.setInstancia(ia);
    }

    @After
    public void tearDown() {
        OpenRouterUtil.setInstancia(null);
    }

    /** A IA responde em outra thread; o Espresso não a conhece, então esperamos até 5 s. */
    private void aguardarTexto(Matcher<View> matcher) {
        AssertionError ultimo = null;
        for (int i = 0; i < 50; i++) {
            try {
                onView(matcher).check(matches(isDisplayed()));
                return;
            } catch (AssertionError | RuntimeException e) {
                if (e instanceof AssertionError) ultimo = (AssertionError) e;
                try { Thread.sleep(100); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
        onView(matcher).check(matches(isDisplayed()));
    }

    /** Chama performClick(): o clique por coordenada do Espresso não alcança o botão sobreposto ao campo. */
    private static ViewAction clicarDireto() {
        return new ViewAction() {
            @Override public Matcher<View> getConstraints() { return isDisplayed(); }
            @Override public String getDescription() { return "performClick"; }
            @Override public void perform(UiController uiController, View view) { view.performClick(); }
        };
    }

    private void digitarEEnviar(String texto) {
        onView(withId(R.id.promptEditText)).perform(replaceText(texto), closeSoftKeyboard());
        onView(withId(R.id.enviarButton)).perform(clicarDireto());
    }

    @Test
    public void enviarPergunta_mostraRespostaDaIa() {
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            digitarEEnviar("O que é um laço for?");

            onView(withText("O que é um laço for?")).check(matches(isDisplayed()));
            aguardarTexto(withText("Resposta simulada da IA"));
            onView(withId(R.id.enviarButton)).check(matches(isEnabled()));
            assertEquals(1, ia.chamadas);
        }
    }

    @Test
    public void erroDaIa_mostraMensagemAmigavel() {
        ia.erro = OpenRouterUtil.Erro.SEM_CONEXAO;
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            digitarEEnviar("Olá");

            aguardarTexto(withText(R.string.erro_sem_conexao));
        }
    }

    @Test
    public void entradaVazia_naoChamaIa() {
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            digitarEEnviar("   ");

            assertEquals(0, ia.chamadas);
            onView(withText(containsString("Olá, eu sou o ClareAI"))).check(matches(isDisplayed()));
        }
    }

    @Test
    public void rotacao_mantemConversa() {
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            digitarEEnviar("Pergunta antes de girar");
            aguardarTexto(withText("Resposta simulada da IA"));

            s.recreate();

            onView(withText("Pergunta antes de girar")).check(matches(isDisplayed()));
            aguardarTexto(withText("Resposta simulada da IA"));
        }
    }

    /** Sem ouvir ninguém o reconhecedor devolve erro; o app deve continuar usável e nada é enviado. */
    @Test
    public void microfone_tocarEPararNaoQuebraENaoEnvia() {
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.microfoneButton)).check(matches(isDisplayed()));
            onView(withId(R.id.microfoneButton)).perform(clicarDireto());
            onView(withId(R.id.microfoneButton)).perform(clicarDireto());

            onView(withId(R.id.enviarButton)).check(matches(isEnabled()));
            assertEquals(0, ia.chamadas);
        }
    }

    @Test
    public void codigoColadoECaracteresEspeciais_saoAceitos() {
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
            digitarEEnviar("for (int i=0;i<3;i++){ System.out.println(\"é ç ~ \" + i); }");

            aguardarTexto(withText("Resposta simulada da IA"));
            assertEquals(1, ia.chamadas);
        }
    }
}
