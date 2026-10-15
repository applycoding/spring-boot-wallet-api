package com.applycoding.ledger.ledger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Locale;

public final class CurrencySupport {

    private CurrencySupport() {
    }

    public static String requireIsoCode(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidMoneyException("currency is required");
        }
        String code = raw.trim().toUpperCase(Locale.ROOT);
        if (!code.matches("[A-Z]{3}")) {
            throw new InvalidMoneyException("currency must be a 3-letter ISO 4217 code");
        }
        Currency currency;
        try {
            currency = Currency.getInstance(code);
        } catch (IllegalArgumentException ex) {
            throw new InvalidMoneyException("unsupported currency: " + code);
        }
        if (currency.getDefaultFractionDigits() < 0) {
            throw new InvalidMoneyException("unsupported currency: " + code);
        }
        return code;
    }

    public static int fractionDigits(String currencyCode) {
        return Currency.getInstance(requireIsoCode(currencyCode)).getDefaultFractionDigits();
    }

    public static String format(BigDecimal amount, String currencyCode) {
        return amount
                .setScale(fractionDigits(currencyCode), RoundingMode.UNNECESSARY)
                .toPlainString();
    }
}
