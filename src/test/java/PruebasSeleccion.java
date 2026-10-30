/*
 * Prueba del procedimiento del punto 4 con el ejemplo del enunciado.
 * Ejecutar con: java -ea PruebasSeleccion  (o ./gradlew test).
 * */
public class PruebasSeleccion {
    public static void main(String[] args) {
        String cadena = SeleccionCombinacion.concatenar("0009012", "0001234", "0005678");
        assert cadena.equals("000123400056780009012") : cadena;
        String hash = SeleccionCombinacion.sha256(cadena);
        assert hash.equals("f29e3c71f1bd144593e36474e7504823362453c0fb69623092aa8ca7b2505593") : hash;
        assert SeleccionCombinacion.valor(hash) == 3 : "BST + arreglo desordenado";
        System.out.println("Prueba de selección de combinación: OK");
    }
}
