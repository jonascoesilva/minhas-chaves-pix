package br.com.chavespix;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.zxing.WriterException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public final class QrActivity extends AppCompatActivity {
    public static final String EXTRA_ID = "pix_key_id";
    public static final String EXTRA_BENEFICIARY = "pix_key_beneficiary";
    public static final String EXTRA_BANK = "pix_key_bank";
    public static final String EXTRA_KEY = "pix_key_value";
    public static final String EXTRA_TYPE = "pix_key_type";
    public static final String EXTRA_CITY = "pix_key_city";
    public static final String EXTRA_CARD_COLOR = "pix_card_color";

    private static final String PREFERENCES = "pix_card_preferences";
    private static final String BRIGHTNESS_KEY = "qr_brightness_percent";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable regenerateRunnable = this::regenerateQr;

    private String beneficiary;
    private String bank;
    private String key;
    private String city;
    private String type;
    private Bitmap qrBitmap;
    private LinearLayout qrRoot;
    private LinearLayout qrTopBar;
    private LinearLayout qrActionBar;
    private ImageView qrImage;
    private androidx.core.widget.NestedScrollView qrContentScroll;
    private TextView amountDisplay;
    private TextView disclaimer;
    private TextInputLayout amountLayout;
    private TextInputEditText amountInput;
    private MaterialButton copyPayloadButton;
    private MaterialButton shareQrButton;
    private String currentPayload;
    private int brightnessPercent = 70;
    private boolean formattingAmount;
    private boolean keyboardVisible;
    private int systemTopInset;
    private int systemBottomInset;
    private int imeBottomInset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr);
        bindLayout();
        applyQrWindowInsets();

        beneficiary = getIntent().getStringExtra(EXTRA_BENEFICIARY);
        bank = getIntent().getStringExtra(EXTRA_BANK);
        key = getIntent().getStringExtra(EXTRA_KEY);
        String displayKey = key;
        city = getIntent().getStringExtra(EXTRA_CITY);
        type = PixKeyFormatter.normalizeType(getIntent().getStringExtra(EXTRA_TYPE));
        if (beneficiary == null || bank == null || key == null || key.trim().isEmpty()
                || beneficiary.trim().isEmpty()) {
            Toast.makeText(this, R.string.qr_data_missing, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        key = PixPayloadBuilder.formatPixKey(type, key);
        amountDisplay = findViewById(R.id.amount_display);
        disclaimer = findViewById(R.id.qr_disclaimer);
        amountLayout = findViewById(R.id.amount_layout);
        amountInput = findViewById(R.id.amount_input);
        copyPayloadButton = findViewById(R.id.copy_payload_button);
        shareQrButton = findViewById(R.id.share_qr_button);
        ImageButton backButton = findViewById(R.id.qr_back_button);
        backButton.setOnClickListener(view -> finish());
        ((TextView) findViewById(R.id.beneficiary)).setText(beneficiary);
        ((TextView) findViewById(R.id.bank)).setText(bank);
        ((TextView) findViewById(R.id.pix_key)).setText(
                PixKeyFormatter.formatForQrDisplay(type, displayKey)
        );
        ((TextView) findViewById(R.id.key_type)).setText(
                type == null ? "" : PixKeyValidator.typeLabel(type)
        );
        int cardColor = getIntent().getIntExtra(EXTRA_CARD_COLOR, getColor(R.color.pix_teal));
        ((TextView) findViewById(R.id.bank)).setTextColor(cardColor);

        brightnessPercent = getSharedPreferences(PREFERENCES, MODE_PRIVATE)
                .getInt(BRIGHTNESS_KEY, 70);
        SeekBar brightness = findViewById(R.id.brightness_slider);
        brightness.setProgress(Math.max(0, Math.min(90, brightnessPercent - 10)));
        brightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    brightnessPercent = progress + 10;
                    persistBrightness();
                    applyBrightness();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        amountInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (formattingAmount) {
                    return;
                }
                String digits = PixKeyValidator.digitsOnly(s.toString());
                if (digits.length() > 11) {
                    digits = digits.substring(0, 11);
                }
                String formatted = digits.isEmpty() ? "" : formatCurrencyInput(digits);
                if (!formatted.equals(s.toString())) {
                    formattingAmount = true;
                    amountInput.setText(formatted);
                    amountInput.setSelection(formatted.length());
                    formattingAmount = false;
                }
                updateAmountState();
                copyPayloadButton.setEnabled(false);
                shareQrButton.setEnabled(false);
                handler.removeCallbacks(regenerateRunnable);
                handler.postDelayed(regenerateRunnable, 300);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });
        amountInput.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus && keyboardVisible) {
                scrollAmountIntoView();
            }
        });
        amountInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE
                    || (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER)) {
                hideKeyboard();
                amountInput.clearFocus();
                return true;
            }
            return false;
        });

        copyPayloadButton.setOnClickListener(view -> copyPayload());
        shareQrButton.setOnClickListener(view -> shareQrImage());
        qrRoot.addOnLayoutChangeListener((view, left, top, right, bottom,
                                          oldLeft, oldTop, oldRight, oldBottom) -> {
            if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) {
                updateQrImageSize();
            }
        });
        qrRoot.post(this::updateQrImageSize);
        updateAmountState();
        regenerateQr();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        applyBrightness();
    }

    @Override
    protected void onPause() {
        restoreSystemBrightness();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(regenerateRunnable);
        recycleQr();
        super.onDestroy();
    }

    private void bindLayout() {
        qrRoot = findViewById(R.id.qr_root);
        qrTopBar = findViewById(R.id.qr_top_bar);
        qrActionBar = findViewById(R.id.qr_action_bar);
        qrImage = findViewById(R.id.qr_image);
        qrContentScroll = findViewById(R.id.qr_content_scroll);
    }

    private void applyQrWindowInsets() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
        } else {
            getWindow().setNavigationBarColor(getColor(R.color.pix_teal_dark));
        }
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), qrRoot);
        boolean darkTheme = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        controller.setAppearanceLightStatusBars(!darkTheme);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            controller.setAppearanceLightNavigationBars(!darkTheme);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }

        ViewCompat.setOnApplyWindowInsetsListener(qrRoot, (view, windowInsets) -> {
            Insets systemInsets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );
            Insets imeInsets = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            boolean imeVisible = imeInsets.bottom > systemInsets.bottom;
            boolean keyboardChanged = keyboardVisible != imeVisible;
            if (keyboardChanged) {
                android.transition.AutoTransition transition =
                        new android.transition.AutoTransition();
                transition.setDuration(220);
                android.transition.TransitionManager.beginDelayedTransition(qrRoot, transition);
                keyboardVisible = imeVisible;
                qrActionBar.setVisibility(imeVisible ? View.GONE : View.VISIBLE);
            }

            systemTopInset = systemInsets.top;
            systemBottomInset = systemInsets.bottom;
            imeBottomInset = imeInsets.bottom;
            qrRoot.setPadding(
                    systemInsets.left,
                    0,
                    systemInsets.right,
                    imeVisible ? imeInsets.bottom : 0
            );
            qrTopBar.setPadding(
                    dp(16),
                    dp(8) + systemInsets.top,
                    dp(16),
                    dp(8)
            );
            qrActionBar.setPadding(
                    0,
                    0,
                    0,
                    imeVisible ? 0 : systemInsets.bottom
            );
            qrRoot.post(this::updateQrImageSize);
            if (keyboardChanged && imeVisible && amountInput != null && amountInput.hasFocus()) {
                scrollAmountIntoView();
            }
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(qrRoot);
    }

    private void updateQrImageSize() {
        if (qrRoot == null || qrImage == null || qrRoot.getWidth() == 0
                || qrRoot.getHeight() == 0) {
            return;
        }
        int widthAvailable = qrRoot.getWidth()
                - qrRoot.getPaddingLeft()
                - qrRoot.getPaddingRight()
                - dp(48);
        int heightAvailable = qrRoot.getHeight()
                - qrRoot.getPaddingTop()
                - qrRoot.getPaddingBottom()
                - systemTopInset;
        if (!keyboardVisible) {
            heightAvailable -= systemBottomInset;
        } else {
            heightAvailable = Math.min(heightAvailable, imeBottomInset > 0
                    ? qrRoot.getHeight() - systemTopInset - imeBottomInset
                    : heightAvailable);
        }
        int heightLimit = Math.round(Math.max(0, heightAvailable)
                * (keyboardVisible ? 0.25f : 0.38f));
        int size = Math.min(widthAvailable, Math.max(dp(64), heightLimit));
        if (size <= 0) {
            return;
        }
        android.view.ViewGroup.LayoutParams params = qrImage.getLayoutParams();
        if (params.width != size || params.height != size) {
            params.width = size;
            params.height = size;
            qrImage.setLayoutParams(params);
        }
    }

    private void scrollAmountIntoView() {
        qrContentScroll.post(() -> {
            int[] fieldLocation = new int[2];
            int[] scrollLocation = new int[2];
            amountLayout.getLocationInWindow(fieldLocation);
            qrContentScroll.getLocationInWindow(scrollLocation);
            int overflow = fieldLocation[1] + amountLayout.getHeight()
                    - (scrollLocation[1] + qrContentScroll.getHeight())
                    + dp(20);
            if (overflow > 0) {
                qrContentScroll.smoothScrollBy(0, overflow);
            }
        });
    }

    private void hideKeyboard() {
        InputMethodManager inputMethodManager =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null && amountInput.getWindowToken() != null) {
            inputMethodManager.hideSoftInputFromWindow(amountInput.getWindowToken(), 0);
        }
    }

    private void updateAmountState() {
        String digits = PixKeyValidator.digitsOnly(textOf(amountInput));
        if (digits.isEmpty()) {
            amountLayout.setError(null);
            amountDisplay.setVisibility(View.GONE);
            disclaimer.setText(PixQrText.disclaimerFor(null));
            return;
        }
        BigDecimal amount = new BigDecimal(digits).movePointLeft(2);
        amountDisplay.setVisibility(View.VISIBLE);
        String formattedAmount = formatCurrency(amount);
        amountDisplay.setText(formattedAmount);
        boolean valid = amount.compareTo(BigDecimal.ZERO) > 0
                && amount.compareTo(new BigDecimal("10000000")) < 0;
        amountLayout.setError(valid ? null : getString(R.string.invalid_amount));
        disclaimer.setText(valid
                ? PixQrText.disclaimerFor(formattedAmount)
                : getString(R.string.qr_disclaimer_invalid));
    }

    private void regenerateQr() {
        String digits = PixKeyValidator.digitsOnly(textOf(amountInput));
        BigDecimal amount = digits.isEmpty()
                ? null
                : new BigDecimal(digits).movePointLeft(2);
        if (amount != null && (amount.compareTo(BigDecimal.ZERO) <= 0
                || amount.compareTo(new BigDecimal("10000000")) >= 0)) {
            currentPayload = null;
            copyPayloadButton.setEnabled(false);
            shareQrButton.setEnabled(false);
            recycleQr();
            qrImage.setImageDrawable(null);
            return;
        }
        try {
            currentPayload = PixPayloadBuilder.buildStaticPayload(
                    type,
                    key,
                    beneficiary,
                    city,
                    amount
            );
            Bitmap generated = QrCodeUtils.createKeyBitmap(currentPayload, 900);
            recycleQr();
            qrBitmap = generated;
            qrImage.setImageBitmap(qrBitmap);
            copyPayloadButton.setEnabled(true);
            shareQrButton.setEnabled(true);
        } catch (WriterException | IllegalArgumentException error) {
            Toast.makeText(this, R.string.qr_error, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void copyPayload() {
        if (currentPayload == null) {
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("Pix copia e cola", currentPayload));
            Toast.makeText(this, R.string.pix_payload_copied, Toast.LENGTH_SHORT).show();
        }
    }

    private void shareQrImage() {
        if (qrBitmap == null) {
            return;
        }
        try {
            File directory = new File(getCacheDir(), "qr");
            if (!directory.exists() && !directory.mkdirs()) {
                throw new IOException("Não foi possível criar a pasta temporária do QR.");
            }
            File imageFile = new File(directory, "pix-key-" + System.currentTimeMillis() + ".png");
            Bitmap shareBitmap = createShareBitmap();
            try (FileOutputStream output = new FileOutputStream(imageFile)) {
                if (!shareBitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                    throw new IOException("Falha ao codificar a imagem QR.");
                }
            } finally {
                shareBitmap.recycle();
            }

            Uri imageUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    imageFile
            );
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/png");
            intent.putExtra(Intent.EXTRA_STREAM, imageUri);
            intent.setClipData(ClipData.newUri(getContentResolver(), "QR Pix", imageUri));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, getString(R.string.share_qr)));
        } catch (IOException error) {
            Toast.makeText(this, R.string.share_error, Toast.LENGTH_LONG).show();
        }
    }

    private Bitmap createShareBitmap() {
        int width = 1000;
        int qrSize = 860;
        int qrTop = 225;
        Bitmap result = Bitmap.createBitmap(width, qrTop + qrSize + 55, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.drawColor(Color.WHITE);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLACK);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT,
                android.graphics.Typeface.BOLD
        ));
        drawFittedText(canvas, paint, beneficiary, width / 2f, 66, 48, 26, width - 80);

        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        drawFittedText(canvas, paint, bank, width / 2f, 121, 34, 22, width - 80);
        if (amountDisplay.getVisibility() == View.VISIBLE
                && amountLayout.getError() == null) {
            drawFittedText(
                    canvas,
                    paint,
                    textOf(amountDisplay),
                    width / 2f,
                    174,
                    30,
                    20,
                    width - 80
            );
        }

        android.graphics.RectF bounds = new android.graphics.RectF(
                (width - qrSize) / 2f,
                qrTop,
                (width + qrSize) / 2f,
                qrTop + qrSize
        );
        canvas.drawBitmap(qrBitmap, null, bounds, paint);
        return result;
    }

    private void drawFittedText(
            Canvas canvas,
            Paint paint,
            String text,
            float centerX,
            float baseline,
            float maximumSize,
            float minimumSize,
            float maximumWidth
    ) {
        paint.setTextSize(maximumSize);
        while (paint.getTextSize() > minimumSize && paint.measureText(text) > maximumWidth) {
            paint.setTextSize(paint.getTextSize() - 1);
        }
        canvas.drawText(text, centerX, baseline, paint);
    }

    private void applyBrightness() {
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        attributes.screenBrightness = brightnessPercent / 100f;
        getWindow().setAttributes(attributes);
    }

    private void restoreSystemBrightness() {
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        attributes.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        getWindow().setAttributes(attributes);
    }

    private void persistBrightness() {
        getSharedPreferences(PREFERENCES, MODE_PRIVATE)
                .edit()
                .putInt(BRIGHTNESS_KEY, brightnessPercent)
                .apply();
    }

    private String formatCurrencyInput(String digits) {
        long cents = Long.parseLong(digits);
        return formatCurrency(BigDecimal.valueOf(cents, 2));
    }

    private String formatCurrency(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(new Locale("pt", "BR"))
                .format(amount)
                .replace('\u00A0', ' ')
                .replace('\u202F', ' ');
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void recycleQr() {
        if (qrBitmap != null && !qrBitmap.isRecycled()) {
            qrBitmap.recycle();
            qrBitmap = null;
        }
    }

    private static String textOf(TextView view) {
        return view.getText() == null ? "" : view.getText().toString();
    }
}
