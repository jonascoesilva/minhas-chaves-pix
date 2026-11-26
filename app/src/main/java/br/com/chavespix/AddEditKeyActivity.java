package br.com.chavespix;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.database.SQLException;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class AddEditKeyActivity extends AppCompatActivity {
    public static final String EXTRA_ID = "key_id";
    public static final String EXTRA_BENEFICIARY = "beneficiary";
    public static final String EXTRA_BANK = "bank";
    public static final String EXTRA_KEY = "pix_key";
    public static final String EXTRA_THEME = "bank_theme";
    public static final String EXTRA_TYPE = "pix_type";
    public static final String EXTRA_CARD_ID = "card_id";
    public static final String EXTRA_COLOR_ARGB = "color_argb";

    private static final String[] BANKS = {
            "Nubank", "Itaú", "Bradesco", "Banco do Brasil", "Caixa", "Santander",
            "Inter", "C6 Bank", "PicPay", "Mercado Pago", "Sicoob", "Sicredi",
            "BTG", "Neon", "Next", "Original", "Will Bank", "PagBank"
    };

    private PixCardDbHelper database;
    private long recordId = -1;
    private PixCard originalCard;
    private String selectedColor = "#006B62";
    private final TextInputLayout[] keyLayouts = new TextInputLayout[5];
    private final TextInputEditText[] keyInputs = new TextInputEditText[5];
    private final String[] keyTypes = PixCard.KEY_TYPES.clone();
    private TextInputLayout beneficiaryLayout;
    private TextInputLayout bankLayout;
    private TextInputLayout cityLayout;
    private MaterialAutoCompleteTextView beneficiaryInput;
    private MaterialAutoCompleteTextView bankInput;
    private TextInputEditText cityInput;
    private KeyColorPaletteView colorPalette;
    private MaterialButton saveButton;
    private TextView title;
    private TextView previewBeneficiary;
    private TextView previewBank;
    private LinearLayout previewKeys;
    private View previewAccent;
    private boolean formatting;
    private boolean updatingColorText;
    private boolean saving;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_key);
        SystemBarInsets.applyWithIme(this, findViewById(R.id.wizard_root));

        database = new PixCardDbHelper(this);
        recordId = getIntent().getLongExtra(
                EXTRA_CARD_ID,
                getIntent().getLongExtra(EXTRA_ID, -1)
        );
        originalCard = recordId > 0 ? database.getById(recordId) : null;
        if (originalCard == null && recordId > 0) {
            recordId = -1;
        }

        bindViews();
        setupBackAndSave();
        populateForm();
        setupSuggestions();
        setupColorPalette();
        setupInputFormatting();
        setupPasteActions();
        addTextWatchers();
        updateForm();
        focusRequestedKey();
    }

    @Override
    protected void onDestroy() {
        if (database != null) {
            database.close();
        }
        super.onDestroy();
    }

    private void bindViews() {
        View page = getLayoutInflater().inflate(
                R.layout.step_pix_key,
                findViewById(R.id.wizard_step_container),
                false
        );
        ((ViewGroup) findViewById(R.id.wizard_step_container)).addView(page);
        page.findViewById(R.id.back_button).setOnClickListener(view -> finish());
        title = page.findViewById(R.id.step_title);
        beneficiaryLayout = page.findViewById(R.id.beneficiary_layout);
        bankLayout = page.findViewById(R.id.bank_layout);
        cityLayout = page.findViewById(R.id.city_layout);
        beneficiaryInput = page.findViewById(R.id.beneficiary_input);
        bankInput = page.findViewById(R.id.bank_input);
        cityInput = page.findViewById(R.id.city_input);
        colorPalette = page.findViewById(R.id.color_palette);
        saveButton = findViewById(R.id.action_button);
        previewBeneficiary = page.findViewById(R.id.preview_beneficiary);
        previewBank = page.findViewById(R.id.preview_bank);
        previewKeys = page.findViewById(R.id.preview_keys);
        previewAccent = page.findViewById(R.id.preview_accent);

        int[] layoutIds = {
                R.id.cpf_layout, R.id.phone_layout, R.id.email_layout,
                R.id.random1_layout, R.id.random2_layout
        };
        int[] inputIds = {
                R.id.cpf_input, R.id.phone_input, R.id.email_input,
                R.id.random1_input, R.id.random2_input
        };
        for (int i = 0; i < keyTypes.length; i++) {
            keyLayouts[i] = page.findViewById(layoutIds[i]);
            keyInputs[i] = page.findViewById(inputIds[i]);
        }
    }

    private void setupBackAndSave() {
        saveButton.setText(R.string.save_card);
        saveButton.setOnClickListener(view -> saveCard());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    private void populateForm() {
        title.setText(originalCard == null ? R.string.new_card_title : R.string.edit_card_title);
        if (originalCard != null) {
            beneficiaryInput.setText(originalCard.getBeneficiaryName(), false);
            bankInput.setText(originalCard.getBankName(), false);
            cityInput.setText(valueOf(originalCard.getCity()));
            selectedColor = colorToHex(originalCard.getColorArgb());
            for (int i = 0; i < keyTypes.length; i++) {
                keyInputs[i].setText(valueOf(originalCard.getKey(keyTypes[i])));
            }
        } else {
            beneficiaryInput.setText(valueOf(getIntent().getStringExtra(EXTRA_BENEFICIARY)), false);
            bankInput.setText(valueOf(getIntent().getStringExtra(EXTRA_BANK)), false);
            selectedColor = colorFor(
                    getIntent().getStringExtra(EXTRA_THEME),
                    getIntent().getIntExtra(EXTRA_COLOR_ARGB, Color.parseColor("#006B62"))
            );
            String legacyKey = valueOf(getIntent().getStringExtra(EXTRA_KEY));
            if (!legacyKey.isEmpty()) {
                String type = valueOf(getIntent().getStringExtra(EXTRA_TYPE));
                int index = keyIndex(type);
                keyInputs[index < 0 ? 0 : index].setText(legacyKey);
            }
        }
    }

    private void setupSuggestions() {
        Set<String> names = new LinkedHashSet<>();
        Set<String> banks = new LinkedHashSet<>();
        for (PixCard card : database.getAll()) {
            if (card.getBeneficiaryName() != null && !card.getBeneficiaryName().isEmpty()) {
                names.add(card.getBeneficiaryName());
            }
            if (card.getBankName() != null && !card.getBankName().isEmpty()) {
                banks.add(card.getBankName());
            }
        }
        banks.addAll(java.util.Arrays.asList(BANKS));
        beneficiaryInput.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                new ArrayList<>(names)
        ));
        bankInput.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                new ArrayList<>(banks)
        ));
        beneficiaryInput.setThreshold(1);
        bankInput.setThreshold(1);
    }

    private void setupColorPalette() {
        colorPalette.setOnColorSelectedListener(color -> {
            selectedColor = color;
            updateForm();
        });
        colorPalette.setOnCustomColorRequestListener(this::showCustomColorSheet);
        colorPalette.setSelectedColor(selectedColor);
    }

    private void setupInputFormatting() {
        addFormatter(keyInputs[0], text -> PixKeyValidator.formatDocument(text));
        addFormatter(keyInputs[1], text -> PixKeyValidator.formatPhone(text));
        addFormatter(keyInputs[2], text ->
                text.replaceAll("\\s+", "").toLowerCase(Locale.ROOT));
        addFormatter(keyInputs[3], text -> text.trim().toLowerCase(Locale.ROOT));
        addFormatter(keyInputs[4], text -> text.trim().toLowerCase(Locale.ROOT));
    }

    private void setupPasteActions() {
        keyLayouts[3].setEndIconOnClickListener(view -> pasteInto(keyInputs[3]));
        keyLayouts[4].setEndIconOnClickListener(view -> pasteInto(keyInputs[4]));
    }

    private void addTextWatchers() {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateForm();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        beneficiaryInput.addTextChangedListener(watcher);
        bankInput.addTextChangedListener(watcher);
        cityInput.addTextChangedListener(watcher);
        for (TextInputEditText input : keyInputs) {
            input.addTextChangedListener(watcher);
        }
    }

    private void addFormatter(TextInputEditText input, Formatter formatter) {
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (formatting) {
                    return;
                }
                String current = editable.toString();
                String formatted = formatter.format(current);
                if (!current.equals(formatted)) {
                    formatting = true;
                    input.setText(formatted);
                    input.setSelection(formatted.length());
                    formatting = false;
                }
            }
        });
    }

    private void updateForm() {
        boolean valid = !textOf(beneficiaryInput).trim().isEmpty()
                && !textOf(bankInput).trim().isEmpty();
        boolean hasKey = false;
        List<String> canonicalValues = new ArrayList<>();

        beneficiaryLayout.setError(null);
        bankLayout.setError(null);
        cityLayout.setError(null);
        for (TextInputLayout layout : keyLayouts) {
            layout.setError(null);
        }

        for (int i = 0; i < keyInputs.length; i++) {
            String value = textOf(keyInputs[i]).trim();
            if (value.isEmpty()) {
                continue;
            }
            hasKey = true;
            String type = keyTypes[i];
            if (!PixKeyValidator.isValid(type, value)) {
                keyLayouts[i].setError(errorFor(type));
                valid = false;
                continue;
            }
            String canonical = canonicalForStorage(type, value);
            if (canonicalValues.contains(canonical)) {
                keyLayouts[i].setError(getString(R.string.duplicate_key));
                valid = false;
                continue;
            }
            canonicalValues.add(canonical);
            PixCard duplicateOwner = database.findDuplicateKeyOwner(type, value, recordId);
            if (duplicateOwner != null) {
                keyLayouts[i].setError(getString(
                        R.string.duplicate_key_in_card,
                        duplicateOwner.getBankName(),
                        duplicateOwner.getBeneficiaryName()
                ));
                valid = false;
            }
        }
        if (!hasKey) {
            valid = false;
        }
        if (saving) {
            valid = false;
        }
        saveButton.setEnabled(valid);
        updateSaveButtonColor();
        updatePreview();
    }

    private void updateSaveButtonColor() {
        int color = Color.parseColor(selectedColor);
        saveButton.setBackgroundTintList(ColorStateList.valueOf(color));
        double luminance = (
                0.2126 * channelLuminance(Color.red(color))
                        + 0.7152 * channelLuminance(Color.green(color))
                        + 0.0722 * channelLuminance(Color.blue(color))
        );
        saveButton.setTextColor(luminance > 0.45 ? Color.BLACK : Color.WHITE);
        previewAccent.setBackgroundColor(color);
    }

    private void updatePreview() {
        String beneficiary = textOf(beneficiaryInput).trim();
        previewBeneficiary.setText(
                beneficiary.isEmpty() ? getString(R.string.beneficiary_hint) : beneficiary
        );
        String bank = textOf(bankInput).trim();
        previewBank.setText(bank.isEmpty() ? getString(R.string.bank_hint) : bank);
        previewKeys.removeAllViews();
        for (int i = 0; i < keyInputs.length; i++) {
            String value = textOf(keyInputs[i]).trim();
            if (value.isEmpty()) {
                continue;
            }
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            TextView label = new TextView(this);
            label.setText(PixKeyValidator.typeLabel(keyTypes[i]));
            label.setTextColor(getColor(R.color.text_secondary));
            label.setTextSize(12);
            row.addView(label, new LinearLayout.LayoutParams(dp(105), -2));
            TextView previewValue = new TextView(this);
            previewValue.setText(maskPreview(keyTypes[i], value));
            previewValue.setTextColor(getColor(R.color.text_primary));
            previewValue.setTextSize(13);
            previewValue.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            previewValue.setMaxLines(1);
            previewValue.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
            row.addView(previewValue, new LinearLayout.LayoutParams(0, -2, 1));
            previewKeys.addView(row);
        }
        if (previewKeys.getChildCount() == 0) {
            TextView hint = new TextView(this);
            hint.setText(R.string.at_least_one_key);
            hint.setTextColor(getColor(R.color.text_secondary));
            hint.setTextSize(13);
            previewKeys.addView(hint);
        }
    }

    private void saveCard() {
        updateForm();
        if (!saveButton.isEnabled()) {
            return;
        }
        saving = true;
        saveButton.setText(R.string.saving);
        updateForm();

        String[] keys = new String[keyTypes.length];
        for (int i = 0; i < keys.length; i++) {
            String input = textOf(keyInputs[i]).trim();
            keys[i] = input.isEmpty() ? null : canonicalForStorage(keyTypes[i], input);
        }
        PixCard card = new PixCard(
                recordId,
                textOf(beneficiaryInput).trim(),
                textOf(bankInput).trim(),
                Color.parseColor(selectedColor),
                emptyToNull(textOf(cityInput).trim()),
                keys[0],
                keys[1],
                keys[2],
                keys[3],
                keys[4]
        );
        boolean saved;
        try {
            saved = originalCard == null
                    ? (recordId = database.insert(card)) > 0
                    : database.update(card);
        } catch (SQLException error) {
            saved = false;
        }
        if (!saved) {
            saving = false;
            saveButton.setText(R.string.save_card);
            updateForm();
            Toast.makeText(this, R.string.storage_error, Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, R.string.card_saved, Toast.LENGTH_SHORT).show();
        hideKeyboard();
        finish();
    }

    private void showCustomColorSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(20), dp(24), dp(24));

        TextView heading = new TextView(this);
        heading.setText(R.string.custom_color);
        heading.setTextColor(getColor(R.color.text_primary));
        heading.setTextSize(21);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        content.addView(heading);

        ColorWheelView wheel = new ColorWheelView(this);
        wheel.setContentDescription(getString(R.string.color_wheel_description));
        LinearLayout.LayoutParams wheelParams = new LinearLayout.LayoutParams(dp(220), dp(220));
        wheelParams.gravity = Gravity.CENTER_HORIZONTAL;
        wheelParams.topMargin = dp(12);
        content.addView(wheel, wheelParams);
        wheel.setColor(Color.parseColor(selectedColor));

        BrightnessBarView brightness = new BrightnessBarView(this);
        brightness.setContentDescription(getString(R.string.brightness_description));
        brightness.setBrightness(wheel.getBrightness());
        brightness.setGradientColor(wheel.getFullBrightnessColor());
        brightness.setSelectedColor(wheel.getColor());
        content.addView(brightness, new LinearLayout.LayoutParams(-1, dp(48)));

        LinearLayout previews = new LinearLayout(this);
        previews.setGravity(Gravity.CENTER_VERTICAL);
        previews.setOrientation(LinearLayout.HORIZONTAL);
        TextView currentLabel = new TextView(this);
        currentLabel.setText(getString(R.string.current_color));
        currentLabel.setTextColor(getColor(R.color.text_secondary));
        previews.addView(currentLabel);
        View currentDot = colorDot(Color.parseColor(selectedColor));
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(32), dp(32));
        dotParams.setMarginStart(dp(8));
        dotParams.setMarginEnd(dp(18));
        previews.addView(currentDot, dotParams);
        TextView candidateLabel = new TextView(this);
        candidateLabel.setText(getString(R.string.preview_color));
        candidateLabel.setTextColor(getColor(R.color.text_secondary));
        previews.addView(candidateLabel);
        View candidateDot = colorDot(wheel.getColor());
        LinearLayout.LayoutParams candidateParams = new LinearLayout.LayoutParams(dp(32), dp(32));
        candidateParams.setMarginStart(dp(8));
        previews.addView(candidateDot, candidateParams);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(-1, -2);
        previewParams.topMargin = dp(14);
        content.addView(previews, previewParams);

        TextInputLayout hexLayout = new TextInputLayout(this);
        hexLayout.setHint(getString(R.string.hex_color));
        hexLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        TextInputEditText hexInput = new TextInputEditText(this);
        hexInput.setSingleLine(true);
        hexInput.setText(selectedColor);
        hexInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        hexLayout.addView(hexInput);
        LinearLayout.LayoutParams hexParams = new LinearLayout.LayoutParams(-1, -2);
        hexParams.topMargin = dp(14);
        content.addView(hexLayout, hexParams);

        final boolean[] changingFromWheel = {false};
        final int[] candidate = {Color.parseColor(selectedColor)};
        wheel.setOnColorChangedListener(color -> {
            candidate[0] = color;
            brightness.setGradientColor(wheel.getFullBrightnessColor());
            brightness.setBrightness(wheel.getBrightness());
            brightness.setSelectedColor(color);
            candidateDot.setBackground(circleDrawable(color));
            changingFromWheel[0] = true;
            hexInput.setText(colorToHex(color));
            changingFromWheel[0] = false;
            hexLayout.setError(null);
        });
        brightness.setOnBrightnessChangedListener(value -> wheel.setBrightness(value));
        hexInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (changingFromWheel[0]) {
                    return;
                }
                String text = s.toString().trim();
                if (text.matches("#?[0-9A-Fa-f]{6}")) {
                    String normalized = text.startsWith("#") ? text : "#" + text;
                    candidate[0] = Color.parseColor(normalized);
                    candidateDot.setBackground(circleDrawable(candidate[0]));
                    hexLayout.setError(null);
                    changingFromWheel[0] = true;
                    wheel.setColor(candidate[0]);
                    changingFromWheel[0] = false;
                    brightness.setBrightness(wheel.getBrightness());
                    brightness.setGradientColor(wheel.getFullBrightnessColor());
                    brightness.setSelectedColor(wheel.getColor());
                } else if (!text.isEmpty()) {
                    hexLayout.setError(getString(R.string.invalid_hex_color));
                } else {
                    hexLayout.setError(getString(R.string.invalid_hex_color));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        LinearLayout buttons = new LinearLayout(this);
        buttons.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        MaterialButton cancel = new MaterialButton(this);
        cancel.setText(R.string.cancel);
        cancel.setOnClickListener(view -> dialog.dismiss());
        buttons.addView(cancel);
        MaterialButton apply = new MaterialButton(this);
        apply.setText(R.string.apply);
        apply.setOnClickListener(view -> {
            if (hexLayout.getError() != null) {
                return;
            }
            selectedColor = colorToHex(candidate[0]);
            colorPalette.addRecentColor(selectedColor);
            updateForm();
            dialog.dismiss();
        });
        LinearLayout.LayoutParams applyParams = new LinearLayout.LayoutParams(-2, -2);
        applyParams.setMarginStart(dp(8));
        buttons.addView(apply, applyParams);
        LinearLayout.LayoutParams buttonsParams = new LinearLayout.LayoutParams(-1, -2);
        buttonsParams.topMargin = dp(16);
        content.addView(buttons, buttonsParams);

        dialog.setContentView(content);
        dialog.show();
    }

    private View colorDot(int color) {
        View dot = new View(this);
        dot.setBackground(circleDrawable(color));
        return dot;
    }

    private android.graphics.drawable.GradientDrawable circleDrawable(int color) {
        android.graphics.drawable.GradientDrawable drawable =
                new android.graphics.drawable.GradientDrawable();
        drawable.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        drawable.setColor(color);
        drawable.setStroke(dp(1), getColor(R.color.divider));
        return drawable;
    }

    private void pasteInto(TextInputEditText input) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null || !clipboard.hasPrimaryClip()) {
            return;
        }
        ClipData clip = clipboard.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) {
            return;
        }
        CharSequence value = clip.getItemAt(0).coerceToText(this);
        if (value != null) {
            input.setText(value.toString().trim());
            input.setSelection(input.length());
        }
    }

    private String errorFor(String type) {
        if (PixKeyValidator.TYPE_CPF.equals(type)) {
            return getString(R.string.invalid_cpf);
        }
        if (PixKeyValidator.TYPE_PHONE.equals(type)) {
            return getString(R.string.invalid_phone);
        }
        if (PixKeyValidator.TYPE_EMAIL.equals(type)) {
            return getString(R.string.invalid_email);
        }
        return getString(R.string.invalid_random_key);
    }

    private String canonicalForStorage(String type, String value) {
        if (PixKeyValidator.TYPE_PHONE.equals(type)) {
            return PixKeyValidator.normalizePhoneNational(value);
        }
        if (PixKeyValidator.TYPE_EMAIL.equals(type)
                || PixKeyValidator.isRandomType(type)) {
            return value.trim().toLowerCase(Locale.ROOT);
        }
        return PixKeyValidator.canonicalValue(type, value);
    }

    private String maskPreview(String type, String value) {
        if (PixKeyValidator.TYPE_CPF.equals(type)) {
            String digits = PixKeyValidator.digitsOnly(value);
            if (digits.length() >= 11) {
                return "***.***." + digits.substring(6, 9) + "-" + digits.substring(9);
            }
        }
        if (PixKeyValidator.TYPE_PHONE.equals(type)) {
            return PixKeyValidator.formatPhone(value);
        }
        if (PixKeyValidator.isRandomType(type) && value.length() > 15) {
            return value.substring(0, 7) + "…" + value.substring(value.length() - 6);
        }
        return value;
    }

    private int keyIndex(String type) {
        if (PixKeyValidator.TYPE_RANDOM.equals(type)) {
            return 3;
        }
        for (int i = 0; i < keyTypes.length; i++) {
            if (keyTypes[i].equals(type)) {
                return i;
            }
        }
        return -1;
    }

    private void focusRequestedKey() {
        String type = valueOf(getIntent().getStringExtra(EXTRA_TYPE));
        int index = keyIndex(type);
        if (originalCard != null && index >= 0) {
            keyInputs[index].requestFocus();
        }
    }

    private String colorFor(String theme, int fallback) {
        if (theme != null && theme.matches("#[0-9A-Fa-f]{6}")) {
            return theme.toUpperCase(Locale.ROOT);
        }
        if (fallback != Color.parseColor("#006B62")) {
            return colorToHex(fallback);
        }
        switch (valueOf(theme).toLowerCase(Locale.ROOT)) {
            case "purple":
                return "#6C63FF";
            case "blue":
                return "#1565C0";
            case "red":
                return "#D81B60";
            case "orange":
                return "#F4511E";
            case "green":
                return "#43A047";
            default:
                return "#006B62";
        }
    }

    private static String colorToHex(int color) {
        return String.format(Locale.ROOT, "#%06X", 0xFFFFFF & color);
    }

    private static String textOf(android.widget.TextView view) {
        return view.getText() == null ? "" : view.getText().toString();
    }

    private static String valueOf(String value) {
        return value == null ? "" : value;
    }

    private static String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }

    private static double channelLuminance(int channel) {
        double normalized = channel / 255.0;
        return normalized <= 0.03928
                ? normalized / 12.92
                : Math.pow((normalized + 0.055) / 1.055, 2.4);
    }

    private void hideKeyboard() {
        View current = getCurrentFocus();
        if (current != null) {
            InputMethodManager manager =
                    (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (manager != null) {
                manager.hideSoftInputFromWindow(current.getWindowToken(), 0);
            }
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private interface Formatter {
        String format(String value);
    }
}
