package br.com.example.codexai;

import java.util.ArrayList;

public class Conversa {

    private String titulo;
    private ArrayList<Mensagem> mensagens;

    public Conversa(String titulo) {
        this.titulo = titulo;
        this.mensagens = new ArrayList<>();
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public ArrayList<Mensagem> getMensagens() {
        return mensagens;
    }

    public void setMensagens(ArrayList<Mensagem> mensagens) {
        this.mensagens = mensagens;
    }
}