package com.eza.spicyex.lyrics;

import android.content.Context;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Per-renderer frame style deduplication. Batching these high-frequency property writes keeps the
 * vsync path from repeatedly setting the same values, and owning the queues per shell prevents one
 * destroyed lyrics view from pinning another shell's views through static strong references.
 */
public final class FrameStyleBatcher {
    private final WeakHashMap<View, AppliedStyleState> styleCache = new WeakHashMap<>();
    private final LinkedHashMap<View, PendingStyleState> styleQueue = new LinkedHashMap<>();
    private final float density;

    public FrameStyleBatcher(Context context) {
        density = context == null ? 1f : context.getResources().getDisplayMetrics().density;
    }

    public void applyAlphaIfChanged(View view, float alpha) {
        queueFloatStyle(view, StyleField.ALPHA, alpha, 0.01f);
    }

    public void applyScaleIfChanged(View view, float scaleX, float scaleY) {
        queueFloatStyle(view, StyleField.SCALE_X, scaleX, 0.002f);
        queueFloatStyle(view, StyleField.SCALE_Y, scaleY, 0.002f);
    }

    public void applyTranslationYIfChanged(View view, float translationY) {
        queueFloatStyle(view, StyleField.TRANSLATION_Y, translationY, 0.5f);
    }

    public void queueBlurIfChanged(View view, float blurPx, float epsilon) {
        queueFloatStyle(view, StyleField.BLUR, blurPx, epsilon);
    }

    public void clearPendingWrites() {
        styleQueue.clear();
    }

    public void flush() {
        if (styleQueue.isEmpty()) return;
        for (Map.Entry<View, PendingStyleState> entry : styleQueue.entrySet()) {
            View view = entry.getKey();
            if (view == null || !view.isAttachedToWindow()) continue;
            PendingStyleState pending = entry.getValue();
            if (pending.alpha != null) view.setAlpha(pending.alpha);
            if (pending.scaleX != null) view.setScaleX(pending.scaleX);
            if (pending.scaleY != null) view.setScaleY(pending.scaleY);
            if (pending.translationY != null) view.setTranslationY(pending.translationY);
            if (pending.blurPx != null) applyBlurEffectImmediate(view, pending.blurPx);
        }
        styleQueue.clear();
    }

    public void invalidateRecursive(View view) {
        if (view == null) return;
        styleCache.remove(view);
        styleQueue.remove(view);
        if (!(view instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            invalidateRecursive(group.getChildAt(i));
        }
    }

    private void queueFloatStyle(View view, StyleField field, float value, float epsilon) {
        if (view == null) return;
        AppliedStyleState applied = styleCache.get(view);
        if (applied == null) {
            applied = new AppliedStyleState();
            styleCache.put(view, applied);
        }
        Float previous = applied.get(field);
        if (previous != null && Math.abs(previous - value) <= epsilon) return;
        applied.set(field, value);
        PendingStyleState pending = styleQueue.get(view);
        if (pending == null) {
            pending = new PendingStyleState();
            styleQueue.put(view, pending);
        }
        pending.set(field, value);
    }

    private void applyBlurEffectImmediate(View view, float blurPx) {
        if (view == null || Build.VERSION.SDK_INT < 31) return;
        if (blurPx <= 0.05f) {
            view.setRenderEffect(null);
        } else {
            float radius = blurPx * density;
            view.setRenderEffect(RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP));
        }
    }

    private enum StyleField { ALPHA, SCALE_X, SCALE_Y, TRANSLATION_Y, BLUR }

    private static class AppliedStyleState {
        Float alpha;
        Float scaleX;
        Float scaleY;
        Float translationY;
        Float blurPx;

        Float get(StyleField field) {
            switch (field) {
                case ALPHA: return alpha;
                case SCALE_X: return scaleX;
                case SCALE_Y: return scaleY;
                case TRANSLATION_Y: return translationY;
                case BLUR: return blurPx;
            }
            return null;
        }

        void set(StyleField field, Float value) {
            switch (field) {
                case ALPHA: alpha = value; break;
                case SCALE_X: scaleX = value; break;
                case SCALE_Y: scaleY = value; break;
                case TRANSLATION_Y: translationY = value; break;
                case BLUR: blurPx = value; break;
            }
        }
    }

    private static final class PendingStyleState extends AppliedStyleState {}
}
