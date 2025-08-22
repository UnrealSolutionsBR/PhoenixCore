package com.phoenixcore.utils;

import java.text.DecimalFormat;

public class FormatUtils {

    private static final DecimalFormat df = new DecimalFormat("0.##");

    private static final String[] SUFFIXES = {
            "",     // < 1000
            "K",    // 10^3
            "M",    // 10^6
            "B",    // 10^9
            "T",    // 10^12
            "Qa",   // 10^15
            "Qi"    // 10^18
    };

    /**
     * Formatea un número largo en estilo K, M, B, T, Qa, Qi
     * Ejemplo: 1,500 -> "1.5K", 2,000,000 -> "2M"
     */
    public static String formatNumber(long number) {
        if (number < 1000) {
            return String.valueOf(number);
        }

        int exp = (int) (Math.log10(number) / 3); // cada 3 ceros cambiamos de sufijo
        if (exp >= SUFFIXES.length) {
            exp = SUFFIXES.length - 1; // limitar a Qi
        }

        double value = number / Math.pow(1000, exp);
        return df.format(value) + SUFFIXES[exp];
    }

    /**
     * Genera una barra de progreso de texto.
     *
     * @param current     Valor actual
     * @param max         Valor máximo
     * @param totalBars   Cantidad de barras a mostrar
     * @param symbolFull  Símbolo lleno (ej: "§a▮")
     * @param symbolEmpty Símbolo vacío (ej: "§8▮")
     * @return String con la barra de progreso
     */
    public static String getProgressBar(int current, int max, int totalBars, String symbolFull, String symbolEmpty) {
        if (max <= 0) max = 1;
        double percent = (double) current / max;
        int progressBars = (int) (totalBars * percent);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < totalBars; i++) {
            if (i < progressBars) {
                sb.append(symbolFull);
            } else {
                sb.append(symbolEmpty);
            }
        }
        return sb.toString();
    }
}
