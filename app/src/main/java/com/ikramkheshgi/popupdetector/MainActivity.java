package com.ikramkheshgi.popupdetector;

import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends android.app.Activity {

    private static final String PREFS = "popup_detector_prefs";
    private static final String KEY_DETECTION_COUNT =
            "detection_count";

    private LinearLayout detectionContainer;
    private TextView detectionCountText;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        buildMainScreen();
        refreshStatus();
        scanApps();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (statusText != null) {
            refreshStatus();
        }
    }

    private void buildMainScreen() {

        ScrollView scrollView = new ScrollView(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 24, 28, 40);
        root.setBackgroundColor(Color.WHITE);

        scrollView.addView(root);

        // ---------------------------------------------------------
        // HOME PAGE PHOTO
        // ---------------------------------------------------------

        ImageView introPhoto = new ImageView(this);

        introPhoto.setImageResource(
                R.drawable.ikramkheshgi_intro
        );

        introPhoto.setScaleType(
                ImageView.ScaleType.CENTER_INSIDE
        );

        introPhoto.setContentDescription(
                "ikramKheshgi"
        );

        LinearLayout.LayoutParams photoParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        260
                );

        photoParams.setMargins(0, 5, 0, 15);

        root.addView(
                introPhoto,
                photoParams
        );

        // ---------------------------------------------------------
        // APP NAME
        // ---------------------------------------------------------

        TextView title = new TextView(this);

        title.setText("ikramKheshgi");

        title.setTextSize(30);

        title.setTextColor(
                Color.rgb(15, 25, 40)
        );

        title.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        title.setGravity(
                Gravity.CENTER
        );

        root.addView(
                title,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        TextView subtitle = new TextView(this);

        subtitle.setText(
                "Popup Detector"
        );

        subtitle.setTextSize(19);

        subtitle.setTextColor(
                Color.rgb(25, 150, 210)
        );

        subtitle.setGravity(
                Gravity.CENTER
        );

        LinearLayout.LayoutParams subtitleParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        subtitleParams.setMargins(
                0,
                4,
                0,
                25
        );

        root.addView(
                subtitle,
                subtitleParams
        );

        // ---------------------------------------------------------
        // DESCRIPTION
        // ---------------------------------------------------------

        TextView information = new TextView(this);

        information.setText(
                "Detect possible pop-up advertisements "
                        + "from third-party apps.\n\n"
                        + "The detector uses Android Accessibility "
                        + "access to observe visible app windows "
                        + "and on-screen text for possible "
                        + "advertisements or pop-ups."
        );

        information.setTextSize(16);

        information.setTextColor(
                Color.DKGRAY
        );

        information.setGravity(
                Gravity.CENTER
        );

        information.setPadding(
                10,
                0,
                10,
                20
        );

        root.addView(
                information
        );

        // ---------------------------------------------------------
        // STATUS
        // ---------------------------------------------------------

        statusText = new TextView(this);

        statusText.setTextSize(16);

        statusText.setGravity(
                Gravity.CENTER
        );

        statusText.setPadding(
                15,
                18,
                15,
                18
        );

        root.addView(
                statusText
        );

        // ---------------------------------------------------------
        // ENABLE DETECTOR
        // ---------------------------------------------------------

        Button enableButton =
                new Button(this);

        enableButton.setText(
                "Enable Detector"
        );

        enableButton.setOnClickListener(
                v -> showAccessibilityDisclosure()
        );

        root.addView(
                enableButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        // ---------------------------------------------------------
        // FLOATING BUTTON PERMISSION
        // ---------------------------------------------------------

        Button overlayButton =
                new Button(this);

        overlayButton.setText(
                "Allow Floating Button"
        );

        overlayButton.setOnClickListener(
                v -> openOverlaySettings()
        );

        LinearLayout.LayoutParams overlayParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        overlayParams.setMargins(
                0,
                10,
                0,
                0
        );

        root.addView(
                overlayButton,
                overlayParams
        );

        // ---------------------------------------------------------
        // DETECTION COUNT
        // ---------------------------------------------------------

        detectionCountText =
                new TextView(this);

        detectionCountText.setTextSize(18);

        detectionCountText.setTextColor(
                Color.rgb(200, 0, 0)
        );

        detectionCountText.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        detectionCountText.setGravity(
                Gravity.CENTER
        );

        detectionCountText.setPadding(
                0,
                28,
                0,
                15
        );

        root.addView(
                detectionCountText
        );

        // ---------------------------------------------------------
        // DETECTED APPS TITLE
        // ---------------------------------------------------------

        TextView resultsTitle =
                new TextView(this);

        resultsTitle.setText(
                "Detected Apps"
        );

        resultsTitle.setTextSize(21);

        resultsTitle.setTextColor(
                Color.rgb(15, 25, 40)
        );

        resultsTitle.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        root.addView(
                resultsTitle
        );

        detectionContainer =
                new LinearLayout(this);

        detectionContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        root.addView(
                detectionContainer,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        // ---------------------------------------------------------
        // PRIVACY / SAFETY
        // ---------------------------------------------------------

        TextView privacy =
                new TextView(this);

        privacy.setText(
                "\nPrivacy & Safety\n\n"
                        + "This app is designed to detect "
                        + "possible pop-up advertisements "
                        + "from third-party apps.\n\n"
                        + "Accessibility access is used only "
                        + "for popup detection. You control "
                        + "whether monitoring is enabled.\n\n"
                        + "The app does not secretly install, "
                        + "uninstall, or control other apps."
        );

        privacy.setTextSize(14);

        privacy.setTextColor(
                Color.GRAY
        );

        privacy.setPadding(
                5,
                30,
                5,
                10
        );

        root.addView(
                privacy
        );

        setContentView(scrollView);
    }

    // -------------------------------------------------------------
    // ACCESSIBILITY DISCLOSURE
    // -------------------------------------------------------------

    private void showAccessibilityDisclosure() {

        CheckBox consent =
                new CheckBox(this);

        consent.setText(
                "I understand and agree to enable "
                        + "Accessibility access for popup detection."
        );

        consent.setTextSize(15);

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                30,
                10,
                30,
                10
        );

        layout.addView(
                consent
        );

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                "Accessibility Access"
                        )
                        .setMessage(
                                "Accessibility access allows "
                                        + "this app to monitor visible "
                                        + "app windows and on-screen "
                                        + "text to identify possible "
                                        + "advertisements or pop-ups "
                                        + "from third-party apps.\n\n"
                                        + "It is required for background "
                                        + "monitoring. You can disable "
                                        + "the service at any time from "
                                        + "Android Accessibility settings.\n\n"
                                        + "The app does not automatically "
                                        + "install, uninstall, or control "
                                        + "other apps."
                        )
                        .setView(layout)
                        .setNegativeButton(
                                "Cancel",
                                null
                        )
                        .setPositiveButton(
                                "Continue",
                                null
                        )
                        .create();

        dialog.setOnShowListener(
                d -> {

                    Button continueButton =
                            dialog.getButton(
                                    AlertDialog.BUTTON_POSITIVE
                            );

                    continueButton.setEnabled(
                            false
                    );

                    consent.setOnCheckedChangeListener(
                            (buttonView, isChecked) ->
                                    continueButton.setEnabled(
                                            isChecked
                                    )
                    );

                    continueButton.setOnClickListener(
                            v -> {

                                getSharedPreferences(
                                        PREFS,
                                        MODE_PRIVATE
                                )
                                        .edit()
                                        .putBoolean(
                                                "accessibility_disclosure_accepted",
                                                true
                                        )
                                        .apply();

                                dialog.dismiss();

                                Intent intent =
                                        new Intent(
                                                Settings.ACTION_ACCESSIBILITY_SETTINGS
                                        );

                                startActivity(intent);
                            }
                    );
                }
        );

        dialog.show();
    }

    // -------------------------------------------------------------
    // OVERLAY SETTINGS
    // -------------------------------------------------------------

    private void openOverlaySettings() {

        try {

            Intent intent =
                    new Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse(
                                    "package:"
                                            + getPackageName()
                            )
                    );

            startActivity(intent);

        } catch (Exception e) {

            Intent intent =
                    new Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION
                    );

            startActivity(intent);
        }
    }

    // -------------------------------------------------------------
    // ACCESSIBILITY STATUS
    // -------------------------------------------------------------

    private boolean isAccessibilityServiceEnabled() {

        String enabledServices =
                Settings.Secure.getString(
                        getContentResolver(),
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                );

        if (enabledServices == null) {
            return false;
        }

        String serviceName =
                getPackageName()
                        + "/"
                        + PopupAccessibilityService.class.getName();

        return enabledServices.contains(
                serviceName
        );
    }

    // -------------------------------------------------------------
    // OVERLAY STATUS
    // -------------------------------------------------------------

    private boolean hasOverlayPermission() {

        AppOpsManager appOps =
                (AppOpsManager)
                        getSystemService(
                                Context.APP_OPS_SERVICE
                        );

        if (appOps == null) {
            return false;
        }

        return Settings.canDrawOverlays(
                this
        );
    }

    // -------------------------------------------------------------
    // STATUS
    // -------------------------------------------------------------

    private void refreshStatus() {

        if (statusText == null) {
            return;
        }

        boolean accessibilityEnabled =
                isAccessibilityServiceEnabled();

        boolean overlayEnabled =
                hasOverlayPermission();

        if (accessibilityEnabled
                && overlayEnabled) {

            statusText.setText(
                    "● Detector is ready\n"
                            + "Accessibility: ON\n"
                            + "Floating button: ON"
            );

            statusText.setTextColor(
                    Color.rgb(0, 130, 70)
            );

        } else if (accessibilityEnabled) {

            statusText.setText(
                    "● Monitoring enabled\n"
                            + "Accessibility: ON\n"
                            + "Floating button: OFF"
            );

            statusText.setTextColor(
                    Color.rgb(0, 130, 70)
            );

        } else {

            statusText.setText(
                    "● Detector is not enabled\n"
                            + "Enable Accessibility access "
                            + "to start monitoring."
            );

            statusText.setTextColor(
                    Color.rgb(190, 80, 0)
            );
        }

        int count =
                getSharedPreferences(
                        PREFS,
                        MODE_PRIVATE
                )
                        .getInt(
                                KEY_DETECTION_COUNT,
                                0
                        );

        detectionCountText.setText(
                "Detections: " + count
        );
    }

    // -------------------------------------------------------------
    // SCAN THIRD-PARTY APPS
    // -------------------------------------------------------------

    private void scanApps() {

        if (detectionContainer == null) {
            return;
        }

        detectionContainer.removeAllViews();

        PackageManager pm =
                getPackageManager();

        List<ApplicationInfo> apps =
                pm.getInstalledApplications(
                        PackageManager.GET_META_DATA
                );

        int count = 0;

        Set<String> seen =
                new HashSet<>();

        for (ApplicationInfo app : apps) {

            String packageName =
                    app.packageName;

            if (packageName.equals(
                    getPackageName()
            )) {
                continue;
            }

            if (isSystemApp(app)) {
                continue;
            }

            if (seen.contains(
                    packageName
            )) {
                continue;
            }

            if (!canDrawOverlaysForPackage(
                    packageName
            )) {
                continue;
            }

            seen.add(
                    packageName
            );

            addAppRow(app);

            count++;
        }

        if (count == 0) {

            TextView empty =
                    new TextView(this);

            empty.setText(
                    "No third-party apps with "
                            + "overlay capability were found."
            );

            empty.setTextSize(15);

            empty.setTextColor(
                    Color.GRAY
            );

            empty.setPadding(
                    0,
                    15,
                    0,
                    15
            );

            detectionContainer.addView(
                    empty
            );
        }
    }

    private boolean isSystemApp(
            ApplicationInfo app
    ) {

        return (app.flags
                & ApplicationInfo.FLAG_SYSTEM) != 0
                || (app.flags
                & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
    }

    private boolean canDrawOverlaysForPackage(
            String packageName
    ) {

        try {

            getPackageManager()
                    .getApplicationInfo(
                            packageName,
                            0
                    );

            String[] permissions =
                    getPackageManager()
                            .getPackageInfo(
                                    packageName,
                                    PackageManager.GET_PERMISSIONS
                            )
                            .requestedPermissions;

            if (permissions == null) {
                return false;
            }

            for (String permission :
                    permissions) {

                if ("android.permission.SYSTEM_ALERT_WINDOW"
                        .equals(permission)) {

                    return true;
                }
            }

        } catch (Exception ignored) {
        }

        return false;
    }

    // -------------------------------------------------------------
    // APP ROW
    // -------------------------------------------------------------

    private void addAppRow(
            ApplicationInfo app
    ) {

        LinearLayout row =
                new LinearLayout(this);

        row.setOrientation(
                LinearLayout.HORIZONTAL
        );

        row.setGravity(
                Gravity.CENTER_VERTICAL
        );

        row.setPadding(
                8,
                15,
                8,
                15
        );

        Drawable icon =
                app.loadIcon(
                        getPackageManager()
                );

        ImageView image =
                new ImageView(this);

        image.setImageDrawable(
                icon
        );

        LinearLayout.LayoutParams imageParams =
                new LinearLayout.LayoutParams(
                        60,
                        60
                );

        row.addView(
                image,
                imageParams
        );

        LinearLayout textLayout =
                new LinearLayout(this);

        textLayout.setOrientation(
                LinearLayout.VERTICAL
        );

        textLayout.setPadding(
                18,
                0,
                10,
                0
        );

        TextView name =
                new TextView(this);

        name.setText(
                app.loadLabel(
                        getPackageManager()
                )
        );

        name.setTextSize(17);

        name.setTextColor(
                Color.BLACK
        );

        name.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        textLayout.addView(
                name
        );

        TextView packageText =
                new TextView(this);

        packageText.setText(
                app.packageName
        );

        packageText.setTextSize(12);

        packageText.setTextColor(
                Color.GRAY
        );

        textLayout.addView(
                packageText
        );

        TextView info =
                new TextView(this);

        info.setText(
                "OVERLAY CAPABILITY"
        );

        info.setTextSize(12);

        info.setTextColor(
                Color.rgb(190, 80, 0)
        );

        textLayout.addView(
                info
        );

        row.addView(
                textLayout,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        Button uninstall =
                new Button(this);

        uninstall.setText(
                "Uninstall"
        );

        uninstall.setOnClickListener(
                v -> requestUninstall(
                        app.packageName
                )
        );

        row.addView(
                uninstall
        );

        detectionContainer.addView(
                row
        );
    }

    // -------------------------------------------------------------
    // NORMAL ANDROID UNINSTALL
    // -------------------------------------------------------------

    private void requestUninstall(
            String packageName
    ) {

        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_DELETE
                    );

            intent.setData(
                    Uri.parse(
                            "package:"
                                    + packageName
                    )
            );

            intent.putExtra(
                    Intent.EXTRA_RETURN_RESULT,
                    true
            );

            startActivity(
                    intent
            );

        } catch (Exception e) {
            // Android could not open uninstall confirmation.
        }
    }
}
