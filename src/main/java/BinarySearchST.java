import java.util.NoSuchElementException;

/**
 * Tabla de símbolos ordenada con dos arreglos paralelos ordenados (keys[], vals[]) y búsqueda binaria.
 * Los arreglos se redimensionan duplicando cuando se llenan.
 *
 * Modelo de costo (documentado para el informe):
 *  - compares: una comparación de tres vías por iteración de la búsqueda binaria (compare(a,b)),
 *    más 1 por la comprobación lo > hi en keys(lo, hi). No se cuenta la igualdad aparte.
 *    Una búsqueda en una tabla de n llaves hace a lo sumo floor(lg n) + 1 comparaciones.
 *  - moves: cada par (llave, valor) desplazado una posición en put cuenta 1.
 *    (La copia por redimensionamiento NO se cuenta como move.) Insertar en la posición i corre n - i pares.
 *
 * Invariantes: 0 <= n <= keys.length; keys[0..n-1] estrictamente creciente (orden ordinal);
 * vals[i] es el valor de keys[i].
 */
public class BinarySearchST implements TablaOrdenada {
    private String[] keys;
    private int[] vals;
    private int n;
    private long compares, moves;

    public BinarySearchST() { keys = new String[2]; vals = new int[2]; }

    private int compare(String a, String b) { compares++; return a.compareTo(b); }

    /** Devuelve i >= 0 si keys[i] == key; si no, -(punto de inserción) - 1. */
    private int search(String key) {
        int lo = 0, hi = n - 1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            int c = compare(key, keys[mid]);
            if (c < 0) hi = mid - 1;
            else if (c > 0) lo = mid + 1;
            else return mid;
        }
        return -lo - 1;
    }

    private void resize(int cap) {
        String[] k = new String[cap]; int[] v = new int[cap];
        for (int i = 0; i < n; i++) { k[i] = keys[i]; v[i] = vals[i]; }
        keys = k; vals = v;
    }

    @Override public void put(String key, int val) {
        if (key == null) throw new IllegalArgumentException("llave null");
        int i = search(key);
        if (i >= 0) { vals[i] = val; return; }
        int ins = -i - 1;
        if (n == keys.length) resize(2 * n);
        for (int j = n; j > ins; j--) { keys[j] = keys[j - 1]; vals[j] = vals[j - 1]; moves++; }
        keys[ins] = key; vals[ins] = val; n++;
    }

    @Override public Integer get(String key) {
        if (key == null) throw new IllegalArgumentException("llave null");
        int i = search(key);
        return i >= 0 ? Integer.valueOf(vals[i]) : null;
    }

    @Override public boolean contains(String key) { return get(key) != null; }
    @Override public int size() { return n; }
    @Override public boolean isEmpty() { return n == 0; }

    @Override public String min() { if (n == 0) throw new NoSuchElementException("tabla vacía"); return keys[0]; }
    @Override public String max() { if (n == 0) throw new NoSuchElementException("tabla vacía"); return keys[n - 1]; }

    private static void validar(String key) {
        if (key == null) throw new IllegalArgumentException("llave null");
    }

    @Override public String floor(String key) {
        validar(key);
        int i = search(key);
        if (i >= 0) return keys[i];
        int ins = -i - 1;
        return ins == 0 ? null : keys[ins - 1];
    }

    @Override public String ceiling(String key) {
        validar(key);
        int i = search(key);
        if (i >= 0) return keys[i];
        int ins = -i - 1;
        return ins == n ? null : keys[ins];
    }

    @Override public int rank(String key) {
        validar(key);
        int i = search(key);
        return i >= 0 ? i : -i - 1;
    }

    @Override public String select(int k) {
        if (k < 0 || k >= n) throw new IllegalArgumentException("rango fuera de [0, size()-1]: " + k);
        return keys[k];
    }

    @Override public Iterable<String> keys() {
        Cola<String> q = new Cola<>();
        for (int i = 0; i < n; i++) q.enqueue(keys[i]);
        return q;
    }

    @Override public Iterable<Entrada> entries() {
        Cola<Entrada> q = new Cola<>();
        for (int i = 0; i < n; i++) q.enqueue(new Entrada(keys[i], vals[i]));
        return q;
    }

    /** Índices [desde, hasta] de las llaves en [lo, hi]; hasta < desde si el rango es vacío. */
    private int[] indices(String lo, String hi) {
        validar(lo); validar(hi);
        if (compare(lo, hi) > 0) return new int[]{0, -1};
        int desde = rank(lo);
        int s = search(hi);
        int hasta = s >= 0 ? s : -s - 2;
        return new int[]{desde, hasta};
    }

    @Override public Iterable<String> keys(String lo, String hi) {
        Cola<String> q = new Cola<>();
        int[] r = indices(lo, hi);
        for (int i = r[0]; i <= r[1]; i++) q.enqueue(keys[i]);
        return q;
    }

    @Override public Iterable<Entrada> entries(String lo, String hi) {
        Cola<Entrada> q = new Cola<>();
        int[] r = indices(lo, hi);
        for (int i = r[0]; i <= r[1]; i++) q.enqueue(new Entrada(keys[i], vals[i]));
        return q;
    }

    @Override public long compares() { return compares; }
    @Override public long moves() { return moves; }
    @Override public long rotations() { return 0; }
    @Override public long flips() { return 0; }
    @Override public void resetCounters() { compares = 0; moves = 0; }

    // ---- Verificadores ----
    @Override public boolean isBST() {
        for (int i = 1; i < n; i++) if (keys[i - 1].compareTo(keys[i]) >= 0) return false;
        return true;
    }

    @Override public boolean isSizeConsistent() {
        if (n < 0 || n > keys.length || keys.length != vals.length) return false;
        for (int i = 0; i < n; i++) if (keys[i] == null) return false;
        return true;
    }

    @Override public boolean isRankConsistent() {
        long c = compares;
        boolean ok = true;
        for (int i = 0; i < n && ok; i++) if (rank(select(i)) != i) ok = false;
        for (int i = 0; i < n && ok; i++) if (!select(rank(keys[i])).equals(keys[i])) ok = false;
        compares = c;
        return ok;
    }
}