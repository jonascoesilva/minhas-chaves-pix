package br.com.chavespix;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class BankTheme {
    private static final List<BankTheme> ALL = Collections.unmodifiableList(Arrays.asList(
            new BankTheme("teal", "Petróleo", "#006B62"),
            new BankTheme("nubank", "Nubank", "#820AD1"),
            new BankTheme("itau", "Itaú", "#EC7000"),
            new BankTheme("santander", "Santander", "#CC0000"),
            new BankTheme("bb", "Banco do Brasil", "#FFCC00", "#005CA9"),
            new BankTheme("caixa", "Caixa", "#0070AF", "#F39200"),
            new BankTheme("picpay", "PicPay", "#21C25E"),
            new BankTheme("stone", "Stone", "#00A878"),
            new BankTheme("c6inter", "C6 / Inter", "#171717"),
            new BankTheme("tealblue", "Petróleo / Azul", "#00796B", "#1565C0"),
            new BankTheme("violetpink", "Roxo / Rosa", "#6C63FF", "#D94C99"),
            new BankTheme("sunset", "Laranja / Rosa", "#F97316", "#DB2777")
    ));

    private final String id;
    private final String label;
    private final int[] colors;

    private BankTheme(String id, String label, String... colors) {
        this.id = id;
        this.label = label;
        this.colors = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
            this.colors[i] = Color.parseColor(colors[i]);
        }
    }

    public static List<BankTheme> all() {
        return ALL;
    }

    public static BankTheme fromId(String id) {
        for (BankTheme theme : ALL) {
            if (theme.id.equals(id)) {
                return theme;
            }
        }
        if (isHexColor(id)) {
            String normalized = id.toUpperCase(Locale.ROOT);
            return new BankTheme(normalized, "Personalizada", normalized);
        }
        return ALL.get(0);
    }

    public static boolean isStoredValue(String value) {
        if (isHexColor(value)) {
            return true;
        }
        for (BankTheme theme : ALL) {
            if (theme.id.equals(value)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isHexColor(String value) {
        return value != null && value.matches("#[0-9A-Fa-f]{6}");
    }

    public static String normalizeStoredValue(String value) {
        if (isHexColor(value)) {
            return value.toUpperCase(Locale.ROOT);
        }
        for (BankTheme theme : ALL) {
            if (theme.id.equals(value)) {
                return theme.id;
            }
        }
        return ALL.get(0).id;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public int[] getColors() {
        return colors.clone();
    }

    public String getPrimaryColorHex() {
        return String.format(Locale.ROOT, "#%06X", colors[0] & 0xFFFFFF);
    }

    public GradientDrawable createDrawable(int strokeColor, int strokeWidth) {
        GradientDrawable drawable = createBaseDrawable(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(16f);
        drawable.setStroke(strokeWidth, strokeColor);
        return drawable;
    }

    public GradientDrawable createSwatchDrawable(boolean selected) {
        GradientDrawable drawable = createBaseDrawable(GradientDrawable.OVAL);
        drawable.setStroke(
                selected ? 3 : 1,
                selected ? Color.BLACK : Color.WHITE
        );
        return drawable;
    }

    private GradientDrawable createBaseDrawable(int shape) {
        int[] themeColors = getColors();
        GradientDrawable drawable;
        if (themeColors.length == 1) {
            drawable = new GradientDrawable();
            drawable.setColor(themeColors[0]);
        } else {
            drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, themeColors);
        }
        drawable.setShape(shape);
        return drawable;
    }
}
