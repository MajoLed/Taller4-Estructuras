import java.util.NoSuchElementException;

/**
 * Base común de BST y RedBlackBST: el nodo (con size y color), todas las operaciones de LECTURA de la
 * tabla ordenada, la forma del árbol (height, depthStats), los contadores y los verificadores.
 * Las subclases solo definen put (y RedBlackBST sus verificadores propios is23/isBalanced).
 *
 * Todas las operaciones son ITERATIVAS (con Pila/Cola propias): un BST sin balancear construido en orden
 * creciente tiene altura M - 1 ≈ 23 000 y una versión recursiva desbordaría la pila del hilo (punto 6d).
 *
 * Costo: get, contains, floor, ceiling, rank, select en O(h); keys(lo, hi) en O(h + R) donde R es el
 * número de llaves del rango (solo visita la parte del árbol que puede contener llaves del rango);
 * keys() en O(M) sin comparaciones; height y depthStats en O(M).
 *
 * Invariantes: orden simétrico (isBST); x.size = 1 + size(x.left) + size(x.right) para todo nodo.
 */
public abstract class ArbolBusqueda implements TablaOrdenada {
    protected static final boolean RED = true;
    protected static final boolean BLACK = false;

    protected static final class Node {
        final String key;
        int val;
        Node left, right;
        int size;
        boolean color;   // color del enlace que llega desde el padre (solo lo usa RedBlackBST)

        Node(String key, int val, int size, boolean color) {
            this.key = key; this.val = val; this.size = size; this.color = color;
        }
    }

    /** Profundidad promedio de los nodos (dBar) y profundidad promedio ponderada por frecuencia (dBarF). */
    public record DepthStats(double dBar, double dBarF) { }

    protected Node root;
    protected long compares, rotations, flips;

    /** Única comparación de llaves de la estructura: tres vías, cuenta 1. */
    protected final int compare(String a, String b) { compares++; return a.compareTo(b); }

    protected static int size(Node x) { return x == null ? 0 : x.size; }

    protected static void validar(String key) {
        if (key == null) throw new IllegalArgumentException("llave null");
    }

    // ------------------------------------------------------------------ búsqueda
    @Override public Integer get(String key) {
        validar(key);
        Node x = root;
        while (x != null) {
            int c = compare(key, x.key);
            if (c == 0) return x.val;
            x = c < 0 ? x.left : x.right;
        }
        return null;
    }

    @Override public boolean contains(String key) { return get(key) != null; }
    @Override public int size() { return size(root); }
    @Override public boolean isEmpty() { return root == null; }

    // ------------------------------------------------------------------ operaciones ordenadas
    @Override public String min() {
        if (root == null) throw new NoSuchElementException("tabla vacía");
        Node x = root;
        while (x.left != null) x = x.left;
        return x.key;
    }

    @Override public String max() {
        if (root == null) throw new NoSuchElementException("tabla vacía");
        Node x = root;
        while (x.right != null) x = x.right;
        return x.key;
    }

    @Override public String floor(String key) {
        validar(key);
        String mejor = null;
        Node x = root;
        while (x != null) {
            int c = compare(key, x.key);
            if (c == 0) return x.key;
            if (c < 0) x = x.left;
            else { mejor = x.key; x = x.right; }
        }
        return mejor;
    }

    @Override public String ceiling(String key) {
        validar(key);
        String mejor = null;
        Node x = root;
        while (x != null) {
            int c = compare(key, x.key);
            if (c == 0) return x.key;
            if (c > 0) x = x.right;
            else { mejor = x.key; x = x.left; }
        }
        return mejor;
    }

    @Override public int rank(String key) {
        validar(key);
        int r = 0;
        Node x = root;
        while (x != null) {
            int c = compare(key, x.key);
            if (c < 0) x = x.left;
            else if (c > 0) { r += 1 + size(x.left); x = x.right; }
            else return r + size(x.left);
        }
        return r;
    }

    @Override public String select(int k) {
        if (k < 0 || k >= size()) throw new IllegalArgumentException("rango fuera de [0, size()-1]: " + k);
        Node x = root;
        while (true) {
            int t = size(x.left);
            if (k < t) x = x.left;
            else if (k > t) { k -= t + 1; x = x.right; }
            else return x.key;
        }
    }

    // ------------------------------------------------------------------ iteración
    /** Recorrido en orden completo, sin comparar llaves. */
    @Override public Iterable<String> keys() {
        Cola<String> q = new Cola<>();
        recorrerTodo(q, null);
        return q;
    }

    @Override public Iterable<Entrada> entries() {
        Cola<Entrada> q = new Cola<>();
        recorrerTodo(null, q);
        return q;
    }

    private void recorrerTodo(Cola<String> llaves, Cola<Entrada> pares) {
        Pila<Node> pila = new Pila<>();
        Node x = root;
        while (x != null || !pila.isEmpty()) {
            while (x != null) { pila.push(x); x = x.left; }
            x = pila.pop();
            if (llaves != null) llaves.enqueue(x.key);
            else pares.enqueue(new Entrada(x.key, x.val));
            x = x.right;
        }
    }

    @Override public Iterable<String> keys(String lo, String hi) {
        Cola<String> q = new Cola<>();
        recorrerRango(lo, hi, q, null);
        return q;
    }

