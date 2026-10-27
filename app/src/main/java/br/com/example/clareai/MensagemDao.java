package br.com.example.clareai;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MensagemDao {

    @Insert
    long inserir(MensagemEntity mensagem);

    //atualizar a ultima mensagem enviada pelo usuario
    @Query("UPDATE mensagens SET texto = :novoTexto WHERE id = :id")
    void atualizarMensagem(int id, String novoTexto);

    @Query("DELETE FROM mensagens WHERE id = :id")
    void removerPorId(int id);

    @Query("DELETE FROM mensagens WHERE conversa_id = :conversaId")
    void removerPorConversa(int conversaId);

    @Query("SELECT * FROM mensagens WHERE conversa_id = :conversaId ORDER BY id")
    List<MensagemEntity> buscarMensagens(int conversaId);
}
