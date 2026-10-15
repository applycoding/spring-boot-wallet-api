package com.applycoding.ledger.ledger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class RequestHasher {

    private RequestHasher() {
    }

    public static String deposit(DepositCommand command) {
        return sha256(String.join("|",
                "DEPOSIT",
                command.accountId().toString(),
                command.money().toPlainString(),
                command.money().currency()));
    }

    public static String transfer(TransferCommand command) {
        return sha256(String.join("|",
                "TRANSFER",
                command.fromAccountId().toString(),
                command.toAccountId().toString(),
                command.money().toPlainString(),
                command.money().currency()));
    }

    private static String sha256(String canonical) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
