import java.util.NoSuchElementException;

/**
 * Montículo binario en arreglo (pq[1..n]), con swim y sink. Puede orientarse como montículo de MÍNIMOS
 * (implementa ColaPrioridad: min/delMin) o de MÁXIMOS (max/delMax, necesario para la estrategia E2 del
 * punto 6e). top()/delTop() devuelven la cima en cualquiera de las dos orientaciones.
 *
 *  - insert: swim, a lo sumo lg n less y lg n exch.
 *  - delTop: 1 exch (cima con el último) + sink, a lo sumo 2 lg n less y lg n exch.
 *  - Constructor por heapify: copia el arreglo y aplica sink desde floor(n/2) hasta 1, sin insertar
 *    uno a uno: menos de 2n less y menos de n exch (proposición R del texto guía).
 *
 * Instrumentación: less cuenta cada comparación entre dos elementos (una por llamada a prioridad(i, j));
 * exch cuenta cada intercambio.
 * Invariante (orden de montículo): ningún hijo tiene más prioridad que su padre, es decir, en un
 * montículo de mínimos pq[k/2] <= pq[k] y en uno de máximos pq[k/2] >= pq[k] para 2 <= k <= n.
 */
public class MonticuloBinario<T extends Comparable<? super T>> implements ColaPrioridad<T> {
    private T[] pq;
    private int n;
    private final boolean maximos;
    private long less, exch;

    /** Montículo de mínimos vacío. */
    public MonticuloBinario() { this(false); }

    /** Montículo vacío de máximos (maximos = true) o de mínimos (false). */
    @SuppressWarnings("unchecked")
    public MonticuloBinario(boolean maximos) {
        this.maximos = maximos;
        pq = (T[]) new Comparable<?>[2];
    }

    /** Construcción en tiempo lineal por heapify de abajo hacia arriba. El arreglo de entrada no se modifica. */
    @SuppressWarnings("unchecked")
    public MonticuloBinario(T[] elementos, boolean maximos) {
        this.maximos = maximos;
        n = elementos.length;
        pq = (T[]) new Comparable<?>[n + 1];
        for (int i = 0; i < n; i++) {
            if (elementos[i] == null) throw new IllegalArgumentException("elemento null");
            pq[i + 1] = elementos[i];
        }
        for (int k = n / 2; k >= 1; k--) sink(k);
    }

    public static <T extends Comparable<? super T>> MonticuloBinario<T> deMaximos() { return new MonticuloBinario<>(true); }
    public static <T extends Comparable<? super T>> MonticuloBinario<T> deMinimos() { return new MonticuloBinario<>(false); }

    public boolean esDeMaximos() { return maximos; }

    /** true si pq[i] debe quedar DEBAJO de pq[j] (tiene menos prioridad). Una comparación = 1 less. */
    private boolean prioridadMenor(int i, int j) {
        less++;
        int c = pq[i].compareTo(pq[j]);
        return maximos ? c < 0 : c > 0;
    }

    private void exch(int i, int j) { exch++; T t = pq[i]; pq[i] = pq[j]; pq[j] = t; }

    private void swim(int k) {
        while (k > 1 && prioridadMenor(k / 2, k)) { exch(k / 2, k); k /= 2; }
    }

    private void sink(int k) {
        while (2 * k <= n) {
            int j = 2 * k;
            if (j < n && prioridadMenor(j, j + 1)) j++;
            if (!prioridadMenor(k, j)) break;
            exch(k, j);
            k = j;
        }
    }

    @SuppressWarnings("unchecked")
    private void resize(int cap) {
        T[] b = (T[]) new Comparable<?>[cap];
        System.arraycopy(pq, 1, b, 1, n);
        pq = b;
    }

    @Override public void insert(T x) {
        if (x == null) throw new IllegalArgumentException("elemento null");
        if (n == pq.length - 1) resize(2 * pq.length);
        pq[++n] = x;
        swim(n);
    }

    public T top() {
        if (n == 0) throw new NoSuchElementException("cola de prioridad vacía");
        return pq[1];
    }

    public T delTop() {
        if (n == 0) throw new NoSuchElementException("cola de prioridad vacía");
        T x = pq[1];
        exch(1, n--);
        pq[n + 1] = null;
        sink(1);
        return x;
    }

    @Override public T min() { exigir(false); return top(); }
    @Override public T delMin() { exigir(false); return delTop(); }
    public T max() { exigir(true); return top(); }
    public T delMax() { exigir(true); return delTop(); }

    private void exigir(boolean deMaximos) {
        if (maximos != deMaximos)
            throw new IllegalStateException("operación no válida para un montículo de " + (maximos ? "máximos" : "mínimos"));
    }

    // ---- Verificadores (solo pruebas; no tocan los contadores) ----
    /** Orden de montículo según la orientación. */
    public boolean isHeap() {
        for (int k = 2; k <= n; k++) {
            int c = pq[k / 2].compareTo(pq[k]);
            if (maximos ? c < 0 : c > 0) return false;
        }
        return true;
    }

    public boolean isMinHeap() { return !maximos && isHeap(); }
    public boolean isMaxHeap() { return maximos && isHeap(); }

    @Override public int size() { return n; }
    @Override public boolean isEmpty() { return n == 0; }
    @Override public long less() { return less; }
    @Override public long exch() { return exch; }
    @Override public void resetCounters() { less = 0; exch = 0; }
}
