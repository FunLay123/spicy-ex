package com.eza.spicyex.lyrics;

import java.util.List;

/**
 * Pure animation curve math for the native Spicy renderer — ports of the Spicy 6 desktop
 * interpolation ranges plus the letter falloff/pulse helpers. No Android types, no state, so the
 * curve breakpoints (which are where karaoke-timing regressions hide) are unit-testable.
 *
 * Inputs are normalized progress in [0,1]; outputs are scale/offset/opacity/gradient multipliers.
 */
public final class LyricAnimations {

    private LyricAnimations() {
    }

    // --- word-level karaoke curves ---

    /** Spicy 6 ScaleRange: 0 -> 0.95, 0.7 -> 1.075, 1 -> 1. */
    public static float scaleSpline(float t) {
        if (t <= 0.7f) return lerp(0.95f, 1.075f, t / 0.7f);
        return lerp(1.075f, 1.0f, (t - 0.7f) / 0.3f);
    }

    /** Spicy 6 YOffsetRange: 0 -> 0.01, 0.9 -> -1/52.5, 1 -> 0. */
    public static float yOffsetSpline(float t) {
        if (t <= 0.9f) return lerp(0.01f, -(1f / 52.5f), t / 0.9f);
        return lerp(-(1f / 52.5f), 0f, (t - 0.9f) / 0.1f);
    }

    /** Spicy 6 GlowRange: 0 -> 0, 0.15 -> 1, 0.6 -> 1, 1 -> 0. */
    public static float glowSpline(float t) {
        if (t <= 0.15f) return lerp(0f, 1f, t / 0.15f);
        if (t <= 0.6f) return 1f;
        return lerp(1f, 0f, (t - 0.6f) / 0.4f);
    }

    // --- letter-level karaoke curves (pop harder than words) ---

    /** Spicy 6 letter ScaleRange: 0 -> 0.95, 0.7 -> 1.18, 1 -> 1. */
    public static float letterScaleSpline(float t) {
        if (t <= 0.7f) return lerp(0.95f, 1.18f, t / 0.7f);
        return lerp(1.18f, 1.0f, (t - 0.7f) / 0.3f);
    }

    /** Spicy 6 letter YOffsetRange: 0 -> 0.01, 0.9 -> -1/50, 1 -> 0. */
    public static float letterYOffsetSpline(float t) {
        if (t <= 0.9f) return lerp(0.01f, -(1f / 50f), t / 0.9f);
        return lerp(-(1f / 50f), 0f, (t - 0.9f) / 0.1f);
    }

    /** d3 easeSinOut — desktop eases the active-letter gradient sweep with this. */
    public static float easeSinOut(float t) {
        return (float) Math.sin(clamp01(t) * (Math.PI / 2d));
    }

    /** Spatial falloff of the letter scale/offset around the active-letter anchor. */
    public static float letterMotionFalloff(float distance) {
        return (float) (1d / (1d + Math.pow(Math.max(0f, distance), 2.8d)));
    }

    /** Spatial falloff of the letter glow around the active-letter anchor. */
    public static float letterGlowFalloff(float distance) {
        return (float) (1d / (1d + Math.max(0f, distance) * 0.9d));
    }

    /** Fractional index of the active letter for a line at normalized time {@code timeAlpha}. */
    public static float activeLetterPosition(int letterCount, float timeAlpha) {
        if (letterCount <= 0) return 0f;
        float position = timeAlpha * letterCount;
        return Math.max(0f, Math.min(position, letterCount - 1f));
    }

    /** Convenience overload mirroring the renderer call site (uses only the list size). */
    public static float activeLetterPosition(List<?> letters, float timeAlpha) {
        return activeLetterPosition(letters == null ? 0 : letters.size(), timeAlpha);
    }

    // --- interlude dot curves ---

    /** Spicy 6 dot group scale range: [{0,0}, {0.2,1.05}, {0.925,1.15}, {1,0}]. */
    public static float dotMainScaleSpline(float t) {
        if (t <= 0.2f) return lerp(0f, 1.05f, t / 0.2f);
        if (t <= 0.925f) return lerp(1.05f, 1.15f, (t - 0.2f) / 0.725f);
        return lerp(1.15f, 0f, (t - 0.925f) / 0.075f);
    }

    /** Spicy 6 dot group opacity range: [{0,0}, {0.5,1}, {0.925,1}, {1,0}]. */
    public static float dotMainOpacitySpline(float t) {
        if (t <= 0.5f) return lerp(0f, 1f, t / 0.5f);
        if (t <= 0.925f) return 1f;
        return lerp(1f, 0f, (t - 0.925f) / 0.075f);
    }

    public static float dotScaleSpline(float t) {
        if (t <= 0.7f) return lerp(0.75f, 1.10f, t / 0.7f);
        return lerp(1.10f, 1.0f, (t - 0.7f) / 0.3f);
    }

    public static float dotYOffsetSpline(float t) {
        if (t <= 0.7f) return lerp(0.03f, -0.035f, t / 0.7f);
        return lerp(-0.035f, 0f, (t - 0.7f) / 0.3f);
    }

    public static float dotGlowSpline(float t) {
        if (t <= 0.18f) return lerp(0f, 1f, t / 0.18f);
        if (t <= 0.7f) return 1f;
        return lerp(1f, 0f, (t - 0.7f) / 0.3f);
    }

    public static float dotOpacitySpline(float t) {
        if (t <= 0.18f) return lerp(0f, 1f, t / 0.18f);
        if (t <= 0.85f) return 1f;
        return lerp(1f, 0.88f, (t - 0.85f) / 0.15f);
    }

    /** Slow breathing pulse for an idle interlude dot, keyed off playback position. */
    public static float dotPulse(long positionMs, int index) {
        double phase = ((positionMs / 1000d) + index * 0.18d) % 2.25d;
        double normalized = phase / 2.25d;
        if (normalized < 0.34d) return lerp(0.90f, 1.05f, (float) (normalized / 0.34d));
        if (normalized < 0.68d) return lerp(1.05f, 0.90f, (float) ((normalized - 0.34d) / 0.34d));
        return 0.90f;
    }

    // --- primitives (kept private so this class is the single source for the curve math) ---

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * Math.max(0f, Math.min(1f, t));
    }

    public static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
