package com.ikramkheshgi.popupdetector;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends Activity {
    private LinearLayout historyContainer;
    private TextView statusText;
    private TextView countText;

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        statusText = findViewById(R.id.serviceStatus);
        countText = findViewById(R.id.detectionCount);
        historyContainer = findViewById(R.id.historyContainer);

        findViewById(R.id.enableDetector).setOnClickListener(v -> showAccessibilityDisclosure());
        findViewById(R.id.allowOverlay).setOnClickListener(v -> openOverlaySettings());
        findViewById(R.id.scanApps).setOnClickListener(v -> showOverlayApps());
        findViewById(R.id.clearHistory).setOnClickListener(v -> {
            DetectionStore.clear(this);
            renderHistory();
        });
        findViewById(R.id.openLatest).setOnClickListener(v -> {
            JSONObject latest = DetectionStore.latest(this);
            if (latest != null) openDetails(latest);
        });
    }

    @Override protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        boolean accessibility = isAccessibilityServiceEnabled();
        boolean overlay = Settings.canDrawOverlays(this);
        if (accessibility && overlay) statusText.setText("● DETECTOR ACTIVE — ready to identify popup sources");
        else if (accessibility) statusText.setText("● ACCESSIBILITY ACTIVE — allow floating detector for best results");
        else statusText.setText("○ DETECTOR OFF — enable Accessibility to start");
        JSONArray a = DetectionStore.read(this);
        countText.setText(a.length() + " popup event" + (a.length() == 1 ? "" : "s") + " identified");
        renderHistory();
    }

    private void renderHistory() {
        historyContainer.removeAllViews();
        JSONArray a = DetectionStore.read(this);
        if (a.length() == 0) {
            TextView empty = label("No popup source identified yet.\nUse your phone normally after enabling the detector.", 15, Color.rgb(170,190,210));
            empty.setPadding(dp(14), dp(18), dp(14), dp(18));
            historyContainer.addView(empty);
            return;
        }
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o == null) continue;
            historyContainer.addView(historyCard(o));
        }
    }

    private View historyCard(JSONObject o) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(12), dp(12), dp(10), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(16,30,48));
        bg.setCornerRadius(dp(16));
        card.setBackground(bg);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.setMargins(0,0,0,dp(10));
        card.setLayoutParams(cp);

        ImageView icon = new ImageView(this);
        icon.setImageDrawable(loadIcon(o.optString("package")));
        card.addView(icon, new LinearLayout.LayoutParams(dp(52),dp(52)));

        LinearLayout textBox = new LinearLayout(this);
        textBox.setOrientation(LinearLayout.VERTICAL);
        textBox.setPadding(dp(12),0,dp(6),0);
        TextView name = label(o.optString("label", "Unknown app"),16,Color.WHITE);
        name.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        TextView reason = label(o.optString("reason", "Popup-like window"),12,Color.rgb(170,190,210));
        TextView time = label(DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(o.optLong("time",0))),11,Color.rgb(120,160,190));
        textBox.addView(name); textBox.addView(reason); textBox.addView(time);
        card.addView(textBox,new LinearLayout.LayoutParams(0,-2,1));
        TextView arrow=label("›",30,Color.rgb(25,184,255));
        card.addView(arrow,new LinearLayout.LayoutParams(dp(30),dp(50)));
        card.setOnClickListener(v -> openDetails(o));
        return card;
    }

    private DrawableHolder loadIcon(String pkg) {
        try { return new DrawableHolder(getPackageManager().getApplicationIcon(pkg)); }
        catch(Exception e) { return new DrawableHolder(getDrawable(android.R.drawable.sym_def_app_icon)); }
    }

    // Small adapter because ImageView.setImageDrawable expects Drawable directly.
    private static class DrawableHolder extends android.graphics.drawable.Drawable {
        private final android.graphics.drawable.Drawable d;
        DrawableHolder(android.graphics.drawable.Drawable d){this.d=d;}
        public void draw(android.graphics.Canvas c){d.setBounds(getBounds());d.draw(c);}
        public void setAlpha(int a){d.setAlpha(a);} public void setColorFilter(android.graphics.ColorFilter f){d.setColorFilter(f);}
        public int getOpacity(){return d.getOpacity();}
        public int getIntrinsicWidth(){return d.getIntrinsicWidth();} public int getIntrinsicHeight(){return d.getIntrinsicHeight();}
    }

    private TextView label(String s, float size, int color) {
        TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); return t;
    }

    private void openDetails(JSONObject o) {
        Intent i = new Intent(this, DetectionDetailsActivity.class);
        i.putExtra("package", o.optString("package"));
        i.putExtra("label", o.optString("label"));
        i.putExtra("reason", o.optString("reason"));
        i.putExtra("time", o.optLong("time"));
        startActivity(i);
    }

    private void showAccessibilityDisclosure() {
        new AlertDialog.Builder(this)
                .setTitle("Enable Popup Detector")
                .setMessage("Accessibility access lets the detector observe app-window changes and visible text so it can identify a possible external popup source. It does not automatically uninstall or control other apps.\n\nFor the closest behavior to the reference app, also allow the floating detector overlay.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Open Accessibility", (d,w) -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)))
                .show();
    }

    private void openOverlaySettings() {
        try { startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName()))); }
        catch(Exception e){ startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)); }
    }

    private boolean isAccessibilityServiceEnabled() {
        String enabled = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) return false;
        String wanted = new ComponentName(this, PopupAccessibilityService.class).flattenToString();
        return enabled.contains(wanted);
    }

    private void showOverlayApps() {
        PackageManager pm=getPackageManager();
        StringBuilder out=new StringBuilder(); int n=0;
        for(ApplicationInfo a:pm.getInstalledApplications(PackageManager.GET_META_DATA)){
            if((a.flags & ApplicationInfo.FLAG_SYSTEM)!=0) continue;
            try{
                PackageInfo p=pm.getPackageInfo(a.packageName,PackageManager.GET_PERMISSIONS);
                boolean overlay=false;
                if(p.requestedPermissions!=null) for(String perm:p.requestedPermissions) if("android.permission.SYSTEM_ALERT_WINDOW".equals(perm)){overlay=true;break;}
                if(overlay){
                    if(n++>0) out.append("\n\n");
                    out.append(a.loadLabel(pm)).append("\n").append(a.packageName);
                }
            }catch(Exception ignored){}
        }
        if(n==0) out.append("No non-system apps requesting overlay permission were found.");
        new AlertDialog.Builder(this).setTitle("Apps with overlay capability").setMessage(out.toString()).setPositiveButton("Close",null).show();
    }
}
