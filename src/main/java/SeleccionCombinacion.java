import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Punto 4: selección determinista de la combinación a evaluar.
 * IDs ordenados alfabéticamente (como cadenas) -> concatenados sin separador -> SHA-256 en hexadecimal ->
 * entero en base 16 completo mod 9 -> fila de la tabla del enunciado.
 *
 * Uso: java SeleccionCombinacion <id1> <id2> [<id3>]
 * Ejemplo del enunciado: 0001234 0005678 0009012 -> f29e3c71...2505593, mod 9 = 3 -> BST + arreglo desordenado.
 */
public final class SeleccionCombinacion {
    private static final String[][] TABLA = {
            {"BinarySearchST", "Arreglo desordenado"}, {"BinarySearchST", "Arreglo ordenado"}, {"BinarySearchST", "Montículo binario"},
            {"BST", "Arreglo desordenado"}, {"BST", "Arreglo ordenado"}, {"BST", "Montículo binario"},
            {"RedBlackBST", "Arreglo desordenado"}, {"RedBlackBST", "Arreglo ordenado"}, {"RedBlackBST", "Montículo binario"}};

    private SeleccionCombinacion() { }

    public static String concatenar(String... ids) {
        String[] copia = ids.clone();
        Arrays.sort(copia);   // String.compareTo: orden de cadenas, ceros a la izquierda incluidos
        return String.join("", copia);
    }

    public static String sha256(String s) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            return String.format("%064x", new BigInteger(1, h));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static int valor(String hashHex) { return new BigInteger(hashHex, 16).mod(BigInteger.valueOf(9)).intValue(); }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Uso: java SeleccionCombinacion <id1> <id2> [<id3>]");
            System.exit(1);
        }
        String[] ordenados = args.clone();
        Arrays.sort(ordenados);
        String cadena = concatenar(args);
        String hash = sha256(cadena);
        int v = valor(hash);
        System.out.println("IDs ordenados:       " + String.join(", ", ordenados));
        System.out.println("Cadena concatenada:  " + cadena);
        System.out.println("SHA-256:             " + hash);
        System.out.println("mod 9:               " + v);
        System.out.println("Combinación:         " + TABLA[v][0] + " + " + TABLA[v][1]);
    }
}
