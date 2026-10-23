package dev.minehelper.language;

import java.util.Arrays;
import java.util.Locale;

/** Response language, independent of the language being studied and the Minecraft UI. */
public enum ExplanationLanguage {
    AUTO("auto", "Game language", "Auto"),
    EN("en", "English", "English"),
    RU("ru", "Russian", "Русский"),
    FR("fr", "French", "Français"),
    DE("de", "German", "Deutsch"),
    ES("es", "Spanish", "Español"),
    PT("pt", "Portuguese", "Português"),
    IT("it", "Italian", "Italiano"),
    UK("uk", "Ukrainian", "Українська"),
    PL("pl", "Polish", "Polski"),
    TR("tr", "Turkish", "Türkçe"),
    ZH("zh", "Simplified Chinese", "简体中文"),
    JA("ja", "Japanese", "日本語"),
    KO("ko", "Korean", "한국어"),
    AR("ar", "Arabic", "العربية"),
    HI("hi", "Hindi", "हिन्दी"),
    ID("id", "Indonesian", "Bahasa Indonesia");

    private final String code, englishName, nativeName;
    ExplanationLanguage(String code, String englishName, String nativeName) {
        this.code = code; this.englishName = englishName; this.nativeName = nativeName;
    }
    public String code() { return code; }
    public String englishName() { return englishName; }
    public String label() { return code.toUpperCase(Locale.ROOT) + " · " + nativeName; }

    /** Accept Minecraft locales and common BCP-47 spellings without accepting arbitrary prompt text. */
    public static ExplanationLanguage fromCode(String code) {
        if (code == null) return AUTO;
        String base = code.strip().toLowerCase(Locale.ROOT).replace('-', '_').split("_", 2)[0];
        return Arrays.stream(values()).filter(l -> l.code.equals(base)).findFirst().orElse(AUTO);
    }
    public static ExplanationLanguage resolve(String preference, String gameLanguage) {
        ExplanationLanguage language = fromCode(preference);
        if (language == AUTO) language = fromCode(gameLanguage);
        return language == AUTO ? EN : language;
    }
}
