import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Random;

/*
 * Pruebas de las tres colas de prioridad (punto 3), con assert.
 * Ejecutar con: java -ea PruebasColas  (o ./gradlew test).
 * */
public class PruebasColas {

    public static void main(String[] args) {
        for (Indice.TipoCola c : Indice.TipoCola.values()) {
            vacia(c);
            repetidos(c);
            ordenes(c);
            genericidad(c);
        }
        contadoresAMano();
        heapify();
        montículoDeMaximos();
        System.out.println("Pruebas de colas de prioridad: OK");
    }

    /** delMin/min sobre cola vacía: NoSuchElementException (comportamiento documentado en ColaPrioridad). */
    static void vacia(Indice.TipoCola c) {
        ColaPrioridad<Integer> pq = c.nueva();
        assert pq.isEmpty() && pq.size() == 0;
        try { pq.delMin(); assert false; } catch (NoSuchElementException ok) { }
        try { pq.min(); assert false; } catch (NoSuchElementException ok) { }
        try { pq.insert(null); assert false; } catch (IllegalArgumentException ok) { }
        pq.insert(1);
        assert pq.delMin() == 1;
        try { pq.delMin(); assert false; } catch (NoSuchElementException ok) { }
    }

    static void repetidos(Indice.TipoCola c) {
        ColaPrioridad<Integer> pq = c.nueva();
        for (int x : new int[]{5, 3, 5, 1, 3, 3}) pq.insert(x);
        assert pq.size() == 6 && pq.min() == 1;
        int[] r = new int[6];
        for (int i = 0; i < 6; i++) r[i] = pq.delMin();
        assert Arrays.equals(r, new int[]{1, 3, 3, 3, 5, 5});
        assert pq.isEmpty();
    }

    /** Creciente, decreciente y aleatorio: n delMin devuelven los n elementos en orden creciente. */
    static void ordenes(Indice.TipoCola c) {
        int n = 2_000;
        int[] creciente = new int[n], decreciente = new int[n], aleatorio = new int[n];
        Random rnd = new Random(7);
        for (int i = 0; i < n; i++) { creciente[i] = i; decreciente[i] = n - i; aleatorio[i] = rnd.nextInt(500); }
        for (int[] datos : new int[][]{creciente, decreciente, aleatorio}) {
            ColaPrioridad<Integer> pq = c.nueva();
            for (int x : datos) pq.insert(x);
            if (pq instanceof MonticuloBinario<Integer> h) assert h.isMinHeap();
            if (pq instanceof MinPQOrdenada<Integer> o) assert o.isDecreciente();
            int[] esperado = datos.clone();
            Arrays.sort(esperado);   // solo en la prueba, como oráculo
            for (int i = 0; i < n; i++) assert pq.delMin() == esperado[i];
            assert pq.isEmpty();
        }
    }

    /** Genérica: funciona con String y con Entrada (la cima es la entrada MENOS relevante). */
    static void genericidad(Indice.TipoCola c) {
        ColaPrioridad<String> ps = c.nueva();
        for (String s : new String[]{"ñu", "zz", "amor", "útil"}) ps.insert(s);
        assert ps.delMin().equals("amor") && ps.delMin().equals("zz") && ps.delMin().equals("ñu");

        ColaPrioridad<Entrada> pe = c.nueva();
        pe.insert(new Entrada("que", 20769));
        pe.insert(new Entrada("zorro", 5));
        pe.insert(new Entrada("amor", 5));
        assert pe.delMin().palabra().equals("zorro");   // empate en 5: "zorro" es menos relevante que "amor"
        assert pe.delMin().palabra().equals("amor");

        // Esquema de cola acotada (fases B y C)
        Cola<Entrada> fuente = new Cola<>();
        String[] ws = {"a", "b", "c", "d", "e", "f"};
        int[] fs = {3, 9, 3, 1, 9, 4};
        for (int i = 0; i < ws.length; i++) fuente.enqueue(new Entrada(ws[i], fs[i]));
        Entrada[] top = Indice.topK(fuente, 3, c.nueva());
        assert Arrays.equals(top, new Entrada[]{new Entrada("b", 9), new Entrada("e", 9), new Entrada("f", 4)});
        assert Indice.topK(fuente, 100, c.nueva()).length == 6 : "si hay menos de K entradas se devuelven todas";
    }

