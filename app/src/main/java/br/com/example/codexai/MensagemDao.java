package br.com.example.codexai;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MensagemDao {

    @Insert
    void inserir(MensagemEntity mensagem);

    @Query("UPDATE mensagens SET texto = :novoTexto WHERE id = :id")
    void atualizarMensagem(int id, String novoTexto);

    @Query("SELECT * FROM mensagens WHERE conversa_id = :conversaId")
    List<MensagemEntity> buscarMensagens(int conversaId);

    @Query("DELETE FROM mensagens")
    void removerTodas();

}