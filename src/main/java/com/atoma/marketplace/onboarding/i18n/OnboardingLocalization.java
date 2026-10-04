package com.atoma.marketplace.onboarding.i18n;

import com.atoma.marketplace.common.enums.BusinessType;
import com.atoma.marketplace.common.i18n.MerchantAppLanguage;
import org.springframework.stereotype.Component;

import java.util.HashMap;
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

    private static final Map<String, Map<MerchantAppLanguage, String>> REVIEW_NOTE_TITLES = Map.of(
            "FIX_BUSINESS_NAME", Map.of(
                    MerchantAppLanguage.EN, "Business name",
                    MerchantAppLanguage.FA, "نام کسب‌وکار",
                    MerchantAppLanguage.PS, "د سوداګرۍ نوم"
            ),
            "FIX_LICENCE", Map.of(
                    MerchantAppLanguage.EN, "Business licence",
                    MerchantAppLanguage.FA, "جواز کسب‌وکار",
                    MerchantAppLanguage.PS, "د سوداګرۍ جواز"
            )
    );

    private static final Map<String, Map<MerchantAppLanguage, String>> REJECTION_REASONS = Map.of(
            "INCOMPLETE_DOCUMENTS", Map.of(
                    MerchantAppLanguage.EN, "Documents were incomplete or unreadable.",
                    MerchantAppLanguage.FA, "اسناد ناقص یا ناخوانا بودند.",
                    MerchantAppLanguage.PS, "اسناد ناقص یا نه لوستل کېدونکي وو."
            )
    );

    private static final Map<String, Map<MerchantAppLanguage, String>> SUSPENSION_REASONS = Map.of(
            "LICENCE_EXPIRED", Map.of(
                    MerchantAppLanguage.EN, "Your business licence has expired.",
                    MerchantAppLanguage.FA, "جواز کسب‌وکار شما منقضی شده است.",
                    MerchantAppLanguage.PS, "ستاسو د سوداګرۍ جواز پای ته رسېدلی دی."
            )
    );

    private static final Map<String, Map<MerchantAppLanguage, String>> CATEGORY_NAMES = buildCategoryNames();

    private static Map<String, Map<MerchantAppLanguage, String>> buildCategoryNames() {
        var m = new HashMap<String, Map<MerchantAppLanguage, String>>();
        putCategory(m, "Food & grocery",
                "Food & grocery", "مواد غذایی و خوراکه", "خواړه او توکي");
        putCategory(m, "Electronics", "Electronics", "الکترونیک", "برېښنايي توکي");
        putCategory(m, "Pharmacy", "Pharmacy", "دواخانه", "درملتون");
        putCategory(m, "Fashion", "Fashion", "مد و فیشن", "فیشن");
        putCategory(m, "Home & kitchen", "Home & kitchen", "خانه و آشپزخانه", "کور او پخلنځی");
        putCategory(m, "Beauty", "Beauty", "زیبایی", "ښکلا");
        putCategory(m, "Handicrafts & carpets", "Handicrafts & carpets", "صنایع دستی و قالی", "لاسي صنایع او غالۍ");
        return Map.copyOf(m);
    }

    private static void putCategory(
            Map<String, Map<MerchantAppLanguage, String>> target,
            String keyEn,
            String en,
            String fa,
            String ps
    ) {
        target.put(normalizeKey(keyEn), Map.of(
                MerchantAppLanguage.EN, en,
                MerchantAppLanguage.FA, fa,
                MerchantAppLanguage.PS, ps
        ));
    }

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

    public String reviewerNoteTitle(String titleKey, MerchantAppLanguage language) {
        return localized(REVIEW_NOTE_TITLES.get(titleKey), language, titleKey);
    }

    public String rejectionReason(String reasonKey, MerchantAppLanguage language) {
        return localized(REJECTION_REASONS.get(reasonKey), language, reasonKey);
    }

    public String suspensionReason(String reasonKey, MerchantAppLanguage language) {
        return localized(SUSPENSION_REASONS.get(reasonKey), language, reasonKey);
    }

    /** Display text or i18n key; translate when a mapping exists. */
    public String displayText(String textOrKey, MerchantAppLanguage language) {
        if (textOrKey == null) {
            return null;
        }
        var fromReview = REVIEW_NOTE_TITLES.get(textOrKey);
        if (fromReview != null) {
            return localized(fromReview, language, textOrKey);
        }
        var fromReject = REJECTION_REASONS.get(textOrKey);
        if (fromReject != null) {
            return localized(fromReject, language, textOrKey);
        }
        var fromSuspend = SUSPENSION_REASONS.get(textOrKey);
        if (fromSuspend != null) {
            return localized(fromSuspend, language, textOrKey);
        }
        return textOrKey;
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
