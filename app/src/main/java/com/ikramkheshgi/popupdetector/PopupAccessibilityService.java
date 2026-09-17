package com.ikramkheshgi.popupdetector;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.ApplicationInfo;
import android.os.SystemClock;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PopupAccessibilityService extends AccessibilityService {

    private long lastEvent = 0;
    private String lastPackage = "";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        int type = event.getEventType();

        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOWS_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            return;
        }

        String pkg = event.getPackageName() == null
                ? ""
                : event.getPackageName().toString();

        if (pkg.isEmpty() || pkg.equals(getPackageName())) {
            return;
        }

        // Only detect third-party/user-installed apps.
        if (!isThirdPartyApp(pkg)) {
            return;
        }

        long now = SystemClock.uptimeMillis();

        if (pkg.equals(lastPackage) && now - lastEvent < 1200) {
            return;
        }

        lastPackage = pkg;
        lastEvent = now;

        String label = pkg;

        try {
            label = getPackageManager()
                    .getApplicationLabel(
                            getPackageManager()
                                    .getApplicationInfo(pkg, 0))
                    .toString();
        } catch (Exception ignored) {
        }

        String text = collectText(event);

        boolean adHint = containsAdWords(text);
        boolean overlayRequested = requestsOverlay(pkg);

        /*
         * Only record a possible detection when there is
         * an ad/popup text hint or the app requests overlay permission.
         */
        if (!adHint && !overlayRequested) {
            return;
        }

        String time = new SimpleDateFormat(
                "HH:mm:ss",
                Locale.getDefault()
        ).format(new Date());

        String line =
                time + "\t" +
                "SUSPECT" + "\t" +
                clean(label) + "\t" +
                clean(pkg);

        String old = getSharedPreferences(
                "detector",
                MODE_PRIVATE
        ).getString("detections_v2", "");

        String[] lines = old.split("\n");

        StringBuilder out = new StringBuilder(line);
        int count = 1;

        for (String l : lines) {
            if (!l.trim().isEmpty() && count++ < 20) {
                out.append("\n").append(l);
            }
        }

        getSharedPreferences(
                "detector",
                MODE_PRIVATE
        ).edit()
                .putString("detections_v2", out.toString())
                .apply();

        /*
         * Keep the old diagnostic log too.
         */
        String oldLog = getSharedPreferences(
                "detector",
                MODE_PRIVATE
        ).getString("log", "");

        String diagnostic =
                time + " [SUSPECT] " +
                label + " " + pkg;

        String[] oldLines = oldLog.split("\n");

        StringBuilder diagnosticOut =
                new StringBuilder(diagnostic);

        int diagnosticCount = 1;

        for (String l : oldLines) {
            if (!l.trim().isEmpty() && diagnosticCount++ < 12) {
                diagnosticOut.append("\n\n").append(l);
            }
        }

        getSharedPreferences(
                "detector",
                MODE_PRIVATE
        ).edit()
                .putString("log", diagnosticOut.toString())
                .apply();
    }

    /*
     * Check whether the detected package is a third-party app.
     *
     * System applications are ignored.
     */
    private boolean isThirdPartyApp(String pkg) {

        try {
            ApplicationInfo appInfo =
                    getPackageManager()
                            .getApplicationInfo(pkg, 0);

            return (appInfo.flags &
                    ApplicationInfo.FLAG_SYSTEM) == 0;

        } catch (Exception ignored) {
            return false;
        }
    }

    private String clean(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\t", " ")
                .replace("\n", " ")
                .replace("\r", " ");
    }

    private boolean requestsOverlay(String pkg) {

        try {

            PackageInfo p =
                    getPackageManager().getPackageInfo(
                            pkg,
                            PackageManager.GET_PERMISSIONS
                    );

            if (p.requestedPermissions != null) {

                for (String perm : p.requestedPermissions) {

                    if ("android.permission.SYSTEM_ALERT_WINDOW"
                            .equals(perm)) {

                        return true;
                    }
                }
            }

        } catch (Exception ignored) {
        }

        return false;
    }

    private String collectText(AccessibilityEvent event) {

        StringBuilder s = new StringBuilder();

        CharSequence description =
                event.getContentDescription();

        if (description != null) {
            s.append(description).append(' ');
        }

        for (CharSequence t : event.getText()) {

            if (t != null) {
                s.append(t).append(' ');
            }
        }

        AccessibilityNodeInfo source =
                event.getSource();

        if (source != null) {

            CharSequence text =
                    source.getText();

            CharSequence content =
                    source.getContentDescription();

            if (text != null) {
                s.append(text).append(' ');
            }

            if (content != null) {
                s.append(content);
            }

            source.recycle();
        }

        return s.toString()
                .toLowerCase(Locale.ROOT);
    }

    private boolean containsAdWords(String s) {

        String[] words = {

                "advertisement",
                "sponsored",
                "install now",
                "skip ad",
                "close ad",
                "rewarded ad",
                "ads by",
                "ad choices",
                "download now",
                "open ad",
                "learn more",
                "buy now"
        };

        for (String word : words) {

            if (s.contains(word)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void onInterrupt() {
    }
}
