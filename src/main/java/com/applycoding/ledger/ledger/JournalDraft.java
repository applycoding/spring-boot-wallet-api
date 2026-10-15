package com.applycoding.ledger.ledger;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class JournalDraft {

    private final JournalType type;
    private final List<Leg> legs;

    public JournalDraft(JournalType type, List<Leg> legs) {
        this.type = type;
        this.legs = List.copyOf(legs);
        assertBalanced();
    }

    public JournalType type() {
        return type;
    }

    public List<Leg> legs() {
        return legs;
    }

    private void assertBalanced() {
        if (legs.size() < 2) {
            throw new IllegalStateException("A journal needs at least two legs");
        }
        Map<String, BigDecimal> net = new HashMap<>();
        for (Leg leg : legs) {
            BigDecimal signed = leg.direction() == EntryDirection.DEBIT
                    ? leg.money().amount()
                    : leg.money().amount().negate();
            net.merge(leg.money().currency(), signed, BigDecimal::add);
        }
        for (BigDecimal value : net.values()) {
            if (value.signum() != 0) {
                throw new IllegalStateException("Journal is not balanced");
            }
        }
    }
}
