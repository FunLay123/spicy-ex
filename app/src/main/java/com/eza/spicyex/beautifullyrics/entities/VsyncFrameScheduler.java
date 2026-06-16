package com.eza.spicyex.beautifullyrics.entities;

import android.view.Choreographer;

public class VsyncFrameScheduler implements Choreographer.FrameCallback {
    public interface FrameListener {
        void onFrame(double deltaTimeSeconds);
    }

    private final Choreographer choreographer;
    private final FrameListener listener;

    private boolean running;
    private long lastFrameNanos;

    public VsyncFrameScheduler(FrameListener listener) {
        this(Choreographer.getInstance(), listener);
    }

    VsyncFrameScheduler(Choreographer choreographer, FrameListener listener) {
        this.choreographer = choreographer;
        this.listener = listener;
    }

    public void start() {
        if (running) {
            return;
        }

        running = true;
        lastFrameNanos = 0L;
        choreographer.postFrameCallback(this);
    }

    public void stop() {
        if (!running) {
            return;
        }

        running = false;
        lastFrameNanos = 0L;
        choreographer.removeFrameCallback(this);
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (!running) {
            return;
        }

        double deltaTimeSeconds = 0.0d;
        if (lastFrameNanos != 0L) {
            deltaTimeSeconds = (frameTimeNanos - lastFrameNanos) / 1_000_000_000.0d;
        }

        lastFrameNanos = frameTimeNanos;
        listener.onFrame(deltaTimeSeconds);

        if (running) {
            choreographer.postFrameCallback(this);
        }
    }
}
