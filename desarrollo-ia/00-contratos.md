# Contratos del Tokenizador y del ADT Entrada

## Tokenizador (`src/main/java/Tokenizador.java`)

- **Entradas**: `tokenizarArchivo(String ruta)` — ruta de un archivo UTF-8 de Project Gutenberg;
  `tokenizar(String texto)` — texto arbitrario.
- **Salidas**: `String[]` con las palabras en orden de aparición, con repeticiones.

**Contrato**

- *Precondiciones*: (archivo) la ruta existe, es UTF-8 y contiene una línea con `*** START OF`
  antes de una línea con `*** END OF`; si no, `IllegalArgumentException`. (cadena) `texto != null`;
  si no, `IllegalArgumentException`.
- *Postcondiciones*: cada elemento coincide con `[a-záéíóúüñ]+`, está en minúsculas y es una
  secuencia **maximal** (no está precedida ni seguida por otra letra del alfabeto). Solo se usa el
  texto estrictamente entre las dos marcas.
- *Invariante*: función pura; el mismo texto produce siempre el mismo arreglo.
- *Complejidad*: lineal en el tamaño del texto. Es E/S y no se cronometra.

**Validación**: `PruebasTokenizador` (mayúsculas con tilde, `ñ`, `ü`, dígitos, puntuación, guiones,
comillas, `ç`/`à` como separadores, vacío, solo separadores, saltos de línea, `null`) y
`PruebasIndice` (N = 383 637 sobre el texto completo).

## ADT Entrada (`src/main/java/Entrada.java`)

- **Entradas**: `Entrada(String palabra, int frecuencia)`.
- **Salidas**: `palabra()`, `frecuencia()`, `toString()` → `"(palabra, frecuencia)"`, `compareTo`.

**Contrato**

- *Precondiciones*: `palabra != null`, `frecuencia >= 0` (si no, `IllegalArgumentException`).
- *Postcondiciones*: `a.compareTo(b) > 0` ⇔ `a` es **más relevante** que `b`: mayor frecuencia, o a
  igual frecuencia palabra menor en orden ordinal. `compareTo == 0` solo si palabra y frecuencia son
  iguales (coherente con `equals`/`hashCode`).
- *Invariantes*: inmutable (clase `final`, campos `private final`). El orden es total (antisimétrico,
  transitivo y sin empates entre entradas distintas), así que el top-K es único.

**Validación**: `PruebasTokenizador` (mayor frecuencia, empate por palabra, iguales, `ñ`/tildes
después de `z`, antisimetría, validación de argumentos) y `PruebasColas.genericidad` (en una MinPQ,
a igual frecuencia sale primero `zorro` que `amor`).
