package com.eza.spicyex.lyrics;

import com.eza.spicyex.SpotifyPlusConfig;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XposedBridge;

/** Local Japanese/Chinese/generic romanization helpers for lyric documents. */
public final class LyricsLocalRomanizer {
    private static final String TAG = "[SpotifyPlusLocalRomanizer]";

    private LyricsLocalRomanizer() {
    }

    public static boolean shouldLocalRomanize(boolean showRomanization, String chineseMode, LyricsDocument doc, LyricsLine line, String fullText) {
        if (!showRomanization || line == null || isBlank(line.text)) return false;
        List<SpicyTextDetection.Script> scripts = SpicyTextDetection.detectPresentScripts(fullText, doc == null ? "" : doc.language, "");
        boolean needsJapaneseReading = scripts.contains(SpicyTextDetection.Script.JAPANESE)
                && SpicyJapaneseChineseProcessor.canRomanizeJapanese(line.text);
        if (needsJapaneseReading) return true;
        boolean needsChineseMode = scripts.contains(SpicyTextDetection.Script.CHINESE)
                && SpicyTextDetection.itemChineseTest(line.text)
                && (isBlank(line.chineseMode) || !normalizeChineseMode(line.chineseMode).equals(normalizeChineseMode(chineseMode)));
        if (needsChineseMode) return true;
        if (!shouldGoogleRomanize(showRomanization, line)) return false;
        return SpicyRomanizer.canRomanizeLocally(line.text, fullText, doc == null ? "" : doc.language);
    }

    public static String romanizeLine(String chineseMode, LyricsDocument doc, LyricsLine line, String fullText) {
        try {
            if (doc != null && SpicyTextDetection.detectPresentScripts(fullText, doc.language, "").contains(SpicyTextDetection.Script.JAPANESE)
                    && SpicyJapaneseChineseProcessor.canRomanizeJapanese(line.text)) {
                SpicyJapaneseChineseProcessor.JapaneseReading local =
                        SpicyJapaneseChineseProcessor.analyzeJapaneseLine(line.text, null);
                if (local != null) {
                    boolean hasProviderFurigana = line.japaneseReading != null
                            && line.japaneseReading.furigana != null
                            && !line.japaneseReading.furigana.isEmpty();
                    if (!hasProviderFurigana) line.japaneseReading = local;
                    if (!isBlank(local.romaji)) return local.romaji;
                }
            }
            if (doc != null && SpicyTextDetection.detectPresentScripts(fullText, doc.language, "").contains(SpicyTextDetection.Script.CHINESE)
                    && SpicyTextDetection.itemChineseTest(line.text)) {
                line.chineseMode = normalizeChineseMode(chineseMode);
                return SpicyJapaneseChineseProcessor.romanizeChineseLine(line.text, line.chineseMode);
            }
            return SpicyRomanizer.romanizeLine(line.text, fullText, doc == null ? "" : doc.language, chineseMode);
        } catch (Throwable t) {
            XposedBridge.log(TAG + " local romanization failed: " + t);
            return "";
        }
    }

    public static void populateLocalSegmentRomanization(String chineseMode, LyricsDocument doc, LyricsLine line, String fullText) {
        if (line == null || line.syllables == null || line.syllables.isEmpty()) return;
        List<SpicyTextDetection.Script> scripts = SpicyTextDetection.detectPresentScripts(fullText, doc == null ? "" : doc.language, "");
        if (scripts.contains(SpicyTextDetection.Script.JAPANESE)
                && SpicyJapaneseChineseProcessor.canRomanizeJapanese(line.text)) {
            ArrayList<String> syllableTexts = new ArrayList<>();
            for (SyllableSegment seg : line.syllables) syllableTexts.add(seg == null ? "" : seg.text);
            List<String> localSyllables = SpicyJapaneseChineseProcessor.romanizeJapaneseSyllables(line.text, syllableTexts);
            if (localSyllables.size() == line.syllables.size()) {
                for (int i = 0; i < line.syllables.size(); i++) {
                    SyllableSegment seg = line.syllables.get(i);
                    if (seg == null || !isBlank(seg.romanizedText)) continue;
                    String local = localSyllables.get(i);
                    seg.romanizedText = !isBlank(local) && !local.equals(seg.text) && !SpicyTextDetection.hasRomanizableScript(local)
                            ? local : "";
                }
                return;
            }
        }
        for (SyllableSegment seg : line.syllables) {
            if (seg == null || isBlank(seg.text)) continue;
            if (!isBlank(seg.romanizedText)) continue;
            String local = romanizeText(chineseMode, doc, seg.text, fullText, line.chineseMode);
            seg.romanizedText = !isBlank(local) && !local.equals(seg.text) && !SpicyTextDetection.hasRomanizableScript(local)
                    ? local : "";
        }
    }

    public static void clearSegmentRomanization(LyricsLine line) {
        if (line == null || line.syllables == null) return;
        for (SyllableSegment seg : line.syllables) {
            if (seg != null) seg.romanizedText = "";
        }
    }

    public static boolean shouldGoogleRomanize(boolean showRomanization, LyricsLine line) {
        if (!showRomanization || line == null || isBlank(line.text) || !SpicyTextDetection.hasRomanizableScript(line.text)) return false;
        return isBlank(line.romanizedText) || SpicyTextDetection.hasRomanizableScript(line.romanizedText);
    }

    public static boolean shouldGoogleTranslate(LyricsDocument doc, LyricsLine line) {
        if (line == null || isBlank(line.text) || !isBlank(line.translatedText)) return false;
        return SpicyProcessing.shouldTranslateLine(line.text, doc == null ? "" : doc.language, "en");
    }

    public static String normalizeChineseMode(String mode) {
        if ("jyutping".equalsIgnoreCase(mode) || "cantonese".equalsIgnoreCase(mode)) return SpotifyPlusConfig.CHINESE_MODE_JYUTPING;
        return SpotifyPlusConfig.CHINESE_MODE_PINYIN;
    }

    public static String romanizeText(String chineseMode, LyricsDocument doc, String text, String fullText, String lineChineseMode) {
        try {
            String language = doc == null ? "" : doc.language;
            List<SpicyTextDetection.Script> scripts = SpicyTextDetection.detectPresentScripts(fullText, language, "");
            if (scripts.contains(SpicyTextDetection.Script.JAPANESE)
                    && SpicyJapaneseChineseProcessor.canRomanizeJapanese(text)) {
                SpicyJapaneseChineseProcessor.JapaneseReading local =
                        SpicyJapaneseChineseProcessor.analyzeJapaneseLine(text, null);
                return local == null ? "" : safe(local.romaji);
            }
            if (scripts.contains(SpicyTextDetection.Script.CHINESE)
                    && SpicyTextDetection.itemChineseTest(text)) {
                String mode = normalizeChineseMode(isBlank(lineChineseMode) ? chineseMode : lineChineseMode);
                return SpicyJapaneseChineseProcessor.romanizeChineseLine(text, mode);
            }
            return SpicyRomanizer.romanizeLine(text, fullText, language, chineseMode);
        } catch (Throwable t) {
            XposedBridge.log(TAG + " local segment romanization failed: " + t);
            return "";
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
