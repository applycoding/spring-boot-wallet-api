package com.applycoding.ledger.ledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void normalizesScaleToTheCurrency() {
        Money usd = Money.of(new BigDecimal("10.5"), "usd");
        Money yen = Money.of(new BigDecimal("1500"), "JPY");
        Money dinar = Money.of(new BigDecimal("1.234"), "KWD");

        assertEquals("10.50", usd.toPlainString());
        assertEquals("USD", usd.currency());
        assertEquals("1500", yen.toPlainString());
        assertEquals("1.234", dinar.toPlainString());
    }

    @Test
    void rejectsExcessScaleZeroAndUnknownCurrencies() {
        assertThrows(InvalidMoneyException.class, () -> Money.of(new BigDecimal("1.001"), "USD"));
        assertThrows(InvalidMoneyException.class, () -> Money.of(new BigDecimal("0"), "USD"));
        assertThrows(InvalidMoneyException.class, () -> Money.of(new BigDecimal("-1"), "USD"));
        assertThrows(InvalidMoneyException.class, () -> Money.of(new BigDecimal("1"), "XYZ"));
        assertThrows(InvalidMoneyException.class, () -> Money.of(new BigDecimal("1"), "XAU"));
    }
}
