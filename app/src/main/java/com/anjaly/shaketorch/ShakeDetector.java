package com.anjaly.shaketorch;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

/**
 * Detects a Moto-style "chop chop": two sharp jerks of the phone within a short window.
 * Requiring two jerks (not one) keeps it from firing when you walk, run or drop the phone on a bed.
 */
public class ShakeDetector implements SensorEventListener {

    public interface Listener { void onShake(); }

    private static final long MIN_GAP_BETWEEN_CHOPS_MS = 150;  // ignore the same jerk being counted twice
    private static final long MAX_WINDOW_MS = 900;             // both chops must happen within this
    private static final long COOLDOWN_MS = 1500;              // pause after a toggle

    private final Listener listener;
    private volatile float threshold;

    // Low-pass filter to estimate gravity, so we can remove it from raw readings
    private final float[] gravity = new float[3];
    private boolean gravityReady = false;

    private int chopCount = 0;
    private long firstChopAt = 0;
    private long lastChopAt = 0;
    private long lastTriggerAt = 0;

    public ShakeDetector(Listener listener, float threshold) {
        this.listener = listener;
        this.threshold = threshold;
    }

    public void setThreshold(float threshold) { this.threshold = threshold; }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0], y = event.values[1], z = event.values[2];

        if (!gravityReady) {
            gravity[0] = x; gravity[1] = y; gravity[2] = z;
            gravityReady = true;
            return;
        }
        final float alpha = 0.8f;
        gravity[0] = alpha * gravity[0] + (1 - alpha) * x;
        gravity[1] = alpha * gravity[1] + (1 - alpha) * y;
        gravity[2] = alpha * gravity[2] + (1 - alpha) * z;

        float lx = x - gravity[0], ly = y - gravity[1], lz = z - gravity[2];
        float force = (float) Math.sqrt(lx * lx + ly * ly + lz * lz);

        long now = System.currentTimeMillis();
        if (now - lastTriggerAt < COOLDOWN_MS) return;
        if (force < threshold) return;
        if (now - lastChopAt < MIN_GAP_BETWEEN_CHOPS_MS) return;

        if (chopCount == 0 || now - firstChopAt > MAX_WINDOW_MS) {
            chopCount = 1;
            firstChopAt = now;
        } else {
            chopCount++;
        }
        lastChopAt = now;

        if (chopCount >= 2) {
            chopCount = 0;
            lastTriggerAt = now;
            listener.onShake();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
