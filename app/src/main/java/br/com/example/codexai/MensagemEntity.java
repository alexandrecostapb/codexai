package br.com.example.codexai;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "mensagens")
public class MensagemEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private int id;

    @ColumnInfo(name = "conversa_id")
    private int conversaId;

    @ColumnInfo(name = "texto")
    private String texto;

    @ColumnInfo(name = "usuario")
    private boolean usuario;

    @ColumnInfo(name = "imagem")
    private String imagemBase64;

    public MensagemEntity(
            int conversaId,
            String texto,
            boolean usuario,
            String imagemBase64
    ) {
        this.conversaId = conversaId;
        this.texto = texto;
        this.usuario = usuario;
        this.imagemBase64 = imagemBase64;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getConversaId() {
        return conversaId;
    }

    public void setConversaId(int conversaId) {
        this.conversaId = conversaId;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public boolean isUsuario() {
        return usuario;
    }

    public void setUsuario(boolean usuario) {
        this.usuario = usuario;
    }

    public String getImagemBase64() {
        return imagemBase64;
    }

    public void setImagemBase64(String imagemBase64) {
        this.imagemBase64 = imagemBase64;
    }
}