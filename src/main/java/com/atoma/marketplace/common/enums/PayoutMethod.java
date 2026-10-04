package com.atoma.marketplace.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/** Mobile contract: {@code BANK} | {@code WALLET}. Legacy names still accepted on input. */
public enum PayoutMethod {
    BANK,
    WALLET;

    @JsonCreator
    public static PayoutMethod fromJson(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return switch (value.trim().toUpperCase()) {
            case "BANK", "BANK_ACCOUNT" -> BANK;
            case "WALLET", "ATOMA_WALLET" -> WALLET;
            default -> throw new IllegalArgumentException("Invalid payout method: " + value);
        };
    }
}
