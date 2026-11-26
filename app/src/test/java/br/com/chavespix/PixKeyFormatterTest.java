package br.com.chavespix;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class PixKeyFormatterTest {
    @Test
    public void formatsCpfFromDigitsOrPunctuationAndNormalizesTypeCase() {
        assertEquals(
                "529.982.247-25",
                PixKeyFormatter.formatForDisplay("CPF", "52998224725")
        );
        assertEquals(
                "529.982.247-25",
                PixKeyFormatter.formatForDisplay("cpf", "529.982.247-25")
        );
    }

    @Test
    public void masksCpfForQrFromDigitsOrPunctuation() {
        assertEquals(
                "***.456.789-**",
                PixKeyFormatter.formatForQrDisplay("CPF", "12345678900")
        );
        assertEquals(
                "***.982.247-**",
                PixKeyFormatter.formatForQrDisplay("cpf", "529.982.247-25")
        );
    }

    @Test
    public void doesNotExposeMissingOrInvalidCpfOnQr() {
        assertEquals("", PixKeyFormatter.formatForQrDisplay("cpf", null));
        assertEquals("", PixKeyFormatter.formatForQrDisplay("cpf", ""));
        assertEquals(
                "***.***.***-**",
                PixKeyFormatter.formatForQrDisplay("cpf", "52998")
        );
    }

    @Test
    public void leavesEmptyAndIncompleteCpfAsProvided() {
        assertEquals("", PixKeyFormatter.formatForDisplay("CPF", ""));
        assertEquals("", PixKeyFormatter.formatForDisplay("CPF", null));
        assertEquals(
                "52998",
                PixKeyFormatter.formatForDisplay("CPF", "52998")
        );
        assertEquals(
                "52.998",
                PixKeyFormatter.formatForDisplay("CPF", "52.998")
        );
    }

    @Test
    public void formatsNationalPhoneAndLeavesOtherKeyTypesUnchanged() {
        assertEquals(
                "(11) 91234-5678",
                PixKeyFormatter.formatForDisplay("phone", "+55 (11) 91234-5678")
        );
        assertEquals(
                "teste@exemplo.com",
                PixKeyFormatter.formatForDisplay("email", "teste@exemplo.com")
        );
        assertEquals(
                "00000000-0000-4000-8000-000000000001",
                PixKeyFormatter.formatForDisplay(
                        "random1",
                        "00000000-0000-4000-8000-000000000001"
                )
        );
    }

    @Test
    public void qrFormattingPreservesOtherKeyTypes() {
        assertEquals(
                "(11) 91234-5678",
                PixKeyFormatter.formatForQrDisplay("phone", "+55 (11) 91234-5678")
        );
        assertEquals(
                "teste@exemplo.com",
                PixKeyFormatter.formatForQrDisplay("email", "teste@exemplo.com")
        );
        assertEquals(
                "12.345.678/0001-90",
                PixKeyFormatter.formatForQrDisplay("cnpj", "12.345.678/0001-90")
        );
        assertEquals(
                "00000000-0000-4000-8000-000000000001",
                PixKeyFormatter.formatForQrDisplay(
                        "random1",
                        "00000000-0000-4000-8000-000000000001"
                )
        );
    }
}
