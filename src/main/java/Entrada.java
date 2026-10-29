/**
 * Par inmutable (palabra, frecuencia), comparable por RELEVANCIA.
 *
 * Una entrada es MAYOR (más relevante) que otra si tiene mayor frecuencia o, a igual frecuencia,
 * si su palabra es MENOR en el orden ordinal de las llaves. Es un orden total, así que el top-K
 * es único. Con una MinPQ, la raíz es siempre la entrada MENOS relevante.
 *
 * Contrato:
 *  - Precondiciones: palabra != null; frecuencia >= 0.
 *  - Postcondiciones: compareTo es antisimétrico y transitivo; compareTo == 0 solo si las dos
 *    entradas tienen la misma palabra y la misma frecuencia.
 *  - Invariante: los campos nunca cambian después de construir el objeto.
 */
public final class Entrada implements Comparable<Entrada> {
    private final String palabra;
    private final int frecuencia;

    public Entrada(String palabra, int frecuencia) {
        if (palabra == null) throw new IllegalArgumentException("palabra null");
        if (frecuencia < 0) throw new IllegalArgumentException("frecuencia negativa");
        this.palabra = palabra;
        this.frecuencia = frecuencia;
    }

    public String palabra() { return palabra; }
    public int frecuencia() { return frecuencia; }

    @Override
    public int compareTo(Entrada otra) {
        if (frecuencia != otra.frecuencia) return Integer.compare(frecuencia, otra.frecuencia);
        // String.compareTo en Java es ordinal (por unidad de código), no depende de la cultura.
        // Invertido: a igual frecuencia, la palabra menor es la más relevante (la "mayor" entrada).
        return otra.palabra.compareTo(palabra);
    }

    @Override public boolean equals(Object o) {
        return o instanceof Entrada e && e.frecuencia == frecuencia && e.palabra.equals(palabra);
    }
    @Override public int hashCode() { return 31 * palabra.hashCode() + frecuencia; }
    @Override public String toString() { return "(" + palabra + ", " + frecuencia + ")"; }
}