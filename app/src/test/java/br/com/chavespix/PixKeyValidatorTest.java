package br.com.chavespix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class PixKeyValidatorTest {
    @Test
    public void validatesCpfCheckDigitsAndRejectsRepeatedDigits() {
        assertTrue(PixKeyValidator.isValidCpf("529.982.247-25"));
        assertFalse(PixKeyValidator.isValidCpf("529.982.247-24"));
        assertFalse(PixKeyValidator.isValidCpf("111.111.111-11"));
        assertFalse(PixKeyValidator.isValidCpf("123.456.789"));
        assertTrue(PixKeyValidator.isValidDocument("11.222.333/0001-81"));
    }

    @Test
    public void validatesPhoneDddAndMobileLength() {
        assertTrue(PixKeyValidator.isValidPhone("(11) 91234-5678"));
        assertTrue(PixKeyValidator.isValidPhone("+55 (21) 91234-5678"));
        assertTrue(PixKeyValidator.isValidPhone("55912345678"));
        assertFalse(PixKeyValidator.isValidPhone("(00) 91234-5678"));
        assertFalse(PixKeyValidator.isValidPhone("(11) 8765-4321"));
    }

    @Test
    public void normalizesPhoneToNationalDigitsWithoutRemovingDdd55() {
        assertEquals(
                "11912345678",
                PixKeyValidator.normalizePhoneNational("+55 (11) 91234-5678")
        );
        assertEquals(
                "11912345678",
                PixKeyValidator.normalizePhoneNational("5511912345678")
        );
        assertEquals(
                "55912345678",
                PixKeyValidator.normalizePhoneNational("55912345678")
        );
        assertEquals(
                "11912345678",
                PixKeyValidator.normalizePhoneNational("11912345678")
        );
    }

    @Test
    public void validatesEmailAndUuidFormats() {
        assertTrue(PixKeyValidator.isValidEmail("Pessoa+pix@example.com"));
        assertFalse(PixKeyValidator.isValidEmail("pessoa @example.com"));
        assertFalse(PixKeyValidator.isValidEmail(" pessoa@example.com"));
        assertFalse(PixKeyValidator.isValidEmail("pessoa@example"));
        assertTrue(PixKeyValidator.isValidRandomKey(
                "550e8400-e29b-41d4-a716-446655440000"
        ));
        assertFalse(PixKeyValidator.isValidRandomKey(
                "550e8400-e29b-41d4-a716-44665544000z"
        ));
    }

    @Test
    public void canonicalizesAndFormatsSavedKeys() {
        assertEquals("52998224725", PixKeyValidator.canonicalValue(
                PixKeyValidator.TYPE_CPF,
                "529.982.247-25"
        ));
        assertEquals("11912345678", PixKeyValidator.canonicalValue(
                PixKeyValidator.TYPE_PHONE,
                "+55 (11) 91234-5678"
        ));
        assertEquals("person@example.com", PixKeyValidator.canonicalValue(
                PixKeyValidator.TYPE_EMAIL,
                " Person@Example.com "
        ));
        assertEquals("529.982.247-25", PixKeyValidator.formatDocument("52998224725"));
        assertEquals(
                "529.982.247-25",
                PixKeyValidator.formatForDisplay(PixKeyValidator.TYPE_CPF, "52998224725")
        );
        assertEquals("(11) 91234-5678", PixKeyValidator.formatPhone("11912345678"));
        assertEquals(
                "(11) 91234-5678",
                PixKeyValidator.formatForDisplay(PixKeyValidator.TYPE_PHONE, "+5511912345678")
        );
    }

    @Test
    public void formatsCpfAndPhoneMasksDuringInputAndAcceptsPastedPhonePrefix() {
        assertEquals("529.9", PixKeyValidator.formatCpf("5299"));
        assertEquals("529.982.247-25", PixKeyValidator.formatCpf("529.982.247-25"));
        assertEquals("(11) 9", PixKeyValidator.formatPhone("119"));
        assertEquals("(11) 91234-5678", PixKeyValidator.formatPhone("11912345678"));
        assertEquals(
                "(11) 91234-5678",
                PixKeyValidator.formatPhone("+55 (11) 91234-5678")
        );
    }

    @Test
    public void exposesConfiguredPerTypeLimits() {
        assertEquals(1, PixKeyValidator.limitForType(PixKeyValidator.TYPE_CPF));
        assertEquals(1, PixKeyValidator.limitForType(PixKeyValidator.TYPE_PHONE));
        assertEquals(1, PixKeyValidator.limitForType(PixKeyValidator.TYPE_EMAIL));
        assertEquals(2, PixKeyValidator.limitForType(PixKeyValidator.TYPE_RANDOM));
    }
}
