package com.anjaly.shaketorch;

import android.content.Context;
import android.content.SharedPreferences;

/** Small helper around the app's saved settings. */
public final class Prefs {
    private static final String FILE = "shake_torch";
    public static final String KEY_ENABLED = "enabled";
    public static final String KEY_SENSITIVITY = "sensitivity";   // 0 (hard) .. 10 (easy)
    public static final String KEY_SCREEN_OFF = "screen_off";
    public static final String KEY_VIBRATE = "vibrate";

    private Prefs() {}

    public static SharedPreferences get(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static boolean enabled(Context c)     { return get(c).getBoolean(KEY_ENABLED, false); }
    public static int sensitivity(Context c)     { return get(c).getInt(KEY_SENSITIVITY, 5); }
    public static boolean screenOff(Context c)   { return get(c).getBoolean(KEY_SCREEN_OFF, true); }
    public static boolean vibrate(Context c)     { return get(c).getBoolean(KEY_VIBRATE, true); }

    /**
     * Converts the 0–10 slider into a force threshold (m/s² above gravity).
     * 0 = needs a very hard shake (~30), 10 = triggers easily (~12).
     */
    public static float thresholdFor(int sensitivity) {
        return 30f - sensitivity * 1.8f;
    }
}
