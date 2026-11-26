package br.com.chavespix;

public final class PixCardMigration {
    private PixCardMigration() {
    }

    public static PixCard fromLegacy(
            long id,
            String beneficiary,
            String bank,
            String key,
            String themeId,
            String type
    ) {
        String normalizedType = legacyType(type, key);
        int color = legacyColor(themeId);
        PixCard card = new PixCard(
                id,
                beneficiary,
                bank,
                color,
                null,
                null,
                null,
                null,
                null,
                null
        );
        String storedKey = key == null || key.trim().isEmpty() ? null : key.trim();
        if (PixKeyValidator.TYPE_PHONE.equals(normalizedType) && storedKey != null) {
            String nationalPhone = PixKeyValidator.normalizePhoneNational(storedKey);
            if (!nationalPhone.isEmpty()) {
                storedKey = nationalPhone;
            }
        }
        if (PixKeyValidator.TYPE_RANDOM.equals(normalizedType)) {
            return card.withKey(PixKeyValidator.TYPE_RANDOM1, storedKey);
        }
        return card.withKey(normalizedType, storedKey);
    }

    private static String legacyType(String type, String key) {
        if (PixKeyValidator.TYPE_CPF.equals(type)
                || PixKeyValidator.TYPE_PHONE.equals(type)
                || PixKeyValidator.TYPE_EMAIL.equals(type)) {
            return type;
        }
        if (PixKeyValidator.TYPE_RANDOM.equals(type)
                || PixKeyValidator.TYPE_RANDOM1.equals(type)
                || PixKeyValidator.TYPE_RANDOM2.equals(type)) {
            return PixKeyValidator.TYPE_RANDOM;
        }
        return PixKeyValidator.inferType(key);
    }

    private static int legacyColor(String value) {
        if (value != null && value.matches("#[0-9A-Fa-f]{6}")) {
            return (int) (0xFF000000L | Long.parseLong(value.substring(1), 16));
        }
        switch (value == null ? "" : value) {
            case "nubank":
                return 0xFF820AD1;
            case "itau":
                return 0xFFEC7000;
            case "santander":
                return 0xFFCC0000;
            case "bb":
                return 0xFFFFCC00;
            case "caixa":
                return 0xFF0070AF;
            case "picpay":
                return 0xFF21C25E;
            case "stone":
                return 0xFF00A878;
            case "c6inter":
                return 0xFF171717;
            case "tealblue":
                return 0xFF00796B;
            case "violetpink":
                return 0xFF6C63FF;
            case "sunset":
                return 0xFFF97316;
            default:
                return 0xFF006B62;
        }
    }
}
