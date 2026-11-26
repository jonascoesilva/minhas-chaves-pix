package br.com.chavespix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.math.BigDecimal;

public final class PixPayloadBuilderTest {
    @Test
    public void crc16CcittMatchesStandardCheckValue() {
        assertEquals(0x29B1, PixPayloadBuilder.crc16Ccitt("123456789"));
    }

    @Test
    public void staticPayloadContainsExactPixTemplatesAndValidCrc() {
        String payload = PixPayloadBuilder.buildStaticPayload(
                "(11) 91234-5678",
                "Maria Teste"
        );
        int crcStart = payload.length() - 8;
        String payloadBeforeCrc = payload.substring(0, payload.length() - 4);
        Map<String, String> fields = parseTlv(payload.substring(0, crcStart));
        Map<String, String> account = parseTlv(fields.get("26"));
        Map<String, String> additionalData = parseTlv(fields.get("62"));

        assertEquals("000201010211", payload.substring(0, 12));
        assertEquals("01", fields.get("00"));
        assertEquals("11", fields.get("01"));
        assertEquals("BRASIL", fields.get("60"));
        assertEquals(2, account.size());
        assertEquals("br.gov.bcb.pix", account.get("00"));
        assertEquals("+5511912345678", account.get("01"));
        assertEquals(1, additionalData.size());
        assertEquals("***", additionalData.get("05"));
        assertEquals("6304", payload.substring(crcStart, crcStart + 4));
        assertEquals(
                PixPayloadBuilder.crc16Ccitt(payloadBeforeCrc),
                Integer.parseInt(payload.substring(payload.length() - 4), 16)
        );
    }

    @Test
    public void formatsPhoneCpfCnpjAndLeavesEmailAndUuidUnchanged() {
        assertEquals("+5511912345678", PixPayloadBuilder.formatPixKey("11912345678"));
        assertEquals("+5511912345678", PixPayloadBuilder.formatPixKey("(11) 91234-5678"));
        assertEquals("+5511912345678", PixPayloadBuilder.formatPixKey("+55 (11) 91234-5678"));
        assertEquals("12345678909", PixPayloadBuilder.formatPixKey("123.456.789-09"));
        assertEquals("12345678000190", PixPayloadBuilder.formatPixKey("12.345.678/0001-90"));
        assertEquals("user+pix@example.com", PixPayloadBuilder.formatPixKey(" user+pix@example.com "));
        assertEquals(
                "550e8400-e29b-41d4-a716-446655440000",
                PixPayloadBuilder.formatPixKey(" 550e8400-e29b-41d4-a716-446655440000 ")
        );
    }

    @Test
    public void formatsKeyAccordingToItsStoredType() {
        assertEquals(
                "52998224725",
                PixPayloadBuilder.formatPixKey(PixKeyValidator.TYPE_CPF, "529.982.247-25")
        );
        assertEquals(
                "+5511912345678",
                PixPayloadBuilder.formatPixKey(PixKeyValidator.TYPE_PHONE, "5511912345678")
        );
        assertEquals(
                "+5555912345678",
                PixPayloadBuilder.formatPixKey(PixKeyValidator.TYPE_PHONE, "55912345678")
        );
        assertEquals(
                "person@example.com",
                PixPayloadBuilder.formatPixKey(PixKeyValidator.TYPE_EMAIL, "Person@Example.com")
        );
        assertEquals(
                "550e8400-e29b-41d4-a716-446655440000",
                PixPayloadBuilder.formatPixKey(
                        PixKeyValidator.TYPE_RANDOM1,
                        "550E8400-E29B-41D4-A716-446655440000"
                )
        );

        String cpfPayload = PixPayloadBuilder.buildStaticPayload(
                PixKeyFormatter.normalizeType("CPF"),
                "529.982.247-25",
                "Pessoa",
                null,
                null
        );
        Map<String, String> fields = parseTlv(
                cpfPayload.substring(0, cpfPayload.length() - 8)
        );
        assertEquals(
                "52998224725",
                parseTlv(fields.get("26")).get("01")
        );
    }

    @Test
    public void merchantFieldsAreDeaccentedFilteredAndLimited() {
        String payload = PixPayloadBuilder.buildStaticPayload(
                "key@example.com",
                "Nome do Beneficiário! Muito Longo Para o Campo"
        );
        Map<String, String> fields = parseTlv(payload.substring(0, payload.length() - 8));

        assertEquals("NOME DO BENEFICIARIO MUIT", fields.get("59"));
        assertEquals("BRASIL", fields.get("60"));
        assertTrue(fields.get("59").matches("[A-Z0-9 ]{1,25}"));
        assertTrue(fields.get("60").matches("[A-Z0-9 ]{1,15}"));
    }

    @Test
    public void emptyNameIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> PixPayloadBuilder.buildStaticPayload("key@example.com", " ")
        );
    }

    @Test
    public void buildsPayloadWithOptionalValueAndNormalizedCity() {
        String payload = PixPayloadBuilder.buildStaticPayload(
                "person@example.com",
                "Maria Teste",
                "São Paulo, SP",
                new BigDecimal("25.00")
        );
        int crcStart = payload.length() - 8;
        Map<String, String> fields = parseTlv(payload.substring(0, crcStart));

        assertEquals("25.00", fields.get("54"));
        assertEquals("SAO PAULO SP", fields.get("60"));
        assertEquals("MARIA TESTE", fields.get("59"));
        assertEquals(
                PixPayloadBuilder.crc16Ccitt(payload.substring(0, payload.length() - 4)),
                Integer.parseInt(payload.substring(payload.length() - 4), 16)
        );
    }

    @Test
    public void omitsDefaultValueAndRejectsOutOfRangeAmounts() {
        String withoutAmount = PixPayloadBuilder.buildStaticPayload(
                "person@example.com",
                "Pessoa",
                "Cidade",
                null
        );
        Map<String, String> fields = parseTlv(withoutAmount.substring(0, withoutAmount.length() - 8));
        assertFalse(fields.containsKey("54"));
        assertEquals("CIDADE", fields.get("60"));

        assertThrows(IllegalArgumentException.class, () ->
                PixPayloadBuilder.buildStaticPayload(
                        "person@example.com",
                        "Pessoa",
                        "Cidade",
                        new BigDecimal("0")
                )
        );
        assertThrows(IllegalArgumentException.class, () ->
                PixPayloadBuilder.buildStaticPayload(
                        "person@example.com",
                        "Pessoa",
                        "Cidade",
                        new BigDecimal("10000000")
                )
        );
    }

    private Map<String, String> parseTlv(String data) {
        Map<String, String> fields = new HashMap<>();
        int offset = 0;
        while (offset < data.length()) {
            String tag = data.substring(offset, offset + 2);
            int length = Integer.parseInt(data.substring(offset + 2, offset + 4));
            int valueStart = offset + 4;
            int valueEnd = valueStart + length;
            fields.put(tag, data.substring(valueStart, valueEnd));
            offset = valueEnd;
        }
        return fields;
    }
}
