package br.com.example.codexai;

import android.graphics.Bitmap;
public class Mensagem {
    private int id;
    private String texto;
    private Bitmap imagem;
    private boolean usuario;

    public Mensagem(String texto, boolean usuario){
        this.texto = texto;
        this.usuario = usuario;
    }

    public Mensagem(Bitmap imagem, boolean usuario){
        this.imagem = imagem;
        this.usuario = usuario;
    }

    // Novo construtor para imagem + texto (com adicao do id)
    public Mensagem(Bitmap imagem, String texto, boolean usuario){
        this.imagem = imagem;
        this.texto = texto;
        this.usuario = usuario;
    }

    public Mensagem(int id, String texto, Bitmap imagem, boolean usuario) {
        this.id = id;
        this.texto = texto;
        this.imagem = imagem;
        this.usuario = usuario;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
    public String getTexto(){ return texto; }
    public void setTexto(String texto){ this.texto = texto; }

    public Bitmap getImagem(){ return imagem; }
    public void setImagem(Bitmap imagem){ this.imagem = imagem; }

    public boolean isUsuario(){ return usuario; }
    public void setUsuario(boolean usuario){ this.usuario = usuario; }
}