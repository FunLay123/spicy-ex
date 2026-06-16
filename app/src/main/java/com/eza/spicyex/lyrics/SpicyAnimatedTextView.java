package com.eza.spicyex.lyrics;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.os.Build;
import android.widget.TextView;

/**
 * TextView with the Spicy sung/unsung karaoke gradient. The gradient position is in Spicy's
 * -20..100 coordinate space (-20 fully unsung, 100 fully sung); glow nudges the sung edge
 * toward full white.
 */
public class SpicyAnimatedTextView extends TextView {
    private float gradientPosition = -20f;
    private float glow = 0f;
    // Cached shader: rebuilding a LinearGradient on every frame for every word (with a software
    // layer) was a major source of scroll/animation jank. Rebuild only when an input changes.
    private Shader cachedShader;
    private float shaderPos = Float.NaN;
    private float shaderGlow = Float.NaN;
    private int shaderWidth = -1;

    // Words/letters use horizontal fill; line-level rows can switch to vertical fill by setting.
    private boolean verticalGradient;

    public SpicyAnimatedTextView(Context context) {
        super(context);
    }

    public void setVerticalGradient(boolean vertical) {
        if (this.verticalGradient == vertical) return;
        this.verticalGradient = vertical;
        cachedShader = null;
    }

    public void setGradientPosition(float gradientPosition, float glow) {
        if (Math.abs(this.gradientPosition - gradientPosition) < 0.5f && Math.abs(this.glow - glow) < 0.03f) return;
        this.gradientPosition = gradientPosition;
        this.glow = glow;
        if (Build.VERSION.SDK_INT >= 16) postInvalidateOnAnimation();
        else invalidate();
    }

    private Shader resolveShader(int extent) {
        if (cachedShader != null && extent == shaderWidth
                && Math.abs(gradientPosition - shaderPos) < 0.5f
                && Math.abs(glow - shaderGlow) < 0.03f) {
            return cachedShader;
        }
        // Spicy CSS parity (Mixed.css): --gradient-alpha 0.85 (sung), --gradient-alpha-end 0.35
        // (unsung). glow nudges the sung edge toward full white (desktop does this via text-shadow).
        int startAlpha = Math.round(255f * (0.85f + 0.15f * Math.max(0f, Math.min(1f, glow))));
        int endAlpha = Math.round(255f * 0.35f);
        int sungColor = Color.argb(startAlpha, 255, 255, 255);
        int unsungColor = Color.argb(endAlpha, 255, 255, 255);
        float origin = verticalGradient ? getPaddingTop() : getPaddingLeft();
        float far = origin + extent;
        float x0 = 0, y0 = 0, x1 = 0, y1 = 0;
        if (verticalGradient) { y0 = origin; y1 = far; } else { x0 = origin; x1 = far; }
        if (gradientPosition <= -19.5f) {
            cachedShader = new LinearGradient(x0, y0, x1, y1,
                    new int[]{unsungColor, unsungColor}, null, Shader.TileMode.CLAMP);
        } else if (gradientPosition >= 99.5f) {
            cachedShader = new LinearGradient(x0, y0, x1, y1,
                    new int[]{sungColor, sungColor}, null, Shader.TileMode.CLAMP);
        } else {
            float p0 = Math.max(0f, Math.min(1f, gradientPosition / 100f));
            float p1 = Math.max(p0 + 0.001f, Math.min(1f, (gradientPosition + 20f) / 100f));
            cachedShader = new LinearGradient(x0, y0, x1, y1,
                    new int[]{sungColor, unsungColor}, new float[]{p0, p1}, Shader.TileMode.CLAMP);
        }
        shaderPos = gradientPosition;
        shaderGlow = glow;
        shaderWidth = extent;
        return cachedShader;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Paint paint = getPaint();
        Shader oldShader = paint.getShader();
        int oldColor = paint.getColor();
        int extent = verticalGradient
                ? Math.max(1, getHeight() - getPaddingTop() - getPaddingBottom())
                : Math.max(1, getWidth() - getPaddingLeft() - getPaddingRight());
        // Drive ALL states through a shader, never paint.setColor(): TextView.onDraw resets the
        // paint color to mCurTextColor before drawing the layout, which would silently discard the
        // sung/unsung alpha and render every word uniformly. A shader survives that reset.
        paint.setShader(resolveShader(extent));
        super.onDraw(canvas);
        paint.setShader(oldShader);
        paint.setColor(oldColor);
    }
}
