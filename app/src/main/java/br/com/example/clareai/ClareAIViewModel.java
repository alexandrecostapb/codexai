package br.com.example.clareai;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;

/**
 * Guarda o estado do chat para sobreviver à rotação de tela e executa
 * banco de dados e rede fora da thread principal.
 * Todo o estado exposto (listas, flags) só é alterado na thread principal.
 */
public class ClareAIViewModel extends AndroidViewModel {

    public static final int LIMITE_CHATS = 5;
    public static final int MAX_CARACTERES = 4000;
    private static final int MAX_HISTORICO = 6;
    private static final int LADO_MAX_IMAGEM = 768;
    private static final int QUALIDADE_JPEG = 80;

    private final BancoDeDados banco;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private final List<Conversa> historico = new ArrayList<>();
    private final List<Mensagem> mensagens = new ArrayList<>();
    private final MutableLiveData<Long> mudancas = new MutableLiveData<>(0L);
    private final MutableLiveData<Integer> aviso = new MutableLiveData<>();

    private Conversa conversaAtual;
    private Bitmap imagemSelecionada;
    private boolean enviando;
    private boolean carregando;
    private int cargaAtual;
    private int conversaEnviandoId = -1;
    private Call chamadaAtual;
    private String textoDevolvido;

    public ClareAIViewModel(@NonNull Application application) {
        super(application);
        banco = BancoDeDados.getDatabase(application);
        mensagens.add(new Mensagem(application.getString(R.string.msg_boas_vindas), false));
        io.execute(() -> {
            List<Conversa> conversas = banco.conversaDao().listarTodas();
            main.post(() -> {
                historico.clear();
                historico.addAll(conversas);
                notificar();
            });
        });
    }

    // ---- Estado observável ----

    public LiveData<Long> getMudancas() { return mudancas; }

    public LiveData<Integer> getAviso() { return aviso; }

    public void avisoLido() { aviso.setValue(null); }

    /** Texto rejeitado após o envio (ex.: limite de chats), para o campo de digitação. */
    @Nullable
    public String consumirTextoDevolvido() {
        String texto = textoDevolvido;
        textoDevolvido = null;
        return texto;
    }

    public List<Conversa> getHistorico() { return historico; }

    public List<Mensagem> getMensagens() { return mensagens; }

    public boolean isEnviando() { return enviando; }

    @Nullable
    public Bitmap getImagemSelecionada() { return imagemSelecionada; }

    private void notificar() {
        Long atual = mudancas.getValue();
        mudancas.setValue(atual == null ? 1 : atual + 1);
    }

    private void avisar(int recursoTexto) {
        aviso.setValue(recursoTexto);
    }

    private boolean naTela(int conversaId) {
        return conversaAtual != null && conversaAtual.getId() == conversaId;
    }

    // ---- Conversas ----

    public void novoChat() {
        cargaAtual++;
        carregando = false;
        conversaAtual = null;
        mensagens.clear();
        notificar();
    }

    public void abrirConversa(Conversa conversa) {
        final int carga = ++cargaAtual;
        conversaAtual = conversa;
        carregando = true;
        mensagens.clear();
        notificar();
        io.execute(() -> {
            List<Mensagem> carregadas = new ArrayList<>();
            for (MensagemEntity e : banco.mensagemDao().buscarMensagens(conversa.getId())) {
                carregadas.add(paraMensagem(e));
            }
            main.post(() -> {
                if (carga != cargaAtual) return;
                carregando = false;
                mensagens.addAll(carregadas);
                if (enviando && conversaEnviandoId == conversa.getId()) {
                    mensagens.add(Mensagem.criarPendente(textoPensando()));
                }
                notificar();
            });
        });
    }

    public void renomear(Conversa conversa, String novoNome) {
        String nome = novoNome.trim();
        if (nome.isEmpty()) return;
        conversa.setTitulo(nome);
        notificar();
        io.execute(() -> banco.conversaDao().atualizar(conversa));
    }

    public void excluir(Conversa conversa) {
        historico.remove(conversa);
        if (conversaAtual != null && conversaAtual.getId() == conversa.getId()) {
            novoChat();
        } else {
            notificar();
        }
        io.execute(() -> banco.runInTransaction(() -> {
            banco.mensagemDao().removerPorConversa(conversa.getId());
            banco.conversaDao().remover(conversa);
        }));
    }

    // ---- Imagem ----

    public void descartarImagem() {
        imagemSelecionada = null;
        notificar();
    }

