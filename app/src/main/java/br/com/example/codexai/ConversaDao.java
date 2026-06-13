package br.com.example.codexai;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ConversaDao {

    @Insert
    long inserir(Conversa conversa);

    @Update
    void atualizar(Conversa conversa);

    @Delete
    void remover(Conversa conversa);

    @Query("SELECT * FROM conversas ORDER BY id DESC")
    List<Conversa> listarTodas();
}