package br.com.chavespix;

public final class PixCardDisplayFormatter {
    private PixCardDisplayFormatter() {
    }

    public static String formatSummaryValue(String type, String value) {
        return PixKeyFormatter.formatForSummary(type, value);
    }
}
