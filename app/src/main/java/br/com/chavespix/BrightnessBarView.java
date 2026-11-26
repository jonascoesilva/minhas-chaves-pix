package br.com.chavespix;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public final class BrightnessBarView extends View {
    public interface OnBrightnessChangedListener {
        void onBrightnessChanged(float brightness);
    }

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF trackBounds = new RectF();
    private float brightness = 1f;
    private float trackLeft;
    private float trackRight;
    private float centerY;
    private int gradientColor = Color.rgb(0, 107, 98);
    private int selectedColor = gradientColor;
    private OnBrightnessChangedListener listener;

    public BrightnessBarView(Context context) {
        super(context);
        initialize();
    }

    public BrightnessBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public BrightnessBarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    private void initialize() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setFocusable(true);
        thumbPaint.setStyle(Paint.Style.FILL);
        thumbPaint.setShadowLayer(dp(3), 0f, dp(1), 0x55000000);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dp(1));
        borderPaint.setColor(Color.rgb(177, 188, 184));
        thumbBorderPaint.setStyle(Paint.Style.STROKE);
        thumbBorderPaint.setStrokeWidth(dp(2));
        thumbBorderPaint.setColor(Color.WHITE);
    }

    public void setBrightness(float value) {
        brightness = clamp(value);
        invalidate();
    }

    public float getBrightness() {
        return brightness;
    }

    public void setGradientColor(int color) {
        gradientColor = color;
        invalidate();
    }

    public void setSelectedColor(int color) {
        selectedColor = color;
        invalidate();
    }

    public void setOnBrightnessChangedListener(OnBrightnessChangedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        float thumbRadius = dp(11);
        trackLeft = thumbRadius + dp(2);
        trackRight = Math.max(trackLeft, width - thumbRadius - dp(2));
        centerY = height / 2f;
        float halfTrackHeight = dp(7);
        trackBounds.set(trackLeft, centerY - halfTrackHeight, trackRight, centerY + halfTrackHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        trackPaint.setShader(new LinearGradient(
                trackBounds.left,
                centerY,
                trackBounds.right,
                centerY,
                Color.BLACK,
                gradientColor,
                Shader.TileMode.CLAMP
        ));
        canvas.drawRoundRect(trackBounds, dp(7), dp(7), trackPaint);
        trackPaint.setShader(null);
        canvas.drawRoundRect(trackBounds, dp(7), dp(7), borderPaint);

        float thumbX = trackLeft + brightness * (trackRight - trackLeft);
        float thumbRadius = dp(11);
        thumbPaint.setColor(selectedColor);
        canvas.drawCircle(thumbX, centerY, thumbRadius, thumbPaint);
        canvas.drawCircle(thumbX, centerY, thumbRadius, thumbBorderPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                requestParentDisallowIntercept(true);
                updateBrightness(event.getX());
                return true;
            case MotionEvent.ACTION_MOVE:
                requestParentDisallowIntercept(true);
                updateBrightness(event.getX());
                return true;
            case MotionEvent.ACTION_UP:
                updateBrightness(event.getX());
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

    private void updateBrightness(float x) {
        float range = trackRight - trackLeft;
        setBrightness(range <= 0f ? 0f : (x - trackLeft) / range);
        if (listener != null) {
            listener.onBrightnessChanged(brightness);
        }
    }

    private void requestParentDisallowIntercept(boolean disallow) {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(disallow);
        }
    }

    private float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
