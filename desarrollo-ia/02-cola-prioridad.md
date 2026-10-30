# Colas de prioridad (arreglo desordenado, arreglo ordenado, montículo binario)

## 1. Entender y descomponer (sin IA)

> **Pendiente de revisión por el equipo** (esta sección se hace sin IA según el enunciado).

Se necesita una MinPQ genérica para el esquema de cola acotada: mantener las K entradas más
relevantes descartando siempre la menos relevante (la cima de una MinPQ). Subproblemas: (1) las tres
representaciones con la misma API; (2) instrumentación less/exch; (3) heapify lineal y orientación de
máximos para la estrategia E2; (4) comportamiento definido con cola vacía.

## 2. PRD

### Requerimientos
- Entradas: elementos `T extends Comparable<? super T>` no nulos (en el programa, `Entrada`).
- Salidas: `T` en `min`/`delMin` (y `max`/`delMax` en el montículo de máximos); `int size()`;
  `long less()`, `long exch()`.

### Contrato
- Precondiciones: `insert(x)` con `x != null` (`IllegalArgumentException`).
- Postcondiciones: n `delMin` sucesivos devuelven los n elementos en orden creciente; `min`/`delMin`
  en cola vacía lanzan `NoSuchElementException`; el constructor por heapify no modifica el arreglo de
  entrada.
- Invariantes: desordenada — elementos vivos en `a[0..n-1]`; ordenada — `a[0] >= … >= a[n-1]`;
  montículo — `pq[k/2]` tiene prioridad ≥ `pq[k]` para `2 <= k <= n`.

### Restricciones de implementación
- Complejidad: desordenada insert O(1), delMin O(n); ordenada insert O(n), delMin O(1); montículo
  insert/delMin O(lg n), heapify O(n) (< 2n less, < n exch).
- Espacio: O(n), arreglos redimensionables.
- Buenas prácticas: genéricas (no especializadas en `Entrada`); una única función de comparación por
  estructura que incrementa `less`.
- Prohibiciones: `PriorityQueue`, `TreeMap`, ordenamientos de biblioteca (solo en las pruebas como
  oráculo y en `IndiceReferencia`).

### Criterios de aceptación
- Típico: top-10 del texto completo idéntico con las tres colas.
- Bordes: vacía, un elemento, repetidos, orden creciente y decreciente, K mayor que el número de
  entradas.
- Error: `null`, `delMin` en vacía, `delMin` en un montículo de máximos.
- Contadores: desordenada con 5 elementos, primer `delMin` = 4 less.

## 3. Prompt(s) utilizado(s)

Ver [prompts.md](prompts.md) (prompt 1).

## 4. Registro de validación crítica

- **Casos de aceptación**: top-10, autocompletado de `dul` y `san` idénticos con las tres colas y con
  `PriorityQueue` (`PruebasIndice`) — **OK**.
- **Casos de borde** (`PruebasColas`): vacía, `null`, repetidos, creciente/decreciente/aleatorio con
  2 000 elementos, `String` y `Entrada`, empates de relevancia, K > entradas — **OK**.
- **Heapify**: `isHeap` y cotas < 2n less y < n exch para n = 1 … 3 000 (paso 37), orientaciones de
  mínimos y máximos, peor caso (creciente para máximos) y arreglo vacío — **OK**. E2 = E1 en el top-K.
- **Contadores a mano** (ver [docs/PUNTOS-CLAVE.md](../docs/PUNTOS-CLAVE.md#contadores-verificados-a-mano)):
  desordenada 4 less / 1 exch; ordenada 10/10 (creciente) y 4/0 (decreciente); montículo
  `insert 3,1,2` = 2/1 y `delMin` = +1/+1; heapify `[5,4,3,2,1]` = 6/3 — **OK**.
- **Contraste con el texto completo** (fase B, K = 10, arreglo desordenado): `less = 229 385 =
  (M − 10)·10 + 45`, `exch = 22 944 = (M − 10) + 10` — coincide con el modelo.
- **Complejidad deducida del código** (*el equipo debe rehacerla para la cola asignada*):
  - Desordenada: `indiceMin` hace exactamente n − 1 llamadas a `less` (`MinPQDesordenada.java:38`),
    **independiente del orden** de los datos.
  - Ordenada: el bucle de `MinPQOrdenada.java:33` hace una comparación por posición recorrida más la
    que lo detiene (salvo al llegar a la posición 0).
  - Montículo: `swim` ≤ lg n comparaciones; `sink` ≤ 2 por nivel.

## 5. Problemas encontrados y correcciones

| Problema | Origen | Corrección |
|---|---|---|
| ¿Cuenta `exch` el intercambio del mínimo con el último cuando ya es el último? | Ambigüedad del PRD | Se cuenta siempre (1 exch por `delMin`): el código siempre ejecuta el intercambio. Documentado en la clase. |
| Llamar `delMin` sobre un montículo de máximos devolvería el máximo en silencio. | Riesgo de diseño | `min`/`delMin` y `max`/`delMax` exigen la orientación correcta (`IllegalStateException`); `top`/`delTop` son neutrales. |
| Avisos `rawtypes` por `new Comparable[n]`. | Detalle de la IA | `new Comparable<?>[n]` con conversión no comprobada documentada. |

## 6. Reflexión final (prueba de propiedad)

> **Pendiente — debe escribirla el equipo** (3–5 líneas).
