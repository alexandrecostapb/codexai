package br.com.example.clareai;

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

    @Query("SELECT COUNT(*) FROM conversas WHERE id = :id")
    int contarPorId(int id);

    @Query("SELECT COUNT(*) FROM conversas")
    int contar();

    @Query("SELECT * FROM conversas ORDER BY id DESC")
    List<Conversa> listarTodas();
}