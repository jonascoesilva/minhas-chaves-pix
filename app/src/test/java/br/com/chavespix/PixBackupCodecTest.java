package br.com.chavespix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

public final class PixBackupCodecTest {
    @Test
    public void roundTripsCardsAndToleratesUnknownFields() throws Exception {
        PixCard source = new PixCard(
                24,
                "Maria Teste",
                "Banco Exemplo",
                0xFF006B62,
                "São Paulo",
                "529.982.247-25",
                "+55 (11) 91234-5678",
                null,
                "550e8400-e29b-41d4-a716-446655440000",
                null
        );
        JSONObject backup = PixBackupCodec.encode(
                Collections.singletonList(source),
                "2026-03-24T12:00:00Z"
        );
        backup.getJSONArray("cards")
                .getJSONObject(0)
                .put("phone", "+55 (11) 91234-5678");
        backup.put("futureProperty", true);

        List<PixCard> decoded = PixBackupCodec.decode(backup.toString());

        assertEquals(1, decoded.size());
        PixCard card = decoded.get(0);
        assertEquals(24, card.getId());
        assertEquals("Maria Teste", card.getBeneficiaryName());
        assertEquals("Banco Exemplo", card.getBankName());
        assertEquals("São Paulo", card.getCity());
        assertEquals(source.getColorArgb(), card.getColorArgb());
        assertEquals(source.getCpf(), card.getCpf());
        assertEquals("11912345678", card.getPhone());
        assertEquals(source.getRandom1(), card.getRandom1());
        assertNull(card.getEmail());
    }

    @Test
    public void rejectsUnsupportedSchemaAndDuplicateKeysInOneCard() throws Exception {
        JSONObject backup = PixBackupCodec.encode(
                Collections.singletonList(new PixCard(
                        0,
                        "Pessoa",
                        "Banco",
                        0xFF006B62,
                        null,
                        null,
                        null,
                        null,
                        "550e8400-e29b-41d4-a716-446655440000",
                        "550e8400-e29b-41d4-a716-446655440000"
                )),
                "2026-03-24T12:00:00Z"
        );
        assertThrows(JSONException.class, () -> PixBackupCodec.decode(backup.toString()));

        backup.put("schemaVersion", 8);
        assertThrows(JSONException.class, () -> PixBackupCodec.decode(backup.toString()));
    }
}
