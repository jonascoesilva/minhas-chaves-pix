package br.com.chavespix;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PixCardDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "pix_keys.db";
    private static final int DATABASE_VERSION = 7;
    private static final String TABLE_CARDS = "pix_cards";
    private static final String TABLE_LEGACY_KEYS = "pix_keys";

    public PixCardDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createCardsTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (tableExists(db, TABLE_LEGACY_KEYS)) {
            if (oldVersion < 3) {
                db.execSQL("CREATE TABLE pix_keys_migrated (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "beneficiary TEXT NOT NULL, " +
                        "bank TEXT NOT NULL, " +
                        "pix_key TEXT NOT NULL)");
                db.execSQL("INSERT INTO pix_keys_migrated (id, beneficiary, bank, pix_key) " +
                        "SELECT id, beneficiary, bank, pix_key FROM " + TABLE_LEGACY_KEYS);
                db.execSQL("DROP TABLE " + TABLE_LEGACY_KEYS);
                db.execSQL("ALTER TABLE pix_keys_migrated RENAME TO " + TABLE_LEGACY_KEYS);
            }
            if (oldVersion < 4) {
                db.execSQL("ALTER TABLE " + TABLE_LEGACY_KEYS
                        + " ADD COLUMN bank_theme TEXT NOT NULL DEFAULT 'teal'");
            }
            if (oldVersion < 5) {
                db.execSQL("ALTER TABLE " + TABLE_LEGACY_KEYS
                        + " ADD COLUMN pix_type TEXT NOT NULL DEFAULT 'random'");
                inferLegacyTypes(db);
            }
            if (oldVersion < 6) {
                migrateLegacyRows(db);
            }
        } else {
            createCardsTable(db);
        }
        if (oldVersion < 7 && tableExists(db, TABLE_CARDS)) {
            normalizeStoredPhones(db);
        }
    }

    public long insert(PixCard card) {
        return getWritableDatabase().insertOrThrow(TABLE_CARDS, null, valuesFor(card));
    }

    public boolean update(PixCard card) {
        return getWritableDatabase().update(
                TABLE_CARDS,
                valuesFor(card),
                "id = ?",
                new String[]{String.valueOf(card.getId())}
        ) == 1;
    }

    public PixCard getById(long id) {
        try (Cursor cursor = getReadableDatabase().query(
                TABLE_CARDS,
                cardColumns(),
                "id = ?",
                new String[]{String.valueOf(id)},
                null,
                null,
                null
        )) {
            return cursor.moveToFirst() ? cardFrom(cursor) : null;
        }
    }

    public List<PixCard> getAll() {
        return getAll(getReadableDatabase());
    }

    public boolean hasDuplicateKey(String type, String value, long excludedCardId) {
        return hasDuplicateKey(getReadableDatabase(), type, value, excludedCardId);
    }

    public PixCard findDuplicateKeyOwner(String type, String value, long excludedCardId) {
        return PixCardKeyRules.findDuplicateOwner(
                getAll(getReadableDatabase()),
                type,
                value,
                excludedCardId
        );
    }

    public boolean deleteCard(long id) {
        return getWritableDatabase().delete(
                TABLE_CARDS,
                "id = ?",
                new String[]{String.valueOf(id)}
        ) == 1;
    }

    public ImportSummary mergeCards(List<PixCard> incomingCards) {
        SQLiteDatabase db = getWritableDatabase();
        int imported = 0;
        int ignored = 0;
        db.beginTransaction();
        try {
            for (PixCard incoming : incomingCards) {
                PixCard target = findByIdentity(
                        db,
                        incoming.getBeneficiaryName(),
                        incoming.getBankName()
                );
                if (target == null) {
                    PixCard sanitized = new PixCard(
                            0,
                            incoming.getBeneficiaryName(),
                            incoming.getBankName(),
                            incoming.getColorArgb(),
                            incoming.getCity(),
                            null,
                            null,
                            null,
                            null,
                            null
                    );
                    for (String type : PixCard.KEY_TYPES) {
                        String value = incoming.getKey(type);
                        if (hasValue(value)
                                && !hasDuplicateInCard(sanitized, type, value)
                                && !hasDuplicateKey(db, type, value, -1)) {
                            sanitized = sanitized.withKey(type, value);
                        }
                    }
                    if (sanitized.getKeyCount() == 0) {
                        ignored++;
                        continue;
                    }
                    insert(db, sanitized);
                    imported++;
                    continue;
                }

                PixCard merged = target;
                int addedKeys = 0;
                for (String incomingType : PixCard.KEY_TYPES) {
                    String value = incoming.getKey(incomingType);
                    if (!hasValue(value)) {
                        continue;
                    }
                    String targetType = incomingType;
                    if (hasValue(merged.getKey(targetType))) {
                        if (PixKeyValidator.canonicalValue(targetType, value).equals(
                                PixKeyValidator.canonicalValue(
                                        targetType,
                                        merged.getKey(targetType)
                                )
                        )) {
                            continue;
                        }
                        if (PixKeyValidator.isRandomType(incomingType)) {
                            targetType = PixCard.KEY_TYPES[
                                    incomingType.equals(PixKeyValidator.TYPE_RANDOM1) ? 4 : 3
                            ];
                            if (hasValue(merged.getKey(targetType))) {
                                continue;
                            }
                        } else {
                            continue;
                        }
                    }
                    if (hasDuplicateInCard(merged, targetType, value)
                            || hasDuplicateKey(db, targetType, value, target.getId())) {
                        continue;
                    }
                    merged = merged.withKey(targetType, value);
                    addedKeys++;
                }
                if (addedKeys == 0) {
                    ignored++;
                    continue;
                }
                String city = hasValue(merged.getCity())
                        ? merged.getCity()
                        : incoming.getCity();
                merged = merged.withMetadata(
                        merged.getBeneficiaryName(),
                        merged.getBankName(),
                        merged.getColorArgb(),
                        city
                );
                update(db, merged);
                imported++;
            }
            db.setTransactionSuccessful();
            return new ImportSummary(imported, ignored);
        } finally {
            db.endTransaction();
        }
    }

    public ImportSummary replaceAll(List<PixCard> incomingCards) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE_CARDS, null, null);
            for (PixCard card : incomingCards) {
                if (card.getKeyCount() == 0) {
                    throw new SQLException("Cartão sem chave Pix");
                }
                PixCard accumulated = new PixCard(
                        0,
                        card.getBeneficiaryName(),
                        card.getBankName(),
                        card.getColorArgb(),
                        card.getCity(),
                        null,
                        null,
                        null,
                        null,
                        null
                );
                for (PixCard.KeyEntry key : card.getKeys()) {
                    if (hasDuplicateInCard(accumulated, key.getType(), key.getValue())
                            || hasDuplicateKey(db, key.getType(), key.getValue(), -1)) {
                        throw new SQLException("Chave duplicada no backup");
                    }
                    accumulated = accumulated.withKey(key.getType(), key.getValue());
                }
                insert(db, card.withId(0));
            }
            db.setTransactionSuccessful();
            return new ImportSummary(incomingCards.size(), 0);
        } finally {
            db.endTransaction();
        }
    }

    public void deleteAll() {
        getWritableDatabase().delete(TABLE_CARDS, null, null);
    }

    private void createCardsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CARDS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "beneficiary_name TEXT NOT NULL, " +
                "bank_name TEXT NOT NULL, " +
                "color_argb INTEGER NOT NULL, " +
                "city TEXT, " +
                "cpf TEXT, " +
                "phone TEXT, " +
                "email TEXT, " +
                "random1 TEXT, " +
                "random2 TEXT, " +
                "CHECK(cpf IS NOT NULL OR phone IS NOT NULL OR email IS NOT NULL " +
                "OR random1 IS NOT NULL OR random2 IS NOT NULL))");
    }

    private void migrateLegacyRows(SQLiteDatabase db) {
        createCardsTable(db);
        try (Cursor cursor = db.query(
                TABLE_LEGACY_KEYS,
                new String[]{"id", "beneficiary", "bank", "pix_key", "bank_theme", "pix_type"},
                null,
                null,
                null,
                null,
                null
        )) {
            while (cursor.moveToNext()) {
                PixCard migrated = PixCardMigration.fromLegacy(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getString(5)
                );
                insert(db, migrated);
            }
        }
        db.execSQL("DROP TABLE " + TABLE_LEGACY_KEYS);
    }

    private void inferLegacyTypes(SQLiteDatabase db) {
        List<Long> ids = new ArrayList<>();
        List<String> types = new ArrayList<>();
        try (Cursor cursor = db.query(
                TABLE_LEGACY_KEYS,
                new String[]{"id", "pix_key"},
                null,
                null,
                null,
                null,
                null
        )) {
            while (cursor.moveToNext()) {
                ids.add(cursor.getLong(0));
                types.add(PixKeyValidator.inferType(cursor.getString(1)));
            }
        }
        for (int i = 0; i < ids.size(); i++) {
            ContentValues values = new ContentValues();
            values.put("pix_type", types.get(i));
            db.update(TABLE_LEGACY_KEYS, values, "id = ?", new String[]{
                    String.valueOf(ids.get(i))
            });
        }
    }

    private List<PixCard> getAll(SQLiteDatabase db) {
        List<PixCard> cards = new ArrayList<>();
        try (Cursor cursor = db.query(
                TABLE_CARDS,
                cardColumns(),
                null,
                null,
                null,
                null,
                "beneficiary_name COLLATE NOCASE ASC, bank_name COLLATE NOCASE ASC, id ASC"
        )) {
            while (cursor.moveToNext()) {
                cards.add(cardFrom(cursor));
            }
        }
        return cards;
    }

    private PixCard findByIdentity(SQLiteDatabase db, String beneficiary, String bank) {
        String normalizedBeneficiary = normalizeIdentity(beneficiary);
        String normalizedBank = normalizeIdentity(bank);
        for (PixCard card : getAll(db)) {
            if (normalizeIdentity(card.getBeneficiaryName()).equals(normalizedBeneficiary)
                    && normalizeIdentity(card.getBankName()).equals(normalizedBank)) {
                return card;
            }
        }
        return null;
    }

    private boolean hasDuplicateKey(
            SQLiteDatabase db,
            String type,
            String value,
            long excludedCardId
    ) {
        return PixCardKeyRules.findDuplicateOwner(
                getAll(db),
                type,
                value,
                excludedCardId
        ) != null;
    }

    private void normalizeStoredPhones(SQLiteDatabase db) {
        List<Long> ids = new ArrayList<>();
        List<String> phones = new ArrayList<>();
        try (Cursor cursor = db.query(
                TABLE_CARDS,
                new String[]{"id", "phone"},
                "phone IS NOT NULL",
                null,
                null,
                null,
                null
        )) {
            while (cursor.moveToNext()) {
                ids.add(cursor.getLong(0));
                phones.add(cursor.getString(1));
            }
        }
        for (int i = 0; i < ids.size(); i++) {
            String phone = phones.get(i);
            String national = PixKeyValidator.normalizePhoneNational(phone);
            if (!national.isEmpty() && !national.equals(phone)) {
                ContentValues values = new ContentValues();
                values.put("phone", national);
                db.update(TABLE_CARDS, values, "id = ?", new String[]{
                        String.valueOf(ids.get(i))
                });
            }
        }
    }

    private boolean hasDuplicateInCard(PixCard card, String type, String value) {
        String normalized = PixKeyValidator.canonicalValue(type, value);
        for (PixCard.KeyEntry key : card.getKeys()) {
            if (normalized.equalsIgnoreCase(
                    PixKeyValidator.canonicalValue(key.getType(), key.getValue())
            )) {
                return true;
            }
        }
        return false;
    }

    private long insert(SQLiteDatabase db, PixCard card) {
        return db.insertOrThrow(TABLE_CARDS, null, valuesFor(card));
    }

    private void update(SQLiteDatabase db, PixCard card) {
        db.update(
                TABLE_CARDS,
                valuesFor(card),
                "id = ?",
                new String[]{String.valueOf(card.getId())}
        );
    }

    private PixCard cardFrom(Cursor cursor) {
        return new PixCard(
                cursor.getLong(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getInt(3),
                cursor.isNull(4) ? null : cursor.getString(4),
                cursor.isNull(5) ? null : cursor.getString(5),
                cursor.isNull(6) ? null : cursor.getString(6),
                cursor.isNull(7) ? null : cursor.getString(7),
                cursor.isNull(8) ? null : cursor.getString(8),
                cursor.isNull(9) ? null : cursor.getString(9)
        );
    }

    private ContentValues valuesFor(PixCard card) {
        ContentValues values = new ContentValues();
        if (card.getId() > 0) {
            values.put("id", card.getId());
        }
        values.put("beneficiary_name", card.getBeneficiaryName());
        values.put("bank_name", card.getBankName());
        values.put("color_argb", card.getColorArgb());
        putNullable(values, "city", card.getCity());
        putNullable(values, "cpf", card.getCpf());
        putNullable(values, "phone", card.getPhone());
        putNullable(values, "email", card.getEmail());
        putNullable(values, "random1", card.getRandom1());
        putNullable(values, "random2", card.getRandom2());
        return values;
    }

    private void putNullable(ContentValues values, String name, String value) {
        if (value == null || value.trim().isEmpty()) {
            values.putNull(name);
        } else {
            values.put(name, value);
        }
    }

    private String[] cardColumns() {
        return new String[]{
                "id",
                "beneficiary_name",
                "bank_name",
                "color_argb",
                "city",
                "cpf",
                "phone",
                "email",
                "random1",
                "random2"
        };
    }

    private boolean tableExists(SQLiteDatabase db, String table) {
        try (Cursor cursor = db.rawQuery(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?",
                new String[]{table}
        )) {
            return cursor.moveToFirst();
        }
    }

    private static String normalizeIdentity(String value) {
        return value == null
                ? ""
                : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static boolean hasValue(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static final class ImportSummary {
        private final int imported;
        private final int ignored;

        public ImportSummary(int imported, int ignored) {
            this.imported = imported;
            this.ignored = ignored;
        }

        public int getImported() {
            return imported;
        }

        public int getIgnored() {
            return ignored;
        }
    }
}
