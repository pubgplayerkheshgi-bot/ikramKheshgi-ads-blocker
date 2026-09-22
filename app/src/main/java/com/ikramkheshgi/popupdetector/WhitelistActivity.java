package com.ikramkheshgi.popupdetector;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Set;

public class WhitelistActivity extends Activity {
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    @Override protected void onCreate(Bundle b){super.onCreate(b);ScrollView s=new ScrollView(this);LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(dp(18),dp(22),dp(18),dp(22));r.setBackgroundColor(Color.rgb(7,17,31));TextView h=new TextView(this);h.setText("WHITELIST");h.setTextSize(25);h.setTextColor(Color.WHITE);h.setGravity(Gravity.CENTER);r.addView(h);TextView d=new TextView(this);d.setText("Whitelisted apps are ignored by the detector. Remove an app from the list to monitor it again.");d.setTextSize(14);d.setTextColor(Color.rgb(175,195,215));d.setPadding(0,dp(12),0,dp(18));r.addView(d);PackageManager pm=getPackageManager();Set<String> set=DetectionStore.whitelist(this);if(set.isEmpty()){TextView n=new TextView(this);n.setText("No apps are whitelisted.");n.setTextColor(Color.WHITE);n.setTextSize(16);r.addView(n);}else for(String pkg:set){Button x=new Button(this);try{x.setText(pm.getApplicationLabel(pm.getApplicationInfo(pkg,0))+"\n"+pkg);}catch(Exception e){x.setText(pkg);}x.setOnClickListener(v->{DetectionStore.setWhitelisted(this,pkg,false);onCreate(null);});r.addView(x);}s.addView(r);setContentView(s);}
}
