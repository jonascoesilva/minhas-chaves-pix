package br.com.chavespix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public final class PixCardKeyRulesTest {
    @Test
    public void allowsFiveKeysOnEachOfMultipleCards() {
        PixCard first = card(1, "11912345678", "first@example.com");
        PixCard second = card(2, "21912345678", "second@example.com");

        assertEquals(5, first.getKeyCount());
        assertEquals(5, second.getKeyCount());
    }

    @Test
    public void locatesDuplicateAcrossCardsAndCanExcludeEditedCard() {
        PixCard existing = card(1, "11912345678", "first@example.com");
        List<PixCard> cards = Arrays.asList(existing, card(2, "21912345678", "second@example.com"));

        assertEquals(
                existing,
                PixCardKeyRules.findDuplicateOwner(
                        cards,
                        PixKeyValidator.TYPE_PHONE,
                        "+55 (11) 91234-5678",
                        -1
                )
        );
        assertNull(PixCardKeyRules.findDuplicateOwner(
                cards,
                PixKeyValidator.TYPE_PHONE,
                "+55 (11) 91234-5678",
                existing.getId()
        ));
    }

    private PixCard card(long id, String phone, String email) {
        return new PixCard(
                id,
                "Pessoa " + id,
                "Banco " + id,
                0xFF006B62,
                null,
                id == 1 ? "529.982.247-25" : "111.444.777-35",
                phone,
                email,
                "550e8400-e29b-41d4-a716-44665544000" + id,
                "550e8400-e29b-41d4-a716-44665544001" + id
        );
    }
}
