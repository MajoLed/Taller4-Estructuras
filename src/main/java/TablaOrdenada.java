/**
 * API común de tabla de símbolos ordenada (Sedgewick y Wayne), llaves String y valores int.
 * Las tres implementaciones (BinarySearchST, BST, RedBlackBST) son intercambiables bajo esta API.
 *
 * Contrato común:
 *  - Las llaves se comparan en orden ordinal (String.compareTo). Ninguna llave puede ser null.
 *  - get devuelve null si la llave no está.
 *  - floor/ceiling devuelven null si no existe tal llave; min/max lanzan NoSuchElementException en tabla vacía.
 *  - rank(key) = número de llaves estrictamente menores que key (key puede no estar en la tabla).
 *  - select(k) = la llave de rango k, 0 <= k < size(); fuera de rango lanza IllegalArgumentException.
 *  - keys(lo, hi) y entries(lo, hi) incluyen ambos extremos; si lo > hi el rango es vacío.
 *  - No hay delete: el problema no lo requiere.
 *
 * Instrumentación: compares cuenta cada comparación de tres vías entre llaves dentro de la estructura
 * (incluidas las de floor, ceiling, rank y keys(lo, hi)). Los verificadores de invariantes NO alteran
 * los contadores.
 */
public interface TablaOrdenada {
    void put(String key, int val);
    Integer get(String key);
    boolean contains(String key);
    int size();
    boolean isEmpty();
    String min();
    String max();
    String floor(String key);
    String ceiling(String key);
    int rank(String key);
    String select(int k);
    Iterable<String> keys();
    /** Todas las entradas (llave, valor) en orden de llave, sin comparar llaves (recorrido completo). */
    Iterable<Entrada> entries();
    Iterable<String> keys(String lo, String hi);
    /** Igual que keys(lo, hi) pero con el valor de cada llave, sin repetir una búsqueda por llave. */
    Iterable<Entrada> entries(String lo, String hi);

    // ---- Contadores ----
    long compares();
    /** Solo BinarySearchST; las demás devuelven 0. */
    long moves();
    /** Solo RedBlackBST; las demás devuelven 0. */
    long rotations();
    /** Solo RedBlackBST; las demás devuelven 0. */
    long flips();
    void resetCounters();

    // ---- Verificadores de invariantes (solo pruebas) ----
    boolean isBST();
    boolean isSizeConsistent();
    boolean isRankConsistent();
}
