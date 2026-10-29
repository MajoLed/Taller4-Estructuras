import java.util.Arrays;

/*
 * Pruebas con assert. Ejecutar con: java -ea PruebasTokenizador
 * */

public class PruebasTokenizador {
    public static void main(String[] args) {
        // Mayúsculas con tilde y ñ
        assert Arrays.equals(Tokenizador.tokenizar("ÁRBOL NIÑO ÜBER"),
                new String[]{"árbol", "niño", "über"});
        // Guiones, comillas y números parten palabras
        assert Arrays.equals(Tokenizador.tokenizar("ab-cd 12ef'gh"),
                new String[]{"ab", "cd", "ef", "gh"});
        // Vacío y solo separadores
        assert Tokenizador.tokenizar("").length == 0;
        assert Tokenizador.tokenizar("123 ... ¿?").length == 0;
        // Repeticiones se conservan y el orden es el del texto
        assert Arrays.equals(Tokenizador.tokenizar("a b a"), new String[]{"a", "b", "a"});
        // Letras fuera del alfabeto del enunciado (ç, à) son separadores
        assert Arrays.equals(Tokenizador.tokenizar("caça"), new String[]{"ca", "a"});
        // null se rechaza
        try { Tokenizador.tokenizar(null); assert false; } catch (IllegalArgumentException ok) { }

        // ---- Entrada: criterio de relevancia ----
        Entrada que = new Entrada("que", 20769), de = new Entrada("de", 18410);
        assert que.compareTo(de) > 0 && de.compareTo(que) < 0;          // mayor frecuencia = más relevante
        Entrada a = new Entrada("amor", 5), b = new Entrada("zorro", 5);
        assert a.compareTo(b) > 0 && b.compareTo(a) < 0;                // empate: palabra menor = más relevante
        assert a.compareTo(new Entrada("amor", 5)) == 0;                // iguales
        // Orden ordinal: ñ y tildes quedan después de z
        Entrada n = new Entrada("ñu", 5), z = new Entrada("zz", 5);
        assert z.compareTo(n) > 0;                                       // "zz" < "ñu" => zz más relevante
        Entrada u = new Entrada("útiles", 5);
        assert z.compareTo(u) > 0;
        // Antisimetría
        assert Integer.signum(a.compareTo(b)) == -Integer.signum(b.compareTo(a));
        // Validación
        try { new Entrada(null, 1); assert false; } catch (IllegalArgumentException ok) { }
        try { new Entrada("x", -1); assert false; } catch (IllegalArgumentException ok) { }
        System.out.println("Pruebas de tokenizador y Entrada: OK");
    }
}