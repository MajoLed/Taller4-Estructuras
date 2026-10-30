/**
 * Pila LIFO sobre arreglo redimensionable. Reemplaza a la pila de llamadas en los recorridos
 * iterativos de los árboles (put del BST, keys, height, depthStats, verificadores), de modo que un
 * BST degenerado de altura ~M no desborde la pila del hilo (punto 6d).
 * push/pop amortizados O(1); get(i) permite recorrer el camino guardado desde el fondo.
 */
public class Pila<T> {
    private Object[] a = new Object[16];
    private int n;

    public void push(T x) {
        if (n == a.length) {
            Object[] b = new Object[2 * n];
            System.arraycopy(a, 0, b, 0, n);
            a = b;
        }
        a[n++] = x;
    }

    @SuppressWarnings("unchecked")
    public T pop() {
        if (n == 0) throw new java.util.NoSuchElementException("pila vacía");
        T x = (T) a[--n];
        a[n] = null;
        return x;
    }

    /** Elemento i-ésimo desde el fondo (0 = el primero que se apiló). */
    @SuppressWarnings("unchecked")
    public T get(int i) { return (T) a[i]; }

    public int size() { return n; }
    public boolean isEmpty() { return n == 0; }

    public void clear() {
        for (int i = 0; i < n; i++) a[i] = null;
        n = 0;
    }
}
