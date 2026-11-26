package br.com.chavespix;

import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;

public final class PixThemePreferences {
    public static final String SYSTEM = "system";
    public static final String LIGHT = "light";
    public static final String DARK = "dark";
    private static final String PREFERENCES = "pix_card_preferences";
    private static final String THEME_MODE = "theme_mode";

    private PixThemePreferences() {
    }

    public static String getMode(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .getString(THEME_MODE, SYSTEM);
    }

    public static void setMode(Context context, String mode) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit()
                .putString(THEME_MODE, mode)
                .apply();
        applyMode(mode);
    }

    public static void applySavedMode(Context context) {
        applyMode(getMode(context));
    }

    private static void applyMode(String mode) {
        if (LIGHT.equals(mode)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (DARK.equals(mode)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }
}
