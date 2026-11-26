package br.com.chavespix;

import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.Locale;

public final class PixPayloadBuilder {
    private static final String PIX_GUI = "br.gov.bcb.pix";
    private static final String DEFAULT_MERCHANT_CITY = "BRASIL";

    private PixPayloadBuilder() {
    }

    public static String buildStaticPayload(String pixKey, String beneficiary) {
        return buildStaticPayload(pixKey, beneficiary, null, null);
    }

    public static String buildStaticPayload(
            String pixKey,
            String beneficiary,
            String city,
            BigDecimal amount
    ) {
        return buildStaticPayload(null, pixKey, beneficiary, city, amount);
    }

    public static String buildStaticPayload(
            String type,
            String pixKey,
            String beneficiary,
            String city,
            BigDecimal amount
    ) {
        String merchantName = emvText(beneficiary, 25, "Nome");
        String merchantCity = emvText(
                city == null || city.trim().isEmpty() ? DEFAULT_MERCHANT_CITY : city,
                15,
                "Cidade"
        );
        String formattedKey = formatPixKey(type, pixKey);

        String merchantAccount = tlv("00", PIX_GUI) + tlv("01", formattedKey);
        String additionalData = tlv("05", "***");
        String payload = tlv("00", "01")
                + tlv("01", "11")
                + tlv("26", merchantAccount)
                + tlv("52", "0000")
                + tlv("53", "986");
        if (amount != null) {
            if (amount.compareTo(BigDecimal.ZERO) <= 0
                    || amount.compareTo(new BigDecimal("10000000")) >= 0) {
                throw new IllegalArgumentException("O valor precisa ser maior que zero e menor que o limite.");
            }
            String formattedAmount = amount.setScale(2, RoundingMode.HALF_UP)
                    .toPlainString();
            payload += tlv("54", formattedAmount);
        }
        payload += tlv("58", "BR")
                + tlv("59", merchantName)
                + tlv("60", merchantCity)
                + tlv("62", additionalData)
                + "6304";

        return payload + String.format(Locale.ROOT, "%04X", crc16Ccitt(payload));
    }

    public static String formatPixKey(String pixKey) {
        String key = pixKey == null ? "" : pixKey.trim();
        if (key.isEmpty()) {
            throw new IllegalArgumentException("A chave Pix é obrigatória.");
        }

        if (key.matches("\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}")) {
            return key.replaceAll("[.-]", "");
        }
        if (key.matches("\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}")) {
            return key.replaceAll("[./-]", "");
        }
        if (key.contains("@") || PixKeyValidator.isValidRandomKey(key)) {
            return key.toLowerCase(Locale.ROOT);
        }

        if (key.matches("[+0-9() .-]+")) {
            String compact = key.replaceAll("[() .-]", "");
            if (compact.matches("\\d{10,11}")) {
                return "+55" + compact;
            }
            if (compact.matches("55\\d{10,11}")) {
                return "+" + compact;
            }
            if (compact.matches("\\+55\\d{10,11}")) {
                return compact;
            }
        }
        return key;
    }

    public static String formatPixKey(String type, String pixKey) {
        if (type == null) {
            return formatPixKey(pixKey);
        }
        String key = pixKey == null ? "" : pixKey.trim();
        if (key.isEmpty()) {
            throw new IllegalArgumentException("A chave Pix é obrigatória.");
        }
        if (PixKeyValidator.TYPE_CPF.equals(type)) {
            return PixKeyValidator.digitsOnly(key);
        }
        if (PixKeyValidator.TYPE_PHONE.equals(type)) {
            return "+55" + PixKeyValidator.normalizePhoneNational(key);
        }
        if (PixKeyValidator.TYPE_EMAIL.equals(type) || PixKeyValidator.isRandomType(type)) {
            return key.toLowerCase(Locale.ROOT);
        }
        return formatPixKey(key);
    }

    static int crc16Ccitt(String value) {
        int crc = 0xFFFF;
        for (byte valueByte : value.getBytes(StandardCharsets.UTF_8)) {
            crc ^= (valueByte & 0xFF) << 8;
            for (int bit = 0; bit < 8; bit++) {
                crc = (crc & 0x8000) != 0
                        ? (crc << 1) ^ 0x1021
                        : crc << 1;
                crc &= 0xFFFF;
            }
        }
        return crc;
    }

    private static String tlv(String id, String value) {
        int byteLength = value.getBytes(StandardCharsets.UTF_8).length;
        if (byteLength > 99) {
            throw new IllegalArgumentException("Campo EMV excede 99 bytes: " + id);
        }
        return id + String.format(Locale.ROOT, "%02d", byteLength) + value;
    }

    private static String emvText(String value, int maxLength, String fieldName) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9 ]", "")
                .replaceAll("\\s+", " ")
                .trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " é obrigatório para o QR Pix.");
        }
        return normalized.length() > maxLength
                ? normalized.substring(0, maxLength).trim()
                : normalized;
    }
}
