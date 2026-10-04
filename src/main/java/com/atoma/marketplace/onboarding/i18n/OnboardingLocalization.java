package com.atoma.marketplace.onboarding.i18n;

import com.atoma.marketplace.common.enums.BusinessType;
import com.atoma.marketplace.common.i18n.MerchantAppLanguage;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

@Component
public class OnboardingLocalization {

    private static final Map<BusinessType, Map<MerchantAppLanguage, String>> BUSINESS_TYPE_LABELS = Map.of(
            BusinessType.INDIVIDUAL_SELLER, Map.of(
                    MerchantAppLanguage.EN, "Individual seller",
                    MerchantAppLanguage.FA, "فروشنده انفرادی",
                    MerchantAppLanguage.PS, "انفرادي پلورونکی"
            ),
            BusinessType.REGISTERED_SHOP, Map.of(
                    MerchantAppLanguage.EN, "Registered shop",
                    MerchantAppLanguage.FA, "دکان ثبت‌شده",
                    MerchantAppLanguage.PS, "ثبت شوی هټۍ"
            ),
            BusinessType.COMPANY_OR_PARTNERSHIP, Map.of(
                    MerchantAppLanguage.EN, "Company or partnership",
                    MerchantAppLanguage.FA, "شرکت یا مشارکت",
                    MerchantAppLanguage.PS, "شرکت یا شریکي"
            )
    );

    private static final Map<String, Map<MerchantAppLanguage, String>> STEP_TITLES = Map.of(
            "BUSINESS", Map.of(
                    MerchantAppLanguage.EN, "Business details",
                    MerchantAppLanguage.FA, "جزئیات کسب‌وکار",
                    MerchantAppLanguage.PS, "د سوداګرۍ جزئیات"
            ),
            "OWNER", Map.of(
                    MerchantAppLanguage.EN, "Owner / representative",
                    MerchantAppLanguage.FA, "مالک / نماینده",
                    MerchantAppLanguage.PS, "مالک / استازی"
            ),
            "DOCUMENTS", Map.of(
                    MerchantAppLanguage.EN, "Documents",
                    MerchantAppLanguage.FA, "اسناد",
                    MerchantAppLanguage.PS, "اسناد"
            ),
            "STORE_ADDRESS", Map.of(
                    MerchantAppLanguage.EN, "Store address",
                    MerchantAppLanguage.FA, "آدرس فروشگاه",
                    MerchantAppLanguage.PS, "د پلورنځي پته"
            ),
            "PAYOUT", Map.of(
                    MerchantAppLanguage.EN, "Payout",
                    MerchantAppLanguage.FA, "پرداخت",
                    MerchantAppLanguage.PS, "تادیه"
            ),
            "TERMS", Map.of(
                    MerchantAppLanguage.EN, "Terms & consent",
                    MerchantAppLanguage.FA, "شرایط و رضایت",
                    MerchantAppLanguage.PS, "شرطونه او رضایت"
            )
    );

    /** Known category display names by normalized English name; unknown names fall back to DB value. */
    private static final Map<String, Map<MerchantAppLanguage, String>> CATEGORY_NAMES = Map.of(
            normalizeKey("Food & Grocery"), Map.of(
                    MerchantAppLanguage.EN, "Food & Grocery",
                    MerchantAppLanguage.FA, "مواد غذایی و خوراکه",
                    MerchantAppLanguage.PS, "خواړه او توکي"
            )
    );

    public String businessTypeLabel(BusinessType type, MerchantAppLanguage language) {
        return localized(BUSINESS_TYPE_LABELS.get(type), language, formatEnumFallback(type.name()));
    }

    public String stepTitle(String stepKey, MerchantAppLanguage language, String englishFallback) {
        return localized(STEP_TITLES.get(stepKey), language, englishFallback);
    }

    public String categoryName(String englishName, MerchantAppLanguage language) {
        if (englishName == null) {
            return null;
        }
        var keyed = CATEGORY_NAMES.get(normalizeKey(englishName));
        return localized(keyed, language, englishName);
    }

    private static String localized(Map<MerchantAppLanguage, String> table, MerchantAppLanguage language, String fallback) {
        if (table == null) {
            return fallback;
        }
        return table.getOrDefault(language, table.getOrDefault(MerchantAppLanguage.EN, fallback));
    }

    private static String formatEnumFallback(String enumName) {
        return enumName.replace('_', ' ');
    }

    private static String normalizeKey(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
