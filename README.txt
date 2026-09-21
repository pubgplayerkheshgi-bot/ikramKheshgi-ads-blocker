ikramKheshgi Popup Detector v2.0

This build is a functional popup-source detector modeled around the same user flow as the reference Popup Ad Detector: Accessibility monitoring + floating detector + identified source app + App Info + Android uninstall action.

IMPORTANT
- This app detects possible popup sources using Android Accessibility signals. It cannot guarantee that an identified app is malicious.
- Normal window content changes are NOT logged as detections.
- A detection is based on ad-like visible text, ad-like notification text, or a stronger popup-style window signal from an app capable of drawing overlays.
- Android always controls the final uninstall confirmation.

SETUP
1. Install the APK.
2. Open the app.
3. Enable Accessibility for "ikramKheshgi Popup Detector".
4. Allow "Display over other apps" so the floating detector can appear.
5. Use the phone normally.
6. When a possible external popup is detected, the floating ! button becomes active.
7. Tap it to see the app name, package, reason and time, then use APP INFO or UNINSTALL APP.

BUILD
GitHub Actions is configured to build debug and release APKs.
