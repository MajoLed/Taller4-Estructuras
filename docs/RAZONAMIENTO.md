# Razonamiento de diseño: cómo se llegó a la construcción de los índices

Este documento explica **por qué** el código está construido como está: qué exige cada consulta,
qué estructura la resuelve y qué decisiones se tomaron (y por qué) en cada implementación. El
*cómo fluye* el programa está en [FLUJO.md](FLUJO.md) y el *dónde está cada cosa en el código* en
[PUNTOS-CLAVE.md](PUNTOS-CLAVE.md).

> El análisis analítico del punto 7 y la discusión del punto 8 **no** están aquí: el enunciado
> exige que sean trabajo propio del equipo. Este documento describe decisiones de implementación
> y deja los datos experimentales listos para ese análisis.

---

## 1. El problema, en tres consultas

| Consulta | Pregunta | Estructura que la resuelve |
|---|---|---|
| 1. Frecuencia | ¿Cuántas veces aparece `w`? | Tabla de símbolos `palabra → frecuencia` |
| 2. Top-K | ¿Cuáles son las K palabras más frecuentes? | Cola de prioridad de mínimos **acotada** a K |
| 3. Autocompletado | ¿Cuáles son las K' más frecuentes que empiezan por `p`? | Tabla **ordenada** (rango contiguo) + cola acotada |

Notación (la del enunciado, que se respeta en todo el código y los CSV):

- **N**: palabras del texto con repeticiones (tokens). Es lo único que controla el usuario.
- **M**: palabras distintas = tamaño de la tabla de símbolos (M ≤ N, y crece **sublinealmente**: ley de Heaps).
- **K**: tamaño máximo de la cola en la fase B; **K'** (= 5) en la fase C.
- **h**, **d̄**, **d̄_f**: altura, profundidad promedio y profundidad promedio ponderada por frecuencia.
- **Q**: número de consultas de autocompletado (= 295); **R**: tamaño del rango de un prefijo.

La idea clave del taller es que **cada operación depende de un parámetro distinto**: la tabla de
símbolos de M, la cola de K y el usuario solo controla N. Por eso cada medición del programa reporta
N, M y Q por separado, y la relación M(N) se mide (inciso 6a) para poder traducir costos a N.

## 2. Por qué la tabla de símbolos tiene que ser **ordenada**

Las consultas 1 y 2 se podrían resolver con una tabla hash (sin orden). La consulta 3 no: necesita
encontrar *todas* las palabras que empiezan por `p` sin recorrer las M llaves.

**Afirmación**: las palabras con prefijo `p` son exactamente las llaves `w` con `p ≤ w < p + σ`,
donde σ es un carácter mayor que cualquier carácter que pueda aparecer en una palabra.

*Razonamiento*:

- (⇒) Si `w` empieza por `p`, entonces `p ≤ w` (un prefijo es menor o igual que la cadena), y
  `w < p + σ`: o `w = p`, o el carácter de `w` en la posición `|p|` es una letra, que es menor que σ.
- (⇐) Si `w` **no** empieza por `p` y `w ≥ p`, existe una primera posición `i < |p|` en la que
  difieren, con `w[i] > p[i]`; como `p + σ` coincide con `p` en esa posición, también `w > p + σ`.
  Además `w` no puede ser un prefijo propio de `p` (sería menor que `p`).

En una tabla **ordenada**, el conjunto de llaves de un intervalo `[lo, hi]` ocupa posiciones
consecutivas del orden: son las de rango `rank(lo) … rank(hi)` (o `rank(hi) - 1` si `hi` no está).
Por eso el rango de un prefijo es **contiguo** y se puede recorrer con `keys(lo, hi)` visitando solo
la parte de la estructura que puede contener esas llaves.

**Elección de σ = `'￿'`** (`Indice.SIGMA`): el orden es **ordinal** (por código de carácter,
`String.compareTo` en Java), en el que `á`, `é`, `í`, `ó`, `ú`, `ü`, `ñ` (códigos 225–252) quedan
**después** de la `z` (122). Si se usara `'~'` (126) o `'z'`, se perderían palabras como `saña` del
rango de `sa`. La prueba `PruebasTablas.rangos` comprueba exactamente eso.

