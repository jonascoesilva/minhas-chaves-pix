package br.com.chavespix;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public final class ColorWheelView extends View {
    public interface OnColorChangedListener {
        void onColorChanged(int color);
    }

    private final Paint wheelPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] hsv = {0f, 1f, 1f};
    private Bitmap wheelBitmap;
    private float radius;
    private OnColorChangedListener listener;

    public ColorWheelView(Context context) {
        super(context);
        initialize();
    }

    public ColorWheelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public ColorWheelView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    private void initialize() {
        setFocusable(true);
    }

    public void setColor(int color) {
        Color.colorToHSV(color, hsv);
        invalidate();
        notifyColorChanged();
    }

    public int getColor() {
        return Color.HSVToColor(hsv);
    }

    public int getFullBrightnessColor() {
        return Color.HSVToColor(new float[]{hsv[0], hsv[1], 1f});
    }

    public float getBrightness() {
        return hsv[2];
    }

    public void setBrightness(float brightness) {
        hsv[2] = clamp(brightness, 0f, 1f);
        invalidate();
        notifyColorChanged();
    }

    public void setOnColorChangedListener(OnColorChangedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desired = dp(180);
        int width = resolveSize(desired, widthMeasureSpec);
        int height = resolveSize(desired, heightMeasureSpec);
        int size = Math.min(width, height);
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        radius = Math.max(0f, Math.min(width, height) / 2f - dp(10));
        createWheelBitmap(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (wheelBitmap == null || radius <= 0f) {
            return;
        }

        ColorMatrix brightness = new ColorMatrix(new float[]{
                hsv[2], 0, 0, 0, 0,
                0, hsv[2], 0, 0, 0,
                0, 0, hsv[2], 0, 0,
                0, 0, 0, 1, 0
        });
        ColorFilter previousFilter = wheelPaint.getColorFilter();
        wheelPaint.setColorFilter(new ColorMatrixColorFilter(brightness));
        canvas.drawBitmap(wheelBitmap, 0f, 0f, wheelPaint);
        wheelPaint.setColorFilter(previousFilter);

        double angle = Math.toRadians(hsv[0]);
        float indicatorX = getWidth() / 2f + (float) Math.cos(angle) * hsv[1] * radius;
        float indicatorY = getHeight() / 2f + (float) Math.sin(angle) * hsv[1] * radius;
        markerPaint.setStyle(Paint.Style.STROKE);
        markerPaint.setStrokeWidth(dp(4));
        markerPaint.setColor(Color.BLACK);
        canvas.drawCircle(indicatorX, indicatorY, dp(10), markerPaint);
        markerPaint.setStrokeWidth(dp(2));
        markerPaint.setColor(Color.WHITE);
        canvas.drawCircle(indicatorX, indicatorY, dp(10), markerPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                requestParentDisallowIntercept(true);
                updateSelection(event.getX(), event.getY());
                return true;
            case MotionEvent.ACTION_MOVE:
                requestParentDisallowIntercept(true);
                updateSelection(event.getX(), event.getY());
                return true;
            case MotionEvent.ACTION_UP:
                updateSelection(event.getX(), event.getY());
                requestParentDisallowIntercept(false);
                performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                requestParentDisallowIntercept(false);
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private void updateSelection(float x, float y) {
        float dx = x - getWidth() / 2f;
        float dy = y - getHeight() / 2f;
        float distance = (float) Math.hypot(dx, dy);
        hsv[0] = (float) Math.toDegrees(Math.atan2(dy, dx));
        if (hsv[0] < 0f) {
            hsv[0] += 360f;
        }
        hsv[1] = radius == 0f ? 0f : clamp(distance / radius, 0f, 1f);
        invalidate();
        notifyColorChanged();
    }

    private void createWheelBitmap(int width, int height) {
        if (width <= 0 || height <= 0 || radius <= 0f) {
            wheelBitmap = null;
            return;
        }
        if (wheelBitmap != null) {
            wheelBitmap.recycle();
        }
        wheelBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[width * height];
        float centerX = width / 2f;
        float centerY = height / 2f;
        float[] color = new float[3];
        for (int y = 0; y < height; y++) {
            float dy = y - centerY;
            for (int x = 0; x < width; x++) {
                float dx = x - centerX;
                float saturation = (float) Math.hypot(dx, dy) / radius;
                int index = y * width + x;
                if (saturation > 1f) {
                    pixels[index] = Color.TRANSPARENT;
                    continue;
                }
                color[0] = (float) ((Math.toDegrees(Math.atan2(dy, dx)) + 360d) % 360d);
                color[1] = saturation;
                color[2] = 1f;
                pixels[index] = Color.HSVToColor(255, color);
            }
        }
        wheelBitmap.setPixels(pixels, 0, width, 0, 0, width, height);
    }

    private void notifyColorChanged() {
        if (listener != null) {
            listener.onColorChanged(getColor());
        }
    }

    private void requestParentDisallowIntercept(boolean disallow) {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(disallow);
        }
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
