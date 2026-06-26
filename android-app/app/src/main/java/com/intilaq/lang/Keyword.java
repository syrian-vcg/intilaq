package com.intilaq.lang;

import java.util.HashMap;
import java.util.Map;

/**
 * الكلمات المحجوزة في لغة "الانطلاق".
 * كل كلمة هنا لها معنى ثابت داخل بنية البيانات ولا يمكن استخدامها كاسم حقل عادي
 * إلا إذا وُضعت بين علامتي تنصيص كقيمة نصية.
 */
public enum Keyword {
    ISM("اسم"),
    MUARRIF("معرف"),
    MAJMOUAA("مجموعة"),
    HALQA("حلقة"),
    TAARIF("تعريف"),
    HAJEZ("حاجز"),
    NASS("نص"),
    KOTLA("كتلة"),
    KAEN("كائن"),
    QASS("قص"),
    LASQ("لصق"),
    NAQL("نقل"),
    TAWREED("توريد"),
    TASDEER("تصدير"),
    TANZEEL("تنزيل"),
    TIBAAA("طباعة"),
    JAMA("جمع"),
    QISM("قسم"),
    TARJAMA("ترجمة"),
    FAQRA("فقرة"),
    KHANA("خانة"),
    SAFF("صف"),
    HONA("هنا"),
    ISHAAR("اشعار"),
    BAREED("بريد"),
    TASMIYA("تسمية"),
    HADATH("حدث"),
    NAJAH("نجح"),
    KHATA("خطاء");

    private final String text;

    Keyword(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    private static final Map<String, Keyword> LOOKUP = new HashMap<>();

    static {
        for (Keyword k : values()) {
            LOOKUP.put(k.text, k);
        }
    }

    /** يحاول إيجاد كلمة محجوزة مطابقة للنص المُعطى، أو null إن لم توجد. */
    public static Keyword fromText(String text) {
        return LOOKUP.get(text);
    }

    public static boolean isKeyword(String text) {
        return LOOKUP.containsKey(text);
    }

    /** يرجّع جميع نصوص الكلمات المحجوزة، مفيد للمحرر والتلوين. */
    public static String[] allTexts() {
        String[] result = new String[values().length];
        Keyword[] vals = values();
        for (int i = 0; i < vals.length; i++) {
            result[i] = vals[i].text;
        }
        return result;
    }
}