    static void contadoresAMano() {
        // Arreglo desordenado con 5 elementos: el primer delMin hace exactamente 4 comparaciones y 1 intercambio
        MinPQDesordenada<Integer> d = new MinPQDesordenada<>();
        for (int x : new int[]{4, 2, 5, 1, 3}) d.insert(x);
        assert d.less() == 0 && d.exch() == 0 : "insert no compara";
        assert d.delMin() == 1;
        assert d.less() == 4 && d.exch() == 1;
        d.resetCounters();
        assert d.less() == 0 && d.exch() == 0;

        // Arreglo ordenado (decreciente): insertar 1..5 en orden creciente -> cada uno viaja al frente
        // less = 0+1+2+3+4 = 10, exch = 10 ; en orden decreciente 5..1 -> less = 4, exch = 0 ; delMin: 0 y 0
        MinPQOrdenada<Integer> o = new MinPQOrdenada<>();
        for (int x = 1; x <= 5; x++) o.insert(x);
        assert o.less() == 10 && o.exch() == 10;
        MinPQOrdenada<Integer> o2 = new MinPQOrdenada<>();
        for (int x = 5; x >= 1; x--) o2.insert(x);
        assert o2.less() == 4 && o2.exch() == 0;
        o2.delMin();
        assert o2.less() == 4 && o2.exch() == 0 : "delMin es O(1) sin comparaciones";

        // Montículo: insert 3, 1, 2 -> less 2, exch 1 ; delMin -> +1 exch (cima con último) +1 less (sink)
        MonticuloBinario<Integer> h = new MonticuloBinario<>();
        h.insert(3); h.insert(1); h.insert(2);
        assert h.less() == 2 && h.exch() == 1;
        assert h.delMin() == 1;
        assert h.less() == 3 && h.exch() == 2;
    }

    static void heapify() {
        // A mano: heapify de mínimos sobre [5, 4, 3, 2, 1] -> 6 less y 3 exch (< 2n = 10 y < n = 5)
        Integer[] a = {5, 4, 3, 2, 1};
        MonticuloBinario<Integer> h = new MonticuloBinario<>(a, false);
        assert h.isMinHeap() && h.size() == 5;
        assert h.less() == 6 && h.exch() == 3 : h.less() + " " + h.exch();
        assert Arrays.equals(a, new Integer[]{5, 4, 3, 2, 1}) : "el arreglo de entrada no se modifica";
        for (int x = 1; x <= 5; x++) assert h.delMin() == x;

        // Cotas de la proposición R (menos de 2n less y menos de n exch) en muchos tamaños y órdenes
        Random rnd = new Random(1);
        for (int n = 1; n <= 3_000; n += 37) {
            Integer[] v = new Integer[n];
            for (int i = 0; i < n; i++) v[i] = rnd.nextInt(n);
            for (boolean max : new boolean[]{false, true}) {
                MonticuloBinario<Integer> m = new MonticuloBinario<>(v, max);
                assert m.isHeap() && m.size() == n;
                assert m.less() < 2L * n && m.exch() < n : "n=" + n + " less=" + m.less() + " exch=" + m.exch();
            }
            Integer[] asc = new Integer[n];
            for (int i = 0; i < n; i++) asc[i] = i;
            MonticuloBinario<Integer> peor = new MonticuloBinario<>(asc, true);   // creciente para uno de máximos: peor caso
            assert peor.isMaxHeap() && peor.less() < 2L * n && peor.exch() < n;
        }
        // heapify vacío
        MonticuloBinario<Integer> vacio = new MonticuloBinario<>(new Integer[0], false);
        assert vacio.isEmpty() && vacio.isMinHeap() && vacio.less() == 0;
    }

    static void montículoDeMaximos() {
        MonticuloBinario<Integer> h = MonticuloBinario.deMaximos();
        for (int x : new int[]{3, 9, 1, 9, 4}) h.insert(x);
        assert h.isMaxHeap() && !h.isMinHeap() && h.max() == 9;
        int[] r = new int[5];
        for (int i = 0; i < 5; i++) r[i] = h.delMax();
        assert Arrays.equals(r, new int[]{9, 9, 4, 3, 1});
        try { h.delMin(); assert false; } catch (IllegalStateException ok) { }
        MonticuloBinario<Integer> m = MonticuloBinario.deMinimos();
        try { m.delMax(); assert false; } catch (IllegalStateException ok) { }

        // E2 (heapify de máximos + K delMax) da el mismo top-K que E1 (montículo de mínimos acotado)
        Entrada[] es = new Entrada[200];
        Random rnd = new Random(3);
        Cola<Entrada> fuente = new Cola<>();
        for (int i = 0; i < es.length; i++) { es[i] = new Entrada("w" + i, rnd.nextInt(20)); fuente.enqueue(es[i]); }
        Entrada[] e1 = Indice.topK(fuente, 15, new MonticuloBinario<>());
        MonticuloBinario<Entrada> mx = new MonticuloBinario<>(es, true);
        Entrada[] e2 = new Entrada[15];
        for (int i = 0; i < 15; i++) e2[i] = mx.delMax();
        assert Arrays.equals(e1, e2);
    }
}
