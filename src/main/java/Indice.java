import java.io.IOException;
import java.util.Locale;

/**
 * Programa de construcción del índice de frecuencias y consultas (punto 5).
 *
 *  Lectura y tokenización (NO cronometrada) -> Fase A (construcción) -> Fase B (top-K) -> Fase C (autocompletado)
 *
 *  - Fase A: para cada una de las primeras N palabras, en orden del texto, v = get(w); si v es null
 *    put(w, 1), si no put(w, v + 1) (esquema FrequencyCounter: dos búsquedas por palabra).
 *  - Fase B: recorrer las M entradas en orden de llave; pq.insert(e); si pq.size() > K, pq.delMin().
 *    Extraer con delMin y llenar el resultado desde el final (equivale a invertir).
 *  - Fase C: para cada uno de los Q prefijos distintos de longitud 2, recorrer entries(p, p + SIGMA) y
 *    seleccionar las K' más relevantes con una cola NUEVA, mismo esquema de la fase B.
 *
 * Los contadores se reinician al comenzar cada fase; h, dBar y dBarF se miden al terminar la fase A,
 * fuera del bloque cronometrado. La lista de prefijos también se calcula fuera de los cronómetros:
 * es la entrada de la fase C, no parte de su trabajo.
 *
 * Uso: java Indice [--archivo pg2000.txt] [--tabla BST] [--cola MONTICULO] [--n N] [--k 10] [--kp 5]
 */
public final class Indice {
    /** Carácter mayor que cualquier carácter de [a-záéíóúüñ]: el rango de p es [p, p + SIGMA]. */
    public static final char SIGMA = '\uffff';

    public enum TipoTabla {
        BINARY_SEARCH_ST, BST, RED_BLACK_BST;

        public TablaOrdenada nueva() {
            return switch (this) {
                case BINARY_SEARCH_ST -> new BinarySearchST();
                case BST -> new BST();
                case RED_BLACK_BST -> new RedBlackBST();
            };
        }

        public static TipoTabla de(String s) {
            return switch (s.toUpperCase(Locale.ROOT).replace("-", "_")) {
                case "BINARYSEARCHST", "BINARY_SEARCH_ST", "BS", "BSST" -> BINARY_SEARCH_ST;
                case "BST" -> BST;
                case "REDBLACKBST", "RED_BLACK_BST", "RB", "RBBST", "LLRB" -> RED_BLACK_BST;
                default -> throw new IllegalArgumentException("tabla desconocida: " + s);
            };
        }
    }

    public enum TipoCola {
        DESORDENADA, ORDENADA, MONTICULO;

        public <T extends Comparable<? super T>> ColaPrioridad<T> nueva() {
            return switch (this) {
                case DESORDENADA -> new MinPQDesordenada<>();
                case ORDENADA -> new MinPQOrdenada<>();
                case MONTICULO -> new MonticuloBinario<>();
            };
        }

        public static TipoCola de(String s) {
            return switch (s.toUpperCase(Locale.ROOT)) {
                case "DESORDENADA", "DESORDENADO", "UNORDERED" -> DESORDENADA;
                case "ORDENADA", "ORDENADO", "ORDERED" -> ORDENADA;
                case "MONTICULO", "HEAP", "MONTICULOBINARIO" -> MONTICULO;
                default -> throw new IllegalArgumentException("cola desconocida: " + s);
            };
        }
    }

    /** Todo lo que se mide/reporta en una ejecución del programa. */
    public static final class Resultado {
        public int n, m, q, k, kp;
        public long tA, tB, tC;                                  // nanosegundos
        public long aCompares, aMoves, aRotations, aFlips;
        public long bCompares, bLess, bExch;
        public long cCompares, cLess, cExch;
        public int h = -1;
        public double dBar = Double.NaN, dBarF = Double.NaN;
        public Entrada[] top;
        public String[] prefijos;
        public Entrada[][] autocompletado;
        public TablaOrdenada tabla;

        /** Cadena con todos los contadores: debe ser idéntica entre repeticiones (prueba de sanidad). */
        public String firmaContadores() {
            return aCompares + "," + aMoves + "," + aRotations + "," + aFlips + "," + bCompares + "," + bLess + ","
                    + bExch + "," + cCompares + "," + cLess + "," + cExch + "," + h + "," + m + "," + q;
        }
    }

    private Indice() { }

    // ------------------------------------------------------------------ fases
    /** Fase A: esquema FrequencyCounter (un get seguido de un put por palabra). */
    public static void construir(TablaOrdenada st, String[] palabras, int n) {
        for (int i = 0; i < n; i++) {
            String w = palabras[i];
            Integer v = st.get(w);
            if (v == null) st.put(w, 1);
            else st.put(w, v + 1);
        }
    }

    /**
     * Esquema de cola acotada de las fases B y C: inserta cada entrada y, si la cola pasa de k, retira la
     * menos relevante. Devuelve las min(k, #fuente) entradas más relevantes, de la más a la menos relevante.
     */
    public static Entrada[] topK(Iterable<Entrada> fuente, int k, ColaPrioridad<Entrada> pq) {
        if (k < 1) throw new IllegalArgumentException("k debe ser >= 1");
        for (Entrada e : fuente) {
            pq.insert(e);
            if (pq.size() > k) pq.delMin();
        }
        Entrada[] r = new Entrada[pq.size()];
        for (int i = r.length - 1; i >= 0; i--) r[i] = pq.delMin();
        return r;
    }

    /** Las k palabras más relevantes que empiezan por p: rango contiguo [p, p + SIGMA] de la tabla ordenada. */
    public static Entrada[] autocompletar(TablaOrdenada st, String p, int k, ColaPrioridad<Entrada> pq) {
        return topK(st.entries(p, p + SIGMA), k, pq);
    }

