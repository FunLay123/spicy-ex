package com.eza.spicyex.lyrics;

import com.eza.spicyex.SpotifyPlusConfig;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Android port of Spicy fork romanization behavior.
 *
 * Current exact port:
 * - Cyrillic BGN/PCGN transliteration + post-processing from Fork/Romanization.ts
 * - Script priority/detection shape from ProcessLyrics.ts
 *
 * Pending platform ports:
 * - Japanese furigana renderer wiring
 * - Chinese jyutping package behavior
 *
 * Current Android-native ports/adapters:
 * - Korean revised romanization data path
 * - Greek romanization data path
 */
public final class SpicyRomanizer {
    private static final Map<Integer, String> BGN_PCGN = new HashMap<>();
    private static final Map<Integer, String> GREEK = new HashMap<>();

    private static final String[] HANGUL_INITIAL = {"g", "kk", "n", "d", "tt", "r", "m", "b", "pp", "s", "ss", "", "j", "jj", "ch", "k", "t", "p", "h"};
    private static final String[] HANGUL_VOWEL = {"a", "ae", "ya", "yae", "eo", "e", "yeo", "ye", "o", "wa", "wae", "oe", "yo", "u", "wo", "we", "wi", "yu", "eu", "ui", "i"};
    private static final String[] HANGUL_FINAL = {"", "k", "k", "ks", "n", "nj", "nh", "t", "l", "lk", "lm", "lb", "ls", "lt", "lp", "lh", "m", "p", "ps", "t", "t", "ng", "t", "t", "k", "t", "p", "t"};

    static {
        put("а", "a");
        put("б", "b");
        put("в", "v");
        put("г", "g");
        put("д", "d");
        put("е", "e");
        put("ё", "ë");
        put("ж", "zh");
        put("з", "z");
        put("и", "i");
        put("й", "y");
        put("к", "k");
        put("л", "l");
        put("м", "m");
        put("н", "n");
        put("о", "o");
        put("п", "p");
        put("р", "r");
        put("с", "s");
        put("т", "t");
        put("у", "u");
        put("ф", "f");
        put("х", "kh");
        put("ц", "ts");
        put("ч", "ch");
        put("ш", "sh");
        put("щ", "shch");
        put("ъ", "''");
        put("ы", "y");
        put("ь", "'");
        put("э", "e");
        put("ю", "yu");
        put("я", "ya");

        put("є", "ye");
        put("і", "i");
        put("ї", "yi");
        put("ґ", "g");
        put("ѝ", "ì");
        put("ѓ", "ǵ");
        put("ќ", "ḱ");
        put("ѕ", "ẑ");
        put("ђ", "đ");
        put("ћ", "ć");
        put("љ", "lj");
        put("њ", "nj");
        put("џ", "dž");

        put("А", "A");
        put("Б", "B");
        put("В", "V");
        put("Г", "G");
        put("Д", "D");
        put("Е", "E");
        put("Ё", "Ë");
        put("Ж", "Zh");
        put("З", "Z");
        put("И", "I");
        put("Й", "Y");
        put("К", "K");
        put("Л", "L");
        put("М", "M");
        put("Н", "N");
        put("О", "O");
        put("П", "P");
        put("Р", "R");
        put("С", "S");
        put("Т", "T");
        put("У", "U");
        put("Ф", "F");
        put("Х", "Kh");
        put("Ц", "Ts");
        put("Ч", "Ch");
        put("Ш", "Sh");
        put("Щ", "Shch");
        put("Ъ", "''");
        put("Ы", "Y");
        put("Ь", "'");
        put("Э", "E");
        put("Ю", "Yu");
        put("Я", "Ya");
        put("Є", "Ye");
        put("І", "I");
        put("Ї", "Yi");
        put("Ґ", "G");
        put("Ѓ", "Ǵ");
        put("Ќ", "Ḱ");
        put("Ѕ", "Ẑ");
        put("Ђ", "Đ");
        put("Ћ", "Ć");
        put("Љ", "Lj");
        put("Њ", "Nj");
        put("Џ", "Dž");

        putGreek("Α", "A"); putGreek("α", "a");
        putGreek("Β", "V"); putGreek("β", "v");
        putGreek("Γ", "G"); putGreek("γ", "g");
        putGreek("Δ", "D"); putGreek("δ", "d");
        putGreek("Ε", "E"); putGreek("ε", "e");
        putGreek("Ζ", "Z"); putGreek("ζ", "z");
        putGreek("Η", "I"); putGreek("η", "i");
        putGreek("Θ", "Th"); putGreek("θ", "th");
        putGreek("Ι", "I"); putGreek("ι", "i");
        putGreek("Κ", "K"); putGreek("κ", "k");
        putGreek("Λ", "L"); putGreek("λ", "l");
        putGreek("Μ", "M"); putGreek("μ", "m");
        putGreek("Ν", "N"); putGreek("ν", "n");
        putGreek("Ξ", "X"); putGreek("ξ", "x");
        putGreek("Ο", "O"); putGreek("ο", "o");
        putGreek("Π", "P"); putGreek("π", "p");
        putGreek("Ρ", "R"); putGreek("ρ", "r");
        putGreek("Σ", "S"); putGreek("σ", "s"); putGreek("ς", "s");
        putGreek("Τ", "T"); putGreek("τ", "t");
        putGreek("Υ", "Y"); putGreek("υ", "y");
        putGreek("Φ", "F"); putGreek("φ", "f");
        putGreek("Χ", "Ch"); putGreek("χ", "ch");
        putGreek("Ψ", "Ps"); putGreek("ψ", "ps");
        putGreek("Ω", "O"); putGreek("ω", "o");
    }

