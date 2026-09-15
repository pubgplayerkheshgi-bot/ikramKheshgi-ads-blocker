package com.ikramkheshgi.popupdetector;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.os.SystemClock;
import java.text.SimpleDateFormat;
import java.util.*;

public class PopupAccessibilityService extends AccessibilityService {
    private long lastEvent = 0;
    private String lastPackage = "";

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        int type = event.getEventType();
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOWS_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return;

        String pkg = event.getPackageName() == null ? "" : event.getPackageName().toString();
        if (pkg.isEmpty() || pkg.equals(getPackageName())) return;

        long now = SystemClock.uptimeMillis();
        if (pkg.equals(lastPackage) && now - lastEvent < 1200) return;
        lastPackage = pkg; lastEvent = now;

        String label = pkg;
        try {
            label = getPackageManager().getApplicationLabel(
                    getPackageManager().getApplicationInfo(pkg, 0)).toString();
        } catch (Exception ignored) {}

        boolean overlayRequested = requestsOverlay(pkg);
        String text = collectText(event);
        boolean adHint = containsAdWords(text);

        // Record only a compact local event. No network, account, or personal data is used.
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        String kind = (overlayRequested || adHint) ? "SUSPECT" : "WINDOW";
        String line = time + "  [" + kind + "]  " + label + "\n" +
                pkg + (adHint ? "  • ad/popup text hint" : "") +
                (overlayRequested ? "  • requests overlay permission" : "");

        String old = getSharedPreferences("detector", MODE_PRIVATE)
                .getString("log", "");
        String[] lines = old.split("\n");
        StringBuilder out = new StringBuilder(line);
        int count = 1;
        for (String l : lines) {
            if (!l.trim().isEmpty() && count++ < 12) out.append("\n\n").append(l);
        }
        getSharedPreferences("detector", MODE_PRIVATE).edit()
                .putString("log", out.toString()).apply();
    }

    boolean requestsOverlay(String pkg) {
        try {
            PackageInfo p = getPackageManager().getPackageInfo(pkg, PackageManager.GET_PERMISSIONS);
            if (p.requestedPermissions != null)
                for (String perm : p.requestedPermissions)
                    if ("android.permission.SYSTEM_ALERT_WINDOW".equals(perm)) return true;
        } catch (Exception ignored) {}
        return false;
    }

    String collectText(AccessibilityEvent event) {
        StringBuilder s = new StringBuilder();
        CharSequence c = event.getContentDescription();
        if (c != null) s.append(c).append(' ');
        for (CharSequence t : event.getText()) if (t != null) s.append(t).append(' ');
        AccessibilityNodeInfo src = event.getSource();
        if (src != null) {
            CharSequence tc = src.getText();
            CharSequence cd = src.getContentDescription();
            if (tc != null) s.append(tc).append(' ');
            if (cd != null) s.append(cd);
            src.recycle();
        }
        return s.toString().toLowerCase(Locale.ROOT);
    }

    boolean containsAdWords(String s) {
        String[] words = {"advertisement", "sponsored", "install now", "skip ad",
                "close ad", "rewarded ad", "ads by", "ad choices", "download now"};
        for (String w : words) if (s.contains(w)) return true;
        return false;
    }

    @Override public void onInterrupt() {}
}