    /** Decodifica a imagem já reduzida; apaga o arquivo temporário (foto da câmera), se houver. */
    public void carregarImagem(Uri uri, @Nullable File temporario) {
        io.execute(() -> {
            Bitmap bitmap = null;
            try {
                bitmap = decodificarReduzida(uri);
            } catch (Exception | OutOfMemoryError e) {
                // tratado abaixo com aviso ao aluno
            } finally {
                if (temporario != null) temporario.delete();
            }
            final Bitmap resultado = bitmap;
            main.post(() -> {
                if (resultado == null) {
                    avisar(R.string.erro_imagem);
                } else {
                    imagemSelecionada = resultado;
                    notificar();
                }
            });
        });
    }

    private Bitmap decodificarReduzida(Uri uri) throws java.io.IOException {
        ImageDecoder.Source origem = ImageDecoder.createSource(
                getApplication().getContentResolver(), uri);
        return ImageDecoder.decodeBitmap(origem, (decoder, info, src) -> {
            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            int largura = info.getSize().getWidth();
            int altura = info.getSize().getHeight();
            int maior = Math.max(largura, altura);
            if (maior > LADO_MAX_IMAGEM) {
                float fator = LADO_MAX_IMAGEM / (float) maior;
                decoder.setTargetSize(
                        Math.max(1, Math.round(largura * fator)),
                        Math.max(1, Math.round(altura * fator)));
            }
        });
    }

    // ---- Envio ----

    /** Retorna true se o envio foi aceito (o campo de texto pode ser limpo). */
    public boolean enviar(String textoDigitado) {
        String texto = textoDigitado.trim();
        Bitmap imagem = imagemSelecionada;
        if (texto.isEmpty() && imagem == null) {
            avisar(R.string.aviso_vazio);
            return false;
        }
        if (texto.length() > MAX_CARACTERES) {
            avisar(R.string.aviso_texto_longo);
            return false;
        }
        if (enviando || carregando) {
            avisar(R.string.aviso_aguarde);
            return false;
        }
        enviando = true;
        imagemSelecionada = null;
        final Conversa existente = conversaAtual;
        notificar();

        io.execute(() -> {
            Conversa conversa = existente;
            final boolean criada = conversa == null;
            if (criada) {
                if (banco.conversaDao().contar() >= LIMITE_CHATS) {
                    main.post(() -> {
                        enviando = false;
                        imagemSelecionada = imagem;
                        textoDevolvido = texto;
                        avisar(R.string.aviso_limite_chats);
                        notificar();
                    });
                    return;
                }
                conversa = new Conversa("");
                int id = (int) banco.conversaDao().inserir(conversa);
                conversa.setId(id);
                conversa.setTitulo("Chat " + id);
                banco.conversaDao().atualizar(conversa);
            }
            final Conversa destino = conversa;
            String base64 = bitmapParaBase64(imagem);
            int mensagemId = (int) banco.mensagemDao().inserir(
                    new MensagemEntity(destino.getId(), texto, true, base64));
            main.post(() -> {
                if (criada) {
                    historico.add(0, destino);
                    if (conversaAtual == null) conversaAtual = destino;
                }
                if (naTela(destino.getId())) {
                    Mensagem mensagem = new Mensagem(imagem, texto, true);
                    mensagem.setId(mensagemId);
                    mensagens.add(mensagem);
                }
                notificar();
            });
            pedirResposta(destino.getId(), base64);
        });
        return true;
    }

    /** Substitui o texto da última mensagem do usuário e gera nova resposta. */
    public void editarUltimaMensagem(int mensagemId, String novoTexto) {
        String texto = novoTexto.trim();
        if (texto.isEmpty()) {
            avisar(R.string.aviso_vazio);
            return;
        }
        if (texto.length() > MAX_CARACTERES) {
            avisar(R.string.aviso_texto_longo);
            return;
        }
        if (enviando || carregando || conversaAtual == null) {
            avisar(R.string.aviso_aguarde);
            return;
        }
        enviando = true;
        final int conversaId = conversaAtual.getId();
        notificar();

        io.execute(() -> {
            banco.mensagemDao().atualizarMensagem(mensagemId, texto);
            List<MensagemEntity> todas = banco.mensagemDao().buscarMensagens(conversaId);
            String base64 = null;
            for (MensagemEntity e : todas) {
                if (e.getId() == mensagemId) base64 = e.getImagemBase64();
            }
            if (!todas.isEmpty() && !todas.get(todas.size() - 1).isUsuario()) {
                banco.mensagemDao().removerPorId(todas.get(todas.size() - 1).getId());
            }
            main.post(() -> {
                if (!naTela(conversaId)) return;
                for (Mensagem m : mensagens) {
                    if (m.getId() == mensagemId) {
                        m.setTexto(texto);
                        break;
                    }
                }
                if (!mensagens.isEmpty() && !mensagens.get(mensagens.size() - 1).isUsuario()) {
                    mensagens.remove(mensagens.size() - 1);
                }
                notificar();
            });
            pedirResposta(conversaId, base64);
        });
    }

