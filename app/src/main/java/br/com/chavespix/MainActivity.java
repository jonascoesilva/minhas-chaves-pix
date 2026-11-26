package br.com.chavespix;

import android.content.Intent;
import android.database.SQLException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends AppCompatActivity implements PixCardAdapter.Listener {
    private PixCardDbHelper database;
    private PixCardAdapter adapter;
    private View emptyState;
    private View mainRoot;
    private FloatingActionButton addButton;
    private ProgressBar backupProgress;
    private final ExecutorService backupExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ActivityResultLauncher<String> createDocumentLauncher;
    private ActivityResultLauncher<String[]> openDocumentLauncher;
    private ActivityResultLauncher<Intent> detailLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PixThemePreferences.applySavedMode(this);
        super.onCreate(savedInstanceState);
        createDocumentLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/json"),
                this::writeBackup
        );
        openDocumentLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                this::readBackup
        );
        detailLauncher = registerForActivityResult(
                new StartActivityForResult(),
                this::onDetailResult
        );
        setContentView(R.layout.activity_main);
        mainRoot = findViewById(R.id.main_root);
        SystemBarInsets.apply(this, mainRoot);

        database = new PixCardDbHelper(this);
        RecyclerView keyList = findViewById(R.id.key_list);
        emptyState = findViewById(R.id.empty_state);
        adapter = new PixCardAdapter(this);
        keyList.setLayoutManager(new LinearLayoutManager(this));
        keyList.setAdapter(adapter);

        addButton = findViewById(R.id.add_button);
        backupProgress = findViewById(R.id.backup_progress);
        addButton.setOnClickListener(view -> openEditor(null, null));
        addButton.setContentDescription(getString(R.string.new_card));
        ImageButton moreButton = findViewById(R.id.more_button);
        moreButton.setOnClickListener(this::showBackupMenu);
        refreshKeys();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (database != null) {
            refreshKeys();
        }
    }

    @Override
    protected void onDestroy() {
        if (database != null) {
            database.close();
        }
        backupExecutor.shutdown();
        super.onDestroy();
    }

    @Override
    public void onOpen(PixCard card) {
        Intent intent = new Intent(this, PixCardDetailActivity.class);
        intent.putExtra(PixCardDetailActivity.EXTRA_CARD_ID, card.getId());
        detailLauncher.launch(intent);
    }

    private void openEditor(PixCard card, PixCard.KeyEntry focusKey) {
        Intent intent = new Intent(this, AddEditKeyActivity.class);
        if (card != null) {
            intent.putExtra(AddEditKeyActivity.EXTRA_CARD_ID, card.getId());
            if (focusKey != null) {
                intent.putExtra(AddEditKeyActivity.EXTRA_TYPE, focusKey.getType());
            }
        }
        startActivity(intent);
    }

    private void showBackupMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(Menu.NONE, 1, Menu.NONE, R.string.export_backup);
        menu.getMenu().add(Menu.NONE, 2, Menu.NONE, R.string.import_backup);
        menu.getMenu().add(Menu.NONE, 3, Menu.NONE, R.string.theme);
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                confirmExport();
                return true;
            }
            if (item.getItemId() == 2) {
                openDocumentLauncher.launch(new String[]{"application/json", "text/json"});
                return true;
            }
            if (item.getItemId() == 3) {
                showThemeDialog();
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void confirmExport() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.export_backup)
                .setMessage(R.string.export_plaintext_warning)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.export_backup, (dialog, which) -> {
                    String date = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
                            .format(new Date());
                    createDocumentLauncher.launch("pix-backup-" + date + ".json");
                })
                .show();
    }

    private void showThemeDialog() {
        String[] modes = {
                PixThemePreferences.DARK,
                PixThemePreferences.LIGHT,
                PixThemePreferences.SYSTEM
        };
        String[] labels = {
                getString(R.string.theme_dark),
                getString(R.string.theme_light),
                getString(R.string.theme_system)
        };
        int selected = 2;
        String current = PixThemePreferences.getMode(this);
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equals(current)) {
                selected = i;
            }
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.theme)
                .setSingleChoiceItems(labels, selected, (dialog, which) -> {
                    PixThemePreferences.setMode(this, modes[which]);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void writeBackup(Uri uri) {
        if (uri == null) {
            return;
        }
        List<PixCard> cards = database.getAll();
        setBackupLoading(true);
        backupExecutor.execute(() -> {
            boolean success = false;
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss'Z'",
                        Locale.ROOT
                );
                dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                String exportedAt = dateFormat.format(new Date());
                String json = PixBackupCodec.encode(cards, exportedAt).toString(2);
                try (OutputStream output = getContentResolver().openOutputStream(uri, "wt")) {
                    if (output == null) {
                        throw new IOException("Unable to open backup destination");
                    }
                    output.write(json.getBytes(StandardCharsets.UTF_8));
                }
                success = true;
            } catch (IOException | JSONException | SQLException error) {
                success = false;
            }
            boolean saved = success;
            mainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                setBackupLoading(false);
                if (saved) {
                    Snackbar.make(mainRoot, R.string.backup_exported, Snackbar.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.backup_export_error, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void readBackup(Uri uri) {
        if (uri == null) {
            return;
        }
        setBackupLoading(true);
        backupExecutor.execute(() -> {
            List<PixCard> cards = null;
            boolean valid = false;
            try (InputStream input = getContentResolver().openInputStream(uri)) {
                if (input == null) {
                    throw new IOException("Unable to open backup source");
                }
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    bytes.write(buffer, 0, count);
                }
                cards = PixBackupCodec.decode(
                        new String(bytes.toByteArray(), StandardCharsets.UTF_8)
                );
                valid = true;
            } catch (IOException | JSONException error) {
                valid = false;
            }
            List<PixCard> importedCards = cards;
            boolean parsed = valid;
            mainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                setBackupLoading(false);
                if (!parsed || importedCards == null) {
                    Toast.makeText(this, R.string.backup_import_error, Toast.LENGTH_LONG).show();
                } else if (importedCards.isEmpty()) {
                    Toast.makeText(this, R.string.backup_empty, Toast.LENGTH_LONG).show();
                } else {
                    showImportOptions(importedCards);
                }
            });
        });
    }

    private void showImportOptions(List<PixCard> cards) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.import_backup)
                .setMessage(R.string.import_backup_options)
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.replace_all, (dialog, which) -> confirmReplace(cards))
                .setPositiveButton(R.string.merge_backup, (dialog, which) -> importCards(cards, false))
                .show();
    }

    private void confirmReplace(List<PixCard> cards) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.replace_all)
                .setMessage(R.string.replace_backup_confirmation)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.replace_all, (dialog, which) -> importCards(cards, true))
                .show();
    }

    private void importCards(List<PixCard> cards, boolean replace) {
        try {
            PixCardDbHelper.ImportSummary summary = replace
                    ? database.replaceAll(cards)
                    : database.mergeCards(cards);
            refreshKeys();
            Snackbar.make(
                    mainRoot,
                    getString(
                            R.string.backup_import_summary,
                            summary.getImported(),
                            summary.getIgnored()
                    ),
                    Snackbar.LENGTH_LONG
            ).show();
        } catch (SQLException error) {
            Toast.makeText(this, R.string.backup_import_error, Toast.LENGTH_LONG).show();
        }
    }

    private void refreshKeys() {
        List<PixCard> cards = database.getAll();
        adapter.setCards(cards);
        emptyState.setVisibility(cards.isEmpty() ? View.VISIBLE : View.GONE);
        addButton.setEnabled(true);
        addButton.setAlpha(1f);
        addButton.setContentDescription(getString(R.string.new_card));
    }

    private void setBackupLoading(boolean loading) {
        backupProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (!loading) {
            refreshKeys();
        }
    }

    private void onDetailResult(ActivityResult result) {
        refreshKeys();
        if (result.getResultCode() != RESULT_OK || result.getData() == null) {
            return;
        }
        PixCard deletedCard = getDeletedCard(result.getData());
        if (deletedCard == null) {
            return;
        }
        Snackbar.make(mainRoot, R.string.card_deleted, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, view -> restoreCard(deletedCard))
                .show();
    }

    @SuppressWarnings("deprecation")
    private PixCard getDeletedCard(Intent data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return data.getSerializableExtra(
                    PixCardDetailActivity.EXTRA_DELETED_CARD,
                    PixCard.class
            );
        }
        Object value = data.getSerializableExtra(PixCardDetailActivity.EXTRA_DELETED_CARD);
        return value instanceof PixCard ? (PixCard) value : null;
    }

    private void restoreCard(PixCard card) {
        try {
            database.insert(card);
            refreshKeys();
        } catch (SQLException error) {
            Toast.makeText(this, R.string.storage_error, Toast.LENGTH_LONG).show();
        }
    }
}
