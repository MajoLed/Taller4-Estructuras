# Flujo del programa

Cómo viajan los datos desde `pg2000.txt` hasta las respuestas y los CSV. El *por qué* de cada
decisión está en [RAZONAMIENTO.md](RAZONAMIENTO.md).

## 1. Vista general

```mermaid
flowchart TD
    F[pg2000.txt] -->|Tokenizador.tokenizarArchivo<br/>NO cronometrado| P["String[] palabras (N = 383 637)"]
    P -->|primeras N palabras| A

    subgraph A [Fase A · construcción · cronometrada]
        A1["para cada w: v = get(w)<br/>v == null ? put(w,1) : put(w,v+1)"]
    end

    A --> ST[(Tabla de símbolos ordenada<br/>M = 22 944 llaves)]
    ST -->|height / depthStats<br/>NO cronometrado| S[h, d̄, d̄_f]
    ST -->|prefijos de longitud 2<br/>NO cronometrado| PR["String[] prefijos (Q = 295)"]

    ST --> B
    subgraph B [Fase B · top-K · cronometrada]
        B1["para cada e en entries():<br/>pq.insert(e); si size > K: delMin()"]
        B2["K delMin → arreglo desde el final"]
        B1 --> B2
    end

    ST --> C
    PR --> C
    subgraph C [Fase C · autocompletado · cronometrada]
        C1["para cada prefijo p:<br/>entries(p, p + σ) → cola nueva acotada a K' = 5"]
    end

    B --> R[Resultado: top-K, tiempos, contadores por fase]
    C --> R
    S --> R
    R -->|Indice.main| O1[Consola]
    R -->|Experimentos| O2[resultados/*.csv]
```

## 2. Clases y responsabilidades

```mermaid
classDiagram
    class TablaOrdenada {
        <<interface>>
        put get contains size isEmpty
        min max floor ceiling rank select
        keys() keys(lo,hi) entries() entries(lo,hi)
        compares moves rotations flips resetCounters
        isBST isSizeConsistent isRankConsistent
    }
    class BinarySearchST
    class ArbolBusqueda {
        <<abstract>>
        Node root
        lecturas iterativas
        height() depthStats()
    }
    class BST { put iterativo }
    class RedBlackBST { put recursivo LLRB; is23 isBalanced }
    TablaOrdenada <|.. BinarySearchST
    TablaOrdenada <|.. ArbolBusqueda
    ArbolBusqueda <|-- BST
    ArbolBusqueda <|-- RedBlackBST

    class ColaPrioridad~T~ {
        <<interface>>
        insert min delMin size isEmpty
        less exch resetCounters
    }
    class MinPQDesordenada~T~
    class MinPQOrdenada~T~
    class MonticuloBinario~T~ { heapify; max/delMax; isMinHeap }
    ColaPrioridad <|.. MinPQDesordenada
    ColaPrioridad <|.. MinPQOrdenada
    ColaPrioridad <|.. MonticuloBinario

    class Entrada { palabra frecuencia compareTo }
    class Indice { construir topK autocompletar prefijos ejecutar }
    class Experimentos { 6a 6b 6c 6d 6e 6f }
    class IndiceReferencia { TreeMap + PriorityQueue }
    Indice ..> TablaOrdenada
    Indice ..> ColaPrioridad
    Indice ..> Entrada
    Experimentos ..> Indice
    Experimentos ..> IndiceReferencia
```

| Archivo | Rol |
|---|---|
| `Tokenizador` | Lee el archivo, recorta encabezado/pie de Gutenberg, minúsculas, regex `[a-záéíóúüñ]+`. |
| `Entrada` | Par inmutable `(palabra, frecuencia)` ordenado por relevancia. |
| `TablaOrdenada` | API común de las tres tablas. |
| `BinarySearchST`, `BST`, `RedBlackBST`, `ArbolBusqueda` | Las tres tablas (los árboles comparten la base). |
| `ColaPrioridad`, `MinPQDesordenada`, `MinPQOrdenada`, `MonticuloBinario` | Las tres colas. |
| `Cola`, `Pila` | Estructuras auxiliares propias (iterables de resultados y recorridos iterativos). |
| `Indice` | Programa del punto 5: fases A, B, C y medición; `main` imprime los valores de verificación. |
| `IndiceReferencia` | Punto 6f: mismas fases con `TreeMap` + `PriorityQueue`. |
| `Experimentos`, `Estadistica` | Punto 6: genera los CSV y los ajustes potenciales. |
| `SeleccionCombinacion` | Punto 4: SHA-256 de los ID mod 9. |

