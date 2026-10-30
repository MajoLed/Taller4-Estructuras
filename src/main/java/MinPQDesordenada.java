import java.util.NoSuchElementException;

/**
 * Cola de prioridad de mínimos con ARREGLO DESORDENADO (redimensionable).
 *  - insert: agrega al final, O(1), 0 less, 0 exch.
 *  - delMin: busca el mínimo recorriendo los n elementos (exactamente n - 1 less, sin importar el
 *    orden de los datos), lo intercambia con el último (1 exch, también si ya era el último) y lo retira.
 *  - min: misma búsqueda, n - 1 less, sin intercambio.
 * Invariante: los elementos vivos están en a[0..n-1], en cualquier orden.
 */
public class MinPQDesordenada<T extends Comparable<? super T>> implements ColaPrioridad<T> {
    private T[] a;
    private int n;
    private long less, exch;

    @SuppressWarnings("unchecked")
    public MinPQDesordenada() { a = (T[]) new Comparable<?>[2]; }

    private boolean less(int i, int j) { less++; return a[i].compareTo(a[j]) < 0; }
    private void exch(int i, int j) { exch++; T t = a[i]; a[i] = a[j]; a[j] = t; }

    @SuppressWarnings("unchecked")
    private void resize(int cap) {
        T[] b = (T[]) new Comparable<?>[cap];
        System.arraycopy(a, 0, b, 0, n);
        a = b;
    }

    @Override public void insert(T x) {
        if (x == null) throw new IllegalArgumentException("elemento null");
        if (n == a.length) resize(2 * a.length);
        a[n++] = x;
    }

    private int indiceMin() {
        if (n == 0) throw new NoSuchElementException("cola de prioridad vacía");
        int m = 0;
        for (int i = 1; i < n; i++) if (less(i, m)) m = i;
        return m;
    }

    @Override public T min() { return a[indiceMin()]; }

    @Override public T delMin() {
        int m = indiceMin();
        exch(m, n - 1);
        T x = a[--n];
        a[n] = null;
        return x;
    }

    @Override public int size() { return n; }
    @Override public boolean isEmpty() { return n == 0; }
    @Override public long less() { return less; }
    @Override public long exch() { return exch; }
    @Override public void resetCounters() { less = 0; exch = 0; }
}