**Rango cerrado**: `keys(lo, hi)` incluye ambos extremos (API del texto guía). Usar `hi = p + σ`
como extremo cerrado es equivalente al semiabierto `[p, p + σ)` porque `p + σ` nunca es una palabra
(σ no está en el alfabeto `[a-záéíóúüñ]`).

## 3. Orden de las llaves y criterio de relevancia

- **Llaves**: `String.compareTo` es ordinal. Consecuencia verificada: `max()` es `útiles`, no una
  palabra con `z`.
- **Relevancia** (`Entrada.compareTo`): mayor frecuencia ⇒ más relevante; a igual frecuencia, la
  palabra **menor** es más relevante. Se implementa comparando primero frecuencias y luego
  `otra.palabra.compareTo(palabra)` (invertido). Es un **orden total**: dos entradas solo empatan si
  son iguales, así que el top-K es único.
- **Por qué importa el desempate**: hay empates justo en las fronteras de K = 100 y K = 1000; sin
  desempate, dos implementaciones correctas darían respuestas distintas. Con este orden, las 9
  combinaciones y la referencia de la biblioteca estándar devuelven exactamente lo mismo.
- **Por qué "mayor = más relevante"**: así una **MinPQ** tiene en la cima la entrada *menos*
  relevante, que es justo la que hay que descartar cuando la cola pasa de K elementos.

## 4. Las tres fases del índice

### Fase A — construcción (`Indice.construir`)

Esquema FrequencyCounter obligatorio: `v = get(w); si v == null put(w, 1) si no put(w, v + 1)`.
Son **dos búsquedas por palabra** a propósito: así los contadores son comparables entre equipos,
aunque un `put` que incremente directamente sería más barato.

### Fase B — top-K (`Indice.topK`)

```
para cada entrada e en orden de llave:
    pq.insert(e)
    si pq.size() > K: pq.delMin()      // descarta la menos relevante
extraer con delMin y llenar el arreglo desde el final (equivale a invertir)
```

La cola nunca supera K + 1 elementos, así que cada operación cuesta en función de K, no de M. No se
compara con `min()` antes de insertar: esa optimización la pide analizar el punto 7b.

**Decisión: `entries()` en lugar de `keys()` + `get(w)`.** El enunciado dice "recorrer `keys()` y para
cada palabra `w` insertar `Entrada(w, f(w))`". Obtener `f(w)` con `get` repetiría una búsqueda por
llave (M búsquedas extra), justo lo que el enunciado pide evitar en los rangos. Por eso la API tiene
`entries()`: el mismo recorrido en orden de `keys()`, pero entregando el par. Consecuencias:

- El orden de inserción en la cola es el mismo (orden de llave) ⇒ los contadores **less/exch** de la
  fase B son exactamente los del esquema del enunciado.
- `compares` de la tabla en la fase B es 0 en las tres implementaciones (el recorrido no compara llaves).

### Fase C — autocompletado (`Indice.autocompletar`, `Indice.prefijos`)

- **Consultas**: los prefijos distintos de longitud 2 de las palabras de ≥ 2 letras, en orden. Como
  `keys()` está ordenado, los prefijos iguales salen consecutivos y basta compararlos con el último
  (sin tabla auxiliar). Resultado: Q = 295.
- La lista de prefijos se calcula **fuera** del cronómetro: es la *entrada* de la fase C.
- Para cada `p`: `entries(p, p + σ)` + el **mismo esquema** de la fase B con una cola **nueva** de K' = 5.
- Los rangos de los Q prefijos son disjuntos y cubren todo el vocabulario salvo las palabras de una
  letra (comprobado en `PruebasIndice`).

## 5. Las tres tablas de símbolos

Las tres implementan la interfaz `TablaOrdenada` (API completa del texto guía + `entries`) y son
intercambiables: `Indice.TipoTabla.nueva()` las crea por nombre.

### BinarySearchST — arreglos paralelos ordenados

- `keys[]` y `vals[]` ordenados; `search(key)` es la única búsqueda binaria y devuelve `i` si la llave
  está o `-(punto de inserción) - 1` si no. Todo lo demás (`get`, `put`, `rank`, `floor`, `ceiling`,
  `keys(lo, hi)`) se apoya en ella ⇒ un solo lugar donde se cuentan comparaciones.
