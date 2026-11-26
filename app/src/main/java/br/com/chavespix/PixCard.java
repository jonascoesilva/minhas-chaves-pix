package br.com.chavespix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PixCard implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    public static final String[] KEY_TYPES = {
            PixKeyValidator.TYPE_CPF,
            PixKeyValidator.TYPE_PHONE,
            PixKeyValidator.TYPE_EMAIL,
            PixKeyValidator.TYPE_RANDOM1,
            PixKeyValidator.TYPE_RANDOM2
    };

    private final long id;
    private final String beneficiaryName;
    private final String bankName;
    private final int colorArgb;
    private final String city;
    private final String cpf;
    private final String phone;
    private final String email;
    private final String random1;
    private final String random2;

    public PixCard(
            long id,
            String beneficiaryName,
            String bankName,
            int colorArgb,
            String city,
            String cpf,
            String phone,
            String email,
            String random1,
            String random2
    ) {
        this.id = id;
        this.beneficiaryName = beneficiaryName;
        this.bankName = bankName;
        this.colorArgb = colorArgb;
        this.city = city;
        this.cpf = cpf;
        String nationalPhone = PixKeyValidator.normalizePhoneNational(phone);
        this.phone = phone == null || nationalPhone.isEmpty() ? phone : nationalPhone;
        this.email = email;
        this.random1 = random1;
        this.random2 = random2;
    }

    public long getId() {
        return id;
    }

    public String getBeneficiaryName() {
        return beneficiaryName;
    }

    public String getBankName() {
        return bankName;
    }

    public int getColorArgb() {
        return colorArgb;
    }

    public String getCity() {
        return city;
    }

    public String getCpf() {
        return cpf;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getRandom1() {
        return random1;
    }

    public String getRandom2() {
        return random2;
    }

    public String getKey(String type) {
        switch (type) {
            case PixKeyValidator.TYPE_CPF:
                return cpf;
            case PixKeyValidator.TYPE_PHONE:
                return phone;
            case PixKeyValidator.TYPE_EMAIL:
                return email;
            case PixKeyValidator.TYPE_RANDOM1:
                return random1;
            case PixKeyValidator.TYPE_RANDOM2:
                return random2;
            default:
                return null;
        }
    }

    public int getKeyCount() {
        int count = 0;
        for (String type : KEY_TYPES) {
            if (hasValue(getKey(type))) {
                count++;
            }
        }
        return count;
    }

    public List<KeyEntry> getKeys() {
        List<KeyEntry> entries = new ArrayList<>();
        for (String type : KEY_TYPES) {
            String value = getKey(type);
            if (hasValue(value)) {
                entries.add(new KeyEntry(type, value));
            }
        }
        return Collections.unmodifiableList(entries);
    }

    public PixCard withKey(String type, String value) {
        String normalized = hasValue(value) ? value : null;
        return new PixCard(
                id,
                beneficiaryName,
                bankName,
                colorArgb,
                city,
                PixKeyValidator.TYPE_CPF.equals(type) ? normalized : cpf,
                PixKeyValidator.TYPE_PHONE.equals(type) ? normalized : phone,
                PixKeyValidator.TYPE_EMAIL.equals(type) ? normalized : email,
                PixKeyValidator.TYPE_RANDOM1.equals(type) ? normalized : random1,
                PixKeyValidator.TYPE_RANDOM2.equals(type) ? normalized : random2
        );
    }

    public PixCard withMetadata(
            String newBeneficiaryName,
            String newBankName,
            int newColorArgb,
            String newCity
    ) {
        return new PixCard(
                id,
                newBeneficiaryName,
                newBankName,
                newColorArgb,
                newCity,
                cpf,
                phone,
                email,
                random1,
                random2
        );
    }

    public PixCard withId(long newId) {
        return new PixCard(
                newId,
                beneficiaryName,
                bankName,
                colorArgb,
                city,
                cpf,
                phone,
                email,
                random1,
                random2
        );
    }

    private static boolean hasValue(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static final class KeyEntry {
        private final String type;
        private final String value;

        KeyEntry(String type, String value) {
            this.type = type;
            this.value = value;
        }

        public String getType() {
            return type;
        }

        public String getValue() {
            return value;
        }
    }
}
