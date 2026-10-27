package br.com.example.clareai;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Filtro local de linguagem inadequada, aplicado antes de enviar a mensagem à IA.
 * Não gasta cota nem rede. É só uma primeira barreira: o prompt do sistema continua responsável
 * pelo restante. Compara palavras inteiras, para não bloquear identificadores de código legítimos.
 */
final class FiltroConteudo {
    private static final String[] PROIBIDAS = {
            "porra", "caralho", "carai", "merda", "bosta", "puta", "puto", "putaria", "foda", "fodase",
            "foder", "fodido", "fdp", "pqp", "vsf", "tnc", "buceta", "bct", "viado", "cuzao", "arrombado",
            "babaca", "otario", "imbecil", "retardado", "cacete", "vagabunda", "vagabundo", "desgracado",
            "porno", "pornografia", "nudes", "xvideos"
    };

    private static final Set<String> CONJUNTO = new HashSet<>();

    static {
        for (String palavra : PROIBIDAS) CONJUNTO.add(reduzir(palavra));
    }

    private FiltroConteudo() {}

    /** True se o texto tiver alguma palavra da lista, mesmo com acento, maiúsculas, "p0rra" ou "merdaaa". */
    static boolean temLinguagemInadequada(String texto) {
        if (texto == null || texto.isEmpty()) return false;
        String s = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        StringBuilder palavra = new StringBuilder();
        for (int i = 0; i <= s.length(); i++) {
            char c = i < s.length() ? trocarSimbolo(s.charAt(i)) : ' ';
            if (c >= 'a' && c <= 'z') {
                palavra.append(c);
            } else {
                if (palavra.length() > 0 && CONJUNTO.contains(reduzir(palavra.toString()))) return true;
                palavra.setLength(0);
            }
        }
        return false;
    }

    private static char trocarSimbolo(char c) {
        switch (c) {
            case '0': return 'o';
            case '1': return 'i';
            case '3': return 'e';
            case '4': case '@': return 'a';
            case '5': case '$': return 's';
            default: return c;
        }
    }

    /** Reduz letras repetidas ("merdaaa" e "meerda" viram "merda"). */
    private static String reduzir(String p) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < p.length(); i++) {
            if (i == 0 || p.charAt(i) != p.charAt(i - 1)) sb.append(p.charAt(i));
        }
        return sb.toString();
    }
}
