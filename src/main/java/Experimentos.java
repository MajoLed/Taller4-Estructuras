import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Random;

/**
 * Evaluación experimental (punto 6). Escribe un CSV por inciso en el directorio de salida
 * (por defecto resultados/), más ajustes_<inciso>.csv con las curvas de mejor ajuste (a, b, R²) de ese
 * inciso; así, volver a correr un inciso no borra los ajustes de los demás.
 *
 * Protocolo: calentamiento de la JVM antes de medir; REPS repeticiones por caso (por defecto 10) con
 * promedio y desviación estándar; los contadores se comparan entre repeticiones y la columna
 * contadores_constantes debe ser siempre true. Lectura, tokenización y E/S quedan fuera de los
 * cronómetros (los CSV se escriben al final de cada caso).
 *
 * Para no depender de la combinación asignada (que se calcula con los ID del equipo), 6b y 6f se
 * corren para las 9 combinaciones, 6d para las 3 tablas y 6e para las 3 colas. Con --tabla y --cola
 * se restringe a la combinación asignada.
 *
 * Uso: java Experimentos [6a 6b 6c 6d 6e 6f | todos] [--reps 10] [--reps-degenerado 3]
 *                        [--archivo pg2000.txt] [--salida resultados] [--tabla BST] [--cola MONTICULO]
 */
public final class Experimentos {
    static final int[] TAMANOS = {6_250, 12_500, 25_000, 50_000, 100_000, 200_000};
    static final int[] VALORES_K = {1, 10, 100, 1_000, 10_000};
    static final int[] VALORES_K_DENSO = {1, 2, 5, 10, 20, 50, 100, 200, 500, 1_000, 2_000, 5_000, 10_000, 15_000, 22_944};
    static final int K = 10, KP = 5, CALENTAMIENTO = 3;

    private static int reps = 10, repsDegenerado = 3;
    private static Path salida = Paths.get("resultados");
    private static Indice.TipoTabla[] tablas = Indice.TipoTabla.values();
    private static Indice.TipoCola[] colas = Indice.TipoCola.values();
    private static PrintWriter ajustes;        // ajustes_<inciso>.csv, se abre con el primer ajuste del inciso
    private static String incisoActual;

    private Experimentos() { }

