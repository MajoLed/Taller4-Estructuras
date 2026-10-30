# Taller práctico 4 — Colas de prioridad y árboles binarios de búsqueda

Estructuras de Datos y Algoritmos · Facultad TIC · UPB · 2026-20

Índice de frecuencias de palabras de *Don Quijote de la Mancha* (Project Gutenberg #2000) con tres
consultas — **frecuencia**, **top-K** y **autocompletado por prefijo** — implementado con tablas de
símbolos ordenadas propias (BinarySearchST, BST, RedBlackBST) y colas de prioridad propias (arreglo
desordenado, arreglo ordenado, montículo binario), instrumentadas para la evaluación experimental.

**Equipo**: `Ledesma Maria - 000559241` · `Puente Miguel Angel - 000559418`

**Combinación asignada**: 
- `Tabla de símbolos - BinarySearchST`
- `Cola de prioridad - Arreglo Ordenado`

## Requisitos

- JDK 21 (Gradle descarga el resto con el wrapper).
- `pg2000.txt` en la raíz del repositorio (UTF-8, https://www.gutenberg.org/cache/epub/2000/pg2000.txt).
- Opcional, para la hoja de cálculo: Python 3 con `openpyxl`.

## Uso rápido

```bash
./gradlew test                                             # todas las pruebas (assert habilitados)
./gradlew run                                              # índice completo: RedBlackBST + montículo
./gradlew run --args="--tabla BST --cola DESORDENADA"      # cualquier combinación
./gradlew run --args="--n 50000 --k 100 --kp 5"            # primeras N palabras, K y K'
./gradlew seleccion --args="<id1> <id2> <id3>"             # punto 4: combinación del equipo
./gradlew experimentos                                     # punto 6 completo → resultados/*.csv
./gradlew experimentos --args="6c 6e --reps 10"            # solo algunos incisos
python3 scripts/generar_hoja.py                            # resultados/Taller4-resultados.xlsx
```

Nombres aceptados: tablas `BINARY_SEARCH_ST` (`BS`), `BST`, `RED_BLACK_BST` (`RB`); colas
`DESORDENADA`, `ORDENADA`, `MONTICULO` (`HEAP`).

Sin Gradle (las pruebas son programas con `assert`, salvo el puente JUnit `PruebasTest`):

```bash
javac -encoding UTF-8 -d out src/main/java/*.java $(ls src/test/java/Pruebas*.java | grep -v PruebasTest)
java -ea -cp out Indice --tabla BST --cola MONTICULO
java -ea -cp out PruebasTablas        # ídem PruebasColas, PruebasTokenizador, PruebasSeleccion, PruebasIndice
```

Ejemplo de salida (`./gradlew run`):

```
N = 383637   M = 22944   Q = 295
Top-10: que (20769), de (18410), y (18272), la (10492), a (9876), en (8285), el (8267), no (6346), los (4769), se (4752)
Autocompletado de dul (R = 12): dulcinea (286), dulce (26), dulces (5), dulcísima (4), dulcineas (2)
Autocompletado de san (R = 53): sancho (2174), sansón (77), sangre (65), santa (49), san (32)
rank("quijote") = 17881   min() = a   max() = útiles
```

Las **9 combinaciones** reproducen todos los valores de verificación del enunciado y coinciden
exactamente con la implementación de referencia `TreeMap` + `PriorityQueue`.

## Estructura

```
src/main/java/
  Tokenizador.java, Entrada.java               punto 1: tokenización y ADT Entrada
  TablaOrdenada.java                           punto 2: API común de tabla ordenada
  BinarySearchST.java                            arreglos paralelos + búsqueda binaria
  ArbolBusqueda.java, BST.java, RedBlackBST.java árboles (lecturas iterativas comunes)
  ColaPrioridad.java                           punto 3: API común de MinPQ genérica
  MinPQDesordenada.java, MinPQOrdenada.java, MonticuloBinario.java
  Cola.java, Pila.java                         estructuras auxiliares propias
  SeleccionCombinacion.java                    punto 4: SHA-256 mod 9
  Indice.java                                  punto 5: fases A, B, C y medición
  IndiceReferencia.java                        punto 6f: TreeMap + PriorityQueue
  Experimentos.java, Estadistica.java          punto 6: CSV y ajustes potenciales
src/test/java/
  PruebasTokenizador, PruebasTablas, PruebasColas, PruebasSeleccion, PruebasIndice   (assert)
  PruebasTest                                  puente JUnit para ./gradlew test
scripts/generar_hoja.py                        CSV → hoja de cálculo con gráficas y tendencias
resultados/                                    CSV del punto 6 y hoja de cálculo
docs/                                          documentación técnica (ver abajo)
desarrollo-ia/                                 entregable de ingeniería con IA (PRD, prompts, validación)
```

## Documentación

| Documento | Para qué |
|---|---|
| [docs/RAZONAMIENTO.md](docs/RAZONAMIENTO.md) | Por qué se construyó así: tabla ordenada y contigüidad del rango del prefijo, σ, desempate, fases, diseño de cada estructura, instrumentación, recursión (6d), medición |
| [docs/FLUJO.md](docs/FLUJO.md) | Flujo de datos y de control (diagramas), clases, recorrido podado, experimentos y sus CSV |
| [docs/PUNTOS-CLAVE.md](docs/PUNTOS-CLAVE.md) | Archivo y línea que cumple cada objetivo, contadores verificados a mano, mapa con la rúbrica |
| [desarrollo-ia/README.md](desarrollo-ia/README.md) | PRD, prompts, validación crítica y problemas por componente |

## Decisiones principales

- **Orden ordinal** (`String.compareTo`) y rango de prefijo `[p, p + '￿']`: las letras con tilde y
  la `ñ` quedan después de la `z`, así que `'~'` o `'z'` perderían palabras.
- **Relevancia**: mayor frecuencia; a igual frecuencia, palabra menor. Orden total ⇒ top-K único.
- **`entries()` / `entries(lo, hi)`**: los recorridos entregan el par (palabra, frecuencia) para no
  repetir una búsqueda por llave; el orden de inserción en la cola es el mismo que con `keys()`.
- **Árboles iterativos**: el BST degenerado del inciso 6d (altura 22 943) se procesa completo sin
  desbordar la pila. El `put` del rojo-negro es recursivo porque su altura está acotada por 2 lg M.
- **Un solo `compare` por estructura**: cada comparación de tres vías cuenta exactamente 1.
- Sin colecciones ni ordenamientos de la biblioteca en la lógica de tablas y colas (solo en
  `IndiceReferencia` y como oráculo en las pruebas).

## Estado de los entregables

| Entregable (enunciado) | Estado |
|---|---|
| Código: tokenizador, Entrada, 3 tablas, 3 colas, programa del índice, referencia 6f | ✅ |
| Pruebas unitarias con `assert` (bordes, verificadores, contadores a mano) | ✅ `./gradlew test` |
| Datos experimentales del punto 6 (todos los incisos, 10 repeticiones, media y sd) | ✅ `resultados/*.csv` |
| Hoja de cálculo con datos, gráficas y líneas de tendencia con ecuación y R² | ✅ generada por `scripts/generar_hoja.py`; revisar el formato en Excel/LibreOffice |
| `desarrollo-ia/` (PRD, prompts, validación, problemas) | ✅ — **faltan las reflexiones de propiedad del equipo** |
| Documento de análisis (Word/PDF): hash y combinación, valores de verificación, **punto 7**, gráficas e interpretación del **punto 6**, **punto 8**, recursión en 6d | ⏳ **a cargo del equipo** (el enunciado prohíbe delegar el análisis del punto 7 y la interpretación de 6 y 8) |

### Pendientes del equipo antes de entregar

1. Calcular la combinación con `./gradlew seleccion --args="<ids>"` y registrarla aquí, en
   `desarrollo-ia/README.md` y en el informe.
2. Escribir los nombres de los integrantes en este README y en los archivos fuente y documentos
   (el enunciado exige marcar todos los archivos).
3. Escribir las secciones *Entender y descomponer* (sin IA) y *Reflexión final* de cada archivo de
   `desarrollo-ia/`.
4. Redactar el informe (puntos 6, 7 y 8) a partir de `resultados/` y la hoja de cálculo.
5. Indicar en el informe la ruta esperada de `pg2000.txt` (raíz del proyecto) y la fecha de descarga.
6. Entregar `Taller4-<Apellido1Nombre1><ID1>-...zip` **sin** `pg2000.txt` ni `build/`.
