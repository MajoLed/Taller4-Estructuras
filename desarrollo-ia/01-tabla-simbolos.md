# Tablas de símbolos ordenadas (BinarySearchST, BST, RedBlackBST)

## 1. Entender y descomponer (sin IA)

> **Pendiente de revisión por el equipo**: el enunciado pide que esta sección se haga sin IA.
> Lo siguiente es la descomposición que guió la implementación; el equipo debe reescribirla con sus
> palabras y dejar explícita su hipótesis para la tabla asignada.

Se necesita una tabla `palabra → frecuencia` que además responda consultas **por orden**
(`rank`, `select`, `floor`, `ceiling`, `keys(lo, hi)`), porque el autocompletado es un rango
contiguo de llaves. Subproblemas: (1) búsqueda e inserción; (2) operaciones ordenadas; (3) recorrido
de rango que entregue también el valor; (4) instrumentación; (5) verificadores de invariantes;
(6) operar sin desbordar la pila en árboles degenerados.

## 2. PRD

### Requerimientos
- Entradas: llaves `String` no nulas (orden ordinal), valores `int` (frecuencias).
- Salidas: `Integer` (o `null`) en `get`; `String` en `min/max/floor/ceiling/select`; `int` en `rank`;
  `Iterable<String>` en `keys`; `Iterable<Entrada>` en `entries`; `long` en los contadores;
  `int height()` y `DepthStats depthStats()` en los árboles.

### Contrato
- Precondiciones: ninguna llave `null` (`IllegalArgumentException`); `0 <= k < size()` en `select`.
- Postcondiciones: `put` de una llave existente actualiza el valor sin cambiar `size()`; `rank(k)` =
  número de llaves < k; `keys()`/`entries()` en orden creciente; `keys(lo, hi)` incluye los extremos y
  es vacío si `lo > hi`; `min`/`max` en tabla vacía lanzan `NoSuchElementException`; `floor`/`ceiling`
  devuelven `null` si no existe la llave.
- Invariantes: BinarySearchST: `keys[0..n-1]` estrictamente creciente, `vals[i]` es el valor de
  `keys[i]`. Árboles: orden simétrico; `x.size = 1 + size(x.left) + size(x.right)`. RedBlackBST: sin
  enlaces rojos a la derecha, sin dos rojos seguidos, raíz negra, igual número de enlaces negros en
  todo camino raíz–null.

### Restricciones de implementación
- Complejidad temporal: BinarySearchST `get` O(lg M), `put` O(M) por los corrimientos; BST O(h) con
  h hasta M − 1; RedBlackBST O(lg M) garantizado; `keys(lo, hi)` O(h + R) en los árboles y
  O(lg M + R) en el arreglo.
- Complejidad espacial: O(M).
- Buenas prácticas: API única, un solo método `compare` por estructura, verificadores que no tocan los
  contadores, operaciones iterativas en los árboles (6d).
- Prohibiciones: `TreeMap`, `HashMap`, colecciones y ordenamientos de biblioteca en la lógica (solo se
  usan en `IndiceReferencia` y en las pruebas como oráculo). Sin `delete`.

### Criterios de aceptación
- Caso típico: el texto completo produce M = 22 944, `rank("quijote") = 17 881`, `min() = a`,
  `max() = útiles` en las tres.
- Bordes: vacía, un elemento, actualización, `floor`/`ceiling` presentes/ausentes/fuera de extremos,
  `rank`/`select` en los extremos, `keys(lo, hi)` vacío/un elemento/`lo > hi`, inserción ordenada.
- Error: llave `null`, `select` fuera de rango, `min` en vacía.
- Contadores: BST `m c t a e` = 6 comparaciones.

## 3. Prompt(s) utilizado(s)

Ver [prompts.md](prompts.md) (prompt 1).

## 4. Registro de validación crítica

- **Casos de aceptación** (`PruebasIndice`): las tres tablas reproducen M, `rank("quijote")`, `min`,
  `max`, y las respuestas de top-K y autocompletado — **OK**. Coinciden llave por llave y valor por
  valor con `TreeMap` — **OK**.
