package br.com.chavespix;

public final class PixQrText {
    private PixQrText() {
    }

    public static String disclaimerFor(String formattedAmount) {
        if (formattedAmount == null || formattedAmount.isEmpty()) {
            return "QR Code Pix estático, sem valor definido. Quem for pagar informa o valor. "
                    + "Confira os dados do beneficiário antes de pagar.";
        }
        return "QR Code Pix estático com valor de " + formattedAmount
                + ". Confira os dados do beneficiário antes de pagar.";
    }
}
