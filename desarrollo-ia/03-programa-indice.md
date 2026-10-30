# Programa de construcción del índice, consultas y medición

## 1. Entender y descomponer (sin IA)

> **Pendiente de revisión por el equipo** (esta sección se hace sin IA según el enunciado).

El programa encadena: lectura y tokenización (no cronometrada) → fase A (frecuencias) → fase B
(top-K) → fase C (autocompletado de los Q prefijos), midiendo tiempo y contadores **por fase**, y
excluyendo toda E/S. Sobre él se montan los experimentos del punto 6.

## 2. PRD

### Requerimientos
- Entradas: ruta de `pg2000.txt`; combinación (tabla, cola); N (primeras N palabras); K; K'.
- Salidas: `Indice.Resultado` con N, M, Q, tiempos tA/tB/tC (ns), contadores por fase
  (compares, moves, rotations, flips, less, exch), h, d̄, d̄_f, top-K y las Q respuestas. Los
  experimentos escriben CSV en `resultados/`.

### Contrato
- Precondiciones: `0 <= N <= número de tokens`; `K >= 1`, `K' >= 1`; el archivo cumple el contrato
  del Tokenizador.
- Postcondiciones: la tabla contiene la frecuencia exacta de cada palabra de las primeras N; el top-K
  está ordenado de más a menos relevante; cada autocompletado contiene las min(K', R) entradas más
  relevantes del rango del prefijo.
- Invariantes: cada fase empieza con los contadores en 0; nada de E/S, tokenización ni cálculo de
  forma del árbol dentro de un bloque cronometrado; los contadores son idénticos entre repeticiones.

### Restricciones de implementación
- Esquemas exactos del enunciado: fase A `get` + `put`; fases B y C `insert` y `delMin` si
  `size > K`, sin comparar antes con `min()`.
- `System.nanoTime()` con calentamiento previo de la JVM; ≥ 10 repeticiones, media y desviación estándar.
- Prohibiciones: estructuras de biblioteca fuera de `IndiceReferencia` (6f).

### Criterios de aceptación
- Típico: valores de verificación del enunciado en las 9 combinaciones.
- Bordes: N pequeño (6 250), K mayor que el rango (prefijos con R < 5), palabras de una letra
  excluidas de los prefijos.
- Sanidad: contadores constantes entre repeticiones; misma salida que `TreeMap` + `PriorityQueue`.

## 3. Prompt(s) utilizado(s)

Ver [prompts.md](prompts.md) (prompt 1).

## 4. Registro de validación crítica

- **Valores de verificación** (`PruebasIndice`, las 9 combinaciones): N = 383 637, M = 22 944, Q = 295,
  top-10, `dul` (R = 12), `san` (R = 53), `rank("quijote") = 17 881`, `min = a`, `max = útiles` — **OK**.
- **Referencia**: tabla completa, top-K, prefijos y las 295 respuestas idénticas a `TreeMap` +
  `PriorityQueue` — **OK**.
- **Cobertura de la fase C**: la suma de los R de los Q prefijos más las palabras de una letra = M — **OK**.
- **Separación de fases** (revisión del código de `Indice.ejecutar`): la tokenización ocurre antes en
  `main`; `height`/`depthStats` y `prefijos` se ejecutan entre los cronómetros de A y B; la cola de
  cada consulta se crea dentro del bloque de C (su costo es parte de la consulta).
- **Contadores constantes** entre repeticiones: comprobado en `PruebasIndice` y en todas las filas de
  los CSV (`contadores_constantes = true`).
- **Escenario degenerado (6d)**: estimado primero con una repetición (≈ 5,5 s de construcción y
  ≈ 80–100 s de búsqueda) antes de lanzar la corrida completa.

## 5. Problemas encontrados y correcciones

| Problema | Origen | Corrección |
|---|---|---|
| La consola mostraba `?` en lugar de `í`, `ó`, `ú`. | Entorno (codificación de la salida estándar) | `-Dstdout.encoding=UTF-8` en las tareas de Gradle. |
| `gradlew` estaba en el repositorio sin permiso de ejecución. | Repositorio | `chmod +x gradlew`. |
| `exceptionFormat 'full'` usa sintaxis obsoleta en Gradle 9.6. | Detalle de la IA | `exceptionFormat = 'full'`. |
| La búsqueda sobre el BST degenerado tarda ~100 s por repetición: 10 repeticiones + calentamiento serían > 30 min solo para dos escenarios. | Restricción de tiempo | `--reps-degenerado` (3 por defecto) y sin calentamiento extra en esos dos escenarios (la JVM ya está caliente); se documenta en el CSV (columna `reps`). |
| En la primera corrida de 6b, las fases B y C de las primeras combinaciones salieron más lentas en N chico (p. ej. 0,33 ms frente a 0,06 ms) y sus ajustes tenían R² ≈ 0,3: se midieron con el código aún sin compilar por el JIT (3 calentamientos por caso no bastaban para fases de ~0,1 ms). | Error de la IA en el protocolo | Calentamiento global de las 9 combinaciones sobre el texto completo antes de medir; se repitió 6b. |
| Un solo `ajustes.csv` se sobrescribía al volver a correr un inciso. | Error de la IA | Un archivo por inciso: `ajustes_6a.csv`, `ajustes_6b.csv`, `ajustes_6e.csv`. |
| ¿Dónde se calcula la lista de prefijos? | Ambigüedad del PRD | Fuera del cronómetro: es la entrada de la fase C, no su trabajo. |

## 6. Reflexión final (prueba de propiedad)

> **Pendiente — debe escribirla el equipo** (3–5 líneas).
