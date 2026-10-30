import java.util.NoSuchElementException;

/**
 * Cola de prioridad de mínimos con ARREGLO ORDENADO en orden DECRECIENTE (el mínimo queda al final).
 *  - insert: coloca x al final y lo desplaza hacia el frente con intercambios sucesivos mientras el
 *    anterior sea menor (como en el ordenamiento por inserción). Si x termina en la posición p, hace
 *    n - p exch y min(n, n - p + 1) less: mejor caso 1 less / 0 exch, peor caso n less / n exch.
 *  - delMin y min: O(1), 0 less, 0 exch.
 * Invariante: a[0] >= a[1] >= ... >= a[n-1].
 */
public class MinPQOrdenada<T extends Comparable<? super T>> implements ColaPrioridad<T> {
    private T[] a;
    private int n;
    private long less, exch;

    @SuppressWarnings("unchecked")
    public MinPQOrdenada() { a = (T[]) new Comparable<?>[2]; }

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
        a[n] = x;
        for (int j = n; j > 0 && less(j - 1, j); j--) exch(j - 1, j);
        n++;
    }

    @Override public T min() {
        if (n == 0) throw new NoSuchElementException("cola de prioridad vacía");
        return a[n - 1];
    }

    @Override public T delMin() {
        if (n == 0) throw new NoSuchElementException("cola de prioridad vacía");
        T x = a[--n];
        a[n] = null;
        return x;
    }

    /** Verificador (solo pruebas, no cuenta less): el arreglo está en orden decreciente. */
    public boolean isDecreciente() {
        for (int i = 1; i < n; i++) if (a[i - 1].compareTo(a[i]) < 0) return false;
        return true;
    }

    @Override public int size() { return n; }
    @Override public boolean isEmpty() { return n == 0; }
    @Override public long less() { return less; }
    @Override public long exch() { return exch; }
    @Override public void resetCounters() { less = 0; exch = 0; }
}
