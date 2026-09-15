ikramKheshgi Popup Ad Detector v1.0

This is a native Android project branded with the supplied portrait.

What it does:
1. Lists non-system apps that request Android's "Display over other apps" permission.
2. Provides an Accessibility Service that records recent window/package events.
3. Flags candidates when an app requests overlay permission or the visible accessibility
   text contains common ad/popup phrases.
4. Keeps the short detection log locally on the device; it does not send data to a server.
5. Tapping "ENABLE POPUP DETECTOR" opens Android Accessibility settings.

Important Android limitation:
No ordinary Android app can guarantee identification of every popup/ad source.
Some ads are rendered inside the legitimate app itself, and some overlay behavior is
not exposed consistently to third-party apps. Accessibility detection is therefore a
best-effort diagnostic tool and can produce false positives.

Build:
Open this folder in Android Studio, allow Gradle sync, then Build > Build APK(s).
The generated debug APK will be in app/build/outputs/apk/debug/.
