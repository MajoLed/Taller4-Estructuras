# Puntos clave del código

Para cada objetivo del taller, **dónde** está en el código la línea (o bloque) que lo cumple y
**cómo** se comprueba. Las rutas son relativas a la raíz del repositorio.

## Punto 1 — Tokenización y ADT Entrada

| Objetivo | Código | Por qué lo cumple | Prueba |
|---|---|---|---|
| Solo el texto entre `*** START OF` y `*** END OF` (excluidas) | `src/main/java/Tokenizador.java:29` | Busca la primera línea con `START OF` y la siguiente con `END OF`; concatena las líneas estrictamente entre ellas. Si falta una marca lanza excepción (no tokeniza en silencio un archivo equivocado). | `PruebasIndice`: N = 383 637 |
| Minúsculas + `[a-záéíóúüñ]+` | `src/main/java/Tokenizador.java:24`, `:45` | `toLowerCase(Locale.ROOT)` (independiente del idioma del sistema) y `Matcher.find()` sobre secuencias maximales. | `PruebasTokenizador` (`"¿Qué? Señor, 2 pingüinos..."` → `qué, señor, pingüinos`; `ç`, `à`, dígitos, guiones y comillas como separadores) |
| Relevancia como orden total | `src/main/java/Entrada.java:29-33` | Frecuencia primero; a igual frecuencia `otra.palabra.compareTo(palabra)` (invertido: palabra menor = más relevante). | `PruebasTokenizador`: empates, antisimetría, `ñ`/tildes después de `z` |
| Inmutable | `src/main/java/Entrada.java:14` | Clase `final`, campos `private final`, sin setters; `equals`/`hashCode` coherentes con `compareTo`. | — |

## Punto 2 — Tablas de símbolos ordenadas

| Objetivo | Código | Por qué lo cumple |
|---|---|---|
| API común intercambiable | `src/main/java/TablaOrdenada.java` | Las tres clases la implementan; `Indice.TipoTabla.nueva()` las crea por nombre. |
| compares de tres vías contado **una vez** | `src/main/java/ArbolBusqueda.java:40`, `src/main/java/BinarySearchST.java:24` | Único punto de comparación de llaves de cada estructura. |
| Búsqueda binaria + punto de inserción | `src/main/java/BinarySearchST.java:28` | Devuelve `i` o `-(ins) - 1`; `get`, `put`, `rank`, `floor`, `ceiling` y `keys(lo,hi)` la reutilizan. |
| moves | `src/main/java/BinarySearchST.java:52` | Un `moves++` por par corrido. |
| BST: `size` correcto al actualizar | `src/main/java/BST.java:20`, `:30` | Si la llave existe retorna **antes** de tocar `size`; si es nueva, incrementa `size` en todo el camino guardado. |
| LLRB con rotaciones, flips y `size` | `src/main/java/RedBlackBST.java:24-36`, `:38`, `:62` | Las tres correcciones locales tras cada inserción; las rotaciones recalculan `size`. |
| `rank`/`select` en O(h) | `src/main/java/ArbolBusqueda.java:105`, `:118` | Usan `size` del subárbol izquierdo. |
| `keys(lo, hi)` solo recorre lo necesario y entrega valores | `src/main/java/ArbolBusqueda.java:173`, `:188`; `src/main/java/BinarySearchST.java:113` | Recorrido en orden podado O(h + R) / tramo `[rank(lo), …]` del arreglo; `entries(lo, hi)` entrega `Entrada` sin repetir búsquedas. |
| `height()` y `depthStats()` (d̄, d̄_f) | `src/main/java/ArbolBusqueda.java:199`, `:219` | Por niveles, iterativo; d̄_f pondera cada nodo por su frecuencia. |
| Verificadores | `ArbolBusqueda.java:250` (isBST), `:265` (isSizeConsistent), `:278` (isRankConsistent); `RedBlackBST.java:71` (is23), `:84` (isBalanced); `BinarySearchST.java:143` | No alteran contadores. |
| Sin recursión en el BST (6d) | `src/main/java/ArbolBusqueda.java` completo, `src/main/java/Pila.java` | Todo con `Pila`/`Cola` propias. `PruebasTablas.insercionOrdenada` construye un BST de altura 4 999 sin desbordar. |

