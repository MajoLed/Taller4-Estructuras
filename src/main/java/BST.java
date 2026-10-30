/**
 * Árbol binario de búsqueda SIN balancear, con size en cada nodo.
 * put es iterativo: recorre desde la raíz guardando el camino; si la llave es nueva,
 * incrementa size en todos los nodos del camino; si ya existe, solo actualiza el valor
 * (el size no cambia).
 *
 * Costo: get/put/rank/floor/ceiling O(h); select O(h); h puede llegar a M-1 si se inserta en orden.
 * Insertar en un árbol vacío "m","c","t","a","e" cuesta 0 + 1 + 1 + 2 + 2 = 6 comparaciones.
 */
public class BST extends ArbolBusqueda {
    private final Pila<Node> camino = new Pila<>();

    @Override public void put(String key, int val) {
        validar(key);
        if (root == null) { root = new Node(key, val, 1, BLACK); return; }
        camino.clear();
        Node x = root;
        while (true) {
            int c = compare(key, x.key);
            if (c == 0) { x.val = val; return; }   // actualización: el size no cambia
            camino.push(x);
            Node siguiente = c < 0 ? x.left : x.right;
            if (siguiente == null) {
                Node nuevo = new Node(key, val, 1, BLACK);
                if (c < 0) x.left = nuevo; else x.right = nuevo;
                break;
            }
            x = siguiente;
        }
        for (int i = 0; i < camino.size(); i++) camino.get(i).size++;
    }
}