    /** Prefijos distintos de longitud 2 de las palabras de >= 2 letras, en orden (Q = 295 en el texto completo). */
    public static String[] prefijos(TablaOrdenada st) {
        Cola<String> q = new Cola<>();
        String ultimo = null;
        for (String w : st.keys()) {
            if (w.length() < 2) continue;
            String p = w.substring(0, 2);
            if (!p.equals(ultimo)) { q.enqueue(p); ultimo = p; }   // keys() está ordenado: los iguales son contiguos
        }
        String[] r = new String[q.size()];
        int i = 0;
        for (String p : q) r[i++] = p;
        return r;
    }

    /** Ejecuta las tres fases sobre las primeras n palabras con la combinación dada. */
    public static Resultado ejecutar(String[] palabras, int n, TipoTabla tabla, TipoCola cola, int k, int kp) {
        if (n < 0 || n > palabras.length) throw new IllegalArgumentException("n fuera de rango: " + n);
        Resultado r = new Resultado();
        r.n = n; r.k = k; r.kp = kp;
        TablaOrdenada st = tabla.nueva();
        r.tabla = st;

        // ---- Fase A
        st.resetCounters();
        long t0 = System.nanoTime();
        construir(st, palabras, n);
        r.tA = System.nanoTime() - t0;
        r.aCompares = st.compares(); r.aMoves = st.moves(); r.aRotations = st.rotations(); r.aFlips = st.flips();
        r.m = st.size();
        if (st instanceof ArbolBusqueda arbol) {
            r.h = arbol.height();
            ArbolBusqueda.DepthStats ds = arbol.depthStats();
            r.dBar = ds.dBar(); r.dBarF = ds.dBarF();
        }
        r.prefijos = prefijos(st);
        r.q = r.prefijos.length;

        // ---- Fase B
        st.resetCounters();
        ColaPrioridad<Entrada> pq = cola.nueva();
        t0 = System.nanoTime();
        r.top = topK(st.entries(), k, pq);
        r.tB = System.nanoTime() - t0;
        r.bCompares = st.compares(); r.bLess = pq.less(); r.bExch = pq.exch();

        // ---- Fase C
        st.resetCounters();
        r.autocompletado = new Entrada[r.q][];
        long less = 0, exch = 0;
        t0 = System.nanoTime();
        for (int i = 0; i < r.q; i++) {
            ColaPrioridad<Entrada> pc = cola.nueva();
            r.autocompletado[i] = autocompletar(st, r.prefijos[i], kp, pc);
            less += pc.less(); exch += pc.exch();
        }
        r.tC = System.nanoTime() - t0;
        r.cCompares = st.compares(); r.cLess = less; r.cExch = exch;
        return r;
    }

    // ------------------------------------------------------------------ programa
    public static void main(String[] args) throws IOException {
        String archivo = "pg2000.txt";
        TipoTabla tabla = TipoTabla.RED_BLACK_BST;
        TipoCola cola = TipoCola.MONTICULO;
        int n = -1, k = 10, kp = 5;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--archivo" -> archivo = args[++i];
                case "--tabla" -> tabla = TipoTabla.de(args[++i]);
                case "--cola" -> cola = TipoCola.de(args[++i]);
                case "--n" -> n = Integer.parseInt(args[++i]);
                case "--k" -> k = Integer.parseInt(args[++i]);
                case "--kp" -> kp = Integer.parseInt(args[++i]);
                default -> throw new IllegalArgumentException("argumento desconocido: " + args[i]);
            }
        }
        String[] palabras = Tokenizador.tokenizarArchivo(archivo);   // E/S: fuera de toda medición
        if (n < 0) n = palabras.length;

        for (int i = 0; i < 3; i++) ejecutar(palabras, n, tabla, cola, k, kp);   // calentamiento de la JVM
        Resultado r = ejecutar(palabras, n, tabla, cola, k, kp);
        TablaOrdenada st = r.tabla;

        System.out.printf("Combinación: %s + %s   (N = %d, K = %d, K' = %d)%n", tabla, cola, n, k, kp);
        System.out.printf("N = %d   M = %d   Q = %d%n", r.n, r.m, r.q);
        System.out.println("Top-" + k + ": " + unir(r.top));
        for (String p : new String[]{"dul", "san"}) {
            int rango = 0;
            for (String ignored : st.keys(p, p + SIGMA)) rango++;
            System.out.printf("Autocompletado de %s (R = %d): %s%n", p, rango,
                    unir(autocompletar(st, p, kp, cola.nueva())));
        }
        System.out.printf("rank(\"quijote\") = %d   min() = %s   max() = %s%n", st.rank("quijote"), st.min(), st.max());
        System.out.printf("Tiempos (ms): A = %.3f   B = %.3f   C = %.3f%n", r.tA / 1e6, r.tB / 1e6, r.tC / 1e6);
        System.out.printf("Fase A: compares = %d  moves = %d  rotations = %d  flips = %d%n",
                r.aCompares, r.aMoves, r.aRotations, r.aFlips);
        System.out.printf("Fase B: compares = %d  less = %d  exch = %d%n", r.bCompares, r.bLess, r.bExch);
        System.out.printf("Fase C: compares = %d  less = %d  exch = %d%n", r.cCompares, r.cLess, r.cExch);
        if (r.h >= 0) System.out.printf(Locale.ROOT, "Forma tras la fase A: h = %d   dBar = %.4f   dBarF = %.4f%n", r.h, r.dBar, r.dBarF);
    }

    static String unir(Entrada[] es) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < es.length; i++) sb.append(i == 0 ? "" : ", ").append(es[i].palabra()).append(" (").append(es[i].frecuencia()).append(')');
        return sb.toString();
    }
}
