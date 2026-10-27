package br.com.example.clareai;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Última barreira contra a IA entregar a solução pronta: blocos de código longos que não são
 * cópia do que o próprio aluno enviou são trocados por um aviso. Não pega soluções curtas
 * (isso fica por conta do prompt); pega o caso de programas inteiros.
 */
final class FiltroResposta {
    static final int MAX_LINHAS_CODIGO = 8;
    private static final int MAX_LINHAS_COM_IMAGEM = 16;
    private static final double COPIA_DO_ALUNO = 0.95;

    static final String AVISO_CODIGO_OMITIDO =
            "> *Trecho de código omitido: o ClareAI não entrega a solução pronta. "
                    + "Use as dicas acima, tente escrever você mesmo e me mostre o que fez!*";

    private FiltroResposta() {}

    static String limitarCodigo(String resposta, List<Mensagem> historico, boolean comImagem) {
        Set<String> linhasDoAluno = new HashSet<>();
        for (Mensagem m : historico) {
            if (!m.isUsuario() || m.getTexto() == null) continue;
            for (String linha : m.getTexto().split("\n")) {
                String n = normalizar(linha);
                if (!n.isEmpty()) linhasDoAluno.add(n);
            }
        }
        int limite = comImagem ? MAX_LINHAS_COM_IMAGEM : MAX_LINHAS_CODIGO;

        StringBuilder saida = new StringBuilder();
        StringBuilder bloco = null;
        for (String linha : resposta.split("\n", -1)) {
            if (linha.trim().startsWith("```")) {
                if (bloco == null) {
                    bloco = new StringBuilder(linha).append('\n');
                } else {
                    bloco.append(linha);
                    saida.append(decidir(bloco.toString(), linhasDoAluno, limite)).append('\n');
                    bloco = null;
                }
            } else if (bloco != null) {
                bloco.append(linha).append('\n');
            } else {
                saida.append(linha).append('\n');
            }
        }
        // Bloco sem fechamento (resposta cortada por max_tokens).
        if (bloco != null) saida.append(decidir(bloco.toString(), linhasDoAluno, limite));
        return saida.toString().replaceAll("\n+$", "");
    }

    private static String decidir(String bloco, Set<String> linhasDoAluno, int limite) {
        String[] linhas = bloco.split("\n");
        int codigo = 0;
        int copiadas = 0;
        for (int i = 1; i < linhas.length; i++) {
            String t = linhas[i].trim();
            if (t.isEmpty() || t.startsWith("```") || t.startsWith("//") || t.startsWith("# ") || t.equals("#")) continue;
            codigo++;
            if (linhasDoAluno.contains(normalizar(t))) copiadas++;
        }
        if (codigo <= limite) return bloco;
        if ((double) copiadas / codigo >= COPIA_DO_ALUNO) return bloco; // só citou o código do aluno
        return AVISO_CODIGO_OMITIDO;
    }

    private static String normalizar(String s) {
        return s.trim().replaceAll("\\s+", " ");
    }
}
