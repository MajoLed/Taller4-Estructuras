import org.junit.jupiter.api.Test;

/**
 * Puente JUnit para ./gradlew test: ejecuta los programas de prueba basados en assert.
 * Gradle corre las pruebas con las aserciones habilitadas (-ea), así que un assert fallido hace fallar
 * la prueba correspondiente.
 */
class PruebasTest {
    @Test void aserciones() {
        boolean activas = false;
        assert activas = true;
        if (!activas) throw new AssertionError("Las aserciones deben estar habilitadas (-ea)");
    }

    @Test void tokenizadorYEntrada() { PruebasTokenizador.main(new String[0]); }
    @Test void tablasDeSimbolos() { PruebasTablas.main(new String[0]); }
    @Test void colasDePrioridad() { PruebasColas.main(new String[0]); }
    @Test void seleccionCombinacion() { PruebasSeleccion.main(new String[0]); }
    @Test void indiceCompleto() throws Exception { PruebasIndice.main(new String[0]); }
}
