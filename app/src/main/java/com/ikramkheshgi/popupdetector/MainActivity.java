package com.ikramkheshgi.popupdetector;

import android.app.Activity;
import android.os.Bundle;
import android.provider.Settings;
import android.content.Intent;
import android.content.ComponentName;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    TextView status, detections;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        status = findViewById(R.id.serviceStatus);
        detections = findViewById(R.id.detections);

        findViewById(R.id.enableDetector).setOnClickListener(v ->
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        findViewById(R.id.scanApps).setOnClickListener(v -> scanApps());
        updateStatus();
        showDetections();
    }

    @Override protected void onResume() {
        super.onResume();
        updateStatus();
        showDetections();
    }

    void updateStatus() {
        boolean enabled = false;
        String wanted = new ComponentName(this, PopupAccessibilityService.class).flattenToString();
        String enabledServices = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabledServices != null) enabled = enabledServices.contains(wanted);
        status.setText(enabled ? "● DETECTOR ACTIVE" : "○ Detector is off — enable Accessibility service");
    }

    void scanApps() {
        PackageManager pm = getPackageManager();
        ArrayList<String> found = new ArrayList<>();
        for (ApplicationInfo a : pm.getInstalledApplications(PackageManager.GET_META_DATA)) {
            if ((a.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
            try {
                PackageInfo p = pm.getPackageInfo(a.packageName, PackageManager.GET_PERMISSIONS);
                if (p.requestedPermissions != null) {
                    for (String perm : p.requestedPermissions) {
                        if ("android.permission.SYSTEM_ALERT_WINDOW".equals(perm)) {
                            found.add(a.loadLabel(pm) + "\n" + a.packageName);
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        if (found.isEmpty()) {
            detections.setText("No non-system apps requesting overlay permission were found.");
        } else {
            detections.setText("Apps requesting overlay permission:\n\n" +
                    android.text.TextUtils.join("\n\n", found));
        }
    }

    void showDetections() {
        String s = getSharedPreferences("detector", MODE_PRIVATE)
                .getString("log", "No popup events recorded yet. Enable the detector, then use your phone normally.");
        detections.setText(s);
    }
}
