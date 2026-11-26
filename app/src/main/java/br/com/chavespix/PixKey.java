package br.com.chavespix;

public final class PixKey {
    private final long id;
    private final String beneficiary;
    private final String bank;
    private final String key;
    private final String themeId;
    private final String type;

    public PixKey(long id, String beneficiary, String bank, String key, String themeId) {
        this(id, beneficiary, bank, key, themeId, PixKeyValidator.inferType(key));
    }

    public PixKey(
            long id,
            String beneficiary,
            String bank,
            String key,
            String themeId,
            String type
    ) {
        this.id = id;
        this.beneficiary = beneficiary;
        this.bank = bank;
        this.key = key;
        this.themeId = themeId;
        this.type = type;
    }

    public long getId() {
        return id;
    }

    public String getBeneficiary() {
        return beneficiary;
    }

    public String getBank() {
        return bank;
    }

    public String getKey() {
        return key;
    }

    public String getThemeId() {
        return themeId;
    }

    public String getType() {
        return type;
    }
}
