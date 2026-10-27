package br.com.example.clareai;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import java.util.List;

public class ClareAIViewModel extends AndroidViewModel {

    private BancoDeDados banco;

    public ClareAIViewModel(
            @NonNull Application application
    ) {
        super(application);
        banco = BancoDeDados.getDatabase(
                application
        );
    }

    // Conversas
    public long inserirConversa(
            Conversa conversa
    ){
        return banco
                .conversaDao()
                .inserir(conversa);
    }

    public List<Conversa> listarConversas(){
        return banco
                .conversaDao()
                .listarTodas();
    }

    // Mensagens
    public void inserirMensagem(
            MensagemEntity mensagem
    ){
        banco
                .mensagemDao()
                .inserir(mensagem);
    }

    public List<MensagemEntity> buscarMensagens(
            int conversaId
    ){
        return banco
                .mensagemDao()
                .buscarMensagens(conversaId);
    }
}