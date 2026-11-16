# Shake Torch

Two quick shakes (a "chop-chop" like on Motorola phones) turn the flashlight on or off.
It works even when the app is closed or the screen is off.

## Get the APK (no coding tools needed)

1. Sign in to github.com and create a **new repository** (e.g. `shake-torch`). It can be private.
2. On the new repo page, click **"uploading an existing file"**. Unzip this project on your computer and drag **all of its contents** in, including the `.github` folder. Then click **Commit changes**.
   - If the `.github` folder doesn't upload (hidden folders sometimes get skipped), click **Add file → Create new file**, name it `.github/workflows/build.yml`, and paste in the contents of that file.
3. Open the **Actions** tab. A "Build APK" run starts on its own and takes about 3–5 minutes.
4. When it shows a green tick, click the run, scroll to **Artifacts** and download **ShakeTorch-apk**. Unzip it to get `app-debug.apk`.
5. Send `app-debug.apk` to your phone (WhatsApp to yourself, Google Drive or a USB cable) and open it. Allow "Install unknown apps" when Vivo asks.

Or, if you have Android Studio: **File → Open** this folder, then **Run**.

## Set it up on the Vivo

1. Open Shake Torch and turn on **Shake to toggle flashlight**. Allow notifications.
2. Tap **Allow unrestricted battery use** and choose Allow.
3. Tap **Open app settings** → Battery → set it to **Unrestricted / Allow background activity**. If you see **Autostart**, turn it on too. (On some Funtouch/OriginOS versions this lives in *Settings → Battery → Background power consumption management*.)
4. In Recent apps, **lock** Shake Torch so "clear all" doesn't close it.

Vivo stops background apps very aggressively, so if the shake stops working after a while, one of these steps is usually the reason.

## Tuning

- **Sensitivity**: slide left if it turns on by accident in your pocket; slide right if it's hard to trigger.
- **Work when screen is off**: keeps the motion sensor awake. Uses a bit more battery; turn it off if you only need it while the screen is on.
- The flashlight can't be toggled while the camera app is open, because the camera is using it.
