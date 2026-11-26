package br.com.chavespix;

import java.util.List;

public final class PixCardKeyRules {
    private PixCardKeyRules() {
    }

    public static PixCard findDuplicateOwner(
            List<PixCard> cards,
            String type,
            String value,
            long excludedCardId
    ) {
        String normalized = PixKeyValidator.canonicalValue(type, value);
        for (PixCard card : cards) {
            if (excludedCardId > 0 && card.getId() == excludedCardId) {
                continue;
            }
            for (PixCard.KeyEntry key : card.getKeys()) {
                if (normalized.equalsIgnoreCase(
                        PixKeyValidator.canonicalValue(key.getType(), key.getValue())
                )) {
                    return card;
                }
            }
        }
        return null;
    }
}
