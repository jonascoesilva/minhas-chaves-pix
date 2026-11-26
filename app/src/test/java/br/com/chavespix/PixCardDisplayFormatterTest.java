package br.com.chavespix;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class PixCardDisplayFormatterTest {
    @Test
    public void masksCpfAndFormatsPhoneInHomeSummary() {
        assertEquals(
                "***.***.247-25",
                PixCardDisplayFormatter.formatSummaryValue(
                        PixKeyValidator.TYPE_CPF,
                        "52998224725"
                )
        );
        assertEquals(
                "(11) 91234-5678",
                PixCardDisplayFormatter.formatSummaryValue(
                        PixKeyValidator.TYPE_PHONE,
                        "+5511912345678"
                )
        );
    }

    @Test
    public void shortensRandomKeyAndLeavesEmailForEndEllipsisRendering() {
        assertEquals(
                "550e840…440000",
                PixCardDisplayFormatter.formatSummaryValue(
                        PixKeyValidator.TYPE_RANDOM1,
                        "550e8400-e29b-41d4-a716-446655440000"
                )
        );
        assertEquals(
                "very-long@example.com",
                PixCardDisplayFormatter.formatSummaryValue(
                        PixKeyValidator.TYPE_EMAIL,
                        "very-long@example.com"
                )
        );
    }
}
