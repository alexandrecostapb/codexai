package br.com.example.clareai;

import android.graphics.Bitmap;

public class Mensagem {
    private int id;
    private String texto;
    private Bitmap imagem;
    private boolean usuario;
    // Marcadores de mensagens locais que nunca vão para o banco nem para a IA
    private boolean pendente;
    private boolean erro;

    public Mensagem(String texto, boolean usuario){
        this.texto = texto;
        this.usuario = usuario;
    }
    public Mensagem(Bitmap imagem, String texto, boolean usuario){
        this.imagem = imagem;
        this.texto = texto;
        this.usuario = usuario;
    }

    public static Mensagem criarPendente(String texto) {
        Mensagem m = new Mensagem(texto, false);
        m.pendente = true;
        return m;
    }

    public static Mensagem criarErro(String texto) {
        Mensagem m = new Mensagem(texto, false);
        m.erro = true;
        return m;
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

    public boolean isUsuario(){ return usuario; }

    public boolean isPendente() { return pendente; }

    public boolean isErro() { return erro; }
}
