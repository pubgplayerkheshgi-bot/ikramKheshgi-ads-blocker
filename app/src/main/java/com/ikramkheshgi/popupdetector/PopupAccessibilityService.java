package com.ikramkheshgi.popupdetector;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class PopupAccessibilityService extends AccessibilityService {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, Long> cooldown = new HashMap<>();
    private WindowManager wm;
    private TextView bubble;
    private String foregroundPackage = "";
    private Pending pending;
    private long lastWindowSwitch;

    private static final long COOLDOWN_MS = 15000L;
    private static final long INSPECT_DELAY_MS = 250L;
    private static final String[] STRONG_AD_MARKERS = {"skip ad", "close ad", "ad choices", "ads by", "sponsored content", "rewarded ad", "reward ad", "remove ads", "advertisement"};
    private static final String[] AD_CONTEXT_MARKERS = {"sponsored", "advertisement", "rewarded ad", "reward ad", "ad choices", "ads by", "skip ad", "close ad"};
    private static final String[] POPUP_ACTION_MARKERS = {"allow", "close", "cancel", "continue"};

    private static final class Pending {
        String pkg, label, reason; long time;
        Pending(String p, String l, String r, long t) { pkg=p; label=l; reason=r; time=t; }
    }

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                | AccessibilityEvent.TYPE_WINDOWS_CHANGED
                | AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
                | AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        info.notificationTimeout = 100;
        setServiceInfo(info);
        if (Settings.canDrawOverlays(this)) showBubble();
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e == null) return;
        String pkg = e.getPackageName() == null ? "" : e.getPackageName().toString();
        if (pkg.isEmpty() || pkg.equals(getPackageName()) || isIgnoredPackage(pkg) || DetectionStore.isWhitelisted(this, pkg)) return;

        int type = e.getEventType();
        if (type == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            String text = eventText(e);
            if (notificationAdScore(text) >= 2) {
                detect(pkg, "Ad-like notification text detected: " + shorten(text, 180));
            }
            return;
        }
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && type != AccessibilityEvent.TYPE_WINDOWS_CHANGED) return;

        String text = eventText(e);
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && pkg.equals(foregroundPackage)) {
            if (isStrongAdText(text) && requestsOverlay(pkg)) detect(pkg, "Ad-like popup text appeared in an overlay-capable app: " + shorten(text, 180));
            return;
        }

        if (!pkg.equals(foregroundPackage)) {
            String previous = foregroundPackage;
            foregroundPackage = pkg;
            lastWindowSwitch = System.currentTimeMillis();
            boolean textSignal = isStrongAdText(text);
            if (textSignal && requestsOverlay(pkg)) {
                detect(pkg, "Ad-like text appeared while the app became visible: " + shorten(text, 180));
                return;
            }
            if (!previous.isEmpty() && !previous.equals(pkg) && !isIgnoredPackage(previous)) {
                final String under = previous;
                handler.postDelayed(() -> inspectTransition(pkg, under), INSPECT_DELAY_MS);
            }
        }
    }

    private void inspectTransition(String candidate, String underlying) {
        if (!candidate.equals(foregroundPackage) || System.currentTimeMillis() - lastWindowSwitch < INSPECT_DELAY_MS - 30) return;
        boolean candidateVisible = false, candidateSmall = false, underlyingVisible = false;
        String candidateText = "";
        int sw = getResources().getDisplayMetrics().widthPixels;
        int sh = getResources().getDisplayMetrics().heightPixels;
        long screen = (long) sw * sh;
        try {
            for (AccessibilityWindowInfo w : getWindows()) {
                if (w == null) continue;
                AccessibilityNodeInfo root = w.getRoot();
                String wp = root != null && root.getPackageName() != null ? root.getPackageName().toString() : "";
                Rect b = new Rect(); w.getBoundsInScreen(b);
                long area = Math.max(0, (long)b.width() * b.height());
                if (candidate.equals(wp)) {
                    candidateVisible = true;
                    if (area > 0 && area < screen * 0.75f) candidateSmall = true;
                    if (root != null) candidateText = nodeText(root, 0, 3500);
                } else if (underlying.equals(wp)) {
                    underlyingVisible = true;
                }
                if (root != null) root.recycle();
                w.recycle();
            }
        } catch (Exception ignored) { }

        boolean textSignal = hasStrongAdEvidence(candidateText, false);
        boolean popupSignal = candidateVisible && candidateSmall && underlyingVisible
                && requestsOverlay(candidate) && hasPopupActionEvidence(candidateText);
        if (textSignal) {
            detect(candidate, "Ad-like popup text detected: " + shorten(candidateText, 180));
        } else if (popupSignal) {
            detect(candidate, "Popup-style window appeared above " + appLabel(underlying) + " with popup controls.");
        }
    }

    private String eventText(AccessibilityEvent e) {
        StringBuilder b = new StringBuilder();
        for (CharSequence c : e.getText()) if (c != null) b.append(c).append(' ');
        if (e.getContentDescription() != null) b.append(e.getContentDescription()).append(' ');
        AccessibilityNodeInfo s = e.getSource();
        if (s != null) { b.append(nodeText(s, 0, 3500)); s.recycle(); }
        return b.toString().toLowerCase(Locale.ROOT).trim();
    }

    private String nodeText(AccessibilityNodeInfo n, int depth, int limit) {
        if (n == null || depth > 7) return "";
        StringBuilder b = new StringBuilder();
        appendNode(n, b, depth, limit);
        return b.toString().toLowerCase(Locale.ROOT);
    }

    private void appendNode(AccessibilityNodeInfo n, StringBuilder b, int depth, int limit) {
        if (n == null || depth > 7 || b.length() >= limit) return;
        if (n.getText() != null) b.append(n.getText()).append(' ');
        if (n.getContentDescription() != null) b.append(n.getContentDescription()).append(' ');
        for (int i=0; i<n.getChildCount() && i<24; i++) {
            AccessibilityNodeInfo c = n.getChild(i);
            if (c != null) { appendNode(c, b, depth+1, limit); c.recycle(); }
        }
    }

    private boolean isStrongAdText(String s) {
        if (s == null || s.length() < 4) return false;
        for (String x : STRONG_AD_MARKERS) if (s.contains(x)) return true;
        return false;
    }


    private boolean hasStrongAdEvidence(String s, boolean allowContext) {
        if (s == null || s.length() < 4) return false;
        for (String x : STRONG_AD_MARKERS) if (s.contains(x)) return true;
        if (!allowContext) return false;
        int contextHits = 0;
        for (String x : AD_CONTEXT_MARKERS) if (s.contains(x)) contextHits++;
        boolean hasInstall = s.contains("install now") || s.contains("install app");
        boolean hasDownload = s.contains("download now") || s.contains("download app");
        return contextHits >= 2 || (contextHits >= 1 && (hasInstall || hasDownload));
    }

    private boolean hasPopupActionEvidence(String s) {
        if (s == null || s.length() < 4) return false;
        int hits = 0;
        for (String x : POPUP_ACTION_MARKERS) if (s.contains(x)) hits++;
        return hits >= 2;
    }

    private int notificationAdScore(String s) {
        if (s == null || s.length() < 4) return 0;
        int score = 0;
        for (String x : AD_CONTEXT_MARKERS) if (s.contains(x)) score++;
        if (s.contains("install now") || s.contains("install app")) score++;
        if (s.contains("download now") || s.contains("download app")) score++;
        if (s.contains("click to install") || s.contains("tap to install")) score++;
        return score;
    }

    private boolean requestsOverlay(String pkg) {
        try {
            PackageInfo p = getPackageManager().getPackageInfo(pkg, PackageManager.GET_PERMISSIONS);
            if (p.requestedPermissions != null) for (String x : p.requestedPermissions)
                if (Settings.ACTION_MANAGE_OVERLAY_PERMISSION != null && "android.permission.SYSTEM_ALERT_WINDOW".equals(x)) return true;
        } catch (Exception ignored) { }
        return false;
    }

    private boolean isIgnoredPackage(String pkg) {
        try {
            ApplicationInfo a = getPackageManager().getApplicationInfo(pkg, 0);
            int f = a.flags;
            if ((f & ApplicationInfo.FLAG_SYSTEM) != 0 || (f & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) return true;
            String n = pkg.toLowerCase(Locale.ROOT);
            return n.startsWith("com.android.") || n.startsWith("com.google.android.inputmethod") || n.equals("com.google.android.permissioncontroller");
        } catch (Exception e) { return true; }
    }

    private String appLabel(String pkg) {
        try { return getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg, 0)).toString(); }
        catch (Exception e) { return pkg; }
    }

    private String shorten(String s, int max) {
        if (s == null) return "";
        s = s.trim();
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    private void detect(String pkg, String reason) {
        if (DetectionStore.isWhitelisted(this, pkg)) return;
        long now = System.currentTimeMillis();
        Long last = cooldown.get(pkg);
        if (last != null && now - last < COOLDOWN_MS) return;
        cooldown.put(pkg, now);
        String label = appLabel(pkg);
        DetectionStore.add(this, pkg, label, reason, now);
        pending = new Pending(pkg, label, reason, now);
        setBubble(true, label);
        Toast.makeText(this, "Possible popup source: " + label, Toast.LENGTH_LONG).show();
    }

    private void showBubble() {
        if (bubble != null || !Settings.canDrawOverlays(this)) return;
        try {
            wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            bubble = new TextView(this);
            bubble.setText("✓"); bubble.setTextColor(Color.WHITE); bubble.setTextSize(17); bubble.setGravity(Gravity.CENTER);
            bubble.setPadding(18, 12, 18, 12); bubble.setContentDescription("Popup detector ready");
            setBubble(false, null);
            bubble.setOnClickListener(v -> {
                if (pending != null) openDetails(); else startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            });
            int type = Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
            WindowManager.LayoutParams p = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, type,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, PixelFormat.TRANSLUCENT);
            p.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL; p.x = 10; p.y = 0;
            wm.addView(bubble, p);
        } catch (Exception e) { bubble = null; }
    }

    private void setBubble(boolean alert, String label) {
        if (bubble == null) return;
        GradientDrawable g = new GradientDrawable();
        g.setColor(alert ? Color.rgb(210, 48, 62) : Color.rgb(24, 94, 140));
        g.setCornerRadius(80);
        bubble.setBackground(g);
        bubble.setText(alert ? "!" : "✓");
        bubble.setContentDescription(alert ? "Possible popup from " + label : "Popup detector ready");
    }

    private void openDetails() {
        if (pending == null) return;
        Intent i = new Intent(this, DetectionDetailsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.putExtra("package", pending.pkg); i.putExtra("label", pending.label); i.putExtra("reason", pending.reason); i.putExtra("time", pending.time);
        startActivity(i);
    }

    @Override public void onInterrupt() { }

    @Override public void onDestroy() {
        if (bubble != null && wm != null) try { wm.removeView(bubble); } catch (Exception ignored) { }
        bubble = null;
        super.onDestroy();
    }
}
