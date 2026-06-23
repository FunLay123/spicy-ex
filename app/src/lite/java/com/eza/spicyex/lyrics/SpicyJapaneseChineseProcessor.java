package com.eza.spicyex.lyrics;

import java.util.ArrayList;
import java.util.List;

/** Lite build stub: JP/CN dictionary-backed romanization lives in the full flavor. */
public final class SpicyJapaneseChineseProcessor {
    public static final class FuriganaSegment {
        public final int start;
        public final int end;
        public final String reading;

        public FuriganaSegment(int start, int end, String reading) {
            this.start = start;
            this.end = end;
            this.reading = reading == null ? "" : reading;
        }
    }

    public static final class JapaneseReading {
        public final String sourceText;
        public final String romaji;
        public final List<FuriganaSegment> furigana;

        public JapaneseReading(String sourceText, String romaji, List<FuriganaSegment> furigana) {
            this.sourceText = sourceText == null ? "" : sourceText;
            this.romaji = romaji == null ? "" : romaji;
            this.furigana = furigana == null ? new ArrayList<>() : furigana;
        }
    }

    private SpicyJapaneseChineseProcessor() {
    }

    public static boolean canRomanizeJapanese(String text) {
        return false;
    }

    public static boolean canRomanizeChinese(String text) {
        return false;
    }

    public static JapaneseReading analyzeJapaneseLine(String text, String fullSpacedRomaji) {
        return null;
    }

    public static JapaneseReading analyzeJapaneseLineWithProviderFurigana(String text, List<FuriganaSegment> furigana) {
        return null;
    }

    public static String romanizeJapaneseLine(String text) {
        return "";
    }

    public static String romanizeJapaneseLineFromFurigana(String text, List<FuriganaSegment> furigana) {
        return "";
    }

    public static List<String> romanizeJapaneseSyllables(String lineText, List<String> syllableTexts) {
        ArrayList<String> out = new ArrayList<>();
        if (syllableTexts != null) {
            for (int i = 0; i < syllableTexts.size(); i++) out.add("");
        }
        return out;
    }

    public static String romanizeChineseLine(String text, String mode) {
        return "";
    }

    public static String romanizeChineseLine(String text, String mode, boolean tones) {
        return "";
    }
}
