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

    //atualizar a ultima mensagem enviada pelo usuario
    @Query("UPDATE mensagens SET texto = :novoTexto WHERE id = :id")
    void atualizarMensagem(int id, String novoTexto);
    @Query("DELETE FROM mensagens WHERE id = :id")
    void removerPorId(int id);

    @Query("SELECT * FROM mensagens WHERE conversa_id = :conversaId")
    List<MensagemEntity> buscarMensagens(int conversaId);

    @Query("DELETE FROM mensagens")
    void removerTodas();
}