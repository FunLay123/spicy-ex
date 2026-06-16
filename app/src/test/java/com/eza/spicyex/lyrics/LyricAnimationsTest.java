package com.eza.spicyex.lyrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;

/**
 * Locks the Spicy curve breakpoints in place. These piecewise interpolations are where
 * karaoke-timing regressions hide; the tests assert endpoints, the knot values, and continuity
 * across each breakpoint.
 */
public class LyricAnimationsTest {

    private static final float EPS = 1e-4f;

    private static void assertContinuous(java.util.function.Function<Float, Float> f, float breakpoint) {
        float before = f.apply(breakpoint - 1e-4f);
        float after = f.apply(breakpoint + 1e-4f);
        assertEquals("discontinuity at " + breakpoint, before, after, 2e-3f);
    }

    @Test
    public void scaleSplineKnots() {
        assertEquals(0.95f, LyricAnimations.scaleSpline(0f), EPS);
        assertEquals(1.075f, LyricAnimations.scaleSpline(0.7f), EPS);
        assertEquals(1.0f, LyricAnimations.scaleSpline(1f), EPS);
        assertContinuous(LyricAnimations::scaleSpline, 0.7f);
    }

    @Test
    public void yOffsetSplineKnots() {
        assertEquals(0.01f, LyricAnimations.yOffsetSpline(0f), EPS);
        assertEquals(-(1f / 52.5f), LyricAnimations.yOffsetSpline(0.9f), EPS);
        assertEquals(0f, LyricAnimations.yOffsetSpline(1f), EPS);
        assertContinuous(LyricAnimations::yOffsetSpline, 0.9f);
    }

    @Test
    public void glowSplinePlateau() {
        assertEquals(0f, LyricAnimations.glowSpline(0f), EPS);
        assertEquals(1f, LyricAnimations.glowSpline(0.15f), EPS);
        assertEquals(1f, LyricAnimations.glowSpline(0.4f), EPS);
        assertEquals(1f, LyricAnimations.glowSpline(0.6f), EPS);
        assertEquals(0f, LyricAnimations.glowSpline(1f), EPS);
        assertContinuous(LyricAnimations::glowSpline, 0.15f);
        assertContinuous(LyricAnimations::glowSpline, 0.6f);
    }

    @Test
    public void letterSplinesPopHarderThanWords() {
        assertEquals(1.18f, LyricAnimations.letterScaleSpline(0.7f), EPS);
        assertTrue(LyricAnimations.letterScaleSpline(0.7f) > LyricAnimations.scaleSpline(0.7f));
        assertEquals(0.95f, LyricAnimations.letterScaleSpline(0f), EPS);
        assertEquals(1.0f, LyricAnimations.letterScaleSpline(1f), EPS);
        assertContinuous(LyricAnimations::letterScaleSpline, 0.7f);
        assertContinuous(LyricAnimations::letterYOffsetSpline, 0.9f);
    }

    @Test
    public void easeSinOutEndpoints() {
        assertEquals(0f, LyricAnimations.easeSinOut(0f), EPS);
        assertEquals(1f, LyricAnimations.easeSinOut(1f), EPS);
        assertEquals((float) Math.sin(Math.PI / 4d), LyricAnimations.easeSinOut(0.5f), EPS);
        // clamps out-of-range input
        assertEquals(1f, LyricAnimations.easeSinOut(2f), EPS);
        assertEquals(0f, LyricAnimations.easeSinOut(-1f), EPS);
    }

    @Test
    public void dotMainSplinesKnotsAndContinuity() {
        assertEquals(0f, LyricAnimations.dotMainScaleSpline(0f), EPS);
        assertEquals(1.05f, LyricAnimations.dotMainScaleSpline(0.2f), EPS);
        assertEquals(1.15f, LyricAnimations.dotMainScaleSpline(0.925f), EPS);
        assertEquals(0f, LyricAnimations.dotMainScaleSpline(1f), EPS);
        assertContinuous(LyricAnimations::dotMainScaleSpline, 0.2f);
        assertContinuous(LyricAnimations::dotMainScaleSpline, 0.925f);

        assertEquals(0f, LyricAnimations.dotMainOpacitySpline(0f), EPS);
        assertEquals(1f, LyricAnimations.dotMainOpacitySpline(0.5f), EPS);
        assertEquals(1f, LyricAnimations.dotMainOpacitySpline(0.925f), EPS);
        assertEquals(0f, LyricAnimations.dotMainOpacitySpline(1f), EPS);
    }

    @Test
    public void dotDetailSplinesContinuous() {
        assertContinuous(LyricAnimations::dotScaleSpline, 0.7f);
        assertContinuous(LyricAnimations::dotYOffsetSpline, 0.7f);
        assertContinuous(LyricAnimations::dotGlowSpline, 0.18f);
        assertContinuous(LyricAnimations::dotGlowSpline, 0.7f);
        assertContinuous(LyricAnimations::dotOpacitySpline, 0.18f);
        assertContinuous(LyricAnimations::dotOpacitySpline, 0.85f);
        assertEquals(0.88f, LyricAnimations.dotOpacitySpline(1f), EPS);
    }

    @Test
    public void letterFalloffPeaksAtAnchor() {
        assertEquals(1f, LyricAnimations.letterMotionFalloff(0f), EPS);
        assertEquals(1f, LyricAnimations.letterGlowFalloff(0f), EPS);
        // strictly decreasing with distance
        assertTrue(LyricAnimations.letterMotionFalloff(1f) < LyricAnimations.letterMotionFalloff(0f));
        assertTrue(LyricAnimations.letterMotionFalloff(2f) < LyricAnimations.letterMotionFalloff(1f));
        assertTrue(LyricAnimations.letterGlowFalloff(3f) < LyricAnimations.letterGlowFalloff(1f));
        // negative distance is clamped to the peak
        assertEquals(1f, LyricAnimations.letterMotionFalloff(-5f), EPS);
    }

    @Test
    public void activeLetterPositionTracksTimeAndClamps() {
        assertEquals(0f, LyricAnimations.activeLetterPosition(5, 0f), EPS);
        assertEquals(2.5f, LyricAnimations.activeLetterPosition(5, 0.5f), EPS);
        assertEquals(4f, LyricAnimations.activeLetterPosition(5, 1f), EPS); // clamped to count-1
        assertEquals(0f, LyricAnimations.activeLetterPosition(0, 0.5f), EPS);
        assertEquals(LyricAnimations.activeLetterPosition(3, 0.5f),
                LyricAnimations.activeLetterPosition(Arrays.asList(1, 2, 3), 0.5f), EPS);
    }

    @Test
    public void dotPulseStaysWithinRange() {
        for (long t = 0; t < 5000; t += 50) {
            for (int i = 0; i < 3; i++) {
                float p = LyricAnimations.dotPulse(t, i);
                assertTrue("pulse out of range: " + p, p >= 0.90f - EPS && p <= 1.05f + EPS);
            }
        }
    }

    @Test
    public void lerpClamps() {
        assertEquals(5f, LyricAnimations.lerp(0f, 10f, 0.5f), EPS);
        assertEquals(0f, LyricAnimations.lerp(0f, 10f, -1f), EPS);
        assertEquals(10f, LyricAnimations.lerp(0f, 10f, 2f), EPS);
        assertEquals(0f, LyricAnimations.clamp01(-0.5f), EPS);
        assertEquals(1f, LyricAnimations.clamp01(1.5f), EPS);
    }
}
