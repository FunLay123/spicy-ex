package com.eza.spicyex.lyrics;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

/**
 * Toggle-chip progress spinner. We do NOT hand-draw the arc — this wraps androidx's
 * {@link CircularProgressDrawable} (the standard Material indeterminate refresh spinner used across
 * the platform), which is polished and proven and needs no Material theme. The wrapper only:
 * <ol>
 *   <li>draws nothing while inactive (the bare CircularProgressDrawable would draw a static arc),</li>
 *   <li>sizes the ring to ride the chip's rim,</li>
 *   <li>exposes a simple {@link #setActive} and forwards the inner drawable's animation
 *       invalidations to the host view (so it repaints as a chip foreground).</li>
 * </ol>
 */
public final class ChipSpinnerDrawable extends Drawable {
    private final CircularProgressDrawable inner;
    private boolean active;

    private final Callback relay = new Callback() {
        @Override public void invalidateDrawable(Drawable who) { invalidateSelf(); }
        @Override public void scheduleDrawable(Drawable who, Runnable what, long when) { scheduleSelf(what, when); }
        @Override public void unscheduleDrawable(Drawable who, Runnable what) { unscheduleSelf(what); }
    };

    public ChipSpinnerDrawable(Context context) {
        inner = new CircularProgressDrawable(context);
        inner.setStyle(CircularProgressDrawable.DEFAULT);
        inner.setColorSchemeColors(0xFF1ED760); // Spotify accent green
        inner.setStrokeCap(Paint.Cap.ROUND);
        inner.setCallback(relay);
    }

    /** Start/stop the spinner. Idempotent — safe to call every frame. */
    public void setActive(boolean value) {
        if (value == active) return;
        active = value;
        if (value) {
            inner.start();
        } else {
            inner.stop();
        }
        invalidateSelf();
    }

    public boolean isActive() {
        return active;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        int size = Math.min(bounds.width(), bounds.height());
        if (size <= 0) return;
        float stroke = Math.max(3f, size * 0.075f);
        inner.setStrokeWidth(stroke);
        // Ring centered near the rim so it spins on the button edge.
        inner.setCenterRadius(size / 2f - stroke);
        inner.setBounds(bounds);
    }

    @Override
    public void draw(Canvas canvas) {
        if (!active) return;
        inner.draw(canvas);
    }

    @Override
    public void setAlpha(int alpha) {
        inner.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        inner.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
