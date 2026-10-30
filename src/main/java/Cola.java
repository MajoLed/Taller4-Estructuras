import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Cola FIFO con lista enlazada, propia (no se usan colecciones de la biblioteca en la lógica de las
 * tablas de símbolos). Se usa para devolver keys() / entries() como Iterable.
 * enqueue y dequeue en O(1).
 */
public class Cola<T> implements Iterable<T> {
    private static final class Nodo<T> { T item; Nodo<T> next; Nodo(T item) { this.item = item; } }

    private Nodo<T> first, last;
    private int n;

    public void enqueue(T item) {
        Nodo<T> nuevo = new Nodo<>(item);
        if (last == null) first = nuevo; else last.next = nuevo;
        last = nuevo;
        n++;
    }

    public T dequeue() {
        if (first == null) throw new NoSuchElementException("cola vacía");
        T item = first.item;
        first = first.next;
        if (first == null) last = null;
        n--;
        return item;
    }

    public int size() { return n; }
    public boolean isEmpty() { return n == 0; }

    @Override public Iterator<T> iterator() {
        return new Iterator<>() {
            private Nodo<T> actual = first;
            @Override public boolean hasNext() { return actual != null; }
            @Override public T next() {
                if (actual == null) throw new NoSuchElementException();
                T item = actual.item;
                actual = actual.next;
                return item;
            }
        };
    }
}
