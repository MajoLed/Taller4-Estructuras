import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;

/**
 * Implementación de REFERENCIA (punto 6f) con la biblioteca estándar de Java: TreeMap<String, Integer>
 * (árbol rojo-negro; subMap para el rango del prefijo) y PriorityQueue<Entrada>.
 * Es el ÚNICO lugar del proyecto donde se permiten estas estructuras. Mismas fases y mismo esquema que
 * Indice, para que los resultados sean comparables (prueba de correctitud adicional) y los tiempos también.
 */
public final class IndiceReferencia {
    private IndiceReferencia() { }

    public static final class Resultado {
        public int m, q;
        public long tA, tB, tC;
        public Entrada[] top;
        public String[] prefijos;
        public Entrada[][] autocompletado;
        public TreeMap<String, Integer> tabla;
    }

    private static Entrada[] topK(Iterable<Map.Entry<String, Integer>> fuente, int k) {
        PriorityQueue<Entrada> pq = new PriorityQueue<>();
        for (Map.Entry<String, Integer> e : fuente) {
            pq.add(new Entrada(e.getKey(), e.getValue()));
            if (pq.size() > k) pq.poll();
        }
        Entrada[] r = new Entrada[pq.size()];
        for (int i = r.length - 1; i >= 0; i--) r[i] = pq.poll();
        return r;
    }

    public static Resultado ejecutar(String[] palabras, int n, int k, int kp) {
        Resultado r = new Resultado();
        TreeMap<String, Integer> st = new TreeMap<>();   // String.compareTo: orden ordinal, igual que las tablas propias
        r.tabla = st;

        long t0 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            String w = palabras[i];
            Integer v = st.get(w);
            if (v == null) st.put(w, 1);
            else st.put(w, v + 1);
        }
        r.tA = System.nanoTime() - t0;
        r.m = st.size();

        // Prefijos (fuera de cronómetro, igual que en Indice)
        String[] tmp = new String[st.size()];
        int q = 0;
        String ultimo = null;
        for (String w : st.keySet()) {
            if (w.length() < 2) continue;
            String p = w.substring(0, 2);
            if (!p.equals(ultimo)) { tmp[q++] = p; ultimo = p; }
        }
        r.prefijos = java.util.Arrays.copyOf(tmp, q);
        r.q = q;

        t0 = System.nanoTime();
        r.top = topK(st.entrySet(), k);
        r.tB = System.nanoTime() - t0;

        r.autocompletado = new Entrada[q][];
        t0 = System.nanoTime();
        for (int i = 0; i < q; i++) {
            String p = r.prefijos[i];
            r.autocompletado[i] = topK(st.subMap(p, true, p + Indice.SIGMA, true).entrySet(), kp);
        }
        r.tC = System.nanoTime() - t0;
        return r;
    }
}
