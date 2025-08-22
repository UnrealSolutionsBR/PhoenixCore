package com.phoenixcore.utils;
import java.text.DecimalFormat;
public class FormatUtils {
    public static String formatNumber(int number) {
        if (number < 1000) {
            return String.valueOf(number);
        }
        if (number < 1_000_000) {
            return new DecimalFormat("0.#").format(number / 1000.0) + "K";
        }
        if (number < 1_000_000_000) {
            return new DecimalFormat("0.#").format(number / 1_000_000.0) + "M";
        }
        return new DecimalFormat("0.#").format(number / 1_000_000_000.0) + "B";
    }
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
