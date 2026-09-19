package com.ikramkheshgi.popupdetector;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PopupAccessibilityService extends AccessibilityService {

    private static final long SAME_APP_COOLDOWN = 5000;

    private WindowManager windowManager;
    private TextView floatingButton;

    private int detectionCount = 0;

    private final Map<String, Long> lastDetectionTime =
            new HashMap<>();

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        loadDetectionCount();

        if (Settings.canDrawOverlays(this)) {
            showFloatingButton();
        }

        updateFloatingButton();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        if (event == null) {
            return;
        }

        int type = event.getEventType();

        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                type != AccessibilityEvent.TYPE_WINDOWS_CHANGED &&
                type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {

            return;
        }

        CharSequence packageName =
                event.getPackageName();

        if (packageName == null) {
            return;
        }

        String pkg = packageName.toString().trim();

        if (pkg.isEmpty()) {
            return;
        }

        // Never monitor our own application.
        if (getPackageName().equals(pkg)) {
            return;
        }

        // Only third-party applications.
        if (!isThirdPartyApp(pkg)) {
            return;
        }

        String visibleText = collectText(event);

        /*
         * A detection requires actual popup/ad-related
         * evidence from the visible accessibility content.
         *
         * Overlay permission alone is NOT treated as an ad.
         */
        if (!containsStrongAdIndicator(visibleText)) {
            return;
        }

        long now = SystemClock.uptimeMillis();

        Long previous =
                lastDetectionTime.get(pkg);

        if (previous != null &&
                now - previous < SAME_APP_COOLDOWN) {

            return;
        }

        lastDetectionTime.put(pkg, now);

        recordDetection(pkg);
    }

    private boolean isThirdPartyApp(String pkg) {

        try {

            ApplicationInfo appInfo =
                    getPackageManager()
                            .getApplicationInfo(pkg, 0);

            // Our app is never considered third-party.
            if (getPackageName().equals(pkg)) {
                return false;
            }

            // Android/system applications are ignored.
            return (appInfo.flags &
                    ApplicationInfo.FLAG_SYSTEM) == 0;

        } catch (Exception ignored) {

            return false;
        }
    }

    private void recordDetection(String pkg) {

        String label = pkg;

        try {

            label = getPackageManager()
                    .getApplicationLabel(
                            getPackageManager()
                                    .getApplicationInfo(pkg, 0)
                    )
                    .toString();

        } catch (Exception ignored) {
        }

        detectionCount++;

        saveDetectionCount();
        updateFloatingButton();

        String time =
                new SimpleDateFormat(
                        "HH:mm:ss",
                        Locale.getDefault()
                ).format(new Date());

        String line =
                time + "\t" +
                "SUSPECT" + "\t" +
                clean(label) + "\t" +
                clean(pkg);

        String old =
                getSharedPreferences(
                        "detector",
                        MODE_PRIVATE
                ).getString(
                        "detections_v2",
                        ""
                );

        StringBuilder output =
                new StringBuilder(line);

        if (old != null && !old.trim().isEmpty()) {

            String[] oldLines =
                    old.split("\\n");

            int count = 1;

            for (String oldLine : oldLines) {

                if (oldLine.trim().isEmpty()) {
                    continue;
                }

                if (count >= 20) {
                    break;
                }

                output.append("\n")
                        .append(oldLine);

                count++;
            }
        }

        getSharedPreferences(
                "detector",
                MODE_PRIVATE
        ).edit()
                .putString(
                        "detections_v2",
                        output.toString()
                )
                .apply();

        saveDiagnosticLog(
                time,
                label,
                pkg
        );
    }

    private void saveDiagnosticLog(
            String time,
            String label,
            String pkg) {

        String oldLog =
                getSharedPreferences(
                        "detector",
                        MODE_PRIVATE
                ).getString(
                        "log",
                        ""
                );

        String diagnostic =
                time +
                " [SUSPECT] " +
                clean(label) +
                " " +
                clean(pkg);

        StringBuilder output =
                new StringBuilder(diagnostic);

        if (oldLog != null &&
                !oldLog.trim().isEmpty()) {

            String[] oldLines =
                    oldLog.split("\\n");

            int count = 1;

            for (String line : oldLines) {

                if (line.trim().isEmpty()) {
                    continue;
                }

                if (count >= 12) {
                    break;
                }

                output.append("\n\n")
                        .append(line);

                count++;
            }
        }

        getSharedPreferences(
                "detector",
                MODE_PRIVATE
        ).edit()
                .putString(
                        "log",
                        output.toString()
                )
                .apply();
    }

    private void loadDetectionCount() {

        detectionCount =
                getSharedPreferences(
                        "detector",
                        MODE_PRIVATE
                ).getInt(
                        "detection_count",
                        0
                );
    }

    private void saveDetectionCount() {

        getSharedPreferences(
                "detector",
                MODE_PRIVATE
        ).edit()
                .putInt(
                        "detection_count",
                        detectionCount
                )
                .apply();
    }

    private void showFloatingButton() {

        if (!Settings.canDrawOverlays(this)) {
            return;
        }

        if (floatingButton != null) {
            return;
        }

        windowManager =
                (WindowManager)
                        getSystemService(
                                WINDOW_SERVICE
                        );

        floatingButton =
                new TextView(this);

        floatingButton.setTextSize(22);

        floatingButton.setGravity(
                Gravity.CENTER
        );

        floatingButton.setTextColor(
                Color.WHITE
        );

        int size = dp(58);

        WindowManager.LayoutParams params =
                new WindowManager.LayoutParams(
                        size,
                        size,
                        Build.VERSION.SDK_INT >= 26
                                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                                : WindowManager.LayoutParams.TYPE_PHONE,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT
                );

        params.gravity =
                Gravity.TOP | Gravity.END;

        params.x = dp(12);
        params.y = dp(180);

        floatingButton.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            PopupAccessibilityService.this,
                            MainActivity.class
                    );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            );

            startActivity(intent);

            detectionCount = 0;

            saveDetectionCount();
            updateFloatingButton();
        });

        try {

            windowManager.addView(
                    floatingButton,
                    params
            );

            updateFloatingButton();

        } catch (Exception ignored) {
        }
    }

    private void updateFloatingButton() {

        if (floatingButton == null) {
            return;
        }

        GradientDrawable background =
                new GradientDrawable();

        background.setShape(
                GradientDrawable.OVAL
        );

        if (detectionCount <= 0) {

            floatingButton.setText("🔘");

            background.setColor(
                    Color.rgb(25, 110, 220)
            );

        } else {

            floatingButton.setText(
                    "🔴" + detectionCount
            );

            background.setColor(
                    Color.rgb(190, 25, 35)
            );
        }

        background.setStroke(
                2,
                Color.WHITE
        );

        floatingButton.setBackground(
                background
        );
    }

    private void removeFloatingButton() {

        if (floatingButton != null &&
                windowManager != null) {

            try {

                windowManager.removeView(
                        floatingButton
                );

            } catch (Exception ignored) {
            }

            floatingButton = null;
        }
    }

    private String collectText(
            AccessibilityEvent event) {

        StringBuilder text =
                new StringBuilder();

        CharSequence description =
                event.getContentDescription();

        if (description != null) {

            text.append(description)
                    .append(' ');
        }

        for (CharSequence item :
                event.getText()) {

            if (item != null) {

                text.append(item)
                        .append(' ');
            }
        }

        AccessibilityNodeInfo source =
                event.getSource();

        if (source != null) {

            collectNodeText(
                    source,
                    text
            );

            source.recycle();
        }

        return text.toString()
                .toLowerCase(Locale.ROOT);
    }

    private void collectNodeText(
            AccessibilityNodeInfo node,
            StringBuilder output) {

        if (node == null) {
            return;
        }

        CharSequence text =
                node.getText();

        if (text != null) {

            output.append(text)
                    .append(' ');
        }

        CharSequence description =
                node.getContentDescription();

        if (description != null) {

            output.append(description)
                    .append(' ');
        }

        int childCount =
                node.getChildCount();

        /*
         * Limit traversal so a very large screen
         * cannot create excessive work.
         */
        int maxChildren =
                Math.min(childCount, 40);

        for (int i = 0; i < maxChildren; i++) {

            AccessibilityNodeInfo child =
                    node.getChild(i);

            if (child != null) {

                collectNodeText(
                        child,
                        output
                );

                child.recycle();
            }
        }
    }

    private boolean containsStrongAdIndicator(
            String text) {

        if (text == null ||
                text.trim().isEmpty()) {

            return false;
        }

        String[] strongIndicators = {

                "advertisement",
                "advertising",
                "sponsored",
                "sponsored content",
                "skip ad",
                "close ad",
                "rewarded ad",
                "reward ad",
                "ad choices",
                "ads by",
                "learn more",
                "install now",
                "download now",
                "open ad"
        };

        for (String word :
                strongIndicators) {

            if (text.contains(word)) {
                return true;
            }
        }

        return false;
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

    private int dp(int value) {

        return Math.round(
                value *
                        getResources()
                                .getDisplayMetrics()
                                .density
        );
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {

        removeFloatingButton();

        super.onDestroy();
    }
}
