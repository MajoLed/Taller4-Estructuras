import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tokenizador del Taller 4 (Don Quijote de la Mancha, Project Gutenberg).
 *
 * Contrato:
 *  - Precondición (archivo): la ruta existe, es UTF-8 y contiene una línea con "*** START OF"
 *    antes de una línea con "*** END OF". Si no, lanza IllegalArgumentException.
 *  - Precondición (cadena): el texto no es null.
 *  - Postcondición: devuelve las palabras en orden de aparición, con repeticiones, en minúsculas,
 *    cada una coincidente con la expresión regular [a-záéíóúüñ]+ (secuencias maximales).
 *  - Invariante: es una función pura; el mismo texto produce siempre el mismo arreglo.
 *  - Todo carácter fuera de [a-záéíóúüñ] (tras pasar a minúsculas) es separador.
 */
public final class Tokenizador {
    private static final Pattern PALABRA = Pattern.compile("[a-záéíóúüñ]+");

    private Tokenizador() { }

    /** Lee el archivo, descarta encabezado y pie de Gutenberg y tokeniza el resto. */
    public static String[] tokenizarArchivo(String ruta) throws IOException {
        List<String> lineas = Files.readAllLines(Paths.get(ruta), StandardCharsets.UTF_8);
        int inicio = -1, fin = -1;
        for (int i = 0; i < lineas.size(); i++) {
            String l = lineas.get(i);
            if (inicio < 0 && l.contains("*** START OF")) inicio = i;
            else if (inicio >= 0 && l.contains("*** END OF")) { fin = i; break; }
        }
        if (inicio < 0 || fin < 0)
            throw new IllegalArgumentException("No se encontraron las marcas START/END de Gutenberg");
        StringBuilder sb = new StringBuilder();
        for (int i = inicio + 1; i < fin; i++) sb.append(lineas.get(i)).append('\n');
        return tokenizar(sb.toString());
    }

    /** Convierte a minúsculas y extrae las secuencias maximales de [a-záéíóúüñ]. */
    public static String[] tokenizar(String texto) {
        if (texto == null) throw new IllegalArgumentException("texto null");
        Matcher m = PALABRA.matcher(texto.toLowerCase(Locale.ROOT));
        List<String> palabras = new ArrayList<>();
        while (m.find()) palabras.add(m.group());
        return palabras.toArray(new String[0]);
    }
}