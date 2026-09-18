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

        /*
         * Once Accessibility is enabled,
         * ask for overlay permission.
         */
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

            addDetectionCard(
                    time,
                    type,
                    appName,
                    packageName
            );
        }
    }

    void addDetectionCard(
            String time,
            String type,
            String appName,
            String packageName
    ) {

        LinearLayout card = new LinearLayout(this);

        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(16, 16, 16, 16);
        card.setBackgroundColor(
                Color.rgb(14, 28, 46)
        );

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(0, 0, 0, 12);

        card.setLayoutParams(cardParams);

        LinearLayout top = new LinearLayout(this);

        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        ImageView icon = new ImageView(this);

        int iconSize = dp(58);

        top.addView(
                icon,
                new LinearLayout.LayoutParams(
                        iconSize,
                        iconSize
                )
        );

        LinearLayout information = new LinearLayout(this);

        information.setOrientation(
                LinearLayout.VERTICAL
        );

        LinearLayout.LayoutParams infoParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        infoParams.setMargins(14, 0, 0, 0);

        top.addView(
                information,
                infoParams
        );

        TextView title = new TextView(this);

        title.setText(
                "🚨 Possible Ad/Popup"
        );

        title.setTextColor(
                Color.rgb(255, 190, 70)
        );

        title.setTextSize(17);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        information.addView(title);

        TextView name = new TextView(this);

        name.setText(appName);
        name.setTextColor(Color.WHITE);
        name.setTextSize(18);
        name.setTypeface(null, 1);

        information.addView(name);

        TextView timeView = new TextView(this);

        timeView.setText(
                "Detected: " + time
        );

        timeView.setTextColor(Color.LTGRAY);
        timeView.setTextSize(13);

        information.addView(timeView);

        card.addView(top);

        TextView pkg = new TextView(this);

        pkg.setText(
                "Package: " + packageName
        );

        pkg.setTextColor(
                Color.rgb(190, 210, 225)
        );

        pkg.setTextSize(13);
        pkg.setPadding(0, 12, 0, 10);

        card.addView(pkg);

        Button uninstall = new Button(this);

        uninstall.setText("UNINSTALL APP");

        uninstall.setOnClickListener(v ->
                uninstallApp(packageName)
        );

        card.addView(uninstall);

        try {

            Drawable drawable =
                    getPackageManager()
                            .getApplicationIcon(packageName);

            icon.setImageDrawable(drawable);

        } catch (Exception e) {

            icon.setImageResource(
                    android.R.drawable.sym_def_app_icon
            );
        }

        detectionsContainer.addView(card);
    }

    void uninstallApp(String packageName) {

        if (packageName == null ||
                packageName.trim().isEmpty()) {

            return;
        }

        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_DELETE,
                            Uri.parse("package:" + packageName)
                    );

            startActivity(intent);

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Unable to open uninstall screen.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    void scanApps() {

        PackageManager pm =
                getPackageManager();

        ArrayList<String> found =
                new ArrayList<>();

        for (ApplicationInfo a :
                pm.getInstalledApplications(
                        PackageManager.GET_META_DATA
                )) {

            if ((a.flags &
                    ApplicationInfo.FLAG_SYSTEM) != 0) {

                continue;
            }

            try {

                android.content.pm.PackageInfo p =
                        pm.getPackageInfo(
                                a.packageName,
                                PackageManager.GET_PERMISSIONS
                        );

                if (p.requestedPermissions != null) {

                    for (String perm :
                            p.requestedPermissions) {

                        if ("android.permission.SYSTEM_ALERT_WINDOW"
                                .equals(perm)) {

                            found.add(
                                    a.loadLabel(pm).toString()
                                            + "\n"
                                            + a.packageName
                            );

                            break;
                        }
                    }
                }

            } catch (Exception ignored) {
            }
        }

        detectionsContainer.removeAllViews();

        if (found.isEmpty()) {

            TextView empty = new TextView(this);

            empty.setText(
                    "No non-system apps requesting overlay permission were found."
            );

            empty.setTextColor(Color.WHITE);
            empty.setTextSize(15);
            empty.setPadding(16, 16, 16, 16);

            detectionsContainer.addView(empty);

        } else {

            for (String item : found) {

                String[] parts =
                        item.split("\\n");

                if (parts.length >= 2) {

                    addDetectionCard(
                            "Installed scan",
                            "OVERLAY",
                            parts[0],
                            parts[1]
                    );
                }
            }
        }
    }

    int dp(int value) {

        return Math.round(
                value *
                        getResources()
                                .getDisplayMetrics()
                                .density
        );
    }
}
