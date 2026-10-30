import java.io.IOException;
import java.util.Arrays;

/*
 * Pruebas del programa completo (punto 5) contra los VALORES DE VERIFICACIÓN del enunciado, para las
 * 9 combinaciones, más la coincidencia con la implementación de referencia (6f) y la estabilidad de los
 * contadores entre repeticiones. Requiere pg2000.txt en el directorio de trabajo (o su ruta como argumento).
 * Ejecutar con: java -ea PruebasIndice [ruta]  (o ./gradlew test).
 * */
public class PruebasIndice {

    public static void main(String[] args) throws IOException {
        String ruta = args.length > 0 ? args[0] : "pg2000.txt";
        String[] palabras = Tokenizador.tokenizarArchivo(ruta);
        assert palabras.length == 383_637 : "N = " + palabras.length;

        Entrada[] top10 = {e("que", 20769), e("de", 18410), e("y", 18272), e("la", 10492), e("a", 9876),
                e("en", 8285), e("el", 8267), e("no", 6346), e("los", 4769), e("se", 4752)};
        Entrada[] dul = {e("dulcinea", 286), e("dulce", 26), e("dulces", 5), e("dulcísima", 4), e("dulcineas", 2)};
        Entrada[] san = {e("sancho", 2174), e("sansón", 77), e("sangre", 65), e("santa", 49), e("san", 32)};

        IndiceReferencia.Resultado ref = IndiceReferencia.ejecutar(palabras, palabras.length, 10, 5);
        for (Indice.TipoTabla t : Indice.TipoTabla.values()) {
            for (Indice.TipoCola c : Indice.TipoCola.values()) {
                Indice.Resultado r = Indice.ejecutar(palabras, palabras.length, t, c, 10, 5);
                TablaOrdenada st = r.tabla;
                String id = t + "+" + c;
                assert r.m == 22_944 : id + " M = " + r.m;
                assert r.q == 295 : id + " Q = " + r.q;
                assert Arrays.equals(r.top, top10) : id + " top-10";
                assert Arrays.equals(Indice.autocompletar(st, "dul", 5, c.nueva()), dul) : id + " dul";
                assert Arrays.equals(Indice.autocompletar(st, "san", 5, c.nueva()), san) : id + " san";
                assert PruebasTablas.contar(st.keys("dul", "dul" + Indice.SIGMA)) == 12 : id + " R(dul)";
                assert PruebasTablas.contar(st.keys("san", "san" + Indice.SIGMA)) == 53 : id + " R(san)";
                assert st.rank("quijote") == 17_881 && st.min().equals("a") && st.max().equals("útiles") : id;
                PruebasTablas.invariantes(st);
                assert Experimentos.coinciden(r, ref) : id + " difiere de TreeMap + PriorityQueue";
                // Los contadores son deterministas: idénticos en otra repetición
                assert r.firmaContadores().equals(Indice.ejecutar(palabras, palabras.length, t, c, 10, 5).firmaContadores()) : id;
            }
        }
        // Los rangos de los Q prefijos son disjuntos y cubren todo el vocabulario salvo las palabras de una letra
        TablaOrdenada st = Indice.TipoTabla.RED_BLACK_BST.nueva();
        Indice.construir(st, palabras, palabras.length);
        int cubiertas = 0, unaLetra = 0;
        for (String p : Indice.prefijos(st)) cubiertas += PruebasTablas.contar(st.keys(p, p + Indice.SIGMA));
        for (String w : st.keys()) if (w.length() == 1) unaLetra++;
        assert cubiertas + unaLetra == st.size() : cubiertas + " + " + unaLetra + " != " + st.size();

        // Fase A sobre un prefijo del texto (N pequeño)
        Indice.Resultado chico = Indice.ejecutar(palabras, 6_250, Indice.TipoTabla.BST, Indice.TipoCola.MONTICULO, 10, 5);
        assert chico.n == 6_250 && chico.m < 6_250 && chico.m > 0;
        System.out.println("Pruebas del índice (valores de verificación, 9 combinaciones y referencia): OK");
    }

    static Entrada e(String w, int f) { return new Entrada(w, f); }
}
