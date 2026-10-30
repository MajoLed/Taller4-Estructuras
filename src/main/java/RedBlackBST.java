/**
 * Árbol rojo-negro inclinado a la izquierda (LLRB, Sedgewick y Wayne), con size en cada nodo.
 *
 * put es recursivo: la profundidad de la recursión es la altura, acotada por 2 lg M (≈ 29 para
 * M = 22 944), así que no hay riesgo de desbordar la pila en ningún orden de inserción.
 * Las lecturas (get, rank, keys, ...) son las iterativas de ArbolBusqueda.
 *
 * Instrumentación: rotations cuenta cada rotateLeft/rotateRight; flips cuenta cada flipColors.
 *
 * Invariantes (además de los de ArbolBusqueda): ningún enlace rojo a la derecha, ningún nodo con dos
 * enlaces rojos seguidos (is23), raíz negra, y todo camino raíz-null con el mismo número de enlaces
 * negros (isBalanced). Las rotaciones recalculan size, de modo que isSizeConsistent se mantiene.
 */
public class RedBlackBST extends ArbolBusqueda {

    private static boolean isRed(Node x) { return x != null && x.color == RED; }

    @Override public void put(String key, int val) {
        validar(key);
        root = put(root, key, val);
        root.color = BLACK;
    }

    private Node put(Node h, String key, int val) {
        if (h == null) return new Node(key, val, 1, RED);
        int c = compare(key, h.key);
        if (c < 0) h.left = put(h.left, key, val);
        else if (c > 0) h.right = put(h.right, key, val);
        else h.val = val;

        if (isRed(h.right) && !isRed(h.left)) h = rotateLeft(h);
        if (isRed(h.left) && isRed(h.left.left)) h = rotateRight(h);
        if (isRed(h.left) && isRed(h.right)) flipColors(h);
        h.size = 1 + size(h.left) + size(h.right);
        return h;
    }

    private Node rotateLeft(Node h) {
        rotations++;
        Node x = h.right;
        h.right = x.left;
        x.left = h;
        x.color = h.color;
        h.color = RED;
        x.size = h.size;
        h.size = 1 + size(h.left) + size(h.right);
        return x;
    }

    private Node rotateRight(Node h) {
        rotations++;
        Node x = h.left;
        h.left = x.right;
        x.right = h;
        x.color = h.color;
        h.color = RED;
        x.size = h.size;
        h.size = 1 + size(h.left) + size(h.right);
        return x;
    }

    private void flipColors(Node h) {
        flips++;
        h.color = !h.color;
        h.left.color = !h.left.color;
        h.right.color = !h.right.color;
    }

    // ------------------------------------------------------------------ verificadores propios
    /** Ningún enlace rojo a la derecha, ningún par de rojos seguidos a la izquierda, raíz negra. */
    public boolean is23() {
        if (isRed(root)) return false;
        return is23(root);
    }

    private boolean is23(Node x) {
        if (x == null) return true;
        if (isRed(x.right)) return false;
        if (isRed(x) && isRed(x.left)) return false;
        return is23(x.left) && is23(x.right);
    }

    /** Todos los caminos de la raíz a un enlace nulo tienen el mismo número de enlaces negros. */
    public boolean isBalanced() {
        int negros = 0;
        for (Node x = root; x != null; x = x.left) if (!isRed(x)) negros++;
        return isBalanced(root, negros);
    }

    private boolean isBalanced(Node x, int negros) {
        if (x == null) return negros == 0;
        if (!isRed(x)) negros--;
        return isBalanced(x.left, negros) && isBalanced(x.right, negros);
    }
}