- `put` de una llave nueva corre los pares mayores una posición (`moves`). Redimensiona duplicando
  (la copia del redimensionamiento **no** cuenta como move; está documentado).
- `keys(lo, hi)` calcula los índices `[rank(lo), floor-índice(hi)]` y copia solo ese tramo.

### BST — árbol sin balancear (`BST` + `ArbolBusqueda`)

- `put` **iterativo**: baja desde la raíz guardando el camino en una `Pila`. Si la llave ya existe,
  solo actualiza el valor y **no** toca `size`; si es nueva, cuelga el nodo y luego incrementa `size`
  en todos los nodos del camino. Este es el error silencioso que advierte el enunciado: si se
  incrementara `size` antes de saber si la llave es nueva, `get` y `keys()` seguirían funcionando,
  pero `rank`/`select` quedarían mal. `isSizeConsistent` e `isRankConsistent` lo detectan.

### RedBlackBST — rojo-negro inclinado a la izquierda

- `put` **recursivo** del texto guía, con las tres correcciones locales (rotar a la izquierda, rotar a
  la derecha, `flipColors`). Se dejó recursivo a propósito: la profundidad de la recursión es la
  altura, acotada por 2 lg M ≈ 29, así que es seguro en cualquier orden de inserción.
- Las rotaciones recalculan `size` del nodo que baja y copian el del que sube.
- Contadores `rotations` (cada `rotateLeft`/`rotateRight`) y `flips` (cada `flipColors`).

### Base común `ArbolBusqueda` (todo iterativo)

Las operaciones de lectura son idénticas para los dos árboles, así que viven en la clase base:
`get`, `floor`, `ceiling`, `rank`, `select`, `min`, `max`, `keys`, `entries`, `height`,
`depthStats` y los verificadores. **Todas son iterativas** (con `Pila`/`Cola` propias): un BST
construido en orden creciente tiene altura M − 1 = 22 943 y una versión recursiva desbordaría la
pila del hilo (inciso 6d). Ver la sección 8.

- `keys(lo, hi)`/`entries(lo, hi)`: recorrido en orden **podado**. La pila solo guarda nodos con
  llave ≥ `lo`; al bajar, un nodo con llave < `lo` se salta por la derecha (su subárbol izquierdo
  entero es < `lo`); al desapilar una llave > `hi` se termina, porque todo lo que sigue en el recorrido
  en orden es mayor. Así se visitan O(h + R) nodos y nunca un subárbol que no pueda tener llaves del rango.
- `height()` y `depthStats()` recorren por niveles: el número de nivel *es* la profundidad, sin pila
  de enteros. `d̄_f = Σ f(w)·d(w) / Σ f(w)`, con `Σ f(w) = N`.

## 6. Las tres colas de prioridad

Las tres implementan `ColaPrioridad<T extends Comparable<? super T>>` (genéricas: las pruebas las
usan con `Integer`, `String` y `Entrada`) y se crean por nombre con `Indice.TipoCola.nueva()`.

| Implementación | insert | delMin | Detalle de diseño |
|---|---|---|---|
| `MinPQDesordenada` | O(1), 0 less | n − 1 less, 1 exch | Busca el mínimo, lo intercambia con el último y lo retira (siempre 1 exch, aunque ya sea el último). |
| `MinPQOrdenada` | hasta n less/exch | O(1), 0 less | Orden **decreciente** (mínimo al final); el nuevo entra al final y viaja hacia el frente por intercambios, como en inserción. |
| `MonticuloBinario` | ≤ lg n less | ≤ 2 lg n less | Arreglo 1-indexado, `swim`/`sink`. |

`MonticuloBinario` además:

- **Orientación** mínimos/máximos con una bandera; una sola función `prioridadMenor(i, j)` decide
  la dirección y cuenta 1 `less`. Con orientación de máximos ofrece `max()`/`delMax()` (estrategia
  E2 del inciso 6e); llamar `delMin` sobre uno de máximos lanza `IllegalStateException` para que el
  error no pase en silencio.
- **Heapify lineal**: constructor desde arreglo que copia los elementos y aplica `sink` desde ⌊n/2⌋
  hasta 1, sin insertar uno a uno. Las pruebas comprueban la cota (< 2n less, < n exch) en cientos
  de tamaños y órdenes.
