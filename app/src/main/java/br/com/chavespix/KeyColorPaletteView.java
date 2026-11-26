package br.com.chavespix;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class KeyColorPaletteView extends GridLayout {
    public interface OnColorSelectedListener {
        void onColorSelected(String color);
    }

    public interface OnCustomColorRequestListener {
        void onCustomColorRequested();
    }

    private static final List<String> PALETTE = Arrays.asList(
            "#006B62", "#00897B", "#00796B", "#1565C0",
            "#3949AB", "#6C63FF", "#8E24AA", "#D81B60",
            "#E53935", "#F4511E", "#F9A825", "#43A047"
    );

    private final List<String> colors = new ArrayList<>(PALETTE);
    private final List<FrameLayout> swatches = new ArrayList<>();
    private String selectedColor = PALETTE.get(0);
    private OnColorSelectedListener listener;
    private OnCustomColorRequestListener customColorRequestListener;
    private FrameLayout customColorButton;

    public KeyColorPaletteView(Context context) {
        this(context, null);
    }

    public KeyColorPaletteView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setColumnCount(6);
        setAlignmentMode(ALIGN_MARGINS);
        for (String color : ColorPreferences.getRecentColors(context)) {
            if (!colors.contains(color)) {
                colors.add(color);
            }
        }
        for (String color : colors) {
            addSwatch(color);
        }
        addCustomColorButton();
        updateSelection();
    }

    public void setOnColorSelectedListener(OnColorSelectedListener listener) {
        this.listener = listener;
    }

    public void setOnCustomColorRequestListener(OnCustomColorRequestListener listener) {
        customColorRequestListener = listener;
    }

    public void addRecentColor(String color) {
        if (color == null || !color.matches("#[0-9A-Fa-f]{6}")) {
            return;
        }
        String normalized = color.toUpperCase(Locale.ROOT);
        ColorPreferences.rememberColor(getContext(), normalized);
        if (!colors.contains(normalized)) {
            colors.add(normalized);
            addSwatch(normalized);
        }
        setSelectedColor(normalized);
    }

    public void setSelectedColor(String color) {
        if (color == null || !color.matches("#[0-9A-Fa-f]{6}")) {
            return;
        }
        String normalized = color.toUpperCase(Locale.ROOT);
        if (!colors.contains(normalized)) {
            colors.add(normalized);
            addSwatch(normalized);
        }
        selectedColor = normalized;
        updateSelection();
    }

    public String getSelectedColor() {
        return selectedColor;
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        int columns = Math.max(1, width / dp(48));
        if (getColumnCount() != columns) {
            setColumnCount(columns);
        }
    }

    private void addSwatch(String color) {
        FrameLayout holder = new FrameLayout(getContext());
        int slot = dp(48);
        GridLayout.LayoutParams slotParams = new GridLayout.LayoutParams();
        slotParams.width = slot;
        slotParams.height = slot;
        slotParams.setMargins(dp(1), dp(1), dp(1), dp(1));
        holder.setLayoutParams(slotParams);
        holder.setFocusable(true);
        holder.setClickable(true);

        View dot = new View(getContext());
        FrameLayout.LayoutParams dotParams = new FrameLayout.LayoutParams(dp(40), dp(40));
        dotParams.gravity = Gravity.CENTER;
        dot.setLayoutParams(dotParams);
        dot.setBackground(circle(Color.parseColor(color), Color.WHITE, dp(1)));
        holder.addView(dot);

        TextView check = new TextView(getContext());
        check.setText("✓");
        check.setTextColor(Color.WHITE);
        check.setTextSize(17);
        check.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams checkParams = new FrameLayout.LayoutParams(dp(40), dp(40));
        checkParams.gravity = Gravity.CENTER;
        holder.addView(check, checkParams);

        holder.setContentDescription(color);
        holder.setOnClickListener(view -> {
            selectedColor = color;
            updateSelection();
            if (listener != null) {
                listener.onColorSelected(color);
            }
        });
        addView(holder, customColorButton == null ? getChildCount() : getChildCount() - 1);
        swatches.add(holder);
    }

    private void addCustomColorButton() {
        customColorButton = new FrameLayout(getContext());
        int slot = dp(48);
        GridLayout.LayoutParams slotParams = new GridLayout.LayoutParams();
        slotParams.width = slot;
        slotParams.height = slot;
        slotParams.setMargins(dp(1), dp(1), dp(1), dp(1));
        customColorButton.setLayoutParams(slotParams);
        customColorButton.setFocusable(true);
        customColorButton.setClickable(true);
        customColorButton.setContentDescription(getContext().getString(R.string.custom_color));

        View rainbow = new View(getContext());
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{
                        Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN,
                        Color.BLUE, Color.MAGENTA, Color.RED
                }
        );
        gradient.setShape(GradientDrawable.OVAL);
        rainbow.setBackground(gradient);
        FrameLayout.LayoutParams rainbowParams =
                new FrameLayout.LayoutParams(dp(40), dp(40), Gravity.CENTER);
        customColorButton.addView(rainbow, rainbowParams);

        View center = new View(getContext());
        center.setBackground(circle(Color.WHITE, Color.WHITE, dp(1)));
        FrameLayout.LayoutParams centerParams =
                new FrameLayout.LayoutParams(dp(29), dp(29), Gravity.CENTER);
        customColorButton.addView(center, centerParams);

        TextView plus = new TextView(getContext());
        plus.setText("+");
        plus.setTextColor(Color.rgb(0, 105, 92));
        plus.setTextSize(21);
        plus.setGravity(Gravity.CENTER);
        customColorButton.addView(plus, new FrameLayout.LayoutParams(
                dp(29), dp(29), Gravity.CENTER
        ));
        customColorButton.setOnClickListener(view -> {
            if (customColorRequestListener != null) {
                customColorRequestListener.onCustomColorRequested();
            }
        });
        addView(customColorButton);
    }

    private void updateSelection() {
        for (int i = 0; i < swatches.size(); i++) {
            FrameLayout holder = swatches.get(i);
            View dot = holder.getChildAt(0);
            TextView check = (TextView) holder.getChildAt(1);
            boolean selected = colors.get(i).equals(selectedColor);
            dot.setBackground(circle(
                    Color.parseColor(colors.get(i)),
                    selected ? Color.WHITE : Color.TRANSPARENT,
                    selected ? dp(3) : 0
            ));
            check.setVisibility(selected ? View.VISIBLE : View.GONE);
            holder.setContentDescription(
                    colors.get(i) + (selected ? ", selecionada" : "")
            );
            holder.animate()
                    .scaleX(selected ? 1.04f : 1f)
                    .scaleY(selected ? 1.04f : 1f)
                    .setDuration(120)
                    .start();
            holder.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        }
    }

    private GradientDrawable circle(int fill, int stroke, int width) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(fill);
        if (width > 0) {
            drawable.setStroke(width, stroke);
        }
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
