package com.anjaly.shaketorch;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;

/**
 * Background service that keeps the shake detector running, even when the app is closed.
 * Android requires a visible notification for this; you can hide it from the notification settings.
 */
public class ShakeService extends Service implements ShakeDetector.Listener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String CHANNEL_ID = "shake_torch_service";
    private static final int NOTIFICATION_ID = 1;

    private SensorManager sensorManager;
    private ShakeDetector detector;
    private TorchController torch;
    private HandlerThread sensorThread;
    private PowerManager.WakeLock wakeLock;

    public static void start(Context c) {
        Intent i = new Intent(c, ShakeService.class);
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i);
        else c.startService(i);
    }

    public static void stop(Context c) {
        c.stopService(new Intent(c, ShakeService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startInForeground();

        torch = new TorchController(this);
        detector = new ShakeDetector(this, Prefs.thresholdFor(Prefs.sensitivity(this)));

        // Read the sensor on a background thread so the main thread stays free
        sensorThread = new HandlerThread("shake-sensor");
        sensorThread.start();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accel != null) {
            sensorManager.registerListener(detector, accel,
                    SensorManager.SENSOR_DELAY_GAME, new Handler(sensorThread.getLooper()));
        }

        updateWakeLock();
        Prefs.get(this).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // ask Android to restart us if it kills the service
    }

    private void startInForeground() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID,
                "Shake detection", NotificationManager.IMPORTANCE_MIN);
        ch.setShowBadge(false);
        nm.createNotificationChannel(ch);

        PendingIntent open = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Notification n = new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_torch)
                .setContentTitle("Shake Torch is on")
                .setContentText("Shake twice to toggle the flashlight")
                .setContentIntent(open)
                .setOngoing(true)
                .build();

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, n);
        }
    }

    private void updateWakeLock() {
        boolean want = Prefs.screenOff(this);
        if (want && wakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ShakeTorch::sensor");
            wakeLock.setReferenceCounted(false);
            wakeLock.acquire();
        } else if (!want && wakeLock != null) {
            if (wakeLock.isHeld()) wakeLock.release();
            wakeLock = null;
        }
    }

    @Override
    public void onShake() {
        torch.toggle();
        if (Prefs.vibrate(this)) {
            Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                v.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        if (Prefs.KEY_SENSITIVITY.equals(key)) {
            detector.setThreshold(Prefs.thresholdFor(Prefs.sensitivity(this)));
        } else if (Prefs.KEY_SCREEN_OFF.equals(key)) {
            updateWakeLock();
        }
    }

    @Override
    public void onDestroy() {
        Prefs.get(this).unregisterOnSharedPreferenceChangeListener(this);
        if (sensorManager != null) sensorManager.unregisterListener(detector);
        if (sensorThread != null) sensorThread.quitSafely();
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        if (torch != null) torch.release();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
