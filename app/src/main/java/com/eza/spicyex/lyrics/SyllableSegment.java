package com.eza.spicyex.lyrics;

import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * One timed word/syllable. Carries both parse data and (when mounted) the renderer's view/spring
 * state; view fields are owned by the renderer and are never copied by {@link #copyOf}.
 */
public class SyllableSegment {
    public String text = "";
    public String romanizedText = "";
    public long startMs;
    public long endMs;
    public long totalMs;
    public boolean partOfWord;
    public boolean dot;
    public boolean bgWord;
    public View view;
    public SpicyAnimatedTextView textView;
    public SpicyAnimatedTextView romanizedTextView;
    public final List<AnimatedLetterState> letters = new ArrayList<>();
    public Spring scaleSpring;
    public Spring ySpring;
    public Spring glowSpring;

    public static SyllableSegment copyOf(SyllableSegment source) {
        if (source == null) return null;
        SyllableSegment copy = new SyllableSegment();
        copy.text = LyricsDocument.safe(source.text);
        copy.romanizedText = LyricsDocument.safe(source.romanizedText);
        copy.startMs = source.startMs;
        copy.endMs = source.endMs;
        copy.totalMs = source.totalMs;
        copy.partOfWord = source.partOfWord;
        copy.dot = source.dot;
        copy.bgWord = source.bgWord;
        return copy;
    }
}
