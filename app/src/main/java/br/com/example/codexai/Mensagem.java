package br.com.example.codexai;

import android.graphics.Bitmap;
public class Mensagem {


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


    // Novo construtor para imagem + texto
    public Mensagem(Bitmap imagem, String texto, boolean usuario){
        this.imagem = imagem;
        this.texto = texto;
        this.usuario = usuario;
    }


    public String getTexto(){ return texto; }
    public void setTexto(String texto){ this.texto = texto; }


    public Bitmap getImagem(){ return imagem; }
    public void setImagem(Bitmap imagem){ this.imagem = imagem; }


    public boolean isUsuario(){ return usuario; }
    public void setUsuario(boolean usuario){ this.usuario = usuario; }
}

