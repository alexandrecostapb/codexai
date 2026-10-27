package br.com.example.clareai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class FiltroRespostaTest {

    private static String bloco(int linhas) {
        StringBuilder sb = new StringBuilder("```java\n");
        for (int i = 0; i < linhas; i++) sb.append("int x").append(i).append(" = ").append(i).append(";\n");
        return sb.append("```").toString();
    }

    private static List<Mensagem> aluno(String texto) {
        return Collections.singletonList(new Mensagem(texto, true));
    }

    @Test
    public void blocoCurto_ficaIntacto() {
        String r = "Veja:\n" + bloco(5) + "\nTente você.";
        assertEquals(r, FiltroResposta.limitarCodigo(r, aluno("oi"), false));
    }

    @Test
    public void blocoLongo_viraAviso_eTextoAoRedorSobrevive() {
        String r = "Veja:\n" + bloco(12) + "\nTente você.";
        String saida = FiltroResposta.limitarCodigo(r, aluno("faça o exercício"), false);
        assertTrue(saida.contains(FiltroResposta.AVISO_CODIGO_OMITIDO));
        assertTrue(saida.startsWith("Veja:"));
        assertTrue(saida.endsWith("Tente você."));
        assertFalse(saida.contains("int x3"));
    }

    @Test
    public void citacaoExataDoCodigoDoAluno_ficaIntacta() {
        String codigo = bloco(12).replace("```java\n", "").replace("```", "");
        String r = "Seu código:\n" + bloco(12);
        assertEquals(r, FiltroResposta.limitarCodigo(r, aluno(codigo), false));
    }

    @Test
    public void versaoCorrigidaDoCodigoDoAluno_viraAviso() {
        String codigo = bloco(12).replace("```java\n", "").replace("```", "");
        String corrigido = bloco(12).replace("int x3 = 3;", "int x3 = 33;");
        String saida = FiltroResposta.limitarCodigo(corrigido, aluno(codigo), false);
        assertEquals(FiltroResposta.AVISO_CODIGO_OMITIDO, saida);
    }

    @Test
    public void blocoSemFechamento_eComentariosNaoContam() {
        StringBuilder sb = new StringBuilder("```python\n");
        for (int i = 0; i < 30; i++) sb.append("# comentário ").append(i).append('\n');
        sb.append("x = 1\n");
        String r = sb.toString();
        assertEquals(r.replaceAll("\n+$", ""), FiltroResposta.limitarCodigo(r, aluno("oi"), false));

        String aberto = bloco(12).replace("```\n", "").replaceAll("```$", "");
        assertEquals(FiltroResposta.AVISO_CODIGO_OMITIDO,
                FiltroResposta.limitarCodigo(aberto, aluno("oi"), false));
    }

    @Test
    public void comImagem_limiteMaior() {
        String r = bloco(12);
        assertEquals(r, FiltroResposta.limitarCodigo(r, Arrays.asList(new Mensagem("veja", true)), true));
    }

    @Test
    public void semCodigo_naoMuda() {
        String r = "Uma **variável** é uma caixa com etiqueta.\n\nVocê entendeu?";
        assertEquals(r, FiltroResposta.limitarCodigo(r, aluno("o que é variável"), false));
    }
}
