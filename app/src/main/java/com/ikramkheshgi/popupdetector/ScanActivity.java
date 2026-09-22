package com.ikramkheshgi.popupdetector;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class ScanActivity extends Activity {
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    @Override protected void onCreate(Bundle b){super.onCreate(b);ScrollView s=new ScrollView(this);LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(dp(18),dp(22),dp(18),dp(22));r.setBackgroundColor(Color.rgb(7,17,31));TextView h=new TextView(this);h.setText("SCANNING");h.setTextSize(25);h.setTextColor(Color.WHITE);h.setGravity(Gravity.CENTER);r.addView(h);TextView info=new TextView(this);info.setText("Apps requesting overlay capability can potentially place content above other apps. This scan does not label an app malicious.");info.setTextSize(14);info.setTextColor(Color.rgb(175,195,215));info.setPadding(0,dp(12),0,dp(18));r.addView(info);PackageManager pm=getPackageManager();for(ApplicationInfo a:pm.getInstalledApplications(PackageManager.GET_META_DATA)){if((a.flags&(ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))!=0)continue;try{PackageInfo p=pm.getPackageInfo(a.packageName,PackageManager.GET_PERMISSIONS);boolean overlay=false;if(p.requestedPermissions!=null)for(String x:p.requestedPermissions)if("android.permission.SYSTEM_ALERT_WINDOW".equals(x)){overlay=true;break;}if(overlay){TextView t=new TextView(this);t.setText(a.loadLabel(pm)+"\n"+a.packageName);t.setTextSize(15);t.setTextColor(Color.WHITE);t.setPadding(dp(12),dp(12),dp(12),dp(12));r.addView(t);}}catch(Exception ignored){}}s.addView(r);setContentView(s);}
}
