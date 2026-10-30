import java.util.NoSuchElementException;
import java.util.Random;

/*
 * Pruebas de las tres tablas de símbolos ordenadas (punto 2), con assert.
 * Ejecutar con: java -ea PruebasTablas  (o ./gradlew test).
 * */
public class PruebasTablas {

    public static void main(String[] args) {
        for (Indice.TipoTabla t : Indice.TipoTabla.values()) {
            vacia(t);
            unElemento(t);
            actualizacion(t);
            operacionesOrdenadas(t);
            rangos(t);
            validacion(t);
        }
        contadoresAMano();
        equivalencia();
        insercionOrdenada();
        System.out.println("Pruebas de tablas de símbolos: OK");
    }

    static void invariantes(TablaOrdenada st) {
        long c = st.compares();
        assert st.isBST() : "orden simétrico";
        assert st.isSizeConsistent() : "size";
        assert st.isRankConsistent() : "rank/select";
        if (st instanceof RedBlackBST rb) {
            assert rb.is23() : "is23";
            assert rb.isBalanced() : "isBalanced";
        }
        assert st.compares() == c : "los verificadores no deben tocar los contadores";
    }

    static int contar(Iterable<?> it) { int n = 0; for (Object ignored : it) n++; return n; }

    static void vacia(Indice.TipoTabla t) {
        TablaOrdenada st = t.nueva();
        assert st.size() == 0 && st.isEmpty();
        assert st.get("x") == null && !st.contains("x");
        assert st.floor("x") == null && st.ceiling("x") == null;
        assert st.rank("x") == 0;
        assert contar(st.keys()) == 0 && contar(st.keys("a", "z")) == 0 && contar(st.entries()) == 0;
        try { st.min(); assert false; } catch (NoSuchElementException ok) { }
        try { st.max(); assert false; } catch (NoSuchElementException ok) { }
        try { st.select(0); assert false; } catch (IllegalArgumentException ok) { }
        if (st instanceof ArbolBusqueda a) {
            assert a.height() == -1;
            assert a.depthStats().dBar() == 0 && a.depthStats().dBarF() == 0;
        }
        invariantes(st);
    }

    static void unElemento(Indice.TipoTabla t) {
        TablaOrdenada st = t.nueva();
        st.put("m", 7);
        assert st.size() == 1 && !st.isEmpty() && st.get("m") == 7 && st.contains("m");
        assert st.min().equals("m") && st.max().equals("m");
        assert st.select(0).equals("m") && st.rank("m") == 0 && st.rank("a") == 0 && st.rank("z") == 1;
        assert st.floor("a") == null && st.floor("z").equals("m");
        assert st.ceiling("a").equals("m") && st.ceiling("z") == null;
        if (st instanceof ArbolBusqueda a) {
            assert a.height() == 0;
            assert a.depthStats().dBar() == 0;
        }
        invariantes(st);
    }

    static void actualizacion(Indice.TipoTabla t) {
        TablaOrdenada st = t.nueva();
        for (String k : new String[]{"m", "c", "t", "a", "e"}) st.put(k, 1);
        st.put("c", 99);
        st.put("t", 50);
        assert st.size() == 5 : "actualizar no cambia el tamaño";
        assert st.get("c") == 99 && st.get("t") == 50 && st.get("a") == 1;
        invariantes(st);
    }

    /** Llaves: a c e h m r t x */
    static TablaOrdenada ejemplo(Indice.TipoTabla t) {
        TablaOrdenada st = t.nueva();
        String[] ks = {"m", "c", "t", "a", "e", "r", "x", "h"};
        for (int i = 0; i < ks.length; i++) st.put(ks[i], i + 1);
        return st;
    }