**Pruebas** (`src/test/java/PruebasTablas.java`): vacía, un elemento, actualización (el tamaño no
cambia), `floor`/`ceiling` de presentes, ausentes, < min y > max, `rank`/`select` en extremos,
`keys(lo, hi)` vacío / un elemento / `lo > hi`, rango de prefijo con σ frente a `'~'`, equivalencia
de las tres implementaciones con 20 000 `put` aleatorios (con `ñ`, tildes y `ü`), invariantes después
de cada escenario, y contadores a mano.

## Punto 3 — Colas de prioridad

| Objetivo | Código | Por qué lo cumple |
|---|---|---|
| Genéricas e intercambiables | `src/main/java/ColaPrioridad.java` | `T extends Comparable<? super T>`; pruebas con `Integer`, `String`, `Entrada`. |
| Desordenada: delMin = buscar, intercambiar con el último, retirar | `src/main/java/MinPQDesordenada.java:38`, `:46` | n − 1 `less` exactos y 1 `exch`. |
| Ordenada decreciente, inserción por intercambios | `src/main/java/MinPQOrdenada.java:33` | Bucle de inserción; `delMin` O(1). |
| Montículo `swim`/`sink` | `src/main/java/MonticuloBinario.java:61`, `:65` | Una función `prioridadMenor` (`:53`) cuenta cada comparación y decide la orientación. |
| Heapify lineal | `src/main/java/MonticuloBinario.java:36-44` | Copia + `sink` desde ⌊n/2⌋ hasta 1, sin inserciones. |
| Montículo de máximos (6e) | `src/main/java/MonticuloBinario.java:106` | `delMax` exige orientación de máximos (`exigir`, `:108`). |
| `isMinHeap` / `isMaxHeap` | `src/main/java/MonticuloBinario.java:115` | Recorre padres e hijos sin contar. |

**Pruebas** (`src/test/java/PruebasColas.java`): cola vacía (excepción documentada), repetidos,
orden creciente / decreciente / aleatorio con n `delMin` en orden, genericidad, top-K con empates,
heapify (cota < 2n y < n en cientos de casos, peor caso creciente para máximos, arreglo vacío), E1 = E2.

## Punto 4 — Selección de la combinación

`src/main/java/SeleccionCombinacion.java:38`: `BigInteger(hash, 16).mod(9)` sobre el entero
**completo** (no la suma de dígitos). `PruebasSeleccion` reproduce el ejemplo del enunciado (→ 3, BST
+ arreglo desordenado). Ejecutar con `./gradlew seleccion --args="<ids>"`.

## Punto 5 — Programa del índice

| Objetivo | Código |
|---|---|
| Fase A con esquema FrequencyCounter (get + put) | `src/main/java/Indice.java:93` |
| Esquema de cola acotada, sin comparar con `min()` antes | `src/main/java/Indice.java:106-113` |
| Rango del prefijo con σ = `'￿'` | `src/main/java/Indice.java:24`, `:118` |
| Prefijos de longitud 2 (Q = 295), fuera del cronómetro | `src/main/java/Indice.java:123`, `:157` |
| Fases cronometradas por separado, contadores reiniciados por fase | `src/main/java/Indice.java:146-147`, `:161`, `:169` |
| h, d̄, d̄_f al terminar la fase A, fuera del cronómetro | `src/main/java/Indice.java:153` |
| Cola **nueva** por consulta de autocompletado | `src/main/java/Indice.java:174` |
| Tokenización fuera de toda medición + calentamiento | `src/main/java/Indice.java:200`, `:203` |
| Firma de contadores (sanidad entre repeticiones) | `src/main/java/Indice.java:83` |

## Punto 6 — Experimentos

| Inciso | Código | Salida |
|---|---|---|
| 6a Heaps y Zipf | `src/main/java/Experimentos.java:80` | `6a_heaps.csv`, `6a_zipf.csv`, ajustes |
| 6b Escalamiento | `src/main/java/Experimentos.java:111` | `6b_escalamiento.csv`, ajustes por fase y contador |
| 6c Forma de los árboles | `src/main/java/Experimentos.java:164` | `6c_forma.csv` |
| 6d Orden de inserción | `src/main/java/Experimentos.java:180`, `:207` | `6d_orden.csv` |
| 6e Efecto de K, E1 vs E2 | `src/main/java/Experimentos.java:256` | `6e_efectoK.csv`, `6e_E1_vs_E2.csv` |
| 6f Referencia | `src/main/java/Experimentos.java:330`, `src/main/java/IndiceReferencia.java` | `6f_referencia.csv` |
| Ajuste `T = a·N^b` con R² | `src/main/java/Estadistica.java:24` | `ajustes_6a.csv`, `ajustes_6b.csv`, `ajustes_6e.csv` |

