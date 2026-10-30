# Taller práctico 4 — Colas de prioridad y árboles binarios de búsqueda

Estructuras de Datos y Algoritmos · Facultad TIC · UPB · 2026-20

Implementación de un índice de frecuencias de palabras a partir del texto *Don Quijote de la Mancha*, utilizando tablas de símbolos y colas de prioridad.

## Integrantes

- Maria Jose Ledesma - 0000559241
- Miguel Angel Puente Mejía - 0000559418

## Descripción

El proyecto permite procesar un archivo de texto, contabilizar la frecuencia de sus palabras y realizar consultas sobre la información obtenida.

Entre sus funcionalidades se encuentran:

- Construcción de un índice de frecuencias.
- Consulta de las palabras más frecuentes.
- Búsqueda de palabras y consulta de sus posiciones mediante `rank`.
- Obtención de las palabras mínima y máxima.
- Autocompletado de palabras a partir de prefijos.
- Medición de operaciones y tiempos de ejecución.

## Estructuras de datos

El proyecto implementa diferentes estructuras para almacenar y consultar las frecuencias de las palabras.

### Tablas de símbolos

- **BinarySearchST:** tabla de símbolos implementada mediante arreglos ordenados y búsqueda binaria.
- **BST:** árbol binario de búsqueda.
- **RedBlackBST:** árbol binario de búsqueda balanceado mediante reglas de colores y rotaciones.

### Colas de prioridad

- **MinPQDesordenada:** cola de prioridad implementada mediante un arreglo desordenado.
- **MinPQOrdenada:** cola de prioridad implementada mediante un arreglo ordenado.
- **MonticuloBinario:** cola de prioridad implementada mediante un montículo binario.

## Combinación asignada

La combinación seleccionada para el desarrollo y análisis del proyecto es:

**RedBlackBST + Montículo binario**

La selección se obtiene a partir de los identificadores institucionales de los integrantes, mediante el cálculo SHA-256 y la operación módulo 9.

| Identificador | Valor |
|---|---|
| Integrante 1 | `0000559241` |
| Integrante 2 | `0000559418` |
| SHA-256 | `88b0113335defc408e81069825fb307c2d4dcdd34459e719d493a82fda9b77e2` |
| Resultado módulo 9 | `8` |

## Funcionalidades

### Índice de frecuencias

Procesa el texto de entrada y registra la cantidad de apariciones de cada palabra.

### Top-K

Obtiene las K palabras con mayor frecuencia utilizando una cola de prioridad.

### Autocompletado

Busca palabras que coinciden con un prefijo determinado y las organiza según su frecuencia.

### Consultas

Permite consultar la frecuencia de una palabra, su posición en la tabla de símbolos y los valores mínimo y máximo.

### Análisis de rendimiento

Registra tiempos de ejecución y contadores de operaciones para estudiar el comportamiento de las estructuras implementadas.

## Tecnologías y requisitos

- Java 21.
- Gradle.
- JUnit para pruebas.

## Ejecución

Para ejecutar las pruebas:

```bash
./gradlew test
```

Ejecutar el programa con la combinación asignada:

```bash
./gradlew run --args="--tabla RED_BLACK_BST --cola MONTICULO"
```

Calcular la combinación a partir de los identificadores:

```bash
./gradlew seleccion --args="0000559241 0000559418"
```

## Experimentos

Los experimentos permiten evaluar el comportamiento de las estructuras mediante diferentes operaciones y configuraciones.

Para ejecutar un experimento específico:

```bash
./gradlew experimentos --args="6a"
```

Para ejecutar todos los experimentos disponibles:

```bash
./gradlew experimentos
```

Los resultados generados se almacenan en la carpeta `resultados/`, en formato CSV.

## Organización del proyecto

La organización general del código es la siguiente:

```text
Taller4-Estructuras/
├── src/
│   ├── main/
│   │   └── java/
│   └── test/
│       └── java/
├── resultados/
├── pg2000.txt
├── build.gradle
├── settings.gradle
└── README.md
```

El código fuente contiene las implementaciones de las estructuras, las clases encargadas del procesamiento del texto y los programas de ejecución de los experimentos.