    static void operacionesOrdenadas(Indice.TipoTabla t) {
        TablaOrdenada st = ejemplo(t);
        assert st.min().equals("a") && st.max().equals("x");
        // floor/ceiling de llaves presentes
        assert st.floor("h").equals("h") && st.ceiling("h").equals("h");
        // ausentes
        assert st.floor("g").equals("e") && st.ceiling("g").equals("h");
        assert st.floor("n").equals("m") && st.ceiling("n").equals("r");
        // menores que min y mayores que max
        assert st.floor("0") == null && st.ceiling("0").equals("a");
        assert st.floor("z").equals("x") && st.ceiling("z") == null;
        // rank/select en los extremos
        assert st.rank("a") == 0 && st.select(0).equals("a");
        assert st.rank("x") == 7 && st.select(7).equals("x");
        assert st.rank("0") == 0 && st.rank("z") == 8 && st.rank("n") == 5;
        try { st.select(8); assert false; } catch (IllegalArgumentException ok) { }
        try { st.select(-1); assert false; } catch (IllegalArgumentException ok) { }
        // keys() en orden
        StringBuilder sb = new StringBuilder();
        for (String k : st.keys()) sb.append(k);
        assert sb.toString().equals("acehmrtx");
        invariantes(st);
    }

    static void rangos(Indice.TipoTabla t) {
        TablaOrdenada st = ejemplo(t);
        // rango vacío entre dos llaves consecutivas
        assert contar(st.keys("f", "g")) == 0;
        // rango de un solo elemento (con extremos presentes y ausentes)
        assert contar(st.keys("h", "h")) == 1 && contar(st.keys("f", "i")) == 1;
        // lo > hi
        assert contar(st.keys("t", "c")) == 0 && contar(st.entries("t", "c")) == 0;
        // rango completo y parcial
        assert contar(st.keys("0", "zz")) == 8;
        StringBuilder sb = new StringBuilder();
        for (String k : st.keys("d", "s")) sb.append(k);
        assert sb.toString().equals("ehmr");
        // entries entrega el valor sin repetir búsquedas
        sb.setLength(0);
        for (Entrada e : st.entries("c", "h")) sb.append(e.palabra()).append(e.frecuencia());
        assert sb.toString().equals("c2e5h8");
        // rango de prefijo con SIGMA: las tildes y la ñ quedan después de la z y entran en el rango
        TablaOrdenada p = t.nueva();
        for (String k : new String[]{"sa", "san", "sancho", "sansón", "saña", "sb", "s", "sz"}) p.put(k, 1);
        sb.setLength(0);
        for (String k : p.keys("sa", "sa" + Indice.SIGMA)) sb.append(k).append(' ');
        assert sb.toString().equals("sa san sancho sansón saña ") : sb;
        sb.setLength(0);
        for (String k : p.keys("sa", "sa~")) sb.append(k).append(' ');
        assert !sb.toString().contains("saña") : "con '~' se pierden palabras: por eso se usa SIGMA";
    }

    static void validacion(Indice.TipoTabla t) {
        TablaOrdenada st = t.nueva();
        try { st.put(null, 1); assert false; } catch (IllegalArgumentException ok) { }
        try { st.get(null); assert false; } catch (IllegalArgumentException ok) { }
        try { st.floor(null); assert false; } catch (IllegalArgumentException ok) { }
        try { st.keys(null, "a"); assert false; } catch (IllegalArgumentException ok) { }
    }

