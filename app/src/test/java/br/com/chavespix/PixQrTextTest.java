package br.com.chavespix;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class PixQrTextTest {
    @Test
    public void disclaimerReflectsWhetherAmountIsIncluded() {
        assertEquals(
                "QR Code Pix estático, sem valor definido. Quem for pagar informa o valor. "
                        + "Confira os dados do beneficiário antes de pagar.",
                PixQrText.disclaimerFor(null)
        );
        assertEquals(
                "QR Code Pix estático com valor de R$ 100,00. "
                        + "Confira os dados do beneficiário antes de pagar.",
                PixQrText.disclaimerFor("R$ 100,00")
        );
    }
}
