package com.ikramkheshgi.popupdetector;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class PopupAccessibilityService extends AccessibilityService {

    private static final String PREFS = "popup_detector_prefs";
    private static final String KEY_DETECTION_COUNT = "detection_count";

    private static final long PACKAGE_COOLDOWN = 5000L;

    private final Map<String, Long> lastDetectionTime =
            new HashMap<>();

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private WindowManager windowManager;
    private TextView floatingButton;

    private int detectionCount = 0;

    private final String[] AD_INDICATORS = {

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

    @Override
    protected void onServiceConnected() {

        super.onServiceConnected();

        AccessibilityServiceInfo info =
                new AccessibilityServiceInfo();

        info.eventTypes =
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                        | AccessibilityEvent.TYPE_WINDOWS_CHANGED
                        | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;

        info.feedbackType =
                AccessibilityServiceInfo.FEEDBACK_GENERIC;

        info.flags =
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
                        | AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;

        info.notificationTimeout = 150;

        info.packageNames = null;

        setServiceInfo(info);

        detectionCount =
                getSharedPreferences(
                        PREFS,
                        MODE_PRIVATE
                ).getInt(
                        KEY_DETECTION_COUNT,
                        0
                );

        if (Settings.canDrawOverlays(this)) {
            showFloatingButton();
        }
    }

    @Override
    public void onAccessibilityEvent(
            AccessibilityEvent event
    ) {

        if (event == null) {
            return;
        }

        CharSequence packageName =
                event.getPackageName();

        if (packageName == null) {
            return;
        }

        String pkg =
                packageName.toString();

        if (pkg.equals(getPackageName())) {
            return;
        }

        if (isSystemApp(pkg)) {
            return;
        }

        String text =
                collectEventText(event);

        if (text.length() == 0) {
            return;
        }

        if (!containsAdIndicator(text)) {
            return;
        }

        long now =
                System.currentTimeMillis();

        Long previous =
                lastDetectionTime.get(pkg);

        if (previous != null
                && now - previous < PACKAGE_COOLDOWN) {

            return;
        }

        lastDetectionTime.put(pkg, now);

        registerDetection(pkg, text);
    }

    private String collectEventText(
            AccessibilityEvent event
    ) {

        StringBuilder builder =
                new StringBuilder();

        if (event.getText() != null) {

            for (CharSequence value :
                    event.getText()) {

                if (value != null) {

                    builder.append(value)
                            .append(" ");
                }
            }
        }

        AccessibilityNodeInfo source =
                event.getSource();

        if (source != null) {

            collectNodeText(
                    source,
                    builder,
                    0
            );

            source.recycle();
        }

        return builder.toString()
                .toLowerCase(Locale.US)
                .trim();
    }

    private void collectNodeText(
            AccessibilityNodeInfo node,
            StringBuilder builder,
            int depth
    ) {

        if (node == null || depth > 40) {
            return;
        }

        CharSequence text =
                node.getText();

        if (text != null) {

            builder.append(text)
                    .append(" ");
        }

        CharSequence description =
                node.getContentDescription();

        if (description != null) {

            builder.append(description)
                    .append(" ");
        }

        int childCount =
                node.getChildCount();

        for (int i = 0; i < childCount; i++) {

            AccessibilityNodeInfo child =
                    node.getChild(i);

            if (child != null) {

                collectNodeText(
                        child,
                        builder,
                        depth + 1
                );

                child.recycle();
            }
        }
    }

    private boolean containsAdIndicator(
            String text
    ) {

        for (String indicator :
                AD_INDICATORS) {

            if (text.contains(indicator)) {
                return true;
            }
        }

        return false;
    }

    private boolean isSystemApp(
            String packageName
    ) {

        try {

            ApplicationInfo info =
                    getPackageManager()
                            .getApplicationInfo(
                                    packageName,
                                    0
                            );

            return (info.flags
                    & ApplicationInfo.FLAG_SYSTEM) != 0
                    || (info.flags
                    & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;

        } catch (Exception e) {

            return true;
        }
    }

    private void registerDetection(
            String packageName,
            String detectedText
    ) {

        detectionCount++;

        getSharedPreferences(
                PREFS,
                MODE_PRIVATE
        )
                .edit()
                .putInt(
                        KEY_DETECTION_COUNT,
                        detectionCount
                )
                .apply();

        String appName =
                packageName;

        try {

            ApplicationInfo info =
                    getPackageManager()
                            .getApplicationInfo(
                                    packageName,
                                    0
                            );

            appName =
                    getPackageManager()
                            .getApplicationLabel(info)
                            .toString();

        } catch (Exception ignored) {
        }

        String time =
                new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.getDefault()
                ).format(
                        new Date()
                );

        String record =
                appName
                        + "\n"
                        + packageName
                        + "\n"
                        + "Detected: "
                        + time
                        + "\n"
                        + "Reason: "
                        + shorten(
                                detectedText,
                                160
                        );

        getSharedPreferences(
                PREFS,
                MODE_PRIVATE
        )
                .edit()
                .putString(
                        "detection_" + detectionCount,
                        record
                )
                .apply();

        updateFloatingButton();

        Toast.makeText(
                this,
                "Possible popup detected: "
                        + appName,
                Toast.LENGTH_SHORT
        ).show();
    }

    private String shorten(
            String text,
            int maxLength
    ) {

        if (text.length() <= maxLength) {
            return text;
        }

        return text.substring(
                0,
                maxLength
        ) + "...";
    }

    private void showFloatingButton() {

        if (floatingButton != null) {
            return;
        }

        if (!Settings.canDrawOverlays(this)) {
            return;
        }

        windowManager =
                (WindowManager)
                        getSystemService(
                                WINDOW_SERVICE
                        );

        floatingButton =
                new TextView(this);

        floatingButton.setText(
                detectionCount > 0
                        ? "● " + detectionCount
                        : "●"
        );

        floatingButton.setTextColor(Color.WHITE);
        floatingButton.setTextSize(16);
        floatingButton.setGravity(Gravity.CENTER);
        floatingButton.setPadding(
                18,
                12,
                18,
                12
        );

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(
                Color.rgb(
                        210,
                        25,
                        35
                )
        );

        background.setCornerRadius(80);

        floatingButton.setBackground(background);

        floatingButton.setOnClickListener(
                v -> openMainActivity()
        );

        int windowType;

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            windowType =
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

        } else {

            windowType =
                    WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams params =
                new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        windowType,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT
                );

        params.gravity =
                Gravity.CENTER_VERTICAL
                        | Gravity.RIGHT;

        params.x = 20;
        params.y = 0;

        try {

            windowManager.addView(
                    floatingButton,
                    params
            );

        } catch (Exception e) {

            floatingButton = null;
        }
    }

    private void updateFloatingButton() {

        if (floatingButton == null) {

            if (Settings.canDrawOverlays(this)) {
                showFloatingButton();
            }

            return;
        }

        handler.post(() -> {

            if (floatingButton != null) {

                floatingButton.setText(
                        "● " + detectionCount
                );
            }
        });
    }

    private void openMainActivity() {

        Intent intent =
                new Intent(
                        this,
                        MainActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
        );

        startActivity(intent);
    }

    @Override
    public void onInterrupt() {
        // Monitoring interrupted by Android.
    }

    @Override
    public void onDestroy() {

        removeFloatingButton();

        super.onDestroy();
    }

    private void removeFloatingButton() {

        if (floatingButton != null
                && windowManager != null) {

            try {

                windowManager.removeView(
                        floatingButton
                );

            } catch (Exception ignored) {
            }

            floatingButton = null;
        }
    }
}