    private SpicyRomanizer() {
    }

    public static boolean canRomanizeLocally(String text, String wholeSongText, String language) {
        if (text == null || text.trim().isEmpty()) return false;
        List<SpicyTextDetection.Script> scripts = SpicyTextDetection.detectPresentScripts(wholeSongText, language, "");
        return (scripts.contains(SpicyTextDetection.Script.JAPANESE) && SpicyJapaneseChineseProcessor.canRomanizeJapanese(text))
                || (scripts.contains(SpicyTextDetection.Script.CHINESE) && SpicyTextDetection.itemChineseTest(text))
                || (scripts.contains(SpicyTextDetection.Script.CYRILLIC) && SpicyTextDetection.itemCyrillicTest(text))
                || (scripts.contains(SpicyTextDetection.Script.KOREAN) && SpicyTextDetection.itemKoreanTest(text))
                || (scripts.contains(SpicyTextDetection.Script.GREEK) && SpicyTextDetection.itemGreekTest(text));
    }

    public static String romanizeLine(String text, String wholeSongText, String language) {
        return romanizeLine(text, wholeSongText, language, SpotifyPlusConfig.CHINESE_MODE_PINYIN);
    }

    public static String romanizeLine(String text, String wholeSongText, String language, String chineseMode) {
        if (text == null || text.trim().isEmpty()) return text;
        String result = Normalizer.normalize(text, Normalizer.Form.NFKC);
        List<SpicyTextDetection.Script> scripts = SpicyTextDetection.detectPresentScripts(wholeSongText, language, "");
        boolean changed = false;

        for (SpicyTextDetection.Script script : scripts) {
            if (script == SpicyTextDetection.Script.JAPANESE && SpicyJapaneseChineseProcessor.canRomanizeJapanese(result)) {
                result = SpicyJapaneseChineseProcessor.romanizeJapaneseLine(result);
                changed = true;
            } else if (script == SpicyTextDetection.Script.CHINESE && SpicyTextDetection.itemChineseTest(result)) {
                result = SpicyJapaneseChineseProcessor.romanizeChineseLine(result, chineseMode);
                changed = true;
            } else if (script == SpicyTextDetection.Script.CYRILLIC && SpicyTextDetection.itemCyrillicTest(result)) {
                result = romanizeCyrillic(result);
                changed = true;
            } else if (script == SpicyTextDetection.Script.KOREAN && SpicyTextDetection.itemKoreanTest(result)) {
                result = romanizeKorean(result);
                changed = true;
            } else if (script == SpicyTextDetection.Script.GREEK && SpicyTextDetection.itemGreekTest(result)) {
                result = romanizeGreek(result);
                changed = true;
            }
        }

        return changed ? result : null;
    }

    /**
     * Port of Fork/Romanization.ts romanizeCyrillic():
     * transliterPkg.transliter(text, "bgn-pcgn") plus ASCII cleanup.
     */
    public static String romanizeCyrillic(String text) {
        if (text == null) return null;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            String mapped = BGN_PCGN.get(cp);
            if (mapped == null) out.appendCodePoint(cp);
            else out.append(mapped);
            i += Character.charCount(cp);
        }
        return postProcessCyrillic(out.toString());
    }

    private static String postProcessCyrillic(String value) {
        return value
                .replace("Ё", "Yo")
                .replace("ё", "yo")
                .replace("Ë", "Yo")
                .replace("ë", "yo")
                .replace("'", "")
                .replace("’", "")
                .replace("ǵ", "g")
                .replace("Ǵ", "G")
                .replace("ḱ", "k")
                .replace("Ḱ", "K")
                .replace("ẑ", "dz")
                .replace("Ẑ", "Dz")
                .replace("ì", "i")
                .replace("đ", "dj")
                .replace("Đ", "Dj")
                .replace("ć", "c")
                .replace("Ć", "C")
                .replace("ž", "zh")
                .replace("Ž", "Zh");
    }

    public static String romanizeKorean(String text) {
        if (text == null) return null;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            if (cp >= 0xAC00 && cp <= 0xD7A3) {
                int s = cp - 0xAC00;
                int initial = s / 588;
                int vowel = (s % 588) / 28;
                int fin = s % 28;
                out.append(HANGUL_INITIAL[initial]).append(HANGUL_VOWEL[vowel]).append(HANGUL_FINAL[fin]);
            } else {
                out.appendCodePoint(cp);
            }
            i += Character.charCount(cp);
        }
        return out.toString();
    }

    public static String romanizeGreek(String text) {
        if (text == null) return null;
        String normalized = stripGreekDiacritics(text);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < normalized.length(); ) {
            int cp = normalized.codePointAt(i);
            String mapped = GREEK.get(cp);
            if (mapped == null) out.appendCodePoint(cp);
            else out.append(mapped);
            i += Character.charCount(cp);
        }
        return out.toString();
    }

    private static String stripGreekDiacritics(String text) {
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < decomposed.length(); ) {
            int cp = decomposed.codePointAt(i);
            int type = Character.getType(cp);
            if (type != Character.NON_SPACING_MARK && type != Character.COMBINING_SPACING_MARK) out.appendCodePoint(cp);
            i += Character.charCount(cp);
        }
        return Normalizer.normalize(out.toString(), Normalizer.Form.NFC);
    }

    private static void put(String source, String target) {
        BGN_PCGN.put(source.codePointAt(0), target);
    }

    private static void putGreek(String source, String target) {
        GREEK.put(source.codePointAt(0), target);
    }
}
