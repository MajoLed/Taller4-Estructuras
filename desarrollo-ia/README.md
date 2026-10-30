# Desarrollo con IA — Taller 4

Documentación de ingeniería con IA exigida por el enunciado (sección *Uso de asistentes de IA*),
organizada como carpeta con un archivo por componente.

| Archivo | Componente |
|---|---|
| [00-contratos.md](00-contratos.md) | Contratos del Tokenizador y del ADT Entrada |
| [01-tabla-simbolos.md](01-tabla-simbolos.md) | Tablas de símbolos ordenadas (BinarySearchST, BST, RedBlackBST), verificadores e instrumentación |
| [02-cola-prioridad.md](02-cola-prioridad.md) | Colas de prioridad (desordenada, ordenada, montículo binario), less/exch, heapify |
| [03-programa-indice.md](03-programa-indice.md) | Programa del índice, consultas y medición (punto 5) y experimentos (punto 6) |
| [prompts.md](prompts.md) | Registro textual de los prompts usados |

**Herramienta**: Claude Code (modelo Claude Opus 5.5), 29/10/2026.

**Combinación asignada**: se implementaron las tres variantes de cada familia, como exige el
enunciado. La combinación del equipo se obtiene con
`./gradlew seleccion --args="<id1> <id2> <id3>"`; el equipo debe registrar aquí los ID ordenados,
la cadena, el hash y la combinación, y concentrar el análisis en ella:

```
IDs ordenados:      <completar>
Cadena:             <completar>
SHA-256:            <completar>
mod 9 / combinación: <completar>
```

## Qué es trabajo de la IA y qué es del equipo

- **IA (implementación)**: código de las estructuras, del programa y de los experimentos; pruebas;
  verificación de que el código hace lo que dice (pruebas, contadores a mano, valores de verificación).
- **Equipo (obligatorio, no delegable según el enunciado)**: análisis analítico del punto 7,
  interpretación de los resultados de los puntos 6 y 8, y las **reflexiones de propiedad** de cada
  componente (sección 6 de cada archivo, marcadas como *pendiente*). El equipo además debe releer
  y validar los PRD y el registro de validación: el enunciado pide que el estudiante *decida,
  especifique y verifique*.