    /** Contadores calculados a mano (ver docs/PUNTOS-CLAVE.md). */
    static void contadoresAMano() {
        String[] ks = {"m", "c", "t", "a", "e"};
        // BST: 0 + 1 + 1 + 2 + 2 = 6 (ejemplo del enunciado)
        BST bst = new BST();
        for (String k : ks) bst.put(k, 1);
        assert bst.compares() == 6 : bst.compares();
        // get("e") en ese BST: m, c, e -> 3 comparaciones (profundidad 2 + 1)
        bst.resetCounters();
        bst.get("e");
        assert bst.compares() == 3;
        // floor("d"): m, c, e -> 3
        bst.resetCounters();
        assert bst.floor("d").equals("c") && bst.compares() == 3;

        // BinarySearchST: compares 0 + 1 + 2 + 2 + 2 = 7 ; moves 0 + 1 + 0 + 3 + 2 = 6
        BinarySearchST bs = new BinarySearchST();
        for (String k : ks) bs.put(k, 1);
        assert bs.compares() == 7 : bs.compares();
        assert bs.moves() == 6 : bs.moves();

        // RedBlackBST con m c t a e: 6 comparaciones, 0 rotaciones, 2 flips
        RedBlackBST rb = new RedBlackBST();
        for (String k : ks) rb.put(k, 1);
        assert rb.compares() == 6 && rb.rotations() == 0 && rb.flips() == 2 : rb.compares() + " " + rb.rotations() + " " + rb.flips();
        // a b c: 0 + 1 + 1 comparaciones, 1 rotación izquierda, 1 flip
        RedBlackBST rb2 = new RedBlackBST();
        for (String k : new String[]{"a", "b", "c"}) rb2.put(k, 1);
        assert rb2.compares() == 2 && rb2.rotations() == 1 && rb2.flips() == 1;
        rb2.resetCounters();
        assert rb2.compares() == 0 && rb2.rotations() == 0 && rb2.flips() == 0;
        invariantes(rb2);
    }

    /** Las tres implementaciones producen la misma secuencia keys() y los mismos valores. */
    static void equivalencia() {
        Random rnd = new Random(42);
        String alfabeto = "abcdeñáéüz";
        String[] seq = new String[20_000];
        for (int i = 0; i < seq.length; i++) {
            int len = 1 + rnd.nextInt(4);
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < len; j++) sb.append(alfabeto.charAt(rnd.nextInt(alfabeto.length())));
            seq[i] = sb.toString();
        }
        TablaOrdenada[] sts = {new BinarySearchST(), new BST(), new RedBlackBST()};
        for (TablaOrdenada st : sts) Indice.construir(st, seq, seq.length);
        String[] ref = null;
        for (TablaOrdenada st : sts) {
            invariantes(st);
            StringBuilder llaves = new StringBuilder(), valores = new StringBuilder();
            for (String k : st.keys()) { llaves.append(k).append(';'); valores.append(st.get(k)).append(';'); }
            StringBuilder pares = new StringBuilder();
            for (Entrada e : st.entries()) pares.append(e).append(';');
            String[] actual = {llaves.toString(), valores.toString(), pares.toString(), st.min(), st.max(),
                    String.valueOf(st.size()), String.valueOf(st.rank("c")), st.floor("cz"), st.ceiling("cz"),
                    String.valueOf(contar(st.keys("b", "d")))};
            if (ref == null) ref = actual;
            else assert java.util.Arrays.equals(ref, actual) : st.getClass().getSimpleName();
        }
    }

    /** Orden creciente: el BST degenera (h = n - 1) y las operaciones iterativas no desbordan la pila. */
    static void insercionOrdenada() {
        int n = 5_000;
        String[] ks = new String[n];
        for (int i = 0; i < n; i++) ks[i] = String.format("k%05d", i);
        BST bst = new BST();
        RedBlackBST rb = new RedBlackBST();
        for (String k : ks) { bst.put(k, 1); rb.put(k, 1); }
        assert bst.height() == n - 1;
        assert bst.compares() == (long) n * (n - 1) / 2 : "inserción en orden: 0 + 1 + ... + (n-1)";
        assert Math.abs(bst.depthStats().dBar() - (n - 1) / 2.0) < 1e-9;
        assert rb.height() <= 2 * Math.ceil(Estadistica.lg(n));
        invariantes(bst);
        invariantes(rb);
        assert bst.get(ks[n - 1]) == 1 && bst.rank(ks[n - 1]) == n - 1 && contar(bst.keys()) == n;
        // Orden decreciente para el BinarySearchST: cada put corre todos los pares -> 0 + 1 + ... + (n-1) moves
        BinarySearchST bs = new BinarySearchST();
        for (int i = n - 1; i >= 0; i--) bs.put(ks[i], 1);
        assert bs.moves() == (long) n * (n - 1) / 2;
        invariantes(bs);
    }
}
