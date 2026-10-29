/**
 * Árbol binario de búsqueda SIN balancear, con size en cada nodo.
 * put es iterativo: recorre desde la raíz guardando el camino; si la llave es nueva,
 * incrementa size en todos los nodos del camino; si ya existe, solo actualiza el valor
 * (el size no cambia).
 *
 * Costo: get/put/rank/floor/ceiling O(h); select O(h); h puede llegar a M-1 si se inserta en orden.
 */
public class BST extends ArbolBusqueda {
    private final Pila camino = new Pila();

    @Override public void put(String key, int val) {
        if (key == null) throw new IllegalArgumentException("llave null");
        if (root == null) { root = new Node(key, val, 1, BLACK); return; }
        camino.clear();
        Node x = root;
        while (true) {
            int c = compare(key, x.key);
            if (c == 0) { x.val = val; return; }
            camino.push(x);
            Node siguiente = c < 0 ? x.left : x.right;
            if (siguiente == null) {
                Node nuevo = new Node(key, val, 1, BLACK);
                if (c < 0) x.left = nuevo; else x.right = nuevo;
                break;
            }
            x = siguiente;
        }
        for (int i = 0; i < camino.n; i++) camino.nodos[i].size++;
    }
}