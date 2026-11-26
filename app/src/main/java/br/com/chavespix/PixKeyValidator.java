package br.com.chavespix;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class PixKeyValidator {
    public static final String TYPE_CPF = "cpf";
    public static final String TYPE_PHONE = "phone";
    public static final String TYPE_EMAIL = "email";
    public static final String TYPE_RANDOM = "random";
    public static final String TYPE_RANDOM1 = "random1";
    public static final String TYPE_RANDOM2 = "random2";

    private static final Set<String> VALID_DDDS = new HashSet<>(Arrays.asList(
            "11", "12", "13", "14", "15", "16", "17", "18", "19",
            "21", "22", "24", "27", "28",
            "31", "32", "33", "34", "35", "37", "38",
            "41", "42", "43", "44", "45", "46", "47", "48", "49",
            "51", "53", "54", "55",
            "61", "62", "63", "64", "65", "66", "67", "68", "69",
            "71", "73", "74", "75", "77", "79",
            "81", "82", "83", "84", "85", "86", "87", "88", "89",
            "91", "92", "93", "94", "95", "96", "97", "98", "99"
    ));

    private PixKeyValidator() {
    }

    public static boolean isValid(String type, String value) {
        if (type == null || value == null) {
            return false;
        }
        switch (type) {
            case TYPE_CPF:
                return isValidDocument(value);
            case TYPE_PHONE:
                return isValidPhone(value);
            case TYPE_EMAIL:
                return isValidEmail(value);
            case TYPE_RANDOM:
            case TYPE_RANDOM1:
            case TYPE_RANDOM2:
                return isValidRandomKey(value);
            default:
                return false;
        }
    }

    public static boolean isValidCpf(String value) {
        String digits = digitsOnly(value);
        if (digits.length() != 11 || hasRepeatedDigits(digits)) {
            return false;
        }
        int first = cpfDigit(digits, 9, 10);
        int second = cpfDigit(digits.substring(0, 9) + first, 10, 11);
        return digits.charAt(9) - '0' == first && digits.charAt(10) - '0' == second;
    }

    public static boolean isValidDocument(String value) {
        String digits = digitsOnly(value);
        return digits.length() == 14 ? isValidCnpj(digits) : isValidCpf(digits);
    }

    public static boolean isValidCnpj(String value) {
        String digits = digitsOnly(value);
        if (digits.length() != 14 || hasRepeatedDigits(digits)) {
            return false;
        }
        int[] firstWeights = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] secondWeights = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int first = cnpjDigit(digits, firstWeights);
        int second = cnpjDigit(digits.substring(0, 12) + first, secondWeights);
        return digits.charAt(12) - '0' == first && digits.charAt(13) - '0' == second;
    }

    public static boolean isValidPhone(String value) {
        String digits = normalizePhoneNational(value);
        return digits.length() == 11
                && VALID_DDDS.contains(digits.substring(0, 2))
                && digits.charAt(2) == '9';
    }

    public static boolean isValidEmail(String value) {
        String email = value;
        return !email.isEmpty()
                && !email.matches(".*\\s+.*")
                && email.matches("(?i)[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}");
    }

    public static boolean isValidRandomKey(String value) {
        return value.trim().matches(
                "(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
        );
    }

    public static String canonicalValue(String type, String value) {
        String trimmed = value == null ? "" : value.trim();
        if (TYPE_CPF.equals(type)) {
            return digitsOnly(trimmed);
        }
        if (TYPE_PHONE.equals(type)) {
            return normalizePhoneNational(trimmed);
        }
        if (TYPE_EMAIL.equals(type) || isRandomType(type)) {
            return trimmed.toLowerCase(Locale.ROOT);
        }
        return trimmed;
    }

    public static String formatCpf(String value) {
        String digits = digitsOnly(value);
        if (digits.length() > 11) {
            digits = digits.substring(0, 11);
        }
        if (digits.length() <= 3) {
            return digits;
        }
        if (digits.length() <= 6) {
            return digits.substring(0, 3) + "." + digits.substring(3);
        }
        if (digits.length() <= 9) {
            return digits.substring(0, 3) + "." + digits.substring(3, 6)
                    + "." + digits.substring(6);
        }
        return digits.substring(0, 3) + "." + digits.substring(3, 6)
                + "." + digits.substring(6, 9) + "-" + digits.substring(9);
    }

    public static String formatDocument(String value) {
        String digits = digitsOnly(value);
        if (digits.length() > 14) {
            digits = digits.substring(0, 14);
        }
        if (digits.length() <= 11) {
            return formatCpf(digits);
        }
        if (digits.length() <= 2) {
            return digits;
        }
        if (digits.length() <= 5) {
            return digits.substring(0, 2) + "." + digits.substring(2);
        }
        if (digits.length() <= 8) {
            return digits.substring(0, 2) + "." + digits.substring(2, 5)
                    + "." + digits.substring(5);
        }
        if (digits.length() <= 12) {
            return digits.substring(0, 2) + "." + digits.substring(2, 5)
                    + "." + digits.substring(5, 8) + "/" + digits.substring(8);
        }
        return digits.substring(0, 2) + "." + digits.substring(2, 5)
                + "." + digits.substring(5, 8) + "/" + digits.substring(8, 12)
                + "-" + digits.substring(12);
    }

    public static String formatPhone(String value) {
        String digits = normalizePhoneNational(value);
        if (digits.length() > 11) {
            digits = digits.substring(0, 11);
        }
        if (digits.length() <= 2) {
            return digits;
        }
        if (digits.length() <= 7) {
            return "(" + digits.substring(0, 2) + ") " + digits.substring(2);
        }
        return "(" + digits.substring(0, 2) + ") " + digits.substring(2, 7)
                + "-" + digits.substring(7);
    }

    public static String normalizePhoneNational(String value) {
        String original = value == null ? "" : value.trim();
        String digits = digitsOnly(original);
        if (original.startsWith("+55") || (digits.length() == 13 && digits.startsWith("55"))) {
            return digits.substring(2);
        }
        return digits;
    }

    public static String digitsOnly(String value) {
        return value == null ? "" : value.replaceAll("[^0-9]", "");
    }

    public static String inferType(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.contains("@")) {
            return TYPE_EMAIL;
        }
        String digits = digitsOnly(trimmed);
        if (digits.length() == 14) {
            return TYPE_CPF;
        }
        if (digits.length() == 11 && isValidCpf(digits)) {
            return TYPE_CPF;
        }
        if (isValidPhone(trimmed)) {
            return TYPE_PHONE;
        }
        if (isValidRandomKey(trimmed)) {
            return TYPE_RANDOM;
        }
        return digits.length() == 11 ? TYPE_CPF : TYPE_RANDOM;
    }

    public static int limitForType(String type) {
        if (isRandomType(type)) {
            return 2;
        }
        return TYPE_CPF.equals(type) || TYPE_PHONE.equals(type) || TYPE_EMAIL.equals(type)
                ? 1 : 0;
    }

    public static String typeLabel(String type) {
        if (TYPE_CPF.equals(type)) {
            return "CPF";
        }
        if (TYPE_PHONE.equals(type)) {
            return "Telefone";
        }
        if (TYPE_EMAIL.equals(type)) {
            return "E-mail";
        }
        return "Chave aleatória";
    }

    public static String formatForDisplay(String type, String value) {
        return PixKeyFormatter.formatForDisplay(type, value);
    }

    public static boolean isRandomType(String type) {
        return TYPE_RANDOM.equals(type)
                || TYPE_RANDOM1.equals(type)
                || TYPE_RANDOM2.equals(type);
    }

    private static int cpfDigit(String digits, int length, int initialWeight) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (digits.charAt(i) - '0') * (initialWeight - i);
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    private static int cnpjDigit(String digits, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += (digits.charAt(i) - '0') * weights[i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    private static boolean hasRepeatedDigits(String digits) {
        char first = digits.charAt(0);
        for (int i = 1; i < digits.length(); i++) {
            if (digits.charAt(i) != first) {
                return false;
            }
        }
        return true;
    }
}
