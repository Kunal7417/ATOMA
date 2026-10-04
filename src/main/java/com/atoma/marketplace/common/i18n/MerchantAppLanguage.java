package com.atoma.marketplace.common.i18n;

import java.util.Locale;

/**
 * Merchant mobile app display languages (Accept-Language: en | fa | ps).
 */
public enum MerchantAppLanguage {
    EN,
    FA,
    PS;

    public static MerchantAppLanguage fromAcceptLanguageHeader(String header) {
        if (header == null || header.isBlank()) {
            return EN;
        }
        var primary = header.split(",")[0].trim().toLowerCase(Locale.ROOT);
        if (primary.startsWith("fa")) {
            return FA;
        }
        if (primary.startsWith("ps")) {
            return PS;
        }
        return EN;
    }
}