    public static void main(String[] args) throws IOException {
        String archivo = "pg2000.txt";
        java.util.LinkedHashSet<String> pedidos = new java.util.LinkedHashSet<>();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--reps" -> reps = Integer.parseInt(args[++i]);
                case "--reps-degenerado" -> repsDegenerado = Integer.parseInt(args[++i]);
                case "--archivo" -> archivo = args[++i];
                case "--salida" -> salida = Paths.get(args[++i]);
                case "--tabla" -> tablas = new Indice.TipoTabla[]{Indice.TipoTabla.de(args[++i])};
                case "--cola" -> colas = new Indice.TipoCola[]{Indice.TipoCola.de(args[++i])};
                default -> pedidos.add(args[i].toLowerCase(Locale.ROOT));
            }
        }
        if (pedidos.isEmpty() || pedidos.contains("todos")) pedidos = new java.util.LinkedHashSet<>(java.util.List.of("6a", "6b", "6c", "6d", "6e", "6f"));
        Files.createDirectories(salida);

        String[] palabras = Tokenizador.tokenizarArchivo(archivo);   // no se cronometra
        log("Tokenizado: N = %d palabras", palabras.length);

        for (String p : pedidos) {
            long t0 = System.nanoTime();
            incisoActual = p;
            switch (p) {
                case "6a" -> exp6a(palabras);
                case "6b" -> exp6b(palabras);
                case "6c" -> exp6c(palabras);
                case "6d" -> exp6d(palabras);
                case "6e" -> exp6e(palabras);
                case "6f" -> exp6f(palabras);
                default -> throw new IllegalArgumentException("experimento desconocido: " + p);
            }
            if (ajustes != null) { ajustes.close(); ajustes = null; }
            log("%s terminado en %.1f s", p, (System.nanoTime() - t0) / 1e9);
        }
    }

    // ================================================================== 6a: Heaps y Zipf
    static void exp6a(String[] palabras) throws IOException {
        int[] ns = conCompleto(palabras.length);
        double[] x = new double[TAMANOS.length], y = new double[TAMANOS.length];
        try (PrintWriter w = abrir("6a_heaps.csv")) {
            w.println("N,M,lnN,lnM,en_ajuste");
            for (int i = 0; i < ns.length; i++) {
                TablaOrdenada st = new RedBlackBST();
                Indice.construir(st, palabras, ns[i]);
                int m = st.size();
                w.printf(Locale.ROOT, "%d,%d,%.6f,%.6f,%b%n", ns[i], m, Math.log(ns[i]), Math.log(m), i < TAMANOS.length);
                if (i < TAMANOS.length) { x[i] = ns[i]; y[i] = m; }
            }
        }
        ajuste("6a", "Heaps (serie geométrica)", "M = c N^beta", x, y);

        TablaOrdenada st = new RedBlackBST();
        Indice.construir(st, palabras, palabras.length);
        Entrada[] top = Indice.topK(st.entries(), 1000, new MonticuloBinario<>());
        double[] rx = new double[top.length], fy = new double[top.length];
        try (PrintWriter w = abrir("6a_zipf.csv")) {
            w.println("rango,palabra,frecuencia,ln_rango,ln_frecuencia");
            for (int i = 0; i < top.length; i++) {
                w.printf(Locale.ROOT, "%d,%s,%d,%.6f,%.6f%n", i + 1, top[i].palabra(), top[i].frecuencia(),
                        Math.log(i + 1), Math.log(top[i].frecuencia()));
                rx[i] = i + 1; fy[i] = top[i].frecuencia();
            }
        }
        ajuste("6a", "Zipf (top-1000 texto completo)", "f = a r^b", rx, fy);
    }

    // ================================================================== 6b: escalamiento con N
    static void exp6b(String[] palabras) throws IOException {
        int[] ns = conCompleto(palabras.length);
        try (PrintWriter w = abrir("6b_escalamiento.csv")) {
            w.println("tabla,cola,N,M,Q,reps,tA_ms,tA_sd,tB_ms,tB_sd,tC_ms,tC_sd,"
                    + "A_compares,A_moves,A_rotations,A_flips,B_compares,B_less,B_exch,C_compares,C_less,C_exch,"
                    + "h,dBar,dBarF,contadores_constantes");
            // Calentamiento global: las fases B y C duran ~0,1 ms en los tamaños chicos; sin esto, las primeras
            // combinaciones se miden con el código aún interpretado por la JVM y el ajuste se distorsiona.
            for (int j = 0; j < 2 * CALENTAMIENTO; j++)
                for (Indice.TipoTabla t : tablas) for (Indice.TipoCola c : colas) Indice.ejecutar(palabras, palabras.length, t, c, K, KP);
            for (Indice.TipoTabla t : tablas) for (Indice.TipoCola c : colas) {
                double[][] serie = new double[7][TAMANOS.length];   // tA, tB, tC, compares, moves, rotations, flips
                for (int i = 0; i < ns.length; i++) {
                    int n = ns[i];
                    for (int j = 0; j < CALENTAMIENTO; j++) Indice.ejecutar(palabras, n, t, c, K, KP);
                    double[] ta = new double[reps], tb = new double[reps], tc = new double[reps];
                    Indice.Resultado r = null;
                    boolean constantes = true;
                    String firma = null;
                    for (int j = 0; j < reps; j++) {
                        r = Indice.ejecutar(palabras, n, t, c, K, KP);
                        ta[j] = r.tA / 1e6; tb[j] = r.tB / 1e6; tc[j] = r.tC / 1e6;
                        if (firma == null) firma = r.firmaContadores();
                        else if (!firma.equals(r.firmaContadores())) constantes = false;
                    }
                    w.printf(Locale.ROOT, "%s,%s,%d,%d,%d,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%s,%s,%b%n",
                            t, c, n, r.m, r.q, reps,
                            Estadistica.media(ta), Estadistica.desviacion(ta), Estadistica.media(tb), Estadistica.desviacion(tb),
                            Estadistica.media(tc), Estadistica.desviacion(tc),
                            r.aCompares, r.aMoves, r.aRotations, r.aFlips, r.bCompares, r.bLess, r.bExch,
                            r.cCompares, r.cLess, r.cExch, r.h, num(r.dBar), num(r.dBarF), constantes);
                    w.flush();
                    if (i < TAMANOS.length) {
                        serie[0][i] = Estadistica.media(ta); serie[1][i] = Estadistica.media(tb); serie[2][i] = Estadistica.media(tc);
                        serie[3][i] = r.aCompares; serie[4][i] = r.aMoves; serie[5][i] = r.aRotations; serie[6][i] = r.aFlips;
                    }
                    if (!constantes) log("ADVERTENCIA: contadores distintos entre repeticiones en %s+%s N=%d", t, c, n);
                }
                String s = t + "+" + c;
                double[] x = aDouble(TAMANOS);
                ajuste("6b", s, "tiempo fase A (ms)", x, serie[0]);
                ajuste("6b", s, "tiempo fase B (ms)", x, serie[1]);
                ajuste("6b", s, "tiempo fase C (ms)", x, serie[2]);
                ajuste("6b", s, "compares fase A", x, serie[3]);
                if (serie[4][0] > 0) ajuste("6b", s, "moves fase A", x, serie[4]);
                if (serie[5][0] > 0) ajuste("6b", s, "rotations fase A", x, serie[5]);
                if (serie[6][0] > 0) ajuste("6b", s, "flips fase A", x, serie[6]);
                log("6b %s listo", s);
            }
        }
    }

    // ================================================================== 6c: forma de los árboles
    static void exp6c(String[] palabras) throws IOException {
        try (PrintWriter w = abrir("6c_forma.csv")) {
            w.println("N,arbol,M,h,dBar,dBarF,lgM,dos_lnM,dos_lgM,dBar_mas_1");
            for (int n : conCompleto(palabras.length)) {
                for (ArbolBusqueda arbol : new ArbolBusqueda[]{new BST(), new RedBlackBST()}) {
                    Indice.construir(arbol, palabras, n);
                    ArbolBusqueda.DepthStats ds = arbol.depthStats();
                    int m = arbol.size();
                    w.printf(Locale.ROOT, "%d,%s,%d,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f%n", n, arbol.getClass().getSimpleName(), m,
                            arbol.height(), ds.dBar(), ds.dBarF(), Estadistica.lg(m), 2 * Math.log(m), 2 * Estadistica.lg(m), ds.dBar() + 1);
                }
            }
        }
    }

    // ================================================================== 6d: sensibilidad al orden de inserción
    static void exp6d(String[] palabras) throws IOException {
        // Vocabulario y frecuencias del texto completo (preparación, no se cronometra)
        RedBlackBST indice = new RedBlackBST();
        Indice.construir(indice, palabras, palabras.length);
        int m = indice.size();
        Entrada[] creciente = new Entrada[m];
        int i = 0;
        for (Entrada e : indice.entries()) creciente[i++] = e;
        Entrada[] decreciente = new Entrada[m];
        for (i = 0; i < m; i++) decreciente[i] = creciente[m - 1 - i];
        Entrada[] texto = new Entrada[m];
        RedBlackBST vistos = new RedBlackBST();   // estructura propia para detectar la primera aparición
        i = 0;
        for (String w : palabras) if (vistos.get(w) == null) { vistos.put(w, 1); texto[i++] = new Entrada(w, indice.get(w)); }

        try (PrintWriter w = abrir("6d_orden.csv")) {
            w.println("tabla,escenario,semilla,M,reps,tConstruccion_ms,tConstruccion_sd,compares,moves,rotations,flips,"
                    + "h,dBar,dBarF,tBusqueda_ms,tBusqueda_sd,compares_busqueda,contadores_constantes");
            for (Indice.TipoTabla t : tablas) {
                escenario6d(w, t, "texto", 0, texto, palabras);
                escenario6d(w, t, "creciente", 0, creciente, palabras);
                escenario6d(w, t, "decreciente", 0, decreciente, palabras);
                for (int s = 1; s <= 10; s++) escenario6d(w, t, "aleatorio", s, permutar(creciente, s), palabras);
            }
        }
    }

    private static void escenario6d(PrintWriter w, Indice.TipoTabla t, String escenario, int semilla,
                                    Entrada[] orden, String[] palabras) {
        boolean degenerado = t == Indice.TipoTabla.BST && (escenario.equals("creciente") || escenario.equals("decreciente"));
        int r = degenerado ? repsDegenerado : reps;
        int calentamiento = degenerado ? 0 : CALENTAMIENTO;   // ~100 s por búsqueda: la JVM ya está caliente por "texto"
        double[] tc = new double[r], tb = new double[r];
        TablaOrdenada st = null;
        long compC = 0, moves = 0, rot = 0, flips = 0, compB = 0;
        boolean constantes = true;
        for (int j = -calentamiento; j < r; j++) {
            st = t.nueva();
            st.resetCounters();
            long t0 = System.nanoTime();
            for (Entrada e : orden) st.put(e.palabra(), e.frecuencia());
            long dt = System.nanoTime() - t0;
            long[] cc = {st.compares(), st.moves(), st.rotations(), st.flips()};
            st.resetCounters();
            t0 = System.nanoTime();
            for (String p : palabras) st.get(p);
            long dtB = System.nanoTime() - t0;
            if (j < 0) continue;
            tc[j] = dt / 1e6; tb[j] = dtB / 1e6;
            if (j == 0) { compC = cc[0]; moves = cc[1]; rot = cc[2]; flips = cc[3]; compB = st.compares(); }
            else if (compC != cc[0] || moves != cc[1] || rot != cc[2] || flips != cc[3] || compB != st.compares()) constantes = false;
        }
        String h = "", dBar = "", dBarF = "";
        if (st instanceof ArbolBusqueda a) {
            ArbolBusqueda.DepthStats ds = a.depthStats();
            h = String.valueOf(a.height()); dBar = num(ds.dBar()); dBarF = num(ds.dBarF());
        }
        w.printf(Locale.ROOT, "%s,%s,%d,%d,%d,%.4f,%.4f,%d,%d,%d,%d,%s,%s,%s,%.4f,%.4f,%d,%b%n", t, escenario, semilla, st.size(), r,
                Estadistica.media(tc), Estadistica.desviacion(tc), compC, moves, rot, flips, h, dBar, dBarF,
                Estadistica.media(tb), Estadistica.desviacion(tb), compB, constantes);
        w.flush();
        log("6d %s %s%s listo", t, escenario, semilla > 0 ? " semilla " + semilla : "");
    }

    /** Fisher-Yates con java.util.Random(semilla): permutación reproducible. */
    static Entrada[] permutar(Entrada[] a, long semilla) {
        Entrada[] b = a.clone();
        Random rnd = new Random(semilla);
        for (int i = b.length - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            Entrada tmp = b[i]; b[i] = b[j]; b[j] = tmp;
        }
        return b;
    }

    // ================================================================== 6e: efecto de K y heapify (E1 vs E2)
    static void exp6e(String[] palabras) throws IOException {
        RedBlackBST st = new RedBlackBST();
        Indice.construir(st, palabras, palabras.length);
        Cola<Entrada> fuente = new Cola<>();
        for (Entrada e : st.entries()) fuente.enqueue(e);
        Entrada[] arreglo = new Entrada[fuente.size()];
        int i = 0;
        for (Entrada e : fuente) arreglo[i++] = e;

        try (PrintWriter w = abrir("6e_efectoK.csv")) {
            w.println("cola,K,reps,less,exch,t_ms,t_sd,contadores_constantes");
            for (Indice.TipoCola c : colas) {
                double[] x = aDouble(VALORES_K), yl = new double[VALORES_K.length], yt = new double[VALORES_K.length];
                for (int ki = 0; ki < VALORES_K.length; ki++) {
                    int k = VALORES_K[ki];
                    double[] t = new double[reps];
                    long less = -1, exch = -1;
                    boolean constantes = true;
                    for (int j = -CALENTAMIENTO; j < reps; j++) {
                        ColaPrioridad<Entrada> pq = c.nueva();
                        long t0 = System.nanoTime();
                        Indice.topK(fuente, k, pq);
                        long dt = System.nanoTime() - t0;
                        if (j < 0) continue;
                        t[j] = dt / 1e6;
                        if (less < 0) { less = pq.less(); exch = pq.exch(); }
                        else if (less != pq.less() || exch != pq.exch()) constantes = false;
                    }
                    w.printf(Locale.ROOT, "%s,%d,%d,%d,%d,%.4f,%.4f,%b%n", c, k, reps, less, exch,
                            Estadistica.media(t), Estadistica.desviacion(t), constantes);
                    w.flush();
                    yl[ki] = less; yt[ki] = Estadistica.media(t);
                    log("6e %s K=%d listo", c, k);
                }
                ajuste("6e", String.valueOf(c), "less fase B vs K", x, yl);
                ajuste("6e", String.valueOf(c), "tiempo fase B (ms) vs K", x, yt);
            }
        }

        try (PrintWriter w = abrir("6e_E1_vs_E2.csv")) {
            w.println("K,requerido,E1_less,E1_exch,E1_ms,E1_sd,E2_less,E2_exch,E2_heapify_less,E2_heapify_exch,E2_ms,E2_sd,mismo_resultado,mejor");
            for (int k : VALORES_K_DENSO) {
                if (k > arreglo.length) continue;
                double[] t1 = new double[reps], t2 = new double[reps];
                long l1 = 0, e1 = 0, l2 = 0, e2 = 0, lh = 0, eh = 0;
                boolean igual = true;
                for (int j = -CALENTAMIENTO; j < reps; j++) {
                    MonticuloBinario<Entrada> pq = new MonticuloBinario<>();
                    long t0 = System.nanoTime();
                    Entrada[] r1 = Indice.topK(fuente, k, pq);
                    long d1 = System.nanoTime() - t0;

                    t0 = System.nanoTime();
                    MonticuloBinario<Entrada> max = new MonticuloBinario<>(arreglo, true);
                    long lhj = max.less(), ehj = max.exch();
                    Entrada[] r2 = new Entrada[k];
                    for (int q = 0; q < k; q++) r2[q] = max.delMax();
                    long d2 = System.nanoTime() - t0;
                    if (j < 0) continue;
                    t1[j] = d1 / 1e6; t2[j] = d2 / 1e6;
                    l1 = pq.less(); e1 = pq.exch(); l2 = max.less(); e2 = max.exch(); lh = lhj; eh = ehj;
                    igual &= java.util.Arrays.equals(r1, r2);
                }
                boolean req = false;
                for (int v : VALORES_K) req |= v == k;
                w.printf(Locale.ROOT, "%d,%b,%d,%d,%.4f,%.4f,%d,%d,%d,%d,%.4f,%.4f,%b,%s%n", k, req, l1, e1,
                        Estadistica.media(t1), Estadistica.desviacion(t1), l2, e2, lh, eh,
                        Estadistica.media(t2), Estadistica.desviacion(t2), igual, l1 <= l2 ? "E1" : "E2");
                w.flush();
            }
        }
    }

    // ================================================================== 6f: comparación con la referencia
    static void exp6f(String[] palabras) throws IOException {
        int n = palabras.length;
        double[][] tRef = new double[3][reps];
        IndiceReferencia.Resultado ref = null;
        for (int j = -CALENTAMIENTO; j < reps; j++) {
            ref = IndiceReferencia.ejecutar(palabras, n, K, KP);
            if (j >= 0) { tRef[0][j] = ref.tA / 1e6; tRef[1][j] = ref.tB / 1e6; tRef[2][j] = ref.tC / 1e6; }
        }
        double refTotal = Estadistica.media(tRef[0]) + Estadistica.media(tRef[1]) + Estadistica.media(tRef[2]);
        try (PrintWriter w = abrir("6f_referencia.csv")) {
            w.println("implementacion,reps,tA_ms,tA_sd,tB_ms,tB_sd,tC_ms,tC_sd,total_ms,factorA,factorB,factorC,factor_total,coincide_con_referencia");
            w.printf(Locale.ROOT, "TreeMap+PriorityQueue,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,1,1,1,1,true%n", reps,
                    Estadistica.media(tRef[0]), Estadistica.desviacion(tRef[0]), Estadistica.media(tRef[1]), Estadistica.desviacion(tRef[1]),
                    Estadistica.media(tRef[2]), Estadistica.desviacion(tRef[2]), refTotal);
            for (Indice.TipoTabla t : tablas) for (Indice.TipoCola c : colas) {
                double[][] tt = new double[3][reps];
                Indice.Resultado r = null;
                for (int j = -CALENTAMIENTO; j < reps; j++) {
                    r = Indice.ejecutar(palabras, n, t, c, K, KP);
                    if (j >= 0) { tt[0][j] = r.tA / 1e6; tt[1][j] = r.tB / 1e6; tt[2][j] = r.tC / 1e6; }
                }
                double a = Estadistica.media(tt[0]), b = Estadistica.media(tt[1]), cc = Estadistica.media(tt[2]);
                w.printf(Locale.ROOT, "%s+%s,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.3f,%.3f,%.3f,%.3f,%b%n", t, c, reps,
                        a, Estadistica.desviacion(tt[0]), b, Estadistica.desviacion(tt[1]), cc, Estadistica.desviacion(tt[2]),
                        a + b + cc, a / Estadistica.media(tRef[0]), b / Estadistica.media(tRef[1]), cc / Estadistica.media(tRef[2]),
                        (a + b + cc) / refTotal, coinciden(r, ref));
                w.flush();
                log("6f %s+%s listo", t, c);
            }
        }
    }

    /** Misma tabla (llaves y frecuencias), mismo top-K, mismos prefijos y mismas sugerencias. */
    static boolean coinciden(Indice.Resultado r, IndiceReferencia.Resultado ref) {
        if (r.m != ref.m || r.q != ref.q) return false;
        java.util.Iterator<java.util.Map.Entry<String, Integer>> it = ref.tabla.entrySet().iterator();
        for (Entrada e : r.tabla.entries()) {
            java.util.Map.Entry<String, Integer> x = it.next();
            if (!e.palabra().equals(x.getKey()) || e.frecuencia() != x.getValue()) return false;
        }
        if (!java.util.Arrays.equals(r.top, ref.top) || !java.util.Arrays.equals(r.prefijos, ref.prefijos)) return false;
        for (int i = 0; i < r.q; i++) if (!java.util.Arrays.equals(r.autocompletado[i], ref.autocompletado[i])) return false;
        return true;
    }

    // ================================================================== utilidades
    private static int[] conCompleto(int total) {
        int[] ns = java.util.Arrays.copyOf(TAMANOS, TAMANOS.length + 1);
        ns[TAMANOS.length] = total;
        return ns;
    }

    private static double[] aDouble(int[] v) {
        double[] r = new double[v.length];
        for (int i = 0; i < v.length; i++) r[i] = v[i];
        return r;
    }

    private static void ajuste(String inciso, String serie, String variable, double[] x, double[] y) {
        for (double v : y) if (v <= 0) return;   // el ajuste log-log exige valores positivos
        double[] f = Estadistica.ajustePotencia(x, y);
        if (ajustes == null) {
            try { ajustes = abrir("ajustes_" + incisoActual + ".csv"); }
            catch (IOException e) { throw new java.io.UncheckedIOException(e); }
            ajustes.println("inciso,serie,variable,a,b,R2");
        }
        ajustes.printf(Locale.ROOT, "%s,\"%s\",\"%s\",%.6g,%.6f,%.6f%n", inciso, serie, variable, f[0], f[1], f[2]);
        ajustes.flush();
    }

    private static String num(double v) { return Double.isNaN(v) ? "" : String.format(Locale.ROOT, "%.4f", v); }

    private static PrintWriter abrir(String nombre) throws IOException {
        return new PrintWriter(Files.newBufferedWriter(salida.resolve(nombre), StandardCharsets.UTF_8));
    }

    private static void log(String fmt, Object... args) { System.err.printf(Locale.ROOT, fmt + "%n", args); }
}
