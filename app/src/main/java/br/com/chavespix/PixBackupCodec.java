package br.com.chavespix;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PixBackupCodec {
    public static final int SCHEMA_VERSION = 1;

    private PixBackupCodec() {
    }

    public static JSONObject encode(List<PixCard> cards, String exportedAt)
            throws JSONException {
        JSONObject backup = new JSONObject();
        backup.put("schemaVersion", SCHEMA_VERSION);
        backup.put("exportedAt", exportedAt);
        JSONArray entries = new JSONArray();
        for (PixCard card : cards) {
            JSONObject entry = new JSONObject();
            entry.put("id", card.getId());
            entry.put("beneficiaryName", card.getBeneficiaryName());
            entry.put("bankName", card.getBankName());
            entry.put("colorArgb", card.getColorArgb());
            putNullable(entry, "city", card.getCity());
            putNullable(entry, "cpf", card.getCpf());
            putNullable(entry, "phone", card.getPhone());
            putNullable(entry, "email", card.getEmail());
            putNullable(entry, "random1", card.getRandom1());
            putNullable(entry, "random2", card.getRandom2());
            entries.put(entry);
        }
        backup.put("cards", entries);
        return backup;
    }

    public static List<PixCard> decode(String source) throws JSONException {
        JSONObject backup = new JSONObject(source);
        if (backup.optInt("schemaVersion", -1) != SCHEMA_VERSION) {
            throw new JSONException("Unsupported Pix backup schema");
        }
        String exportedAt = backup.optString("exportedAt", "");
        if (!exportedAt.matches(
                "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}([.]\\d+)?(Z|[+-]\\d{2}:\\d{2})"
        )) {
            throw new JSONException("Invalid backup export timestamp");
        }
        JSONArray entries = backup.optJSONArray("cards");
        if (entries == null) {
            throw new JSONException("Backup has no cards array");
        }
        List<PixCard> cards = new ArrayList<>();
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.optJSONObject(i);
            if (entry == null) {
                throw new JSONException("Invalid card entry");
            }
            String beneficiary = requiredString(entry, "beneficiaryName");
            String bank = requiredString(entry, "bankName");
            Object colorValue = entry.opt("colorArgb");
            if (!(colorValue instanceof Number)) {
                throw new JSONException("Missing card color");
            }
            int color = ((Number) colorValue).intValue();
            String city = optionalString(entry, "city");
            String cpf = optionalString(entry, "cpf");
            String phone = optionalString(entry, "phone");
            if (phone != null) {
                phone = PixKeyValidator.normalizePhoneNational(phone);
            }
            String email = optionalString(entry, "email");
            String random1 = optionalString(entry, "random1");
            String random2 = optionalString(entry, "random2");

            PixCard card = new PixCard(
                    entry.optLong("id", 0),
                    beneficiary,
                    bank,
                    color,
                    city,
                    cpf,
                    phone,
                    email,
                    random1,
                    random2
            );
            validateCard(card);
            cards.add(card);
        }
        return cards;
    }

    private static void validateCard(PixCard card) throws JSONException {
        if (card.getKeyCount() == 0) {
            throw new JSONException("Card must contain a Pix key");
        }
        Set<String> values = new HashSet<>();
        for (PixCard.KeyEntry key : card.getKeys()) {
            if (!PixKeyValidator.isValid(key.getType(), key.getValue())) {
                throw new JSONException("Invalid Pix key in backup");
            }
            String canonical = PixKeyValidator.canonicalValue(key.getType(), key.getValue());
            if (!values.add(canonical.toLowerCase(java.util.Locale.ROOT))) {
                throw new JSONException("Duplicate Pix keys in card");
            }
        }
    }

    private static String requiredString(JSONObject object, String name) throws JSONException {
        Object value = object.opt(name);
        if (!(value instanceof String) || ((String) value).trim().isEmpty()) {
            throw new JSONException("Missing " + name);
        }
        return ((String) value).trim();
    }

    private static String optionalString(JSONObject object, String name) throws JSONException {
        if (!object.has(name) || object.isNull(name)) {
            return null;
        }
        Object value = object.opt(name);
        if (!(value instanceof String)) {
            throw new JSONException("Invalid " + name);
        }
        String text = ((String) value).trim();
        return text.isEmpty() ? null : text;
    }

    private static void putNullable(JSONObject object, String name, String value)
            throws JSONException {
        object.put(name, value == null ? JSONObject.NULL : value);
    }
}
