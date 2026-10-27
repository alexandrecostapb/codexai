package br.com.example.clareai;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FiltroConteudoTest {

    @Test
    public void detectaPalavraoComVariacoes() {
        assertTrue(FiltroConteudo.temLinguagemInadequada("que merda de código"));
        assertTrue(FiltroConteudo.temLinguagemInadequada("QUE MERDA"));
        assertTrue(FiltroConteudo.temLinguagemInadequada("p0rra, não funciona"));
        assertTrue(FiltroConteudo.temLinguagemInadequada("merdaaaa"));
        assertTrue(FiltroConteudo.temLinguagemInadequada("isso é uma bosta!"));
        assertTrue(FiltroConteudo.temLinguagemInadequada("desgraçado"));
        assertTrue(FiltroConteudo.temLinguagemInadequada("vai, fdp."));
    }

    @Test
    public void naoBloqueiaDuvidasDeProgramacao() {
        assertFalse(FiltroConteudo.temLinguagemInadequada("Como declarar uma variável em Java?"));
        assertFalse(FiltroConteudo.temLinguagemInadequada("for (int i = 0; i < 10; i++) { soma += i; }"));
        assertFalse(FiltroConteudo.temLinguagemInadequada("O que faz o método computar() e a classe Computador?"));
        assertFalse(FiltroConteudo.temLinguagemInadequada("String sexo = scanner.nextLine();"));
        assertFalse(FiltroConteudo.temLinguagemInadequada("class Disputa { int pontuacao; }"));
        assertFalse(FiltroConteudo.temLinguagemInadequada(""));
        assertFalse(FiltroConteudo.temLinguagemInadequada(null));
    }
}
