/**
 * Utilidades numéricas de los experimentos: media, desviación estándar muestral y ajuste potencial
 * y = a x^b por mínimos cuadrados sobre (ln x, ln y), con su R² (igual que la línea de tendencia
 * potencial de Excel/LibreOffice).
 */
public final class Estadistica {
    private Estadistica() { }

    public static double media(double[] v) {
        double s = 0;
        for (double x : v) s += x;
        return s / v.length;
    }

    /** Desviación estándar muestral (divide por n - 1); 0 si hay un solo dato. */
    public static double desviacion(double[] v) {
        if (v.length < 2) return 0;
        double m = media(v), s = 0;
        for (double x : v) s += (x - m) * (x - m);
        return Math.sqrt(s / (v.length - 1));
    }

    /** Ajuste y = a x^b. Devuelve {a, b, R²}. Requiere x, y > 0. */
    public static double[] ajustePotencia(double[] x, double[] y) {
        int n = x.length;
        double[] lx = new double[n], ly = new double[n];
        for (int i = 0; i < n; i++) { lx[i] = Math.log(x[i]); ly[i] = Math.log(y[i]); }
        double mx = media(lx), my = media(ly), sxy = 0, sxx = 0, syy = 0;
        for (int i = 0; i < n; i++) {
            sxy += (lx[i] - mx) * (ly[i] - my);
            sxx += (lx[i] - mx) * (lx[i] - mx);
            syy += (ly[i] - my) * (ly[i] - my);
        }
        double b = sxy / sxx;
        double a = Math.exp(my - b * mx);
        double r2 = syy == 0 ? 1 : (sxy * sxy) / (sxx * syy);
        return new double[]{a, b, r2};
    }

    public static double lg(double x) { return Math.log(x) / Math.log(2); }
}