- **Casos de borde** (`PruebasTablas`): todos los listados en el PRD — **OK** en las tres.
- **Equivalencia**: 20 000 `put` aleatorios con `ñ`, tildes y `ü`: misma secuencia `entries()`,
  mismos `min`, `max`, `size`, `rank`, `floor`, `ceiling` y conteo de rango — **OK**.
- **Contadores a mano** (detalle en [docs/PUNTOS-CLAVE.md](../docs/PUNTOS-CLAVE.md#contadores-verificados-a-mano)):
  BST `m c t a e` = 6; BinarySearchST `m c t a e` = 7 comparaciones y 6 moves; RedBlackBST
  `m c t a e` = 6 comparaciones, 0 rotaciones, 2 flips; `a b c` = 2, 1, 1; BST en orden creciente
  con n llaves = n(n − 1)/2; BinarySearchST en orden decreciente = n(n − 1)/2 moves — **OK**.
- **Estabilidad**: contadores idénticos entre repeticiones (`PruebasIndice` y la columna
  `contadores_constantes` de los CSV).
- **Invariantes**: `isBST`, `isSizeConsistent`, `isRankConsistent` (+ `is23`, `isBalanced`) después de
  cada escenario de prueba y sobre el índice del texto completo — **OK**; se comprueba además que no
  alteran `compares`.
- **Complejidad deducida del código** (*el equipo debe rehacerla para la tabla asignada*):
  - BinarySearchST: `search` hace a lo sumo ⌊lg n⌋ + 1 iteraciones, una comparación cada una; `put`
    nuevo en la posición i corre n − i pares (bucle de `BinarySearchST.java:52`).
  - BST: `get`/`put` comparan una vez por nivel descendido: d(w) + 1 para una llave a profundidad d(w);
    `put` además recorre el camino otra vez para `size` (sin comparaciones).
  - RedBlackBST: igual que el BST en comparaciones, más O(1) rotaciones/flips por nivel al regresar
    de la recursión.
  - `keys(lo, hi)`: 1 comparación (`lo > hi`) + 1 por nodo bajado en `apilarIzquierda` + 1 por nodo
    desapilado.

## 5. Problemas encontrados y correcciones

| Problema | Origen | Corrección |
|---|---|---|
| `RedBlackBST.java` contenía en realidad el código de `BinarySearchST`; `BinarySearchST.java` y `TablaOrdenada.java` eran clases vacías; `BST` heredaba de una clase `ArbolBusqueda` y usaba `Pila`, `Cola`, `Node` que no existían (no compilaba). | Avance previo incompleto | Se movió el código a `BinarySearchST.java`, se escribieron `TablaOrdenada`, `ArbolBusqueda`, `Pila`, `Cola` y un `RedBlackBST` LLRB real. |
| `floor`, `ceiling`, `rank`, `keys(lo, hi)` de BinarySearchST aceptaban `null` y fallaban con `NullPointerException`. | Omisión en el código original | Validación explícita (`IllegalArgumentException`), igual que en los árboles. |
| El enunciado pide recorrer `keys()` y luego usar `f(w)`; con `get` se repetirían M búsquedas. | Ambigüedad del PRD | Se agregó `entries()` a la API: mismo recorrido y mismo orden, entrega el par. Los contadores de la cola no cambian; `compares` de la tabla en la fase B es 0. |
| Un `put` recursivo del BST desbordaría la pila en el escenario creciente de 6d (altura 22 943). | Restricción conocida del enunciado | Todas las operaciones del BST son iterativas (`Pila`, `Cola`). El `put` del rojo-negro sigue recursivo (altura ≤ 2 lg M). |
| σ quedó escrito como el carácter literal U+FFFF (invisible) en el fuente. | Error de la IA al generar el archivo | Se reemplazó por el escape `'￿'`. |

## 6. Reflexión final (prueba de propiedad)

> **Pendiente — debe escribirla el equipo** (3–5 líneas): qué se aprendió, qué se haría distinto y si
> podrían reimplementar sin ayuda la tabla asignada (por ejemplo, explicar sin mirar el código por qué
> el BST no incrementa `size` al actualizar, o cómo poda `keys(lo, hi)`).
