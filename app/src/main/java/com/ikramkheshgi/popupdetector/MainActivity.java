package com.ikramkheshgi.popupdetector;

import android.app.Activity;
import android.os.Bundle;
import android.provider.Settings;
import android.content.Intent;
import android.content.ComponentName;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.Gravity;
import android.widget.*;

import java.util.*;

public class MainActivity extends Activity {

    TextView status;
    LinearLayout detectionsContainer;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        setContentView(R.layout.activity_main);

        status = findViewById(R.id.serviceStatus);
        detectionsContainer = findViewById(R.id.detectionsContainer);

        findViewById(R.id.enableDetector).setOnClickListener(v -> {

            try {
                Intent intent =
                        new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);

                startActivity(intent);

            } catch (Exception e) {

                Toast.makeText(
                        this,
                        "Unable to open Accessibility Settings.",
                        Toast.LENGTH_LONG
                ).show();
            }
        });

        findViewById(R.id.scanApps).setOnClickListener(v ->
                scanApps()
        );

        updateStatus();
        showDetections();
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateStatus();
        showDetections();

        if (isAccessibilityEnabled() &&
                !Settings.canDrawOverlays(this)) {

            Toast.makeText(
                    this,
                    "Please allow floating shortcut permission.",
                    Toast.LENGTH_LONG
            ).show();

            try {

                Intent intent = new Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())
                );

                startActivity(intent);

            } catch (Exception ignored) {
            }
        }
    }

    boolean isAccessibilityEnabled() {

        String wanted = new ComponentName(
                this,
                PopupAccessibilityService.class
        ).flattenToString();

        String enabledServices =
                Settings.Secure.getString(
                        getContentResolver(),
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                );

        return enabledServices != null &&
                enabledServices.contains(wanted);
    }

    void updateStatus() {

        boolean enabled = isAccessibilityEnabled();

        if (enabled) {

            if (Settings.canDrawOverlays(this)) {

                status.setText(
                        "● DETECTOR ACTIVE\n" +
                        "Floating shortcut is ready"
                );

            } else {

                status.setText(
                        "● DETECTOR ACTIVE\n" +
                        "Allow floating shortcut permission"
                );
            }

        } else {

            status.setText(
                    "○ Detector is off — enable Accessibility service"
            );
        }
    }

    void showDetections() {

        detectionsContainer.removeAllViews();

        String data = getSharedPreferences(
                "detector",
                MODE_PRIVATE
        ).getString("detections_v2", "");

        if (data == null || data.trim().isEmpty()) {

            TextView empty = new TextView(this);

            empty.setText(
                    "No possible ad/pop-up detections yet.\n\n" +
                    "Enable the detector and use your phone normally."
            );

            empty.setTextColor(Color.WHITE);
            empty.setTextSize(15);
            empty.setPadding(16, 16, 16, 16);

            detectionsContainer.addView(empty);

            return;
        }

        String[] lines = data.split("\\n");

        for (String line : lines) {

            String[] parts = line.split("\\t");

            if (parts.length < 4) {
                continue;
            }

            String time = parts[0];
            String type = parts[1];
            String appName = parts[2];
            String packageName = parts[3];

            // Never display our own app as a detection.
            if (getPackageName().equals(packageName)) {
                continue;
            }

            addDetection
