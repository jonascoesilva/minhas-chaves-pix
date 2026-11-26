package br.com.chavespix;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ColorPreferences {
    private static final String PREFERENCES = "pix_card_preferences";
    private static final String RECENT_COLORS = "recent_custom_colors";
    private static final int MAX_RECENT = 6;

    private ColorPreferences() {
    }

    public static List<String> getRecentColors(Context context) {
        String stored = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .getString(RECENT_COLORS, "");
        List<String> colors = new ArrayList<>();
        if (stored == null || stored.isEmpty()) {
            return colors;
        }
        for (String color : stored.split(",")) {
            if (isHexColor(color) && !colors.contains(color.toUpperCase(Locale.ROOT))) {
                colors.add(color.toUpperCase(Locale.ROOT));
            }
        }
        return colors;
    }

    public static void rememberColor(Context context, String color) {
        if (!isHexColor(color)) {
            return;
        }
        String normalized = color.toUpperCase(Locale.ROOT);
        List<String> recent = getRecentColors(context);
        recent.remove(normalized);
        recent.add(0, normalized);
        if (recent.size() > MAX_RECENT) {
            recent = new ArrayList<>(recent.subList(0, MAX_RECENT));
        }
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit()
                .putString(RECENT_COLORS, join(recent))
                .apply();
    }

    private static boolean isHexColor(String color) {
        return color != null && color.matches("#[0-9A-Fa-f]{6}");
    }

    private static String join(List<String> colors) {
        StringBuilder result = new StringBuilder();
        for (String color : colors) {
            if (result.length() > 0) {
                result.append(',');
            }
            result.append(color);
        }
        return result.toString();
    }
}
