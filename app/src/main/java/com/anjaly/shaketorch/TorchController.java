package com.anjaly.shaketorch;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Turns the flashlight on/off. Uses the torch API, so no camera permission is needed.
 * It also listens for torch changes made elsewhere (e.g. the quick-settings tile),
 * so a shake always flips whatever state the light is really in.
 */
public class TorchController {
    private static final String TAG = "ShakeTorch";

    private final CameraManager cameraManager;
    private final String cameraId;
    private volatile boolean torchOn = false;

    private final CameraManager.TorchCallback callback = new CameraManager.TorchCallback() {
        @Override
        public void onTorchModeChanged(String id, boolean enabled) {
            if (id.equals(cameraId)) torchOn = enabled;
        }
    };

    public TorchController(Context context) {
        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        cameraId = findFlashCamera();
        if (cameraId != null) {
            cameraManager.registerTorchCallback(callback, new Handler(Looper.getMainLooper()));
        }
    }

    public boolean isAvailable() { return cameraId != null; }

    private String findFlashCamera() {
        try {
            String fallback = null;
            for (String id : cameraManager.getCameraIdList()) {
                CameraCharacteristics c = cameraManager.getCameraCharacteristics(id);
                Boolean hasFlash = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (hasFlash == null || !hasFlash) continue;
                Integer facing = c.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) return id;
                if (fallback == null) fallback = id;
            }
            return fallback;
        } catch (CameraAccessException e) {
            Log.e(TAG, "Cannot list cameras", e);
            return null;
        }
    }

    /** Flips the light. Returns the new state, or the old one if it failed (e.g. camera app is open). */
    public boolean toggle() {
        if (cameraId == null) return false;
        boolean target = !torchOn;
        try {
            cameraManager.setTorchMode(cameraId, target);
            torchOn = target;
        } catch (CameraAccessException | IllegalArgumentException e) {
            Log.w(TAG, "Torch toggle failed (camera may be in use)", e);
        }
        return torchOn;
    }

    public void turnOff() {
        if (cameraId == null || !torchOn) return;
        try {
            cameraManager.setTorchMode(cameraId, false);
            torchOn = false;
        } catch (CameraAccessException | IllegalArgumentException ignored) {}
    }

    public void release() {
        if (cameraId != null) cameraManager.unregisterTorchCallback(callback);
    }
}
