package br.com.chavespix;

import java.util.Locale;

public final class PixKeyFormatter {
    private PixKeyFormatter() {
    }

    public static String normalizeType(String type) {
        if (type == null) {
            return null;
        }
        String normalized = type.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "telefone":
            case "celular":
                return PixKeyValidator.TYPE_PHONE;
            case "e-mail":
                return PixKeyValidator.TYPE_EMAIL;
            case "chave aleatória":
                return PixKeyValidator.TYPE_RANDOM;
            default:
                return normalized;
        }
    }

    public static String formatForDisplay(String type, String value) {
        if (value == null || value.isEmpty()) {
            return value == null ? "" : value;
        }
        String normalizedType = normalizeType(type);
        if (PixKeyValidator.TYPE_CPF.equals(normalizedType)) {
            String digits = PixKeyValidator.digitsOnly(value);
            return digits.length() == 11 ? PixKeyValidator.formatCpf(digits) : value;
        }
        if (PixKeyValidator.TYPE_PHONE.equals(normalizedType)) {
            String nationalDigits = PixKeyValidator.normalizePhoneNational(value);
            return nationalDigits.length() == 11
                    ? PixKeyValidator.formatPhone(nationalDigits)
                    : value;
        }
        return value;
    }

    public static String formatForQrDisplay(String type, String value) {
        if (!PixKeyValidator.TYPE_CPF.equals(normalizeType(type))) {
            return formatForDisplay(type, value);
        }
        if (value == null || value.isEmpty()) {
            return value == null ? "" : value;
        }

        String digits = PixKeyValidator.digitsOnly(value);
        if (digits.length() != 11) {
            return "***.***.***-**";
        }
        return "***." + digits.substring(3, 6)
                + "." + digits.substring(6, 9)
                + "-**";
    }

    public static String formatForSummary(String type, String value) {
        if (value == null) {
            return "";
        }
        String normalizedType = normalizeType(type);
        if (PixKeyValidator.TYPE_CPF.equals(normalizedType)) {
            String digits = PixKeyValidator.digitsOnly(value);
            if (digits.length() >= 5) {
                String visibleDigits = digits.substring(digits.length() - 5);
                return "***.***." + visibleDigits.substring(0, 3)
                        + "-" + visibleDigits.substring(3);
            }
        }
        if (PixKeyValidator.TYPE_PHONE.equals(normalizedType)) {
            return formatForDisplay(normalizedType, value);
        }
        if (PixKeyValidator.isRandomType(normalizedType) && value.length() > 13) {
            return value.substring(0, 7) + "…" + value.substring(value.length() - 6);
        }
        return value;
    }
}