- Verificadores `isHeap`, `isMinHeap`, `isMaxHeap` (no tocan los contadores).

**Cola vacía**: `min()` y `delMin()` lanzan `NoSuchElementException` (documentado en la interfaz).

## 7. Modelo de costo e instrumentación

- **compares** (tablas): cada comparación de **tres vías** entre llaves cuenta 1. Toda comparación
  pasa por un único método `compare(a, b)` que incrementa el contador una vez (en `ArbolBusqueda` y
  en `BinarySearchST`), así es imposible contar dos veces una comparación o "olvidar" las de
  `floor`, `rank` o `keys(lo, hi)`.
- **moves** (solo BinarySearchST): cada par (llave, valor) corrido una posición en `put`.
- **rotations / flips** (solo RedBlackBST).
- **less / exch** (colas): cada llamada al comparador / cada intercambio.
- **Verificadores**: no alteran los contadores (usan `compareTo` directo, o guardan y restauran el
  contador). La prueba `invariantes()` lo comprueba.
- Cada contador se verificó **a mano** en un caso pequeño (ver [PUNTOS-CLAVE.md](PUNTOS-CLAVE.md#contadores-verificados-a-mano)).

## 8. El problema de la recursión (inciso 6d) — cómo se resolvió

**Decisión**: versiones **iterativas** de todas las operaciones del BST que recorren caminos o el
árbol completo (`put`, `get`, `rank`, `select`, `floor`, `ceiling`, `keys`, `keys(lo, hi)`,
`height`, `depthStats`, `isBST`, `isSizeConsistent`). No se aumentó el tamaño de pila ni se redujo M.

**Hasta dónde se llegó**: el escenario completo, M = 22 944, en orden creciente y decreciente.
Se estimó primero con una sola repetición: construir cuesta 263 202 096 comparaciones
(= M(M − 1)/2, como se espera de una lista) y la carga de búsqueda (N = 383 637 `get`) ≈ 4 900 millones de
comparaciones, ≈ 80–100 s por repetición. Por ese costo, los dos escenarios degenerados del BST usan
`--reps-degenerado` (3 por defecto) y sin calentamiento extra; el resto usa 10 repeticiones.

El RedBlackBST conserva su `put` recursivo porque su altura está acotada (≈ 2 lg M).

## 9. Medición (punto 5 y 6)

- Lectura y tokenización ocurren **una vez**, antes de todo cronómetro.
- Cada fase se cronometra por separado con `System.nanoTime()`; los contadores se reinician al
  empezar cada fase. h, d̄ y d̄_f se calculan al terminar la fase A, **fuera** del bloque cronometrado.
- No hay E/S dentro de los cronómetros: los resultados se acumulan en memoria y los CSV se escriben al final.
- **Calentamiento** de la JVM (3 ejecuciones descartadas) y **10 repeticiones** por caso con promedio y
  desviación estándar muestral.
- **Prueba de sanidad**: los datos son deterministas, así que los contadores deben ser idénticos en
  todas las repeticiones; cada CSV tiene la columna `contadores_constantes` que lo comprueba.
- Las permutaciones aleatorias usan Fisher–Yates con `java.util.Random(semilla)`, semillas 1 a 10.

## 10. Valores de verificación obtenidos

Con el `pg2000.txt` del repositorio, **las 9 combinaciones** obtienen:

| Valor | Esperado | Obtenido |
|---|---|---|
| N | 383 637 | 383 637 |
| M | 22 944 | 22 944 |
| Q | 295 | 295 |
| Top-10 | que (20 769), de (18 410), y (18 272), la (10 492), a (9 876), en (8 285), el (8 267), no (6 346), los (4 769), se (4 752) | idéntico |
| `dul` (R = 12) | dulcinea (286), dulce (26), dulces (5), dulcísima (4), dulcineas (2) | idéntico |
| `san` (R = 53) | sancho (2 174), sansón (77), sangre (65), santa (49), san (32) | idéntico |
| `rank("quijote")`, `min()`, `max()` | 17 881, a, útiles | 17 881, a, útiles |

Además coinciden en todo (tabla completa, top-K, prefijos y las 295 respuestas de autocompletado)
con la implementación de referencia `TreeMap` + `PriorityQueue` (`PruebasIndice`).
