package com.ikramkheshgi.popupdetector;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends Activity {
    private LinearLayout records; private TextView status, count;
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    private TextView txt(String s,float z,int c){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);return t;}
    @Override protected void onCreate(Bundle b){super.onCreate(b); build();}
    @Override protected void onResume(){super.onResume();refresh();}
    private void build(){
        ScrollView sv=new ScrollView(this); LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(12),dp(18),dp(24));root.setBackgroundColor(Color.rgb(7,17,31));sv.addView(root);
        ImageView logo=new ImageView(this);logo.setImageResource(com.ikramkheshgi.popupdetector.R.drawable.ikramkheshgi_intro);logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);root.addView(logo,new LinearLayout.LayoutParams(-1,dp(145)));
        TextView title=txt("ikramKheshgi Popup Detector",25,Color.WHITE);title.setGravity(Gravity.CENTER);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);root.addView(title);
        TextView sub=txt("POPUP AD DETECTOR",13,Color.rgb(25,184,255));sub.setGravity(Gravity.CENTER);sub.setTypeface(Typeface.DEFAULT,Typeface.BOLD);root.addView(sub);
        TextView desc=txt("Identify the app behind an external popup, review the evidence, then open App Info or Android's uninstall screen.",14,Color.rgb(215,230,242));desc.setGravity(Gravity.CENTER);desc.setPadding(dp(10),dp(12),dp(10),dp(10));root.addView(desc);
        status=txt("DETECTOR OFF",14,Color.rgb(25,184,255));status.setGravity(Gravity.CENTER);status.setTypeface(Typeface.DEFAULT,Typeface.BOLD);root.addView(status,new LinearLayout.LayoutParams(-1,dp(54)));
        Button start=button("START DETECTOR");start.setOnClickListener(v->showDisclosure());root.addView(start);
        Button overlay=button("ALLOW FLOATING DETECTOR");overlay.setOnClickListener(v->openOverlay());root.addView(overlay);
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
        Button scan=button("SCAN");scan.setOnClickListener(v->startActivity(new Intent(this,ScanActivity.class)));row.addView(scan,new LinearLayout.LayoutParams(0,-2,1));
        Button wl=button("WHITELIST");wl.setOnClickListener(v->startActivity(new Intent(this,WhitelistActivity.class)));row.addView(wl,new LinearLayout.LayoutParams(0,-2,1));root.addView(row);
        count=txt("0 records",18,Color.WHITE);count.setGravity(Gravity.CENTER);count.setTypeface(Typeface.DEFAULT,Typeface.BOLD);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,dp(22),0,dp(8));root.addView(count,cp);
        Button latest=button("OPEN LATEST RECORD");latest.setOnClickListener(v->{JSONObject o=DetectionStore.latest(this);if(o!=null)open(o);});root.addView(latest);
        TextView h=txt("RECENT RECORDS",20,Color.WHITE);h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);h.setPadding(0,dp(22),0,dp(6));root.addView(h);
        records=new LinearLayout(this);records.setOrientation(LinearLayout.VERTICAL);root.addView(records);
        Button clear=button("CLEAR RECORDS");clear.setOnClickListener(v->{DetectionStore.clear(this);refresh();});root.addView(clear);
        TextView foot=txt("Normal page changes are ignored. Detection is heuristic; review the identified app before uninstalling it.",12,Color.rgb(150,180,205));foot.setGravity(Gravity.CENTER);foot.setPadding(0,dp(16),0,0);root.addView(foot);
        setContentView(sv);
    }
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextSize(13);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(4),0,dp(4));b.setLayoutParams(p);return b;}
    private void refresh(){if(status==null)return;boolean a=isAccessibilityEnabled(),o=Settings.canDrawOverlays(this);status.setText(a&&o?"● DETECTOR ACTIVE":a?"● ACCESSIBILITY ACTIVE — ALLOW FLOATING DETECTOR":"○ DETECTOR OFF — ENABLE ACCESSIBILITY");JSONArray x=DetectionStore.read(this);count.setText(x.length()+" record"+(x.length()==1?"":"s"));render(x);}
    private void render(JSONArray a){records.removeAllViews();if(a.length()==0){TextView e=txt("No popup source records yet.\nEnable the detector and use your phone normally.",14,Color.rgb(165,190,210));e.setPadding(dp(12),dp(16),dp(12),dp(16));records.addView(e);return;}for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null)records.addView(card(o));}}
    private View card(JSONObject o){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.HORIZONTAL);c.setGravity(Gravity.CENTER_VERTICAL);c.setPadding(dp(10),dp(10),dp(8),dp(10));GradientDrawable g=new GradientDrawable();g.setColor(Color.rgb(16,30,48));g.setCornerRadius(dp(16));c.setBackground(g);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,0,0,dp(8));c.setLayoutParams(cp);
        ImageView i=new ImageView(this);try{i.setImageDrawable(getPackageManager().getApplicationIcon(o.optString("package")));}catch(Exception e){i.setImageResource(android.R.drawable.sym_def_app_icon);}c.addView(i,new LinearLayout.LayoutParams(dp(48),dp(48)));
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(10),0,dp(5),0);TextView n=txt(o.optString("label","Unknown app"),15,Color.WHITE);n.setTypeface(Typeface.DEFAULT,Typeface.BOLD);box.addView(n);box.addView(txt(o.optString("reason","Popup-style activity"),11,Color.rgb(165,190,210)));box.addView(txt(DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(o.optLong("time",0))),10,Color.rgb(125,160,190)));c.addView(box,new LinearLayout.LayoutParams(0,-2,1));TextView ar=txt("›",30,Color.rgb(25,184,255));c.addView(ar,new LinearLayout.LayoutParams(dp(28),dp(48)));c.setOnClickListener(v->open(o));return c;}
    private void open(JSONObject o){Intent i=new Intent(this,DetectionDetailsActivity.class);i.putExtra("package",o.optString("package"));i.putExtra("label",o.optString("label"));i.putExtra("reason",o.optString("reason"));i.putExtra("time",o.optLong("time"));startActivity(i);}
    private void showDisclosure(){new AlertDialog.Builder(this).setTitle("Accessibility access required").setMessage("ikramKheshgi Popup Detector uses Android AccessibilityService only to observe app-window changes and visible popup-style text so it can identify which installed app may be responsible for an external popup advertisement.\n\nThe detector does not automatically uninstall apps. Review each detection before taking action.\n\nBy continuing, you confirm that you understand why Accessibility access is required.").setNegativeButton("CANCEL",null).setPositiveButton("I UNDERSTAND — CONTINUE",(d,w)->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))).show();}
    private void openOverlay(){try{startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName())));}catch(Exception e){startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));}}
    private boolean isAccessibilityEnabled(){String s=Settings.Secure.getString(getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);if(s==null)return false;String wanted=new ComponentName(this,PopupAccessibilityService.class).flattenToString();return s.contains(wanted);}
}