## Contadores verificados a mano

Todos están como `assert` en `PruebasTablas.contadoresAMano` (`src/test/java/PruebasTablas.java:150`)
y `PruebasColas.contadoresAMano` / `heapify` (`src/test/java/PruebasColas.java:87`, `:116`).

**BST, insertar `m c t a e`** → 6 comparaciones.
`m`: árbol vacío, 0. `c`: compara con `m`, 1. `t`: con `m`, 1. `a`: con `m` y `c`, 2. `e`: con `m` y `c`, 2.

**BinarySearchST, insertar `m c t a e`** → 7 comparaciones, 6 moves.

| put | arreglo antes | comparaciones de la búsqueda binaria | posición | moves |
|---|---|---|---|---|
| m | [] | 0 | 0 | 0 |
| c | [m] | 1 (m) | 0 | 1 |
| t | [c m] | 2 (c, m) | 2 | 0 |
| a | [c m t] | 2 (m, c) | 0 | 3 |
| e | [a c m t] | 2 (c, m) | 2 | 2 |

**RedBlackBST, insertar `m c t a e`** → 6 comparaciones, 0 rotaciones, 2 flips. Al insertar `t`, `m`
queda con dos hijos rojos (flip 1); al insertar `e`, `c` queda con dos hijos rojos (flip 2).
**`a b c`** → 2 comparaciones, 1 rotación izquierda (al insertar `b` como hijo rojo derecho), 1 flip.

**Arreglo desordenado, 5 elementos, primer delMin** → 4 `less`, 1 `exch` (el enunciado pide 4).

**Arreglo ordenado**: insertar 1..5 en orden creciente → `less` = `exch` = 0+1+2+3+4 = 10; en orden
decreciente → 4 `less`, 0 `exch`; `delMin` → 0 y 0.

**Montículo, insertar 3, 1, 2** → 2 `less`, 1 `exch`; luego `delMin` → +1 `exch` (cima ↔ último) y +1
`less` (`sink` con un solo hijo).

**Heapify de mínimos sobre [5, 4, 3, 2, 1]** → 6 `less`, 3 `exch` (< 2n = 10 y < n = 5):
`sink(2)`: elegir hijo (1 less), comparar (1 less), intercambiar → posición 5, sin hijos.
`sink(1)`: elegir hijo (1), comparar (1), intercambiar; en la posición 2: elegir hijo (1), comparar (1), intercambiar.

**Contadores en el texto completo** (fase B, K = 10), consistentes con el modelo:
arreglo desordenado `less = 229 385 = (M − 10)·10 + (9 + 8 + … + 0)` y `exch = 22 944 = (M − 10) + 10`.

## Mapa con la rúbrica

| Fila | Dónde se evidencia |
|---|---|
| a) Tokenización, Entrada, verificación | `Tokenizador`, `Entrada`, `PruebasTokenizador`, `PruebasIndice`; valores en [RAZONAMIENTO.md §10](RAZONAMIENTO.md#10-valores-de-verificación-obtenidos) |
| b) Tablas e instrumentación | Las tres tablas, verificadores, `PruebasTablas` (bordes, contadores a mano, equivalencia de `keys()`) |
| c) Colas e instrumentación | Las tres colas, heapify, `isMinHeap`, `PruebasColas` (bordes, contadores a mano, cota de heapify) |
| d) Evaluación analítica (7) | **A cargo del equipo**; datos de contraste en `resultados/` |
| e) Evaluación experimental (6) | `Experimentos` + `resultados/*.csv` (Heaps, Zipf, forma, orden con recursión resuelta, K, E1/E2, referencia, R²) |
| f) Ingeniería con IA | [`desarrollo-ia/`](../desarrollo-ia/README.md) |
| g) Discusión (8) | **A cargo del equipo**, apoyándose en `resultados/` |
