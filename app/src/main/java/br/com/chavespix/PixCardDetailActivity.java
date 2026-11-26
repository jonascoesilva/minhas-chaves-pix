package br.com.chavespix;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.SQLException;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public final class PixCardDetailActivity extends AppCompatActivity {
    public static final String EXTRA_CARD_ID = "detail_card_id";
    public static final String EXTRA_DELETED_CARD = "deleted_card";

    private PixCardDbHelper database;
    private long cardId;
    private PixCard card;
    private View detailRoot;
    private TextView title;
    private TextView subtitle;
    private View accent;
    private LinearLayout keyRows;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PixThemePreferences.applySavedMode(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pix_card_detail);
        detailRoot = findViewById(R.id.detail_root);
        SystemBarInsets.apply(this, detailRoot);
        database = new PixCardDbHelper(this);
        cardId = getIntent().getLongExtra(EXTRA_CARD_ID, -1);
        title = findViewById(R.id.detail_title);
        subtitle = findViewById(R.id.detail_subtitle);
        accent = findViewById(R.id.detail_accent);
        keyRows = findViewById(R.id.detail_keys);

        findViewById(R.id.back_button).setOnClickListener(view -> finish());
        ImageButton moreButton = findViewById(R.id.detail_more_button);
        moreButton.setOnClickListener(this::showCardMenu);
        reloadCard();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (database != null && cardId > 0) {
            reloadCard();
        }
    }

    @Override
    protected void onDestroy() {
        if (database != null) {
            database.close();
        }
        super.onDestroy();
    }

    private void reloadCard() {
        card = database.getById(cardId);
        if (card == null) {
            finish();
            return;
        }
        title.setText(card.getBeneficiaryName());
        subtitle.setText(card.getBankName());
        accent.setBackgroundColor(card.getColorArgb());
        keyRows.removeAllViews();

        List<PixCard.KeyEntry> keys = card.getKeys();
        for (int i = 0; i < keys.size(); i++) {
            PixCard.KeyEntry key = keys.get(i);
            View row = getLayoutInflater().inflate(R.layout.item_pix_key, keyRows, false);
            TextView value = row.findViewById(R.id.pix_key);
            value.setText(PixKeyFormatter.formatForDisplay(key.getType(), key.getValue()));
            value.setEllipsize(null);
            value.setMaxLines(Integer.MAX_VALUE);

            String label = PixKeyValidator.typeLabel(key.getType());
            if (PixKeyValidator.TYPE_RANDOM2.equals(key.getType())) {
                label += " 2";
            }
            TextView typeLabel = row.findViewById(R.id.key_type);
            typeLabel.setText(label);
            TextView typeIcon = row.findViewById(R.id.key_type_icon);
            typeIcon.setText(iconFor(key.getType()));
            typeIcon.setContentDescription(label);
            typeIcon.setTextColor(card.getColorArgb());
            GradientDrawable iconBackground = new GradientDrawable();
            iconBackground.setShape(GradientDrawable.OVAL);
            iconBackground.setColor(withAlpha(card.getColorArgb(), 28));
            iconBackground.setStroke(dp(1), withAlpha(card.getColorArgb(), 90));
            typeIcon.setBackground(iconBackground);

            row.findViewById(R.id.copy_button).setOnClickListener(view -> copyKey(key));
            row.findViewById(R.id.more_key_button).setOnClickListener(
                    view -> showKeyMenu(view, key)
            );
            keyRows.addView(row);
            if (i < keys.size() - 1) {
                View divider = new View(this);
                divider.setBackgroundColor(ContextCompat.getColor(this, R.color.divider));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(1)
                );
                params.topMargin = dp(4);
                params.bottomMargin = dp(4);
                keyRows.addView(divider, params);
            }
        }
    }

    private void showCardMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(Menu.NONE, 1, Menu.NONE, R.string.edit_card);
        menu.getMenu().add(Menu.NONE, 2, Menu.NONE, R.string.delete_card);
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                openEditor(null);
                return true;
            }
            if (item.getItemId() == 2) {
                confirmDeleteCard();
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void showKeyMenu(View anchor, PixCard.KeyEntry key) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(Menu.NONE, 1, Menu.NONE, R.string.show_qr);
        menu.getMenu().add(Menu.NONE, 2, Menu.NONE, R.string.edit);
        menu.getMenu().add(Menu.NONE, 3, Menu.NONE, R.string.delete);
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                openQr(key);
                return true;
            }
            if (item.getItemId() == 2) {
                openEditor(key.getType());
                return true;
            }
            if (item.getItemId() == 3) {
                confirmDeleteKey(key);
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void confirmDeleteKey(PixCard.KeyEntry key) {
        boolean lastKey = card.getKeyCount() == 1;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_title)
                .setMessage(lastKey ? R.string.last_key_delete_message : R.string.delete_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    if (lastKey) {
                        deleteCardAndReturn();
                    } else {
                        try {
                            if (!database.update(card.withKey(key.getType(), null))) {
                                Toast.makeText(
                                        this,
                                        R.string.storage_error,
                                        Toast.LENGTH_LONG
                                ).show();
                                return;
                            }
                            reloadCard();
                        } catch (SQLException error) {
                            Toast.makeText(this, R.string.storage_error, Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .show();
    }

    private void confirmDeleteCard() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_card)
                .setMessage(R.string.delete_card_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteCardAndReturn())
                .show();
    }

    private void deleteCardAndReturn() {
        try {
            if (!database.deleteCard(card.getId())) {
                finish();
                return;
            }
            Intent result = new Intent();
            result.putExtra(EXTRA_DELETED_CARD, card);
            setResult(RESULT_OK, result);
            finish();
        } catch (SQLException error) {
            Toast.makeText(this, R.string.storage_error, Toast.LENGTH_LONG).show();
        }
    }

    private void copyKey(PixCard.KeyEntry key) {
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("Chave Pix", key.getValue()));
            Snackbar.make(detailRoot, R.string.key_copied, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void openQr(PixCard.KeyEntry key) {
        Intent intent = new Intent(this, QrActivity.class);
        intent.putExtra(QrActivity.EXTRA_BENEFICIARY, card.getBeneficiaryName());
        intent.putExtra(QrActivity.EXTRA_BANK, card.getBankName());
        intent.putExtra(QrActivity.EXTRA_KEY, key.getValue());
        intent.putExtra(QrActivity.EXTRA_TYPE, key.getType());
        intent.putExtra(QrActivity.EXTRA_CITY, card.getCity());
        intent.putExtra(QrActivity.EXTRA_CARD_COLOR, card.getColorArgb());
        startActivity(intent);
    }

    private void openEditor(String focusType) {
        Intent intent = new Intent(this, AddEditKeyActivity.class);
        intent.putExtra(AddEditKeyActivity.EXTRA_CARD_ID, card.getId());
        if (focusType != null) {
            intent.putExtra(AddEditKeyActivity.EXTRA_TYPE, focusType);
        }
        startActivity(intent);
    }

    private static String iconFor(String type) {
        if (PixKeyValidator.TYPE_CPF.equals(type)) {
            return "ID";
        }
        if (PixKeyValidator.TYPE_PHONE.equals(type)) {
            return "☎";
        }
        if (PixKeyValidator.TYPE_EMAIL.equals(type)) {
            return "@";
        }
        return "✦";
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