## 3. Una consulta de autocompletado, paso a paso (`p = "du"`)

```mermaid
sequenceDiagram
    participant I as Indice.autocompletar
    participant T as TablaOrdenada
    participant Q as ColaPrioridad (nueva, K' = 5)
    I->>T: entries("du", "du" + '￿')
    Note over T: recorrido en orden podado:<br/>solo nodos que pueden estar en el rango
    T-->>I: Cola<Entrada> con las R entradas del rango, en orden de llave
    loop por cada entrada e del rango
        I->>Q: insert(e)
        alt size > 5
            I->>Q: delMin()  (sale la menos relevante)
        end
    end
    loop 5 veces
        I->>Q: delMin()
        Note over I: se llena el arreglo desde el final
    end
    I-->>I: [dulcinea, dulce, dulces, dulcísima, dulcineas]
```

## 4. Recorrido podado de `keys(lo, hi)` en un árbol

```
apilarIzquierda(x):                     recorrerRango(lo, hi):
  mientras x != null:                     si lo > hi: rango vacío
    si lo < x.key: push(x); x = x.left    apilarIzquierda(root)
    si lo > x.key: x = x.right            mientras la pila no esté vacía:
    si lo = x.key: push(x); parar           x = pop()
                                            si x.key > hi: terminar
                                            emitir (x.key, x.val)
                                            apilarIzquierda(x.right)
```

Invariante: la pila solo contiene nodos con llave ≥ `lo` cuyo subárbol izquierdo ya se descartó o
ya se emitió; el recorrido emite en orden creciente, así que la primera llave > `hi` lo termina.

## 5. Flujo de los experimentos (`Experimentos`)

```mermaid
flowchart LR
    T[Tokenizar una vez] --> E{incisos pedidos}
    E --> a[6a: M para cada N → Heaps<br/>top-1000 → Zipf]
    E --> b[6b: 9 combinaciones × 7 tamaños<br/>3 calentamientos + 10 reps]
    E --> c[6c: BST y RB por tamaño<br/>h, d̄, d̄_f vs lg M, 2 ln M, 2 lg M]
    E --> d[6d: 3 tablas × órdenes texto/creciente/<br/>decreciente/aleatorio s=1..10<br/>construcción + N get]
    E --> e[6e: 3 colas × K ∈ 1..10 000<br/>E1 vs E2 con heapify]
    E --> f[6f: referencia vs 9 combinaciones<br/>coincidencia + factor de tiempo]
    a & b & c & d & e & f --> CSV[resultados/*.csv + ajustes_6x.csv]
```

| CSV | Inciso | Contenido |
|---|---|---|
| `6a_heaps.csv` | 6a | N, M (y logaritmos) para la serie geométrica y el texto completo |
| `6a_zipf.csv` | 6a | rango, palabra, frecuencia del top-1000 |
| `6b_escalamiento.csv` | 6b | por combinación y N: tiempos por fase (media, sd), contadores por fase, M, Q, h, d̄, d̄_f |
| `6c_forma.csv` | 6c | por N y árbol: h, d̄, d̄_f y curvas de referencia |
| `6d_orden.csv` | 6d | por tabla y escenario: construcción y carga de búsqueda (tiempo y contadores), forma |
| `6e_efectoK.csv` | 6e | por cola y K: less, exch, tiempo de la fase B |
| `6e_E1_vs_E2.csv` | 6e | por K: E1 (MinPQ acotada) frente a E2 (heapify de máximos + K delMax), cuál conviene |
| `6f_referencia.csv` | 6f | tiempos por fase de la referencia y de cada combinación, factor y coincidencia |
| `ajustes_6a.csv`, `ajustes_6b.csv`, `ajustes_6e.csv` | 6a, 6b, 6e | a, b y R² de cada ajuste potencial `y = a·x^b` (solo sobre la serie geométrica) |

## 6. Cómo ejecutar cada parte

```bash
./gradlew test                                     # todas las pruebas (assert habilitados)
./gradlew run                                      # índice con RedBlackBST + montículo, texto completo
./gradlew run --args="--tabla BST --cola DESORDENADA --n 50000 --k 10"
./gradlew seleccion --args="<id1> <id2> <id3>"     # combinación asignada (punto 4)
./gradlew experimentos                             # punto 6 completo (~30 min, la mayoría en 6d)
./gradlew experimentos --args="6a 6c --reps 10"    # solo algunos incisos
./gradlew experimentos --args="todos --tabla BST --cola DESORDENADA"   # solo la combinación asignada
```