    @Override public Iterable<Entrada> entries(String lo, String hi) {
        Cola<Entrada> q = new Cola<>();
        recorrerRango(lo, hi, null, q);
        return q;
    }

    /**
     * Recorrido en orden PODADO del rango [lo, hi]. La pila solo contiene nodos con llave >= lo cuyo
     * subárbol izquierdo ya se descartó o se procesará antes; al desapilar una llave > hi se termina,
     * porque todas las que siguen en el recorrido en orden son mayores. Nunca se baja a un subárbol que
     * no pueda contener llaves del rango: O(h + R) nodos visitados.
     */
    private void recorrerRango(String lo, String hi, Cola<String> llaves, Cola<Entrada> pares) {
        validar(lo); validar(hi);
        if (compare(lo, hi) > 0) return;
        Pila<Node> pila = new Pila<>();
        apilarIzquierda(root, lo, pila);
        while (!pila.isEmpty()) {
            Node x = pila.pop();
            if (compare(hi, x.key) < 0) return;
            if (llaves != null) llaves.enqueue(x.key);
            else pares.enqueue(new Entrada(x.key, x.val));
            apilarIzquierda(x.right, lo, pila);
        }
    }

    /** Baja desde x apilando los nodos con llave >= lo; los de llave < lo se saltan por la derecha. */
    private void apilarIzquierda(Node x, String lo, Pila<Node> pila) {
        while (x != null) {
            int c = compare(lo, x.key);
            if (c < 0) { pila.push(x); x = x.left; }
            else if (c > 0) x = x.right;
            else { pila.push(x); return; }   // x.key == lo: su subárbol izquierdo es < lo
        }
    }

    // ------------------------------------------------------------------ forma del árbol
    /** Número de enlaces del camino más largo raíz-hoja; 0 con un nodo, -1 con el árbol vacío. */
    public int height() {
        int h = -1;
        Cola<Node> nivel = new Cola<>();
        if (root != null) nivel.enqueue(root);
        while (!nivel.isEmpty()) {
            h++;
            Cola<Node> siguiente = new Cola<>();
            for (Node x : nivel) {
                if (x.left != null) siguiente.enqueue(x.left);
                if (x.right != null) siguiente.enqueue(x.right);
            }
            nivel = siguiente;
        }
        return h;
    }

    /**
     * dBar = (1/M) Σ d(x);  dBarF = (1/N) Σ f(w) d(w) con N = Σ f(w) (el valor de cada nodo es su frecuencia).
     * Recorrido por niveles: la profundidad de cada nodo es el número del nivel. Árbol vacío: (0, 0).
     */
    public DepthStats depthStats() {
        if (root == null) return new DepthStats(0, 0);
        long sumaD = 0, sumaFD = 0, sumaF = 0, nodos = 0;
        int d = 0;
        Cola<Node> nivel = new Cola<>();
        nivel.enqueue(root);
        while (!nivel.isEmpty()) {
            Cola<Node> siguiente = new Cola<>();
            for (Node x : nivel) {
                nodos++;
                sumaD += d;
                sumaF += x.val;
                sumaFD += (long) x.val * d;
                if (x.left != null) siguiente.enqueue(x.left);
                if (x.right != null) siguiente.enqueue(x.right);
            }
            nivel = siguiente;
            d++;
        }
        return new DepthStats((double) sumaD / nodos, sumaF == 0 ? 0 : (double) sumaFD / sumaF);
    }

    // ------------------------------------------------------------------ contadores
    @Override public long compares() { return compares; }
    @Override public long moves() { return 0; }
    @Override public long rotations() { return rotations; }
    @Override public long flips() { return flips; }
    @Override public void resetCounters() { compares = 0; rotations = 0; flips = 0; }

    // ------------------------------------------------------------------ verificadores (no tocan contadores)
    /** Orden simétrico: el recorrido en orden es estrictamente creciente. */
    @Override public boolean isBST() {
        String anterior = null;
        Pila<Node> pila = new Pila<>();
        Node x = root;
        while (x != null || !pila.isEmpty()) {
            while (x != null) { pila.push(x); x = x.left; }
            x = pila.pop();
            if (anterior != null && anterior.compareTo(x.key) >= 0) return false;
            anterior = x.key;
            x = x.right;
        }
        return true;
    }

    /** x.size == 1 + size(x.left) + size(x.right) en todo nodo. */
    @Override public boolean isSizeConsistent() {
        Pila<Node> pila = new Pila<>();
        if (root != null) pila.push(root);
        while (!pila.isEmpty()) {
            Node x = pila.pop();
            if (x.size != 1 + size(x.left) + size(x.right)) return false;
            if (x.left != null) pila.push(x.left);
            if (x.right != null) pila.push(x.right);
        }
        return true;
    }

    /** rank(select(i)) == i para todo i, y select(rank(k)) == k para toda llave k. */
    @Override public boolean isRankConsistent() {
        long c = compares;
        boolean ok = true;
        for (int i = 0; i < size() && ok; i++) if (rank(select(i)) != i) ok = false;
        if (ok) for (String k : keys()) if (!k.equals(select(rank(k)))) { ok = false; break; }
        compares = c;
        return ok;
    }
}
