package com.eza.spicyex.lyrics;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.ImageButton;
import android.widget.TextView;

import com.eza.spicyex.Settings;
import com.eza.spicyex.SpotifyPlusConfig;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Text, font, and chip factory for the native lyrics shell. */
public final class LyricsTextFactory {
    private final Activity activity;
    private final SpotifyPlusConfig config;
    private final Map<String, Typeface> typefaceCache = new LinkedHashMap<>();

    public LyricsTextFactory(Activity activity, SpotifyPlusConfig config) {
        this.activity = activity;
        this.config = config;
    }

    public Typeface resolveTypeface(boolean bold) {
        // Weight-driven so the "Bold lyrics" toggle has a real, visible effect: both bundled faces
        // are single-weight (spotifymix = medium, sf-pro-display = bold), so the old code returned a
        // bold face for BOTH states and toggling did nothing. Map bold -> heavy face, regular ->
        // medium face. (LYRICS_FONT family selection needs per-weight assets to honor again — TODO.)
        String key = bold ? "|bold" : "|regular";
        Typeface cached = typefaceCache.get(key);
        if (cached != null) return cached;
        Typeface resolved;
        try {
            resolved = Typeface.createFromAsset(activity.getAssets(),
                    bold ? "fonts/sf-pro-display-bold.ttf" : "fonts/spotifymix-medium.ttf");
        } catch (Throwable t) {
            resolved = bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT;
        }
        typefaceCache.put(key, resolved);
        return resolved;
    }

    public void emphasizePrimaryLyric(TextView view) {
        if (view == null) return;
        view.getPaint().setFakeBoldText(true);
    }

    public TextView createChip(Context context, String value) {
        TextView view = createText(context, value, 14, Color.WHITE, resolveTypeface(true));
        view.setGravity(Gravity.CENTER);
        view.setMinWidth(dp(44));
        view.setMinHeight(dp(44));
        view.setIncludeFontPadding(false);
        view.setPadding(0, 0, 0, dp(1));
        return view;
    }

    public void styleChip(TextView view, boolean enabled) {
        if (view == null) return;
        view.setTextColor(enabled ? Color.rgb(245, 245, 248) : Color.rgb(178, 178, 186));
        view.setAlpha(enabled ? 1.0f : 0.78f);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        bg.setColor(enabled ? Color.argb(78, 255, 255, 255) : Color.argb(30, 255, 255, 255));
        bg.setStroke(dp(1), enabled ? Color.argb(88, 255, 255, 255) : Color.argb(38, 255, 255, 255));
        view.setBackground(bg);
    }

    public void styleIconChip(ImageButton view, boolean enabled) {
        if (view == null) return;
        view.setColorFilter(enabled ? Color.rgb(245, 245, 248) : Color.rgb(178, 178, 186));
        view.setAlpha(enabled ? 0.96f : 0.72f);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        bg.setColor(enabled ? Color.argb(48, 255, 255, 255) : Color.argb(22, 255, 255, 255));
        bg.setStroke(dp(1), enabled ? Color.argb(58, 255, 255, 255) : Color.argb(30, 255, 255, 255));
        view.setBackground(bg);
    }

    public TextView createText(Context context, String value, int sp, int color, Typeface typeface) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(typeface);
        view.setIncludeFontPadding(true);
        view.setLineSpacing(0f, 1.04f);
        return view;
    }

    public SpicyAnimatedTextView createSecondaryAnimatedText(Context context, String value, int sp, Typeface typeface) {
        SpicyAnimatedTextView view = new SpicyAnimatedTextView(context);
        view.setText(value);
        view.setTextSize(sp);
        view.setTypeface(typeface);
        view.setIncludeFontPadding(true);
        view.setLineSpacing(0f, 1.04f);
        view.setGradientPosition(-20f, 0f);
        return view;
    }

    private int dp(int value) {
        float density = activity == null ? 1f : activity.getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
