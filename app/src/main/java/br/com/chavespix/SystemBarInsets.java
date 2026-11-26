package br.com.chavespix;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public final class SystemBarInsets {
    private SystemBarInsets() {
    }

    public static void apply(Activity activity, View root) {
        apply(activity, root, false);
    }

    public static void applyWithIme(Activity activity, View root) {
        apply(activity, root, true);
    }

    private static void apply(Activity activity, View root, boolean includeIme) {
        Window window = activity.getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);

        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, root);
        boolean darkTheme = (activity.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        controller.setAppearanceLightStatusBars(!darkTheme);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.setNavigationBarColor(Color.TRANSPARENT);
            controller.setAppearanceLightNavigationBars(!darkTheme);
        } else {
            window.setNavigationBarColor(ContextCompat.getColor(activity, R.color.pix_teal_dark));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarContrastEnforced(false);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.setNavigationBarDividerColor(Color.TRANSPARENT);
        }

        int left = root.getPaddingLeft();
        int top = root.getPaddingTop();
        int right = root.getPaddingRight();
        int bottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets systemInsets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );
            Insets imeInsets = includeIme
                    ? windowInsets.getInsets(WindowInsetsCompat.Type.ime())
                    : Insets.NONE;
            int bottomInset = Math.max(systemInsets.bottom, imeInsets.bottom);
            view.setPadding(
                    left + systemInsets.left,
                    top + systemInsets.top,
                    right + systemInsets.right,
                    bottom + bottomInset
            );
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);
    }
}