    /** Roda na thread de IO: monta o contexto a partir do banco e chama a IA. */
    private void pedirResposta(int conversaId, @Nullable String base64) {
        List<MensagemEntity> todas = banco.mensagemDao().buscarMensagens(conversaId);
        List<Mensagem> contexto = new ArrayList<>();
        int inicio = Math.max(0, todas.size() - MAX_HISTORICO);
        for (MensagemEntity e : todas.subList(inicio, todas.size())) {
            contexto.add(new Mensagem(e.getTexto(), e.isUsuario()));
        }
        main.post(() -> {
            conversaEnviandoId = conversaId;
            if (naTela(conversaId)) {
                mensagens.add(Mensagem.criarPendente(textoPensando()));
            }
            notificar();
        });

        Call chamada = OpenRouterUtil.getInstancia().enviarMensagem(
                contexto, base64, new OpenRouterUtil.PoeCallback() {
                    @Override
                    public void onResponse(String resposta) {
                        io.execute(() -> salvarResposta(conversaId, resposta));
                    }

                    @Override
                    public void onError(OpenRouterUtil.Erro erro) {
                        main.post(() -> falhar(conversaId, erro));
                    }
                });
        main.post(() -> chamadaAtual = chamada);
    }

    private void salvarResposta(int conversaId, String resposta) {
        int id = -1;
        if (banco.conversaDao().contarPorId(conversaId) > 0) {
            id = (int) banco.mensagemDao().inserir(
                    new MensagemEntity(conversaId, resposta, false, null));
        }
        final int mensagemId = id;
        main.post(() -> {
            removerPendente();
            if (mensagemId > 0 && naTela(conversaId)) {
                Mensagem mensagem = new Mensagem(resposta, false);
                mensagem.setId(mensagemId);
                mensagens.add(mensagem);
            }
            terminarEnvio();
        });
    }

    private void falhar(int conversaId, OpenRouterUtil.Erro erro) {
        removerPendente();
        if (naTela(conversaId)) {
            mensagens.add(Mensagem.criarErro(
                    getApplication().getString(textoDoErro(erro))));
        }
        terminarEnvio();
    }

    private void terminarEnvio() {
        enviando = false;
        conversaEnviandoId = -1;
        chamadaAtual = null;
        notificar();
    }

    private void removerPendente() {
        for (int i = mensagens.size() - 1; i >= 0; i--) {
            if (mensagens.get(i).isPendente()) {
                mensagens.remove(i);
            }
        }
    }

    // ---- Utilitários ----

    private String textoPensando() {
        return getApplication().getString(R.string.msg_pensando);
    }

    static int textoDoErro(OpenRouterUtil.Erro erro) {
        switch (erro) {
            case SEM_CHAVE:
                return R.string.erro_sem_chave;
            case SEM_CONEXAO:
                return R.string.erro_sem_conexao;
            case TEMPO_ESGOTADO:
                return R.string.erro_tempo_esgotado;
            case NAO_AUTORIZADO:
                return R.string.erro_nao_autorizado;
            case LIMITE_EXCEDIDO:
                return R.string.erro_limite;
            case SERVIDOR:
                return R.string.erro_servidor;
            case RESPOSTA_INVALIDA:
                return R.string.erro_resposta_invalida;
            default:
                return R.string.erro_generico;
        }
    }

    private Mensagem paraMensagem(MensagemEntity e) {
        Mensagem mensagem;
        if (e.getImagemBase64() != null) {
            byte[] bytes = Base64.decode(e.getImagemBase64(), Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            mensagem = new Mensagem(bitmap, e.getTexto(), e.isUsuario());
        } else {
            mensagem = new Mensagem(e.getTexto(), e.isUsuario());
        }
        mensagem.setId(e.getId());
        return mensagem;
    }

    @Nullable
    private static String bitmapParaBase64(@Nullable Bitmap bitmap) {
        if (bitmap == null) return null;
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, QUALIDADE_JPEG, saida);
        return Base64.encodeToString(saida.toByteArray(), Base64.NO_WRAP);
    }

    @Override
    protected void onCleared() {
        if (chamadaAtual != null) chamadaAtual.cancel();
        io.shutdown();
    }
}
