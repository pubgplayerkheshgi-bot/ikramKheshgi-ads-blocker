package com.ikramkheshgi.popupdetector;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.ApplicationInfo;
import android.os.SystemClock;
import android.provider.Settings;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PopupAccessibilityService extends AccessibilityService {

    private long lastEvent = 0;
    private String lastPackage = "";

    private WindowManager windowManager;
    private TextView floatingButton;

    private int detectionCount = 0;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        loadDetectionCount();
        showFloatingButton();
        updateFloatingButton();
    }

    @Override
    public void onAccessibilityEvent(
            AccessibilityEvent event) {

        int type = event.getEventType();

        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOWS_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {

            return;
        }

        String pkg = event.getPackageName() == null
                ? ""
                : event.getPackageName().toString();

        if (pkg.isEmpty() ||
                pkg.equals(getPackageName())) {

            return;
        }

        /*
         * Only detect third-party/user-installed apps.
         */
        if (!isThirdPartyApp(pkg)) {
            return;
        }

        long now = SystemClock.uptimeMillis();

        if (pkg.equals(lastPackage) &&
                now - lastEvent < 1200) {

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

        boolean adHint =
                containsAdWords(text);

        boolean overlayRequested =
                requestsOverlay(pkg);

        if (!adHint && !overlayRequested) {
            return;
        }

        /*
         * New detection found.
         */
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

        String[] lines =
                old.split("\n");

        StringBuilder out =
                new StringBuilder(line);

        int count = 1;

        for (String l : lines) {

            if (!l.trim().isEmpty() &&
                    count++ < 20) {

                out.append("\n")
                        .append(l);
            }
        }

        getSharedPreferences(
                "detector",
                MODE_PRIVATE
        ).edit()
                .putString(
                        "detections_v2",
                        out.toString()
                )
                .apply();

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
                label +
                " " +
                pkg;

        String[] oldLines =
                oldLog.split("\n");

        StringBuilder diagnosticOut =
                new StringBuilder(diagnostic);

        int diagnosticCount = 1;

        for (String l : oldLines) {

            if (!l.trim().isEmpty() &&
                    diagnosticCount++ < 12) {

                diagnosticOut
                        .append("\n\n")
                        .append(l);
            }
        }

        getSharedPreferences(
                "detector",
                MODE_PRIVATE
        ).edit()
                .putString(
                        "log",
                        diagnosticOut.toString()
                )
                .apply();
    }

    private boolean isThirdPartyApp(String pkg) {

        try {

            ApplicationInfo appInfo =
                    getPackageManager()
                            .getApplicationInfo(
                                    pkg,
                                    0
                            );

            return (appInfo.flags &
                    ApplicationInfo.FLAG_SYSTEM) == 0;

        } catch (Exception ignored) {

            return false;
        }
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

        GradientDrawable background =
                new GradientDrawable();

        background.setShape(
                GradientDrawable.OVAL
        );

        background.setColor(
                Color.rgb(25, 110, 220)
        );

        background.setStroke(
                2,
                Color.WHITE
        );

        floatingButton.setBackground(
                background
        );

        int size = dp(58);

        WindowManager.LayoutParams params =
                new WindowManager.LayoutParams(
                        size,
                        size,
                        android.os.Build.VERSION.SDK_INT >= 26
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

            /*
             * Open the detection screen.
             */
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

            /*
             * Clear the red counter after opening.
             */
            detectionCount = 0;
            saveDetectionCount();
            updateFloatingButton();
        });

        try {

            windowManager.addView(
                    floatingButton,
                    params
            );

        } catch (Exception ignored) {
        }
    }

    private void updateFloatingButton() {

        if (floatingButton == null) {
            return;
        }

        if (detectionCount <= 0) {

            floatingButton.setText("🔘");

            GradientDrawable background =
                    new GradientDrawable();

            background.setShape(
                    GradientDrawable.OVAL
            );

            background.setColor(
                    Color.rgb(25, 110, 220)
            );

            background.setStroke(
                    2,
                    Color.WHITE
            );

            floatingButton.setBackground(
                    background
            );

        } else {

            floatingButton.setText(
                    "🔴" + detectionCount
            );

            GradientDrawable background =
                    new GradientDrawable();

            background.setShape(
                    GradientDrawable.OVAL
            );

            background.setColor(
                    Color.rgb(190, 25, 35)
            );

            background.setStroke(
                    2,
                    Color.WHITE
            );

            floatingButton.setBackground(
                    background
            );
        }
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

    private int dp(int value) {

        return Math.round(
                value *
                        getResources()
                                .getDisplayMetrics()
                                .density
        );
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
                    getPackageManager()
                            .getPackageInfo(
                                    pkg,
                                    PackageManager.GET_PERMISSIONS
                            );

            if (p.requestedPermissions != null) {

                for (String perm :
                        p.requestedPermissions) {

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

    private String collectText(
            AccessibilityEvent event) {

        StringBuilder s =
                new StringBuilder();

        CharSequence description =
                event.getContentDescription();

        if (description != null) {

            s.append(description)
                    .append(' ');
        }

        for (CharSequence t :
                event.getText()) {

            if (t != null) {

                s.append(t)
                        .append(' ');
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

                s.append(text)
                        .append(' ');
            }

            if (content != null) {

                s.append(content);
            }

            source.recycle();
        }

        return s.toString()
                .toLowerCase(
                        Locale.ROOT
                );
    }

    private boolean containsAdWords(
            String s) {

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

    @Override
    public void onDestroy() {

        removeFloatingButton();

        super.onDestroy();
    }
}
