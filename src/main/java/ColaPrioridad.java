/**
 * API común de cola de prioridad de MÍNIMOS, genérica sobre elementos comparables.
 * Las tres implementaciones (MinPQDesordenada, MinPQOrdenada, MonticuloBinario) son intercambiables.
 *
 * Contrato común:
 *  - insert(x): x != null (si no, IllegalArgumentException). Se admiten repetidos.
 *  - min() y delMin() sobre una cola vacía lanzan NoSuchElementException (comportamiento documentado).
 *  - n llamadas sucesivas a delMin devuelven los n elementos en orden creciente.
 *
 * Instrumentación (igual que en el Taller 3):
 *  - less: cada llamada al comparador (compareTo) entre dos elementos cuenta 1.
 *  - exch: cada intercambio de dos posiciones del arreglo cuenta 1.
 */
public interface ColaPrioridad<T extends Comparable<? super T>> {
    void insert(T x);
    T min();
    T delMin();
    int size();
    boolean isEmpty();

    long less();
    long exch();
    void resetCounters();
}
