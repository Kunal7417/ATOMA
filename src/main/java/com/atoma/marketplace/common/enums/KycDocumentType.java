package com.atoma.marketplace.common.enums;

import java.util.Set;

public enum KycDocumentType {
    BUSINESS_LICENSE,
    OWNER_TAZKIRA,
    TIN,
    @Deprecated
    NIC,
    @Deprecated
    UTILITY_BILL;

    public static final Set<KycDocumentType> REQUIRED_FOR_SUBMISSION = Set.of(
            BUSINESS_LICENSE, OWNER_TAZKIRA, TIN
    );

    public static KycDocumentType fromString(String value) {
        try {
            return KycDocumentType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported document type: " + value);
        }
    }
}
