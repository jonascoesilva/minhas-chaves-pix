package br.com.chavespix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public final class PixCardMigrationTest {
    @Test
    public void migratesLegacyRowWithoutChangingItsIdOrData() {
        PixCard card = PixCardMigration.fromLegacy(
                37,
                "Maria Teste",
                "Banco Exemplo",
                "529.982.247-25",
                "teal",
                PixKeyValidator.TYPE_CPF
        );

        assertEquals(37, card.getId());
        assertEquals("Maria Teste", card.getBeneficiaryName());
        assertEquals("Banco Exemplo", card.getBankName());
        assertEquals("529.982.247-25", card.getCpf());
        assertNull(card.getPhone());
        assertEquals(1, card.getKeyCount());
    }

    @Test
    public void migratesLegacyRandomKeyIntoFirstRandomSlot() {
        PixCard card = PixCardMigration.fromLegacy(
                9,
                "Nome",
                "Banco",
                "550e8400-e29b-41d4-a716-446655440000",
                "#006B62",
                PixKeyValidator.TYPE_RANDOM
        );

        assertEquals("550e8400-e29b-41d4-a716-446655440000", card.getRandom1());
        assertNull(card.getRandom2());
        assertEquals(9, card.getId());
    }

    @Test
    public void migratesLegacyPhoneToNationalDigits() {
        PixCard card = PixCardMigration.fromLegacy(
                12,
                "Pessoa",
                "Banco",
                "+55 (11) 91234-5678",
                "teal",
                PixKeyValidator.TYPE_PHONE
        );

        assertEquals("11912345678", card.getPhone());
        assertEquals(1, card.getKeyCount());
    }
}
