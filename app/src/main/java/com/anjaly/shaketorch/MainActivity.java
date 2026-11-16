package com.anjaly.shaketorch;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/** One simple settings screen, built in code so there are no layout files to manage. */
public class MainActivity extends Activity {

    private TextView sensitivityLabel;
    private TextView batteryStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int pad = dp(20);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, dp(32), pad, pad);

        TextView title = new TextView(this);
        title.setText("Shake Torch");
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView hint = new TextView(this);
        hint.setText("Give the phone two quick shakes (a chop-chop motion) to turn the flashlight on or off.");
        hint.setTextSize(15);
        hint.setPadding(0, dp(8), 0, dp(24));
        root.addView(hint);

        // Main on/off switch
        Switch enable = makeSwitch("Shake to toggle flashlight", Prefs.enabled(this));
        enable.setOnCheckedChangeListener((b, on) -> {
            Prefs.get(this).edit().putBoolean(Prefs.KEY_ENABLED, on).apply();
            if (on) {
                askNotificationPermission();
                ShakeService.start(this);
            } else {
                ShakeService.stop(this);
            }
        });
        root.addView(enable);

        // Works with screen off
        Switch screenOff = makeSwitch("Work when screen is off", Prefs.screenOff(this));
        screenOff.setOnCheckedChangeListener((b, on) ->
                Prefs.get(this).edit().putBoolean(Prefs.KEY_SCREEN_OFF, on).apply());
        root.addView(screenOff);
        root.addView(note("Uses a little more battery, but works like Motorola's gesture."));

        // Vibration feedback
        Switch vibrate = makeSwitch("Vibrate when toggled", Prefs.vibrate(this));
        vibrate.setOnCheckedChangeListener((b, on) ->
                Prefs.get(this).edit().putBoolean(Prefs.KEY_VIBRATE, on).apply());
        root.addView(vibrate);

        // Sensitivity slider
        sensitivityLabel = new TextView(this);
        sensitivityLabel.setTextSize(17);
        sensitivityLabel.setPadding(0, dp(24), 0, dp(4));
        root.addView(sensitivityLabel);

        SeekBar sensitivity = new SeekBar(this);
        sensitivity.setMax(10);
        sensitivity.setProgress(Prefs.sensitivity(this));
        updateSensitivityLabel(Prefs.sensitivity(this));
        sensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                updateSensitivityLabel(p);
                Prefs.get(MainActivity.this).edit().putInt(Prefs.KEY_SENSITIVITY, p).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        root.addView(sensitivity);
        root.addView(note("If it turns on by accident in your pocket, slide left. If it's hard to trigger, slide right."));

        // Vivo battery section — important, or Vivo will kill the app in the background
        TextView vivoTitle = new TextView(this);
        vivoTitle.setText("Keep it running on Vivo");
        vivoTitle.setTextSize(19);
        vivoTitle.setTypeface(Typeface.DEFAULT_BOLD);
        vivoTitle.setPadding(0, dp(32), 0, dp(4));
        root.addView(vivoTitle);

        batteryStatus = new TextView(this);
        batteryStatus.setTextSize(15);
        root.addView(batteryStatus);

        Button battery = new Button(this);
        battery.setText("1. Allow unrestricted battery use");
        battery.setOnClickListener(v -> requestIgnoreBatteryOptimizations());
        root.addView(battery);

        Button appInfo = new Button(this);
        appInfo.setText("2. Open app settings (turn on Autostart / background)");
        appInfo.setOnClickListener(v -> openAppSettings());
        root.addView(appInfo);

        root.addView(note("Also: open Recent apps, pull down on Shake Torch's card (or tap its menu) and choose Lock, so clearing recents doesn't close it."));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);

        // Make sure the service is running if it was enabled before
        if (Prefs.enabled(this)) ShakeService.start(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        boolean ok = pm.isIgnoringBatteryOptimizations(getPackageName());
        batteryStatus.setText(ok ? "✓ Battery restriction removed" : "⚠ Battery is still restricted — tap button 1");
        batteryStatus.setTextColor(ok ? Color.rgb(46, 125, 50) : Color.rgb(198, 40, 40));
    }

    private void updateSensitivityLabel(int p) {
        String word = p <= 3 ? "Hard shake" : p >= 7 ? "Light shake" : "Medium";
        sensitivityLabel.setText("Sensitivity: " + p + " / 10  (" + word + ")");
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    private void requestIgnoreBatteryOptimizations() {
        try {
            Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + getPackageName()));
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            try {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            } catch (ActivityNotFoundException e2) {
                Toast.makeText(this, "Open Settings > Battery and allow Shake Torch to run in background", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void openAppSettings() {
        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName()));
        startActivity(i);
    }

    private Switch makeSwitch(String text, boolean checked) {
        Switch s = new Switch(this);
        s.setText(text);
        s.setTextSize(17);
        s.setChecked(checked);
        s.setPadding(0, dp(12), 0, dp(12));
        s.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return s;
    }

    private TextView note(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(13);
        t.setTextColor(Color.GRAY);
        t.setGravity(Gravity.START);
        t.setPadding(0, 0, 0, dp(8));
        return t;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
